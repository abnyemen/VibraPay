package com.example

import com.example.protocol.AcousticMode
import com.example.protocol.MfskProtocol
import com.example.protocol.MicroOpType
import com.example.protocol.ProximityLevel
import com.example.protocol.StructuredPayload
import com.example.protocol.ToneSymbol
import com.example.protocol.XorStreamCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun mfskFrequencyTable_matchesEngineeringSpecification() {
        val audible = AcousticMode.AUDIBLE
        assertEquals(1200, audible.startToneHz)
        assertEquals(1400, audible.syncToneHz)
        assertEquals(1600, audible.dataToneHz(0))
        assertEquals(3850, audible.dataToneHz(15))
        assertEquals(4150, audible.endToneHz)

        val ultrasonic = AcousticMode.ULTRASONIC
        assertEquals(17000, ultrasonic.startToneHz)
        assertEquals(17200, ultrasonic.syncToneHz)
        assertEquals(17400, ultrasonic.dataToneHz(0))
        assertEquals(19650, ultrasonic.dataToneHz(15))
        assertEquals(19950, ultrasonic.endToneHz)
    }

    @Test
    fun acousticNfcProximityParameters_matchSpecification() {
        assertEquals(205, MfskProtocol.NFC_PROXIMITY_THRESHOLD)
        assertEquals(0.35, MfskProtocol.NFC_TRANSMIT_GAIN, 0.001)
        assertEquals(20, MfskProtocol.AUTO_LISTEN_TIMEOUT_SECONDS)

        assertEquals(ProximityLevel.FAR, MfskProtocol.evaluateProximityLevel(90))
        assertEquals(ProximityLevel.GET_CLOSER, MfskProtocol.evaluateProximityLevel(165))
        assertEquals(ProximityLevel.EXCELLENT_CONTACT, MfskProtocol.evaluateProximityLevel(210))
    }

    @Test
    fun sellerReqAndBuyerPayMicroPackets_encodeAndDecodeCleanly() {
        val ottId = MfskProtocol.generateOttId()
        val reqPacketStr = MfskProtocol.buildMicroPacket(MicroOpType.SELLER_REQUEST, "245.50", ottId)
        assertEquals("REQ|245.50|$ottId", reqPacketStr)

        val encodedReq = MfskProtocol.encodePayload(reqPacketStr, AcousticMode.ULTRASONIC, encrypt = true, secretPin = "2026")
        val decodedReq = MfskProtocol.decodeHexPacket(encodedReq.fullHexWithChecksum, decrypt = true, secretPin = "2026")
        assertTrue(decodedReq.isChecksumValid)
        val reqInvoice = decodedReq.structuredPayload as StructuredPayload.Invoice
        assertEquals(MicroOpType.SELLER_REQUEST, reqInvoice.opType)
        assertEquals("245.50", reqInvoice.amount)
        assertEquals(ottId, reqInvoice.ottId)

        val payPacketStr = MfskProtocol.buildMicroPacket(MicroOpType.BUYER_CONFIRM_PAY, "245.50", ottId)
        assertEquals("PAY|245.50|$ottId", payPacketStr)

        val encodedPay = MfskProtocol.encodePayload(payPacketStr, AcousticMode.ULTRASONIC, encrypt = true, secretPin = "2026")
        val decodedPay = MfskProtocol.decodeHexPacket(encodedPay.fullHexWithChecksum, decrypt = true, secretPin = "2026")
        assertTrue(decodedPay.isChecksumValid)
        val payInvoice = decodedPay.structuredPayload as StructuredPayload.Invoice
        assertEquals(MicroOpType.BUYER_CONFIRM_PAY, payInvoice.opType)
        assertEquals("245.50", payInvoice.amount)
        assertEquals(ottId, payInvoice.ottId)
    }

    @Test
    fun e2eeXorStreamCipher_wrongPinTriggersSecurityAlert() {
        val secretMessage = "REQ|150.00|A1B2"
        val packet = MfskProtocol.encodePayload(
            text = secretMessage,
            mode = AcousticMode.AUDIBLE,
            encrypt = true,
            secretPin = "2026",
            explicitSalt = 0x4F
        )

        val decodedWrongKey = MfskProtocol.decodeHexPacket(
            rawHexReceived = packet.fullHexWithChecksum,
            decrypt = true,
            secretPin = "9999"
        )
        assertFalse(decodedWrongKey.isChecksumValid)
        assertTrue(decodedWrongKey.isKeyMismatch)
        assertEquals(
            XorStreamCipher.SECURITY_ALERT_KEY_MISMATCH,
            decodedWrongKey.securityAlertMessage
        )
    }

    @Test
    fun syncToneAntiRepetitionRule_insertsSyncBetweenIdenticalHexChars() {
        val packet = MfskProtocol.encodePayload("w", AcousticMode.ULTRASONIC, encrypt = false)
        val syncCount = packet.scheduledTones.count { it.symbol is ToneSymbol.Sync }
        assertTrue("Expected SYNC_TONE between repeated hex chars in 0x77", syncCount >= 1)
    }

    @Test
    fun multiCurrencyMicroPacket_preservesYerSarUsdCurrencies() {
        val yerPacket = MfskProtocol.buildMicroPacket(
            opType = MicroOpType.SELLER_REQUEST,
            amount = "15000",
            ottId = "C4D5",
            currency = "YER"
        )
        assertEquals("REQ|15000|C4D5|YER", yerPacket)
        val parsedYer = MfskProtocol.parseStructuredPayload(yerPacket) as StructuredPayload.Invoice
        assertEquals("15000", parsedYer.amount)
        assertEquals("YER", parsedYer.currency)
        assertEquals("C4D5", parsedYer.ottId)

        val usdPacket = MfskProtocol.buildMicroPacket(
            opType = MicroOpType.BUYER_CONFIRM_PAY,
            amount = "75.00",
            ottId = "F9A1",
            currency = "USD"
        )
        assertEquals("PAY|75.00|F9A1|USD", usdPacket)
        val parsedUsd = MfskProtocol.parseStructuredPayload(usdPacket) as StructuredPayload.Invoice
        assertEquals("75.00", parsedUsd.amount)
        assertEquals("USD", parsedUsd.currency)
    }

    @Test
    fun hardwareHmacSignature_detectsUnauthorizedBalanceTampering() {
        val originalPayload = "VIBRAPAY_HW_LEDGER_V1|YER=25000000|SAR=500000|USD=120000|PIN=2026|BURNED=A1B2,C3D4"
        val signatureHex = com.example.security.HardwareSecurityManager.computeKeystoreHmacHex(originalPayload)
        val recomputedOriginal = com.example.security.HardwareSecurityManager.computeKeystoreHmacHex(originalPayload)
        assertTrue(
            "Valid HMAC-SHA256 signature must pass constant-time verification",
            com.example.security.HardwareSecurityManager.constantTimeEquals(recomputedOriginal, signatureHex)
        )

        // Simulate an attacker modifying YER balance locally without the Keystore HMAC key
        val tamperedPayload = "VIBRAPAY_HW_LEDGER_V1|YER=9999999900|SAR=500000|USD=120000|PIN=2026|BURNED=A1B2,C3D4"
        val recomputedTampered = com.example.security.HardwareSecurityManager.computeKeystoreHmacHex(tamperedPayload)
        assertFalse(
            "Tampered balance must fail HMAC-SHA256 verification and trigger app freeze",
            com.example.security.HardwareSecurityManager.constantTimeEquals(recomputedTampered, signatureHex)
        )
    }

    @Test
    fun biquadHighPassFilterAndSmartAutoRetry_suppressSpeechAndPreserveUltrasonic() {
        assertEquals(3, com.example.audio.SonicTransceiverEngine.SMART_RETRY_BURST_COUNT)
        assertEquals(500L, com.example.audio.SonicTransceiverEngine.SMART_RETRY_INTERVAL_MS)
        assertEquals(16000, com.example.audio.SonicTransceiverEngine.ULTRASONIC_HIGHPASS_CUTOFF_HZ)

        val filter = com.example.audio.BiquadHighPassFilter(sampleRate = 48000)
        filter.configureHighPass(16000)

        // Generate 300 Hz human voice sine wave + 18,400 Hz ultrasonic tone
        val n = 2048
        val filteredSpeech = FloatArray(n) { i ->
            val sample = kotlin.math.sin(2.0 * Math.PI * 300.0 * i / 48000.0).toFloat()
            filter.processSample(sample)
        }
        val speechRms = kotlin.math.sqrt(filteredSpeech.drop(256).map { it * it }.average())

        filter.resetState()
        val filteredUltrasonic = FloatArray(n) { i ->
            val sample = kotlin.math.sin(2.0 * Math.PI * 18400.0 * i / 48000.0).toFloat()
            filter.processSample(sample)
        }
        val ultrasonicRms = kotlin.math.sqrt(filteredUltrasonic.drop(256).map { it * it }.average())

        assertTrue("300 Hz human speech must be heavily attenuated (< 0.01 RMS)", speechRms < 0.01)
        assertTrue("18.4 kHz ultrasonic tone must pass cleanly (> 0.35 RMS)", ultrasonicRms > 0.35)
    }
}


