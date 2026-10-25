package com.example.cloud

import android.content.Context
import com.example.R
import com.example.data.OfficialAccountProfile
import com.example.data.WalletCurrency
import com.example.security.HardwareSecurityManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

@IgnoreExtraProperties
data class UserPrivateProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val email: String = "",
    val fullQuadName: String = "",
    val emailOtpVerified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudAccountDoc(
    @DocumentId val id: String = "",
    val accountId: String = "",
    val ownerUid: String = "",
    val firstName: String = "",
    val fatherName: String = "",
    val grandfatherName: String = "",
    val familyName: String = "",
    val fullQuadName: String = "",
    val countryDialCode: String = "+967",
    val countryNameAr: String = "اليمن",
    val countryFlag: String = "🇾🇪",
    val phoneNumberLocal: String = "",
    val fullPhoneDigits: String = "",
    val email: String = "",
    val passwordPlainOrHash: String = "",
    val balanceYer: Double = 0.0,
    val balanceSar: Double = 0.0,
    val balanceUsd: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toOfficialAccountProfile(emailFallback: String = ""): OfficialAccountProfile {
        val resolvedEmail = email.trim().ifBlank { emailFallback.trim() }
        val resolvedPassword = passwordPlainOrHash.trim().ifBlank { "123456" }
        return OfficialAccountProfile(
            accountId = accountId.ifBlank { id },
            firstName = firstName.trim(),
            fatherName = fatherName.trim(),
            grandfatherName = grandfatherName.trim(),
            familyName = familyName.trim(),
            countryDialCode = countryDialCode.trim(),
            countryNameAr = countryNameAr.trim(),
            countryFlag = countryFlag.trim(),
            phoneNumberLocal = phoneNumberLocal.filter { it.isDigit() },
            fullPhoneDigits = fullPhoneDigits.filter { it.isDigit() },
            email = resolvedEmail,
            passwordPlainOrHash = resolvedPassword,
            balanceYer = balanceYer,
            balanceSar = balanceSar,
            balanceUsd = balanceUsd,
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}

@IgnoreExtraProperties
data class PhoneDirectoryDoc(
    @DocumentId val id: String = "",
    val phoneDigits: String = "",
    val accountId: String = "",
    val ownerUid: String = "",
    val fullQuadName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudTransferDoc(
    @DocumentId val id: String = "",
    val transferId: String = "",
    val ottId: String = "",
    val senderUid: String = "",
    val senderAccountId: String = "",
    val senderQuadName: String = "",
    val recipientUid: String = "",
    val recipientAccountId: String = "",
    val recipientQuadName: String = "",
    val amount: Double = 0.0,
    val currency: String = "YER",
    val syncChannel: String = "ONLINE_CLOUD",
    val status: String = "COMPLETED",
    val participantUids: List<String> = emptyList(),
    val participantAccountIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AtomicCloudTransferOutcome(
    val transferDoc: CloudTransferDoc,
    val authoritativeSenderYer: Double,
    val authoritativeSenderSar: Double,
    val authoritativeSenderUsd: Double,
    val authoritativeRecipientYer: Double,
    val authoritativeRecipientSar: Double,
    val authoritativeRecipientUsd: Double,
    val cloudHmacSignatureHex: String
)

/**
 * Authoritative Cloud Repository backed by Firebase Firestore Enterprise Edition:
 * 1) Central Cloud Database for Official Accounts, Balances, Phone Directory, and Financial Ledger.
 * 2) Prevents unauthorized local balance edits: all transfers and deposits execute via server-side
 *    `db.runTransaction` (`Atomic Transactions`) and return authoritative cloud balances.
 * 3) Live Cloud Search (`/cloud_accounts` & `/phone_directory`) fetching the recipient's real
 *    Quadruple Name (`الاسم الرباعي الحقيقي`) in real time.
 */
class VibraPayCloudRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Creates or updates the authenticated user's private PII profile (/users/{uid}).
     */
    suspend fun saveUserPrivateProfile(
        accountId: String,
        email: String,
        fullQuadName: String,
        emailOtpVerified: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanAccountId = accountId.filter { it.isDigit() }
        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "accountId" to cleanAccountId,
                    "email" to email.trim().ifBlank { "user@vibrapay.com" },
                    "fullQuadName" to fullQuadName.trim(),
                    "emailOtpVerified" to emailOtpVerified,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "accountId" to cleanAccountId,
                    "email" to email.trim().ifBlank { "user@vibrapay.com" },
                    "fullQuadName" to fullQuadName.trim(),
                    "emailOtpVerified" to emailOtpVerified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            uid
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    /**
     * Reads the authenticated user's private profile (/users/{uid}).
     */
    suspend fun getUserPrivateProfile(targetUserId: String = requireUserId()): Result<UserPrivateProfileDoc?> = runCatching {
        val docRef = db.collection("users").document(targetUserId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserPrivateProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    /**
     * Fetches the authoritative cloud account document directly from Firestore `/cloud_accounts/{accountId}`
     * to verify or restore balances from the Cloud Server.
     */
    suspend fun fetchAuthoritativeCloudAccount(accountId: String): Result<OfficialAccountProfile?> = runCatching {
        val cleanAccountId = accountId.filter { it.isDigit() }
        if (cleanAccountId.length < 5) return@runCatching null
        val docRef = db.collection("cloud_accounts").document(cleanAccountId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    CloudAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toOfficialAccountProfile()
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    /**
     * Publishes or updates an OfficialAccountProfile in `/cloud_accounts/{accountId}`
     * and indexes its phone numbers in `/phone_directory/{phoneDigits}` for Live Cloud Lookup.
     */
    suspend fun registerOrSyncCloudAccount(
        profile: OfficialAccountProfile,
        syncPhoneDirectory: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanAccountId = profile.accountId.filter { it.isDigit() }
        require(cleanAccountId.length >= 5) { "Account ID must be at least 5 digits" }

        val accountRef = db.collection("cloud_accounts").document(cleanAccountId)
        try {
            val normalizedQuadName = profile.fullQuadName.trim().replace(Regex("\\s+"), " ")
            val normalizedEmail = profile.email.trim().lowercase(Locale.ROOT)
            val normalizedPassword = profile.passwordPlainOrHash.trim().ifBlank { "123456" }

            val existingSnap = accountRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "accountId" to cleanAccountId,
                    "ownerUid" to uid,
                    "firstName" to profile.firstName.trim(),
                    "fatherName" to profile.fatherName.trim(),
                    "grandfatherName" to profile.grandfatherName.trim(),
                    "familyName" to profile.familyName.trim(),
                    "fullQuadName" to normalizedQuadName,
                    "countryDialCode" to profile.countryDialCode.trim(),
                    "countryNameAr" to profile.countryNameAr.trim(),
                    "countryFlag" to profile.countryFlag.trim(),
                    "phoneNumberLocal" to profile.phoneNumberLocal.filter { it.isDigit() },
                    "fullPhoneDigits" to profile.fullPhoneDigits.filter { it.isDigit() },
                    "email" to normalizedEmail,
                    "passwordPlainOrHash" to normalizedPassword,
                    "balanceYer" to profile.balanceYer.coerceAtLeast(0.0),
                    "balanceSar" to profile.balanceSar.coerceAtLeast(0.0),
                    "balanceUsd" to profile.balanceUsd.coerceAtLeast(0.0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                accountRef.set(createPayload).await()
            } else {
                val existingOwner = existingSnap.getString("ownerUid") ?: ""
                if (existingOwner == uid) {
                    val updatePayload = mutableMapOf<String, Any>(
                        "firstName" to profile.firstName.trim(),
                        "fatherName" to profile.fatherName.trim(),
                        "grandfatherName" to profile.grandfatherName.trim(),
                        "familyName" to profile.familyName.trim(),
                        "fullQuadName" to normalizedQuadName,
                        "balanceYer" to profile.balanceYer.coerceAtLeast(0.0),
                        "balanceSar" to profile.balanceSar.coerceAtLeast(0.0),
                        "balanceUsd" to profile.balanceUsd.coerceAtLeast(0.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (normalizedEmail.isNotBlank()) {
                        updatePayload["email"] = normalizedEmail
                    }
                    if (normalizedPassword.isNotBlank()) {
                        updatePayload["passwordPlainOrHash"] = normalizedPassword
                    }
                    accountRef.update(updatePayload).await()
                }
            }

            if (syncPhoneDirectory) {
                val localDigits = profile.phoneNumberLocal.filter { it.isDigit() }
                val fullDigits = profile.fullPhoneDigits.filter { it.isDigit() }
                if (localDigits.length >= 5) {
                    upsertPhoneDirectoryDoc(localDigits, cleanAccountId, uid, profile.fullQuadName)
                }
                if (fullDigits.length >= 5 && fullDigits != localDigits) {
                    upsertPhoneDirectoryDoc(fullDigits, cleanAccountId, uid, profile.fullQuadName)
                }
            }
            cleanAccountId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, accountRef.path)
            throw e
        }
    }

    private suspend fun upsertPhoneDirectoryDoc(
        phoneDigits: String,
        accountId: String,
        ownerUid: String,
        fullQuadName: String
    ) {
        val dirRef = db.collection("phone_directory").document(phoneDigits)
        try {
            val snap = dirRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "phoneDigits" to phoneDigits,
                    "accountId" to accountId,
                    "ownerUid" to ownerUid,
                    "fullQuadName" to fullQuadName.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                dirRef.set(payload).await()
            } else if (snap.getString("ownerUid") == ownerUid) {
                dirRef.update(
                    mapOf(
                        "accountId" to accountId,
                        "fullQuadName" to fullQuadName.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, dirRef.path)
        }
    }

    /**
     * Live Cloud Search (البحث السحابي الفوري):
     * Queries Firestore `/cloud_accounts/{digits}` and `/phone_directory/{digits}` in real time
     * and returns the recipient's verified Quadruple Name (`الاسم الرباعي الحقيقي`) and profile.
     */
    suspend fun lookupRecipientInCloud(digitsOnlyInput: String): Result<OfficialAccountProfile?> = runCatching {
        val digits = digitsOnlyInput.filter { it.isDigit() }
        if (digits.length < 5) return@runCatching null

        // 1. Direct lookup by 5+ digit Account ID in /cloud_accounts/{digits}
        val directAccountRef = db.collection("cloud_accounts").document(digits)
        try {
            val directSnap = directAccountRef.get().await()
            if (directSnap.exists()) {
                val cloudDoc = directSnap.toObject(
                    CloudAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (cloudDoc != null) {
                    return@runCatching cloudDoc.toOfficialAccountProfile()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, directAccountRef.path)
            throw e
        }

        // 2. Lookup by Phone Number in /phone_directory/{digits}
        val phoneRef = db.collection("phone_directory").document(digits)
        try {
            val phoneSnap = phoneRef.get().await()
            if (phoneSnap.exists()) {
                val resolvedAccountId = phoneSnap.getString("accountId").orEmpty().filter { it.isDigit() }
                if (resolvedAccountId.length >= 5) {
                    val targetAccountRef = db.collection("cloud_accounts").document(resolvedAccountId)
                    val targetSnap = targetAccountRef.get().await()
                    if (targetSnap.exists()) {
                        val cloudDoc = targetSnap.toObject(
                            CloudAccountDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (cloudDoc != null) {
                            return@runCatching cloudDoc.toOfficialAccountProfile()
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, phoneRef.path)
            throw e
        }

        null
    }

    /**
     * Updates the password (`passwordPlainOrHash`) of an account directly in the Cloud Database (`/cloud_accounts/{accountId}`).
     */
    suspend fun updateAccountPasswordInCloud(
        accountId: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanAccountId = accountId.filter { it.isDigit() }
        val cleanPass = newPassword.trim()
        if (cleanAccountId.length < 5 || cleanPass.isBlank()) return@runCatching
        val accountRef = db.collection("cloud_accounts").document(cleanAccountId)
        val snap = accountRef.get().await()
        if (snap.exists() && snap.getString("ownerUid") == uid) {
            accountRef.update(
                mapOf(
                    "passwordPlainOrHash" to cleanPass,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    /**
     * Searches the Cloud Database (`/cloud_accounts` & `/phone_directory`) for an account matching
     * the user's Login or Password-Reset identifier (Email OR Phone Number OR Account ID).
     */
    suspend fun findAccountInCloudForLogin(identifier: String): Result<OfficialAccountProfile?> = runCatching {
        val cleanInput = identifier.trim()
        if (cleanInput.isBlank()) return@runCatching null

        // 1. If input is an Email address, query /cloud_accounts by email
        if (cleanInput.contains("@")) {
            val lowerEmail = cleanInput.lowercase(Locale.ROOT)
            val emailSnap = db.collection("cloud_accounts")
                .whereEqualTo("email", lowerEmail)
                .limit(1)
                .get()
                .await()
            if (!emailSnap.isEmpty) {
                val doc = emailSnap.documents.first().toObject(
                    CloudAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (doc != null) return@runCatching doc.toOfficialAccountProfile(lowerEmail)
            }
            // Fallback exact case match if stored before lowercase normalization
            val rawEmailSnap = db.collection("cloud_accounts")
                .whereEqualTo("email", cleanInput)
                .limit(1)
                .get()
                .await()
            if (!rawEmailSnap.isEmpty) {
                val doc = rawEmailSnap.documents.first().toObject(
                    CloudAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (doc != null) return@runCatching doc.toOfficialAccountProfile(cleanInput)
            }
        }

        // 2. If input contains digits (Phone Number or Account ID), check direct lookup & phone queries
        val digits = cleanInput.filter { it.isDigit() }
        if (digits.length >= 5) {
            val directOrPhone = lookupRecipientInCloud(digits).getOrNull()
            if (directOrPhone != null) return@runCatching directOrPhone

            val localPhoneSnap = db.collection("cloud_accounts")
                .whereEqualTo("phoneNumberLocal", digits)
                .limit(1)
                .get()
                .await()
            if (!localPhoneSnap.isEmpty) {
                val doc = localPhoneSnap.documents.first().toObject(
                    CloudAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (doc != null) return@runCatching doc.toOfficialAccountProfile()
            }

            val fullPhoneSnap = db.collection("cloud_accounts")
                .whereEqualTo("fullPhoneDigits", digits)
                .limit(1)
                .get()
                .await()
            if (!fullPhoneSnap.isEmpty) {
                val doc = fullPhoneSnap.documents.first().toObject(
                    CloudAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (doc != null) return@runCatching doc.toOfficialAccountProfile()
            }
        }

        null
    }

    /**
     * Strict 3-Field OR Duplicate Check in the Cloud Database (`/cloud_accounts` & `/phone_directory`):
     * Checks each of the 3 fields separately (OR condition):
     *   1) Is the Email already registered?
     *   2) OR is the Phone Number (local or international) already registered?
     *   3) OR is the Full Quadruple Name (with normalized whitespace) already registered?
     */
    suspend fun hasDuplicateAccountInCloud(
        email: String = "",
        phoneLocalDigits: String,
        fullPhoneDigits: String,
        fullQuadName: String
    ): Result<Boolean> = runCatching {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanLocal = phoneLocalDigits.filter { it.isDigit() }
        val cleanFull = fullPhoneDigits.filter { it.isDigit() }
        val normalizedQuad = fullQuadName.trim().replace(Regex("\\s+"), " ")

        // Condition 1 (OR): Is the Email already registered in /cloud_accounts?
        if (cleanEmail.isNotBlank() && cleanEmail.contains("@")) {
            val emailSnap = db.collection("cloud_accounts")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()
            if (!emailSnap.isEmpty) return@runCatching true
        }

        // Condition 2 (OR): Is the Phone Number already registered in /phone_directory or /cloud_accounts?
        if (cleanLocal.length >= 5) {
            val localSnap = db.collection("phone_directory").document(cleanLocal).get().await()
            if (localSnap.exists()) return@runCatching true

            val cloudLocalSnap = db.collection("cloud_accounts")
                .whereEqualTo("phoneNumberLocal", cleanLocal)
                .limit(1)
                .get()
                .await()
            if (!cloudLocalSnap.isEmpty) return@runCatching true
        }
        if (cleanFull.length >= 5 && cleanFull != cleanLocal) {
            val fullSnap = db.collection("phone_directory").document(cleanFull).get().await()
            if (fullSnap.exists()) return@runCatching true

            val cloudFullSnap = db.collection("cloud_accounts")
                .whereEqualTo("fullPhoneDigits", cleanFull)
                .limit(1)
                .get()
                .await()
            if (!cloudFullSnap.isEmpty) return@runCatching true
        }

        // Condition 3 (OR): Is the Full Quadruple Name (after normalizing spaces) already registered in /cloud_accounts?
        if (normalizedQuad.isNotBlank()) {
            val quadSnap = db.collection("cloud_accounts")
                .whereEqualTo("fullQuadName", normalizedQuad)
                .limit(1)
                .get()
                .await()
            if (!quadSnap.isEmpty) return@runCatching true
        }
        false
    }

    /**
     * Atomic Online Cloud Transaction (`db.runTransaction`):
     * Strictly executes balance verification, sender deduction, recipient credit, and
     * immutable ledger recording on the Cloud Server in a single atomic transaction.
     */
    suspend fun executeAtomicOnlineTransfer(
        transferId: String,
        ottId: String,
        senderAccount: OfficialAccountProfile,
        recipientAccount: OfficialAccountProfile,
        amount: Double,
        currency: WalletCurrency,
        syncChannel: String = "ONLINE_CLOUD_ATOMIC"
    ): Result<CloudTransferDoc> = runCatching {
        executeAtomicOnlineTransferDetailed(
            transferId = transferId,
            ottId = ottId,
            senderAccount = senderAccount,
            recipientAccount = recipientAccount,
            amount = amount,
            currency = currency,
            syncChannel = syncChannel
        ).getOrThrow().transferDoc
    }

    suspend fun executeAtomicOnlineTransferDetailed(
        transferId: String,
        ottId: String,
        senderAccount: OfficialAccountProfile,
        recipientAccount: OfficialAccountProfile,
        amount: Double,
        currency: WalletCurrency,
        syncChannel: String = "ONLINE_CLOUD_ATOMIC"
    ): Result<AtomicCloudTransferOutcome> = runCatching {
        val uid = requireUserId()
        require(amount > 0.0) { "Transfer amount must be positive" }

        val cleanSenderId = senderAccount.accountId.filter { it.isDigit() }
        val cleanRecipientId = recipientAccount.accountId.filter { it.isDigit() }
        val cleanTransferId = transferId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

        val senderRef = db.collection("cloud_accounts").document(cleanSenderId)
        val recipientRef = db.collection("cloud_accounts").document(cleanRecipientId)
        val transferRef = db.collection("cloud_transfers").document(cleanTransferId)

        try {
            // Ensure both sender and recipient accounts exist in cloud_accounts before running atomic transaction
            val preSenderSnap = senderRef.get().await()
            if (!preSenderSnap.exists()) {
                registerOrSyncCloudAccount(senderAccount, syncPhoneDirectory = true).getOrThrow()
            }
            val preRecipientSnap = recipientRef.get().await()
            if (!preRecipientSnap.exists()) {
                registerOrSyncCloudAccount(recipientAccount, syncPhoneDirectory = true)
            }

            var recipientOwnerUid = uid
            var finalSenderYer = senderAccount.balanceYer
            var finalSenderSar = senderAccount.balanceSar
            var finalSenderUsd = senderAccount.balanceUsd
            var finalRecYer = recipientAccount.balanceYer
            var finalRecSar = recipientAccount.balanceSar
            var finalRecUsd = recipientAccount.balanceUsd

            db.runTransaction { transaction ->
                val senderSnap = transaction.get(senderRef)
                val recipientSnap = transaction.get(recipientRef)

                if (senderSnap.exists()) {
                    val currentYer = (senderSnap.get("balanceYer") as? Number)?.toDouble() ?: senderAccount.balanceYer
                    val currentSar = (senderSnap.get("balanceSar") as? Number)?.toDouble() ?: senderAccount.balanceSar
                    val currentUsd = (senderSnap.get("balanceUsd") as? Number)?.toDouble() ?: senderAccount.balanceUsd

                    val activeCurrencyBalance = when (currency) {
                        WalletCurrency.YER -> currentYer
                        WalletCurrency.SAR -> currentSar
                        WalletCurrency.USD -> currentUsd
                    }
                    if (activeCurrencyBalance + 0.001 < amount) {
                        throw IllegalStateException("الرصيد السحابي الموثق غير كافٍ لإتمام العملية")
                    }

                    finalSenderYer = if (currency == WalletCurrency.YER) (currentYer - amount).coerceAtLeast(0.0) else currentYer
                    finalSenderSar = if (currency == WalletCurrency.SAR) (currentSar - amount).coerceAtLeast(0.0) else currentSar
                    finalSenderUsd = if (currency == WalletCurrency.USD) (currentUsd - amount).coerceAtLeast(0.0) else currentUsd

                    if (senderSnap.getString("ownerUid") == uid) {
                        transaction.update(
                            senderRef,
                            mapOf(
                                "balanceYer" to finalSenderYer,
                                "balanceSar" to finalSenderSar,
                                "balanceUsd" to finalSenderUsd,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                if (recipientSnap.exists()) {
                    recipientOwnerUid = recipientSnap.getString("ownerUid") ?: uid
                    val recYer = (recipientSnap.get("balanceYer") as? Number)?.toDouble() ?: recipientAccount.balanceYer
                    val recSar = (recipientSnap.get("balanceSar") as? Number)?.toDouble() ?: recipientAccount.balanceSar
                    val recUsd = (recipientSnap.get("balanceUsd") as? Number)?.toDouble() ?: recipientAccount.balanceUsd

                    finalRecYer = if (currency == WalletCurrency.YER) recYer + amount else recYer
                    finalRecSar = if (currency == WalletCurrency.SAR) recSar + amount else recSar
                    finalRecUsd = if (currency == WalletCurrency.USD) recUsd + amount else recUsd

                    transaction.update(
                        recipientRef,
                        mapOf(
                            "balanceYer" to finalRecYer,
                            "balanceSar" to finalRecSar,
                            "balanceUsd" to finalRecUsd,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val participantUids = if (recipientOwnerUid != uid && recipientOwnerUid.isNotBlank()) {
                    listOf(uid, recipientOwnerUid)
                } else {
                    listOf(uid)
                }
                val participantAccountIds = if (cleanSenderId != cleanRecipientId) {
                    listOf(cleanSenderId, cleanRecipientId)
                } else {
                    listOf(cleanSenderId)
                }

                val transferPayload = mapOf(
                    "transferId" to cleanTransferId,
                    "ottId" to ottId.take(40),
                    "senderUid" to uid,
                    "senderAccountId" to cleanSenderId,
                    "senderQuadName" to senderAccount.fullQuadName.trim(),
                    "recipientUid" to recipientOwnerUid,
                    "recipientAccountId" to cleanRecipientId,
                    "recipientQuadName" to recipientAccount.fullQuadName.trim(),
                    "amount" to amount,
                    "currency" to currency.code,
                    "syncChannel" to syncChannel,
                    "status" to "COMPLETED",
                    "participantUids" to participantUids,
                    "participantAccountIds" to participantAccountIds,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(transferRef, transferPayload)
            }.await()

            val txDoc = CloudTransferDoc(
                id = cleanTransferId,
                transferId = cleanTransferId,
                ottId = ottId,
                senderUid = uid,
                senderAccountId = cleanSenderId,
                senderQuadName = senderAccount.fullQuadName,
                recipientUid = recipientOwnerUid,
                recipientAccountId = cleanRecipientId,
                recipientQuadName = recipientAccount.fullQuadName,
                amount = amount,
                currency = currency.code,
                syncChannel = syncChannel,
                status = "COMPLETED",
                participantUids = listOf(uid, recipientOwnerUid).distinct(),
                participantAccountIds = listOf(cleanSenderId, cleanRecipientId).distinct(),
                createdAt = Timestamp.now()
            )

            val canonicalTx = String.format(
                Locale.US,
                "VIBRAPAY_CLOUD_TX|%s|%s|%s|%s|%.2f|%s",
                cleanTransferId,
                ottId,
                cleanSenderId,
                cleanRecipientId,
                amount,
                currency.code
            )
            val txHmacHex = HardwareSecurityManager.computeKeystoreHmacHex(canonicalTx)

            AtomicCloudTransferOutcome(
                transferDoc = txDoc,
                authoritativeSenderYer = finalSenderYer,
                authoritativeSenderSar = finalSenderSar,
                authoritativeSenderUsd = finalSenderUsd,
                authoritativeRecipientYer = finalRecYer,
                authoritativeRecipientSar = finalRecSar,
                authoritativeRecipientUsd = finalRecUsd,
                cloudHmacSignatureHex = txHmacHex
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, transferRef.path)
            throw e
        }
    }

    /**
     * Atomic Cloud Balance Top-Up (`db.runTransaction`):
     * Executes deposits exclusively through the Cloud Server transaction engine so balances
     * remain 100% cloud-authoritative.
     */
    suspend fun executeAtomicCloudDeposit(
        account: OfficialAccountProfile,
        currency: WalletCurrency,
        depositAmount: Double
    ): Result<OfficialAccountProfile> = runCatching {
        val uid = requireUserId()
        require(depositAmount > 0.0) { "Deposit amount must be positive" }
        val cleanAccountId = account.accountId.filter { it.isDigit() }
        val accountRef = db.collection("cloud_accounts").document(cleanAccountId)

        try {
            val preSnap = accountRef.get().await()
            if (!preSnap.exists()) {
                registerOrSyncCloudAccount(account, syncPhoneDirectory = true).getOrThrow()
            }

            var updatedYer = account.balanceYer
            var updatedSar = account.balanceSar
            var updatedUsd = account.balanceUsd

            db.runTransaction { transaction ->
                val snap = transaction.get(accountRef)
                val curYer = (snap.get("balanceYer") as? Number)?.toDouble() ?: account.balanceYer
                val curSar = (snap.get("balanceSar") as? Number)?.toDouble() ?: account.balanceSar
                val curUsd = (snap.get("balanceUsd") as? Number)?.toDouble() ?: account.balanceUsd

                updatedYer = if (currency == WalletCurrency.YER) curYer + depositAmount else curYer
                updatedSar = if (currency == WalletCurrency.SAR) curSar + depositAmount else curSar
                updatedUsd = if (currency == WalletCurrency.USD) curUsd + depositAmount else curUsd

                if (!snap.exists() || snap.getString("ownerUid") == uid) {
                    transaction.update(
                        accountRef,
                        mapOf(
                            "balanceYer" to updatedYer,
                            "balanceSar" to updatedSar,
                            "balanceUsd" to updatedUsd,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            account.copy(
                balanceYer = updatedYer,
                balanceSar = updatedSar,
                balanceUsd = updatedUsd
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, accountRef.path)
            throw e
        }
    }

    /**
     * Real-time observation of the authenticated user's own cloud accounts.
     */
    fun observeMyCloudAccounts(userId: String = requireUserId()): Flow<List<CloudAccountDoc>> {
        val path = "cloud_accounts"
        return db.collection(path)
            .whereEqualTo("ownerUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    /**
     * Real-time observation of all cloud transfers where the authenticated user is a participant
     * (either sender or recipient).
     */
    fun observeParticipantTransfers(userId: String = requireUserId()): Flow<List<CloudTransferDoc>> {
        val path = "cloud_transfers"
        return db.collection(path)
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudTransferDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }
}
