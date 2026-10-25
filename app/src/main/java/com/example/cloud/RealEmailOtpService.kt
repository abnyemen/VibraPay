package com.example.cloud

import android.content.Context
import android.util.Log
import android.util.Patterns
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Defines the two distinct email template types supported by VibraPay:
 * 1) [EMAIL_VERIFICATION]: Uses the Email Confirmation Template ID (`BuildConfig.EMAILJS_TEMPLATE_ID`)
 * 2) [PASSWORD_RESET]: Uses the Forgot Password / Reset Template ID (`BuildConfig.EMAILJS_RESET_TEMPLATE_ID` / `template_xxxx`)
 */
enum class EmailTemplatePurpose(
    val titleAr: String,
    val actionLabelAr: String
) {
    EMAIL_VERIFICATION(
        titleAr = "تأكيد ملكية البريد الإلكتروني للحساب الرسمي",
        actionLabelAr = "تأكيد البريد الإلكتروني"
    ),
    PASSWORD_RESET(
        titleAr = "إعادة تعيين كلمة المرور عند النسيان",
        actionLabelAr = "استعادة كلمة المرور"
    )
}

data class EmailOtpDispatchResult(
    val otpCode: String,
    val deliveredViaSmtpApi: Boolean,
    val statusMessageAr: String,
    val purpose: EmailTemplatePurpose = EmailTemplatePurpose.EMAIL_VERIFICATION,
    val usedTemplateId: String = ""
)

private data class DispatchedOtpChallenge(
    val emailLower: String,
    val otpCode: String,
    val purpose: EmailTemplatePurpose,
    val createdAtMs: Long = System.currentTimeMillis()
)

/**
 * Real Email OTP Dispatch Service (الإرسال الحصري لرموز التحقق عبر EmailJS إلى Gmail):
 *
 * Security Protocol:
 * 1. Zero Local Exposure: Never displays the OTP code on screen, in status messages, placeholders,
 *    or local system notifications under any circumstance.
 * 2. Exclusive EmailJS Delivery: Programmatically generates a cryptographic 6-digit random OTP and
 *    dispatches it exclusively via EmailJS (`EMAILJS_SERVICE_ID`, `EMAILJS_PUBLIC_KEY`, and the
 *    dedicated `EMAILJS_TEMPLATE_ID` / `EMAILJS_RESET_TEMPLATE_ID`) to the user's real email inbox.
 * 3. Strict Verification Gate: Only accepts an OTP that was genuinely dispatched via EmailJS to
 *    that exact email address and entered manually by the user after checking their inbox.
 */
object RealEmailOtpService {
    private const val TAG = "RealEmailOtpService"
    private const val EMAILJS_ENDPOINT = "https://api.emailjs.com/api/v1.0/email/send"
    private const val OTP_TTL_MILLIS = 15 * 60 * 1000L // 15 minutes validity window

    private val secureRandom = SecureRandom()
    private val activeChallenges = ConcurrentHashMap<String, DispatchedOtpChallenge>()

    private fun challengeKey(email: String, purpose: EmailTemplatePurpose): String {
        return "${purpose.name}:${email.trim().lowercase()}"
    }

    /**
     * Resolves the Email Verification Template ID from the global variable `EMAILJS_TEMPLATE_ID`
     * (or `EMAILJS_VERIFY_TEMPLATE_ID` if set).
     */
    fun getVerificationTemplateId(): String {
        val primary = BuildConfig.EMAILJS_TEMPLATE_ID.trim()
        if (isConfiguredValue(primary)) {
            return primary
        }
        val secondary = BuildConfig.EMAILJS_VERIFY_TEMPLATE_ID.trim()
        if (isConfiguredValue(secondary)) {
            return secondary
        }
        val resetFallback = BuildConfig.EMAILJS_RESET_TEMPLATE_ID.trim()
        if (isConfiguredValue(resetFallback)) {
            return resetFallback
        }
        return primary
    }

    /**
     * Resolves the Password Reset Template ID (`template_xxxx` for forgot password)
     * from `EMAILJS_RESET_TEMPLATE_ID`.
     */
    fun getPasswordResetTemplateId(): String {
        val resetTpl = BuildConfig.EMAILJS_RESET_TEMPLATE_ID.trim()
        if (isConfiguredValue(resetTpl)) {
            return resetTpl
        }
        return getVerificationTemplateId()
    }

    private fun isConfiguredValue(value: String): Boolean {
        val clean = value.trim()
        return clean.isNotEmpty() &&
            !clean.startsWith("DEFAULT_") &&
            clean != "null"
    }

    private fun isValidRealEmailFormat(email: String): Boolean {
        val clean = email.trim()
        if (clean.length < 6 || !clean.contains("@") || !clean.contains(".")) return false
        val parts = clean.split("@")
        if (parts.size != 2) return false
        val localPart = parts[0].trim()
        val domainPart = parts[1].trim().lowercase()
        if (localPart.isEmpty() || domainPart.length < 4 || !domainPart.contains(".")) return false
        return try {
            Patterns.EMAIL_ADDRESS?.matcher(clean)?.matches() ?: true
        } catch (_: Throwable) {
            true
        }
    }

    /**
     * Verifies whether the user-entered OTP code strictly matches the EmailJS-dispatched code
     * for the given [recipientEmail] and [purpose].
     */
    fun verifyDispatchedOtp(
        recipientEmail: String,
        enteredCode: String,
        purpose: EmailTemplatePurpose,
        consumeOnSuccess: Boolean = false
    ): Boolean {
        val cleanCode = enteredCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) return false
        val key = challengeKey(recipientEmail, purpose)
        val challenge = activeChallenges[key] ?: return false
        val now = System.currentTimeMillis()
        if (now - challenge.createdAtMs > OTP_TTL_MILLIS) {
            activeChallenges.remove(key)
            return false
        }
        val matched = challenge.otpCode == cleanCode
        if (matched && consumeOnSuccess) {
            activeChallenges.remove(key)
        }
        return matched
    }

    /**
     * Unified Email Sender Function accepting BOTH separate Template IDs:
     * - [verificationTemplateId]: Template ID dedicated to Email Confirmation (`EMAILJS_TEMPLATE_ID`)
     * - [passwordResetTemplateId]: Template ID dedicated to Forgot Password (`EMAILJS_RESET_TEMPLATE_ID` / `template_xxxx`)
     * Selects the appropriate Template ID according to [purpose] and sends exclusively via EmailJS.
     * NEVER displays or leaks the OTP code locally.
     */
    suspend fun sendEmailWithDualTemplates(
        @Suppress("UNUSED_PARAMETER") context: Context,
        recipientEmail: String,
        recipientFullName: String,
        purpose: EmailTemplatePurpose,
        verificationTemplateId: String = getVerificationTemplateId(),
        passwordResetTemplateId: String = getPasswordResetTemplateId()
    ): EmailOtpDispatchResult = withContext(Dispatchers.IO) {
        val cleanEmail = recipientEmail.trim()
        val key = challengeKey(cleanEmail, purpose)
        activeChallenges.remove(key)

        if (!isValidRealEmailFormat(cleanEmail)) {
            return@withContext EmailOtpDispatchResult(
                otpCode = "",
                deliveredViaSmtpApi = false,
                statusMessageAr = "يرجى إدخال عنوان بريد إلكتروني حقيقي وصحيح (مثل حساب Gmail الخاص بك)",
                purpose = purpose,
                usedTemplateId = ""
            )
        }

        // Generate a cryptographically random 6-digit OTP code in memory only
        val generatedOtpCode = (100000 + secureRandom.nextInt(900000)).toString()

        val serviceId = BuildConfig.EMAILJS_SERVICE_ID.trim()
        val publicKey = BuildConfig.EMAILJS_PUBLIC_KEY.trim()

        // Select the dedicated Template ID based on the programmatic purpose
        val selectedTemplateId = when (purpose) {
            EmailTemplatePurpose.EMAIL_VERIFICATION -> verificationTemplateId.trim()
            EmailTemplatePurpose.PASSWORD_RESET -> passwordResetTemplateId.trim()
        }

        val hasRealEmailJsCredentials =
            isConfiguredValue(serviceId) &&
                isConfiguredValue(publicKey) &&
                isConfiguredValue(selectedTemplateId)

        if (!hasRealEmailJsCredentials) {
            return@withContext EmailOtpDispatchResult(
                otpCode = "",
                deliveredViaSmtpApi = false,
                statusMessageAr = "تعذر الإرسال: يرجى التأكد من إعداد مفاتيح وقوالب خدمة EmailJS الرسمية في إعدادات المشروع لإرسال الرمز إلى بريدك الإلكتروني (Gmail)",
                purpose = purpose,
                usedTemplateId = selectedTemplateId
            )
        }

        val apiSuccess = dispatchHttpEmailJsWithTemplate(
            serviceId = serviceId,
            templateId = selectedTemplateId,
            publicKey = publicKey,
            recipientEmail = cleanEmail,
            recipientName = recipientFullName.ifBlank { "عميل VibraPay" },
            otpCode = generatedOtpCode,
            purpose = purpose
        )

        if (!apiSuccess) {
            return@withContext EmailOtpDispatchResult(
                otpCode = "",
                deliveredViaSmtpApi = false,
                statusMessageAr = "تعذر إرسال رمز التحقق إلى البريد الإلكتروني ($cleanEmail) عبر EmailJS • يرجى التحقق من اتصال الإنترنت وصحة بريدك الإلكتروني (Gmail)",
                purpose = purpose,
                usedTemplateId = selectedTemplateId
            )
        }

        // Record the active challenge in memory only after confirmed EmailJS delivery
        activeChallenges[key] = DispatchedOtpChallenge(
            emailLower = cleanEmail.lowercase(),
            otpCode = generatedOtpCode,
            purpose = purpose
        )

        // Status message NEVER includes the OTP code
        val safeStatusMessageAr = when (purpose) {
            EmailTemplatePurpose.EMAIL_VERIFICATION ->
                "تم إرسال رمز التحقق السري إلى بريدك الإلكتروني ($cleanEmail) بنجاح • يرجى فتح تطبيق البريد (Gmail) ونسخ الكود وإدخاله في الحقل أدناه"
            EmailTemplatePurpose.PASSWORD_RESET ->
                "تم إرسال رمز إعادة تعيين كلمة المرور إلى بريدك الإلكتروني ($cleanEmail) بنجاح • يرجى فتح بريدك (Gmail) ونسخ الكود وإدخاله لإتمام العملية"
        }

        EmailOtpDispatchResult(
            otpCode = generatedOtpCode,
            deliveredViaSmtpApi = true,
            statusMessageAr = safeStatusMessageAr,
            purpose = purpose,
            usedTemplateId = selectedTemplateId
        )
    }

    /**
     * 1) Dedicated Function for Email Verification (دالة إرسال قالب تأكيد البريد الإلكتروني):
     * Explicitly invokes the Email Verification Template ID (`EMAILJS_TEMPLATE_ID`) entered in the global variable.
     */
    suspend fun sendVerificationOtpToEmail(
        context: Context,
        recipientEmail: String,
        recipientFullName: String,
        verificationTemplateId: String = getVerificationTemplateId(),
        passwordResetTemplateId: String = getPasswordResetTemplateId()
    ): EmailOtpDispatchResult {
        return sendEmailWithDualTemplates(
            context = context,
            recipientEmail = recipientEmail,
            recipientFullName = recipientFullName,
            purpose = EmailTemplatePurpose.EMAIL_VERIFICATION,
            verificationTemplateId = verificationTemplateId,
            passwordResetTemplateId = passwordResetTemplateId
        )
    }

    /**
     * 2) Dedicated Function for Password Reset / Forgot Password (دالة إرسال قالب إعادة تعيين كلمة المرور):
     * Explicitly invokes the Password Reset Template ID (`EMAILJS_RESET_TEMPLATE_ID` / `template_xxxx`).
     */
    suspend fun sendPasswordResetOtpToEmail(
        context: Context,
        recipientEmail: String,
        recipientFullName: String,
        passwordResetTemplateId: String = getPasswordResetTemplateId(),
        verificationTemplateId: String = getVerificationTemplateId()
    ): EmailOtpDispatchResult {
        return sendEmailWithDualTemplates(
            context = context,
            recipientEmail = recipientEmail,
            recipientFullName = recipientFullName,
            purpose = EmailTemplatePurpose.PASSWORD_RESET,
            verificationTemplateId = verificationTemplateId,
            passwordResetTemplateId = passwordResetTemplateId
        )
    }

    private fun dispatchHttpEmailJsWithTemplate(
        serviceId: String,
        templateId: String,
        publicKey: String,
        recipientEmail: String,
        recipientName: String,
        otpCode: String,
        purpose: EmailTemplatePurpose
    ): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(EMAILJS_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Origin", "http://localhost")
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
            }

            val templateParams = JSONObject().apply {
                // Standard recipient fields for EmailJS Gmail templates
                put("to_email", recipientEmail)
                put("email", recipientEmail)
                put("user_email", recipientEmail)
                put("recipient_email", recipientEmail)
                put("reply_to", recipientEmail)
                put("to_name", recipientName)
                put("user_name", recipientName)
                put("name", recipientName)
                // Standard OTP / Reset code fields for both verification & password reset templates
                put("otp_code", otpCode)
                put("otp", otpCode)
                put("verification_code", otpCode)
                put("reset_code", otpCode)
                put("passcode", otpCode)
                put("code", otpCode)
                put("message", "رمز التحقق الخاص بك في محفظة VibraPay هو: $otpCode")
                // Metadata fields for template customization
                put("purpose_ar", purpose.titleAr)
                put("action_label_ar", purpose.actionLabelAr)
                put("template_type", purpose.name)
                put("app_name", "VibraPay Official E-Wallet")
            }

            val bodyJson = JSONObject().apply {
                put("service_id", serviceId)
                put("template_id", templateId)
                put("user_id", publicKey)
                put("template_params", templateParams)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(bodyJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "EmailJS dispatch failed for template=$templateId (${purpose.name}): ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }
}
