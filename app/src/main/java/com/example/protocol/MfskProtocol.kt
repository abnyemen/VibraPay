package com.example.protocol

import kotlin.math.roundToInt
import kotlin.random.Random

enum class AcousticMode(
    val titleAr: String,
    val subtitleAr: String,
    val startToneHz: Int,
    val syncToneHz: Int,
    val baseDataToneHz: Int,
    val stepHz: Int,
    val endToneHz: Int
) {
    AUDIBLE(
        titleAr = "الوضع المسموع (Audible)",
        subtitleAr = "1.2kHz – 4.15kHz • للمسافات البعيدة والاختبار المباشر",
        startToneHz = 1200,
        syncToneHz = 1400,
        baseDataToneHz = 1600,
        stepHz = 150,
        endToneHz = 4150
    ),
    ULTRASONIC(
        titleAr = "الوضع الخفي (Ultrasonic)",
        subtitleAr = "17.0kHz – 19.95kHz • موجات فوق صوتية غير مسموعة",
        startToneHz = 17000,
        syncToneHz = 17200,
        baseDataToneHz = 17400,
        stepHz = 150,
        endToneHz = 19950
    );

    fun dataToneHz(hexValue: Int): Int {
        require(hexValue in 0..15) { "Hex value must be in 0..15" }
        return baseDataToneHz + (hexValue * stepHz)
    }

    fun allProtocolTones(): List<ProtocolToneSpec> {
        val list = mutableListOf<ProtocolToneSpec>()
        list.add(ProtocolToneSpec(ToneSymbol.Start, startToneHz, "START"))
        list.add(ProtocolToneSpec(ToneSymbol.Sync, syncToneHz, "SYNC"))
        for (i in 0..15) {
            val hexChar = i.toString(16).uppercase()
            list.add(ProtocolToneSpec(ToneSymbol.Data(i, hexChar[0]), dataToneHz(i), hexChar))
        }
        list.add(ProtocolToneSpec(ToneSymbol.End, endToneHz, "END"))
        return list
    }
}

enum class MicroOpType(
    val code: String,
    val labelAr: String,
    val actionVerbAr: String
) {
    SELLER_REQUEST("REQ", "طلب دفع بائع (REQ)", "طلب دفع"),
    BUYER_CONFIRM_PAY("PAY", "تأكيد دفع مشتري (PAY)", "تحصيل"),
    PAYMENT("P", "دفعة مباشرة (P)", "إرسال"),
    COLLECTION("I", "فاتورة سريعة (I)", "تحصيل"),
    REFUND("R", "استرداد مبلغ (R)", "استرداد");

    companion object {
        fun fromCode(code: String): MicroOpType {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
                ?: COLLECTION
        }
    }
}

enum class ProximityLevel(val labelAr: String) {
    FAR("بعيد (خارج نطاق التلامس)"),
    GET_CLOSER("اقترب أكثر (إشارة متوسطة)"),
    EXCELLENT_CONTACT("تلامس آمن ممتاز (< 10 سم)")
}

sealed class ToneSymbol(val label: String, val durationMs: Int) {
    data object Start : ToneSymbol("START", MfskProtocol.START_END_DURATION_MS)
    data object Sync : ToneSymbol("SYNC", MfskProtocol.SYNC_DURATION_MS)
    data class Data(val hexValue: Int, val hexChar: Char) :
        ToneSymbol(hexChar.toString(), MfskProtocol.SYMBOL_DURATION_MS)
    data object End : ToneSymbol("END", MfskProtocol.START_END_DURATION_MS)
}

data class ProtocolToneSpec(
    val symbol: ToneSymbol,
    val frequencyHz: Int,
    val shortLabel: String
)

data class ScheduledTone(
    val symbol: ToneSymbol,
    val frequencyHz: Int,
    val durationMs: Int
)

data class EncodedPacket(
    val originalText: String,
    val utf8Bytes: ByteArray,
    val transmittedBytes: ByteArray,
    val isEncrypted: Boolean,
    val saltByte: Int?,
    val saltHex: String?,
    val dataHex: String,
    val checksumByte: Int,
    val checksumHex: String,
    val fullHexWithChecksum: String,
    val scheduledTones: List<ScheduledTone>,
    val totalDurationMs: Int
)

data class DecodedPacketResult(
    val rawHexReceived: String,
    val dataHex: String,
    val isEncrypted: Boolean,
    val extractedSaltHex: String?,
    val receivedChecksumHex: String,
    val calculatedChecksumHex: String,
    val isChecksumValid: Boolean,
    val isKeyMismatch: Boolean,
    val isReplayBurnedError: Boolean = false,
    val securityAlertMessage: String?,
    val decodedText: String,
    val structuredPayload: StructuredPayload
)

sealed class StructuredPayload {
    data class FreeText(val text: String) : StructuredPayload()
    data class Invoice(
        val storeName: String,
        val amount: String,
        val currency: String,
        val ottId: String,
        val opType: MicroOpType = MicroOpType.COLLECTION,
        val rawMicroPacket: String = ""
    ) : StructuredPayload()
    data class ContactCard(
        val fullName: String,
        val phone: String,
        val organization: String
    ) : StructuredPayload()
}

/**
 * Lightweight XOR Stream Cipher / PRNG Keystream with 1-Byte Salt.
 * Keeps the encrypted payload size identical to the original message (+1 byte for Salt).
 */
object XorStreamCipher {
    const val DEFAULT_FAST_PIN = "2026"
    const val SECURITY_ALERT_KEY_MISMATCH = "تم التقاط نبضة مشفرة - مفتاح الأمان غير متطابق"

    fun generateKeystream(secretKey: String, salt: Int, length: Int): ByteArray {
        val effectiveKey = secretKey.ifBlank { DEFAULT_FAST_PIN }
        val keyBytes = effectiveKey.toByteArray(Charsets.UTF_8)
        val saltUnsigned = salt and 0xFF

        var state = (0x811C9DC5L.toInt()) xor (saltUnsigned * 0x01000193)
        for (i in keyBytes.indices) {
            val b = keyBytes[i].toInt() and 0xFF
            state = state xor (b + (i * 31) + saltUnsigned)
            state *= 0x01000193
            state = state xor (state ushr 13)
        }
        if (state == 0) {
            state = 0x5A3C9E71 xor (saltUnsigned shl 8)
        }

        val stream = ByteArray(length)
        for (i in 0 until length) {
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            val keyByte = keyBytes[i % keyBytes.size].toInt() and 0xFF
            val mixed = ((state ushr ((i % 4) * 8)) xor keyByte xor ((saltUnsigned + i * 17) and 0xFF)) and 0xFF
            stream[i] = mixed.toByte()
        }
        return stream
    }

    fun encryptWithSalt(
        plainBytes: ByteArray,
        secretKey: String,
        explicitSalt: Int? = null
    ): Pair<Int, ByteArray> {
        val salt = (explicitSalt ?: Random.nextInt(1, 256)) and 0xFF
        val keystream = generateKeystream(secretKey, salt, plainBytes.size)
        val output = ByteArray(plainBytes.size + 1)
        output[0] = salt.toByte()
        for (i in plainBytes.indices) {
            val p = plainBytes[i].toInt() and 0xFF
            val k = keystream[i].toInt() and 0xFF
            output[i + 1] = (p xor k).toByte()
        }
        return salt to output
    }

    fun decryptWithSalt(
        saltPrefixedCipherBytes: ByteArray,
        secretKey: String
    ): Pair<Int, ByteArray> {
        if (saltPrefixedCipherBytes.isEmpty()) return 0 to ByteArray(0)
        val salt = saltPrefixedCipherBytes[0].toInt() and 0xFF
        val cipherLength = (saltPrefixedCipherBytes.size - 1).coerceAtLeast(0)
        val keystream = generateKeystream(secretKey, salt, cipherLength)
        val plainBytes = ByteArray(cipherLength)
        for (i in 0 until cipherLength) {
            val c = saltPrefixedCipherBytes[i + 1].toInt() and 0xFF
            val k = keystream[i].toInt() and 0xFF
            plainBytes[i] = (c xor k).toByte()
        }
        return salt to plainBytes
    }
}

object MfskProtocol {
    const val SAMPLE_RATE = 48000
    const val FFT_SIZE = 4096
    const val SMOOTHING_TIME_CONSTANT = 0.1f
    const val SYMBOL_DURATION_MS = 90
    const val SYNC_DURATION_MS = 60
    const val START_END_DURATION_MS = 250
    const val RAMP_DURATION_MS = 8
    const val DEFAULT_THRESHOLD = 110
    const val NFC_PROXIMITY_THRESHOLD = 205
    const val NFC_TRANSMIT_GAIN = 0.35
    const val STANDARD_TRANSMIT_GAIN = 0.92
    const val AUTO_LISTEN_TIMEOUT_SECONDS = 20
    const val DEBOUNCE_CONSECUTIVE_READS = 2

    private const val HEX_DIGITS = "0123456789ABCDEF"

    /**
     * Evaluates the Acoustic NFC Proximity level from the peak FFT byte magnitude (0..255).
     */
    fun evaluateProximityLevel(magnitude: Int, threshold: Int = NFC_PROXIMITY_THRESHOLD): ProximityLevel {
        return when {
            magnitude >= threshold -> ProximityLevel.EXCELLENT_CONTACT
            magnitude >= 130 -> ProximityLevel.GET_CLOSER
            else -> ProximityLevel.FAR
        }
    }

    /**
     * Generates a unique 4-character Hex One-Time Token (OTT_ID) derived from current time and randomness.
     */
    fun generateOttId(): String {
        val timeLow = (System.currentTimeMillis() and 0xFF).toInt()
        val nanoLow = (System.nanoTime() and 0xFF).toInt()
        val randA = Random.nextInt(0, 256)
        val randB = Random.nextInt(0, 256)
        val byte1 = (timeLow xor randA) and 0xFF
        val byte2 = (nanoLow xor randB) and 0xFF
        return String.format("%02X%02X", byte1, byte2)
    }

    /**
     * Builds a compressed Micro-Packet string for ultra-fast sonic transmission:
     * Format: REQ|Amount|OTT_ID, PAY|Amount|OTT_ID, P|Amount|OTT_ID, I|Amount|OTT_ID, or R|Amount|OTT_ID.
     */
    fun buildMicroPacket(
        opType: MicroOpType,
        amount: String,
        ottId: String,
        storeName: String = "",
        currency: String = "SAR"
    ): String {
        val cleanAmount = amount.trim().ifBlank { "0" }
        val cleanOtt = ottId.trim().uppercase().take(4).padStart(4, '0')
        val cleanStore = storeName.trim().replace("|", "")
        val cleanCurr = currency.trim().uppercase().ifBlank { "SAR" }
        return when {
            cleanStore.isNotEmpty() && cleanStore != "VibraPay" && cleanCurr != "SAR" ->
                "${opType.code}|$cleanAmount|$cleanOtt|$cleanStore|$cleanCurr"
            cleanStore.isNotEmpty() && cleanStore != "VibraPay" ->
                "${opType.code}|$cleanAmount|$cleanOtt|$cleanStore"
            cleanCurr != "SAR" ->
                "${opType.code}|$cleanAmount|$cleanOtt|$cleanCurr"
            else ->
                "${opType.code}|$cleanAmount|$cleanOtt"
        }
    }

    fun frequencyToBinIndex(
        freqHz: Int,
        fftSize: Int = FFT_SIZE,
        sampleRate: Int = SAMPLE_RATE
    ): Int {
        return ((freqHz.toDouble() * fftSize.toDouble()) / sampleRate.toDouble()).roundToInt()
            .coerceIn(1, (fftSize / 2) - 2)
    }

    fun readPeakBinMagnitude(byteFrequencyData: IntArray, freqHz: Int): Int {
        val binIndex = frequencyToBinIndex(freqHz)
        val prev = byteFrequencyData.getOrElse(binIndex - 1) { 0 }
        val curr = byteFrequencyData.getOrElse(binIndex) { 0 }
        val next = byteFrequencyData.getOrElse(binIndex + 1) { 0 }
        return maxOf(prev, curr, next)
    }

    fun encodePayload(
        text: String,
        mode: AcousticMode,
        encrypt: Boolean = false,
        secretPin: String = XorStreamCipher.DEFAULT_FAST_PIN,
        explicitSalt: Int? = null
    ): EncodedPacket {
        val utf8Bytes = text.toByteArray(Charsets.UTF_8)

        var plainByteSum = 0
        for (b in utf8Bytes) {
            plainByteSum += (b.toInt() and 0xFF)
        }
        val checksumByte = plainByteSum % 256
        val checksumHex = StringBuilder(2)
            .append(HEX_DIGITS[(checksumByte ushr 4) and 0x0F])
            .append(HEX_DIGITS[checksumByte and 0x0F])
            .toString()

        val (saltByte, transmittedBytes) = if (encrypt) {
            val (salt, encryptedWithSalt) = XorStreamCipher.encryptWithSalt(
                plainBytes = utf8Bytes,
                secretKey = secretPin,
                explicitSalt = explicitSalt
            )
            salt to encryptedWithSalt
        } else {
            null to utf8Bytes
        }

        val saltHex = saltByte?.let { String.format("%02X", it and 0xFF) }

        val dataHexBuilder = StringBuilder(transmittedBytes.size * 2)
        for (b in transmittedBytes) {
            val unsignedVal = b.toInt() and 0xFF
            dataHexBuilder.append(HEX_DIGITS[(unsignedVal ushr 4) and 0x0F])
            dataHexBuilder.append(HEX_DIGITS[unsignedVal and 0x0F])
        }

        val dataHex = dataHexBuilder.toString()
        val fullHex = dataHex + checksumHex

        val scheduled = mutableListOf<ScheduledTone>()
        scheduled.add(
            ScheduledTone(
                symbol = ToneSymbol.Start,
                frequencyHz = mode.startToneHz,
                durationMs = START_END_DURATION_MS
            )
        )

        var previousChar: Char? = null
        for (ch in fullHex) {
            if (previousChar != null && previousChar == ch) {
                scheduled.add(
                    ScheduledTone(
                        symbol = ToneSymbol.Sync,
                        frequencyHz = mode.syncToneHz,
                        durationMs = SYNC_DURATION_MS
                    )
                )
            }
            val hexVal = HEX_DIGITS.indexOf(ch.uppercaseChar()).coerceAtLeast(0)
            scheduled.add(
                ScheduledTone(
                    symbol = ToneSymbol.Data(hexVal, ch.uppercaseChar()),
                    frequencyHz = mode.dataToneHz(hexVal),
                    durationMs = SYMBOL_DURATION_MS
                )
            )
            previousChar = ch
        }

        scheduled.add(
            ScheduledTone(
                symbol = ToneSymbol.End,
                frequencyHz = mode.endToneHz,
                durationMs = START_END_DURATION_MS
            )
        )

        val totalDurationMs = scheduled.sumOf { it.durationMs }

        return EncodedPacket(
            originalText = text,
            utf8Bytes = utf8Bytes,
            transmittedBytes = transmittedBytes,
            isEncrypted = encrypt,
            saltByte = saltByte,
            saltHex = saltHex,
            dataHex = dataHex,
            checksumByte = checksumByte,
            checksumHex = checksumHex,
            fullHexWithChecksum = fullHex,
            scheduledTones = scheduled,
            totalDurationMs = totalDurationMs
        )
    }

    fun decodeLivePreview(
        accumulatedHex: String,
        decrypt: Boolean = false,
        secretPin: String = XorStreamCipher.DEFAULT_FAST_PIN
    ): String {
        val minHexChars = if (decrypt) 4 else 2
        if (accumulatedHex.length < minHexChars) return ""
        val evenLength = (accumulatedHex.length / 2) * 2
        val rawBytes = ByteArray(evenLength / 2)
        for (i in 0 until evenLength step 2) {
            val high = HEX_DIGITS.indexOf(accumulatedHex[i].uppercaseChar())
            val low = HEX_DIGITS.indexOf(accumulatedHex[i + 1].uppercaseChar())
            if (high >= 0 && low >= 0) {
                rawBytes[i / 2] = ((high shl 4) or low).toByte()
            }
        }
        val previewBytes = if (decrypt) {
            XorStreamCipher.decryptWithSalt(rawBytes, secretPin).second
        } else {
            rawBytes
        }
        return try {
            String(previewBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun decodeHexPacket(
        rawHexReceived: String,
        decrypt: Boolean = false,
        secretPin: String = XorStreamCipher.DEFAULT_FAST_PIN
    ): DecodedPacketResult {
        val cleanHex = rawHexReceived.uppercase().filter { it in HEX_DIGITS }
        val minRequiredHex = if (decrypt) 4 else 2
        if (cleanHex.length < minRequiredHex) {
            return DecodedPacketResult(
                rawHexReceived = cleanHex,
                dataHex = "",
                isEncrypted = decrypt,
                extractedSaltHex = null,
                receivedChecksumHex = "--",
                calculatedChecksumHex = "00",
                isChecksumValid = false,
                isKeyMismatch = false,
                securityAlertMessage = null,
                decodedText = "",
                structuredPayload = StructuredPayload.FreeText("")
            )
        }

        val usableLength = (cleanHex.length / 2) * 2
        val alignedHex = cleanHex.substring(0, usableLength)

        if (alignedHex.length < minRequiredHex) {
            return DecodedPacketResult(
                rawHexReceived = cleanHex,
                dataHex = "",
                isEncrypted = decrypt,
                extractedSaltHex = null,
                receivedChecksumHex = "--",
                calculatedChecksumHex = "00",
                isChecksumValid = false,
                isKeyMismatch = false,
                securityAlertMessage = null,
                decodedText = "",
                structuredPayload = StructuredPayload.FreeText("")
            )
        }

        val dataHex = alignedHex.substring(0, alignedHex.length - 2)
        val receivedChecksumHex = alignedHex.substring(alignedHex.length - 2)

        val rawByteCount = dataHex.length / 2
        val rawDataBytes = ByteArray(rawByteCount)
        for (i in 0 until rawByteCount) {
            val high = HEX_DIGITS.indexOf(dataHex[i * 2])
            val low = HEX_DIGITS.indexOf(dataHex[i * 2 + 1])
            rawDataBytes[i] = (((high shl 4) or low) and 0xFF).toByte()
        }

        val (extractedSaltHex, candidatePlainBytes) = if (decrypt && rawDataBytes.isNotEmpty()) {
            val (salt, decrypted) = XorStreamCipher.decryptWithSalt(rawDataBytes, secretPin)
            String.format("%02X", salt and 0xFF) to decrypted
        } else {
            null to rawDataBytes
        }

        var sum = 0
        for (b in candidatePlainBytes) {
            sum += (b.toInt() and 0xFF)
        }

        val calculatedChecksum = sum % 256
        val calculatedChecksumHex = String.format("%02X", calculatedChecksum)
        val checksumMatches = receivedChecksumHex.equals(calculatedChecksumHex, ignoreCase = true)

        val decodedText = try {
            String(candidatePlainBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }

        val hasInvalidUtf8 = decodedText.contains('\uFFFD') ||
            decodedText.any { ch -> ch.code in 1..8 || ch.code in 14..31 }

        val isChecksumValid = checksumMatches && !(decrypt && hasInvalidUtf8)
        val isKeyMismatch = decrypt && !isChecksumValid
        val securityAlert = if (isKeyMismatch) {
            XorStreamCipher.SECURITY_ALERT_KEY_MISMATCH
        } else {
            null
        }

        return DecodedPacketResult(
            rawHexReceived = cleanHex,
            dataHex = dataHex,
            isEncrypted = decrypt,
            extractedSaltHex = extractedSaltHex,
            receivedChecksumHex = receivedChecksumHex,
            calculatedChecksumHex = calculatedChecksumHex,
            isChecksumValid = isChecksumValid,
            isKeyMismatch = isKeyMismatch,
            securityAlertMessage = securityAlert,
            decodedText = if (isKeyMismatch) "🔒 [بيانات مشفرة غير قابلة للقراءة]" else decodedText,
            structuredPayload = if (isKeyMismatch) {
                StructuredPayload.FreeText("🔒 [بيانات مشفرة غير قابلة للقراءة]")
            } else {
                parseStructuredPayload(decodedText)
            }
        )
    }

    fun buildInvoiceJson(
        storeName: String,
        amount: String,
        currency: String = "SAR",
        transactionId: String,
        opType: MicroOpType = MicroOpType.COLLECTION
    ): String {
        val cleanOtt = transactionId.replace("VP-", "").trim().take(4).padStart(4, '0').uppercase()
        return buildMicroPacket(
            opType = opType,
            amount = amount,
            ottId = cleanOtt,
            storeName = storeName
        )
    }

    fun buildContactJson(
        fullName: String,
        phone: String,
        organization: String
    ): String {
        val cleanName = fullName.trim().replace("|", "")
        val cleanPhone = phone.trim().replace("|", "")
        val cleanOrg = organization.trim().replace("|", "")
        return "C|$cleanName|$cleanPhone|$cleanOrg"
    }

    /**
     * Parses Micro-Packet strings (`REQ|Amount|OTT_ID`, `PAY|Amount|OTT_ID`, `T|Amount|OTT_ID`, etc.),
     * Contact Micro-Packets (`C|Name|Phone|Org`), or JSON payloads into full StructuredPayload objects.
     */
    fun parseStructuredPayload(text: String): StructuredPayload {
        val trimmed = text.trim()

        if (trimmed.contains("|")) {
            val parts = trimmed.split("|")
            if (parts.size >= 3) {
                val prefix = parts[0].trim().uppercase()
                if (prefix in listOf("REQ", "PAY", "P", "I", "R", "T")) {
                    val opType = if (prefix == "T") MicroOpType.COLLECTION else MicroOpType.fromCode(prefix)
                    val amount = parts[1].trim().ifBlank { "0.00" }
                    val ottId = parts[2].trim().uppercase().ifBlank { "0000" }
                    val defaultStore = when (opType) {
                        MicroOpType.SELLER_REQUEST -> "نقطة بيع VibraPay (طلب دفع قريب)"
                        MicroOpType.BUYER_CONFIRM_PAY -> "تأكيد دفع مشتري عبر التلامس"
                        else -> "متجر VibraPay الذكي"
                    }
                    val fourthPart = parts.getOrNull(3)?.trim() ?: ""
                    val fifthPart = parts.getOrNull(4)?.trim() ?: ""
                    val knownCurrencies = setOf("YER", "SAR", "USD", "ر.ي", "ر.س", "$")
                    val (storeName, parsedCurrency) = when {
                        fifthPart.uppercase() in knownCurrencies ->
                            (fourthPart.ifBlank { defaultStore }) to fifthPart.uppercase()
                        fourthPart.uppercase() in knownCurrencies ->
                            defaultStore to fourthPart.uppercase()
                        fourthPart.isNotEmpty() ->
                            fourthPart to "SAR"
                        else ->
                            defaultStore to "SAR"
                    }
                    return StructuredPayload.Invoice(
                        storeName = storeName,
                        amount = amount,
                        currency = parsedCurrency,
                        ottId = ottId,
                        opType = opType,
                        rawMicroPacket = trimmed
                    )
                } else if (prefix == "C" && parts.size >= 3) {
                    return StructuredPayload.ContactCard(
                        fullName = parts[1].trim(),
                        phone = parts[2].trim(),
                        organization = parts.getOrNull(3)?.trim() ?: "VibraPay"
                    )
                }
            }
        }

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val map = parseSimpleJsonObject(trimmed)
            when (map["t"]) {
                "INV" -> return StructuredPayload.Invoice(
                    storeName = map["s"]?.ifBlank { "متجر VibraPay الذكي" } ?: "متجر VibraPay الذكي",
                    amount = map["a"]?.ifBlank { "0.00" } ?: "0.00",
                    currency = map["c"]?.ifBlank { "ر.س" } ?: "ر.س",
                    ottId = (map["id"] ?: "0000").replace("VP-", "").take(4).uppercase(),
                    opType = MicroOpType.COLLECTION,
                    rawMicroPacket = trimmed
                )
                "CRD" -> return StructuredPayload.ContactCard(
                    fullName = map["nm"] ?: "",
                    phone = map["ph"] ?: "",
                    organization = map["org"] ?: ""
                )
            }
        }
        return StructuredPayload.FreeText(text)
    }

    private fun parseSimpleJsonObject(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        for (match in regex.findAll(json)) {
            val key = match.groupValues[1]
            val rawVal = match.groupValues[2]
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\")
            result[key] = rawVal
        }
        return result
    }
}
