package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import com.example.protocol.AcousticMode
import com.example.protocol.DecodedPacketResult
import com.example.protocol.EncodedPacket
import com.example.protocol.MfskProtocol
import com.example.protocol.ProximityLevel
import com.example.protocol.ScheduledTone
import com.example.protocol.ToneSymbol
import com.example.protocol.XorStreamCipher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class ReceiverStatus(val titleAr: String) {
    IDLE("متوقف — اضغط لبدء الاستلام عبر ذبذبات التلامس بين الجهازين"),
    LISTENING("جاهز للتلامس — قرّب الجهازين من بعضهما لأقل من 10 سم"),
    SELLER_WAITING_PAY("بانتظار ذبذبات تأكيد الدفع من جهاز المشتري (< 10 سم)..."),
    RECEIVING("جاري التقاط ذبذبات البيانات المباشرة بين الجهازين..."),
    PACKET_COMPLETE("تم استلام البيانات بنجاح وإيقاف الاستماع تلقائياً"),
    AUTO_TIMEOUT_CLOSED("تم الإيقاف تلقائياً بعد 20 ثانية لعدم تلامس الجهازين"),
    BACKGROUND_KILLED("تم إيقاف الاستماع تلقائياً لحماية الخصوصية")
}

data class ToneRadarBar(
    val label: String,
    val frequencyHz: Int,
    val binIndex: Int,
    val magnitude: Int,
    val isLocked: Boolean,
    val isAboveThreshold: Boolean
)

data class SpectrumFrame(
    val bars: List<ToneRadarBar> = emptyList(),
    val roomBars: List<Int> = List(36) { 0 },
    val dominantLabel: String? = null,
    val dominantFreqHz: Int? = null,
    val dominantMagnitude: Int = 0,
    val roomPeakFreqHz: Int = 0,
    val roomPeakMagnitude: Int = 0,
    val proximityLevel: ProximityLevel = ProximityLevel.FAR,
    val estimatedDistanceCmText: String = "> 35 سم (بعيد)",
    val isHardwareMicActive: Boolean = false,
    val isHighPassFilterActive: Boolean = true,
    val highPassCutoffHz: Int = 16000,
    val suppressedNoisePercent: Int = 0,
    val waveformSamples: FloatArray = FloatArray(128)
)

/**
 * Web Audio API `BiquadFilterNode` (type = "highpass") implementation in Kotlin.
 * Uses a 2-stage cascaded 2nd-order Butterworth High-Pass IIR filter (Q = 0.7071, -24 dB/octave)
 * to strip out human voice (80 Hz – 4,000 Hz) and ambient room noise before the FFT AnalyserNode.
 */
class BiquadHighPassFilter(
    private val sampleRate: Int = MfskProtocol.SAMPLE_RATE
) {
    private var currentCutoffHz: Int = -1
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // Stage 1 state
    private var x1_1 = 0.0
    private var x2_1 = 0.0
    private var y1_1 = 0.0
    private var y2_1 = 0.0

    // Stage 2 state (cascaded for sharp -24 dB/octave attenuation of human voice & room rumble)
    private var x1_2 = 0.0
    private var x2_2 = 0.0
    private var y1_2 = 0.0
    private var y2_2 = 0.0

    fun configureHighPass(cutoffHz: Int, qFactor: Double = 0.70710678) {
        if (cutoffHz == currentCutoffHz) return
        currentCutoffHz = cutoffHz
        val nyquist = sampleRate * 0.48
        val clampedCutoff = cutoffHz.toDouble().coerceIn(50.0, nyquist)
        val omega0 = (2.0 * PI * clampedCutoff) / sampleRate.toDouble()
        val cosOmega = cos(omega0)
        val sinOmega = sin(omega0)
        val alpha = sinOmega / (2.0 * qFactor)

        val rawB0 = (1.0 + cosOmega) / 2.0
        val rawB1 = -(1.0 + cosOmega)
        val rawB2 = (1.0 + cosOmega) / 2.0
        val a0 = 1.0 + alpha
        val rawA1 = -2.0 * cosOmega
        val rawA2 = 1.0 - alpha

        b0 = rawB0 / a0
        b1 = rawB1 / a0
        b2 = rawB2 / a0
        a1 = rawA1 / a0
        a2 = rawA2 / a0
        resetState()
    }

    fun resetState() {
        x1_1 = 0.0; x2_1 = 0.0; y1_1 = 0.0; y2_1 = 0.0
        x1_2 = 0.0; x2_2 = 0.0; y1_2 = 0.0; y2_2 = 0.0
    }

    fun processSample(input: Float): Float {
        val x = input.toDouble()
        // Stage 1 Biquad High-Pass
        val yStage1 = b0 * x + b1 * x1_1 + b2 * x2_1 - a1 * y1_1 - a2 * y2_1
        x2_1 = x1_1
        x1_1 = x
        y2_1 = y1_1
        y1_1 = yStage1

        // Stage 2 Biquad High-Pass
        val yStage2 = b0 * yStage1 + b1 * x1_2 + b2 * x2_2 - a1 * y1_2 - a2 * y2_2
        x2_2 = x1_2
        x1_2 = yStage1
        y2_2 = y1_2
        y1_2 = yStage2

        return yStage2.toFloat().coerceIn(-1f, 1f)
    }
}

class SonicTransceiverEngine(
    private val scope: CoroutineScope,
    private val onPacketDecoded: (DecodedPacketResult, AcousticMode) -> Unit
) {
    companion object {
        const val ULTRASONIC_HIGHPASS_CUTOFF_HZ = 16000
        const val AUDIBLE_HIGHPASS_CUTOFF_HZ = 1050
        const val SMART_RETRY_BURST_COUNT = 3
        const val SMART_RETRY_INTERVAL_MS = 500L
    }

    private val _spectrumFrame = MutableStateFlow(SpectrumFrame())
    val spectrumFrame: StateFlow<SpectrumFrame> = _spectrumFrame.asStateFlow()

    private val _receiverStatus = MutableStateFlow(ReceiverStatus.IDLE)
    val receiverStatus: StateFlow<ReceiverStatus> = _receiverStatus.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _singleRequestLockActive = MutableStateFlow(false)
    val singleRequestLockActive: StateFlow<Boolean> = _singleRequestLockActive.asStateFlow()

    private val _blockedCrowdSignalsCount = MutableStateFlow(0)
    val blockedCrowdSignalsCount: StateFlow<Int> = _blockedCrowdSignalsCount.asStateFlow()

    private val _lastBlockedCrowdAlert = MutableStateFlow<String?>(null)
    val lastBlockedCrowdAlert: StateFlow<String?> = _lastBlockedCrowdAlert.asStateFlow()

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val _transmitProgress = MutableStateFlow(0f)
    val transmitProgress: StateFlow<Float> = _transmitProgress.asStateFlow()

    private val _currentBurstIndex = MutableStateFlow(0)
    val currentBurstIndex: StateFlow<Int> = _currentBurstIndex.asStateFlow()

    private val _totalPlannedBursts = MutableStateFlow(SMART_RETRY_BURST_COUNT)
    val totalPlannedBursts: StateFlow<Int> = _totalPlannedBursts.asStateFlow()

    private val _burstStatusTextAr = MutableStateFlow<String?>(null)
    val burstStatusTextAr: StateFlow<String?> = _burstStatusTextAr.asStateFlow()

    private val _activeTransmitTone = MutableStateFlow<ScheduledTone?>(null)
    val activeTransmitTone: StateFlow<ScheduledTone?> = _activeTransmitTone.asStateFlow()

    private val _liveAccumulatedHex = MutableStateFlow("")
    val liveAccumulatedHex: StateFlow<String> = _liveAccumulatedHex.asStateFlow()

    private val _livePreviewText = MutableStateFlow("")
    val livePreviewText: StateFlow<String> = _livePreviewText.asStateFlow()

    private val _liveDetectedSymbols = MutableStateFlow<List<String>>(emptyList())
    val liveDetectedSymbols: StateFlow<List<String>> = _liveDetectedSymbols.asStateFlow()

    @Volatile
    var currentMode: AcousticMode = AcousticMode.ULTRASONIC

    @Volatile
    var nfcProximityMode: Boolean = true

    @Volatile
    var highPassFilterEnabled: Boolean = true

    @Volatile
    var smartAutoRetryEnabled: Boolean = true

    @Volatile
    var threshold: Int = MfskProtocol.NFC_PROXIMITY_THRESHOLD

    @Volatile
    var loopbackBridgeEnabled: Boolean = false

    @Volatile
    var simulateFarCrowdSignalInLoopback: Boolean = false

    @Volatile
    var encryptionEnabled: Boolean = true

    @Volatile
    var receiverSecretPin: String = XorStreamCipher.DEFAULT_FAST_PIN

    private val loopbackRingBuffer = ShortArray(MfskProtocol.SAMPLE_RATE * 30)
    @Volatile
    private var loopbackWritePos = 0
    @Volatile
    private var loopbackReadPos = 0
    private val loopbackLock = Any()

    private var receiverJob: Job? = null
    private var transmitterJob: Job? = null
    private var activeAudioTrack: AudioTrack? = null

    fun activeHighPassCutoffHz(mode: AcousticMode = currentMode): Int {
        return if (mode == AcousticMode.ULTRASONIC) {
            ULTRASONIC_HIGHPASS_CUTOFF_HZ
        } else {
            AUDIBLE_HIGHPASS_CUTOFF_HZ
        }
    }

    /**
     * Synthesizes the complete MFSK-18 audio packet at 48,000 Hz.
     * In Acoustic NFC Mode, speaker gain is set to 0.35 so the wave does not propagate far.
     */
    fun generatePcmForPacket(
        packet: EncodedPacket,
        gainMultiplier: Double = if (nfcProximityMode) MfskProtocol.NFC_TRANSMIT_GAIN else MfskProtocol.STANDARD_TRANSMIT_GAIN
    ): ShortArray {
        val sampleRate = MfskProtocol.SAMPLE_RATE
        val rampSamples = (MfskProtocol.RAMP_DURATION_MS * sampleRate) / 1000
        val totalSamples = packet.scheduledTones.sumOf { (it.durationMs * sampleRate) / 1000 }
        val pcm = ShortArray(totalSamples)

        var offset = 0
        for (tone in packet.scheduledTones) {
            val toneSamples = (tone.durationMs * sampleRate) / 1000
            val angularFreq = 2.0 * PI * tone.frequencyHz.toDouble() / sampleRate.toDouble()

            for (i in 0 until toneSamples) {
                val ramp = when {
                    i < rampSamples -> i.toDouble() / rampSamples.toDouble()
                    i > toneSamples - rampSamples ->
                        (toneSamples - i).toDouble() / rampSamples.toDouble()
                    else -> 1.0
                }.coerceIn(0.0, 1.0)

                val sampleVal = sin(angularFreq * i) * ramp * gainMultiplier
                if (offset + i < pcm.size) {
                    pcm[offset + i] = (sampleVal * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
            }
            offset += toneSamples
        }
        return pcm
    }

    /**
     * Transmits the MFSK-18 packet using Smart Sequential Auto-Retry (3 fast bursts separated by
     * 500ms, or stops early as soon as reception is confirmed).
     */
    fun transmitPacket(packet: EncodedPacket, onComplete: () -> Unit = {}) {
        transmitterJob?.cancel()
        transmitterJob = scope.launch(Dispatchers.Default) {
            _isTransmitting.value = true
            _transmitProgress.value = 0f
            val totalBursts = if (smartAutoRetryEnabled && !simulateFarCrowdSignalInLoopback) {
                SMART_RETRY_BURST_COUNT
            } else {
                1
            }
            _totalPlannedBursts.value = totalBursts
            val wasListeningAtStart = _isListening.value

            try {
                val sampleRate = MfskProtocol.SAMPLE_RATE
                val rampSamples = (MfskProtocol.RAMP_DURATION_MS * sampleRate) / 1000
                val speakerGain = if (nfcProximityMode) {
                    MfskProtocol.NFC_TRANSMIT_GAIN
                } else {
                    MfskProtocol.STANDARD_TRANSMIT_GAIN
                }

                val minBufSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(sampleRate / 10)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeAudioTrack = track
                track.play()

                val totalTonesPerBurst = packet.scheduledTones.size.coerceAtLeast(1)

                for (burstIdx in 1..totalBursts) {
                    if (!isActive) break

                    // Smart Early Confirmation Check: If receiver already captured & locked the packet, stop!
                    if (burstIdx > 1 && wasListeningAtStart && _singleRequestLockActive.value) {
                        _burstStatusTextAr.value =
                            "تم تأكيد الاستلام بنجاح من الدفعة ${burstIdx - 1}/$totalBursts ✓ (توقف الإرسال المتسلسل تلقائياً)"
                        break
                    }

                    _currentBurstIndex.value = burstIdx
                    _burstStatusTextAr.value = if (totalBursts > 1) {
                        "الإرسال المتسلسل الذكي (Smart Auto-Retry): جاري بث الدفعة $burstIdx من $totalBursts..."
                    } else {
                        "جاري بث الدفعة الصوتية..."
                    }

                    if (loopbackBridgeEnabled && _isListening.value) {
                        synchronized(loopbackLock) {
                            loopbackReadPos = 0
                            loopbackWritePos = 0
                        }
                        pushLoopbackSamples(ShortArray((sampleRate * 40) / 1000))
                    }

                    for ((index, tone) in packet.scheduledTones.withIndex()) {
                        if (!isActive) break
                        // If receiver completed and locked mid-burst during a retry, stop early
                        if (burstIdx > 1 && wasListeningAtStart && _singleRequestLockActive.value) {
                            break
                        }

                        _activeTransmitTone.value = tone
                        val overallProgress = (((burstIdx - 1) * totalTonesPerBurst + index).toFloat()) /
                            (totalBursts * totalTonesPerBurst).toFloat()
                        _transmitProgress.value = overallProgress.coerceIn(0f, 1f)

                        val toneSamples = (tone.durationMs * sampleRate) / 1000
                        val speakerChunk = ShortArray(toneSamples)
                        val loopbackChunk = ShortArray(toneSamples)
                        val angularFreq = 2.0 * PI * tone.frequencyHz.toDouble() / sampleRate.toDouble()

                        val loopbackAmp = if (simulateFarCrowdSignalInLoopback) {
                            // Simulates a person standing >30cm behind in a crowded queue (~155/255 < 205)
                            0.0015
                        } else {
                            // Simulates <10cm direct phone-to-phone NFC contact (>= 225/255)
                            0.92
                        }

                        for (i in 0 until toneSamples) {
                            val ramp = when {
                                i < rampSamples -> i.toDouble() / rampSamples.toDouble()
                                i > toneSamples - rampSamples ->
                                    (toneSamples - i).toDouble() / rampSamples.toDouble()
                                else -> 1.0
                            }.coerceIn(0.0, 1.0)
                            val rawSine = sin(angularFreq * i) * ramp
                            speakerChunk[i] = (rawSine * speakerGain * Short.MAX_VALUE).toInt().toShort()
                            loopbackChunk[i] = (rawSine * loopbackAmp * Short.MAX_VALUE).toInt().toShort()
                        }

                        if (loopbackBridgeEnabled && _isListening.value) {
                            pushLoopbackSamples(loopbackChunk)
                        }

                        // If microphone is not currently listening, still update the spectrum radar with the outgoing wave
                        if (!_isListening.value) {
                            val txWave = FloatArray(128) { idx ->
                                val phase = 2.0 * PI * tone.frequencyHz.toDouble() * idx / sampleRate.toDouble()
                                (sin(phase) * speakerGain).toFloat()
                            }
                            val txMag = if (nfcProximityMode) 222 else 245
                            val activeThreshold = if (nfcProximityMode) {
                                maxOf(threshold, MfskProtocol.NFC_PROXIMITY_THRESHOLD)
                            } else {
                                threshold
                            }
                            val txBars = currentMode.allProtocolTones().map { spec ->
                                val isCurrent = spec.frequencyHz == tone.frequencyHz
                                val mag = if (isCurrent) txMag else 12
                                ToneRadarBar(
                                    label = spec.shortLabel,
                                    frequencyHz = spec.frequencyHz,
                                    binIndex = MfskProtocol.frequencyToBinIndex(spec.frequencyHz),
                                    magnitude = mag,
                                    isLocked = isCurrent,
                                    isAboveThreshold = mag >= activeThreshold
                                )
                            }
                            val txRoomBars = List(36) { idx ->
                                val ratio = idx / 35f
                                val centerRatio = ((tone.frequencyHz % 6000) / 6000f).coerceIn(0.15f, 0.9f)
                                val dist = kotlin.math.abs(ratio - centerRatio)
                                if (dist < 0.06f) txMag else (35f * (1f - dist)).toInt().coerceAtLeast(8)
                            }
                            _spectrumFrame.value = SpectrumFrame(
                                bars = txBars,
                                roomBars = txRoomBars,
                                dominantLabel = tone.symbol.label,
                                dominantFreqHz = tone.frequencyHz,
                                dominantMagnitude = txMag,
                                roomPeakFreqHz = tone.frequencyHz,
                                roomPeakMagnitude = txMag,
                                proximityLevel = MfskProtocol.evaluateProximityLevel(txMag, activeThreshold),
                                estimatedDistanceCmText = "< 10 سم (إرسال الدفعة $burstIdx/$totalBursts)",
                                isHardwareMicActive = false,
                                isHighPassFilterActive = highPassFilterEnabled,
                                highPassCutoffHz = activeHighPassCutoffHz(),
                                suppressedNoisePercent = if (highPassFilterEnabled) 96 else 0,
                                waveformSamples = txWave
                            )
                        }

                        val written = track.write(speakerChunk, 0, speakerChunk.size)
                        if (written <= 0) {
                            delay(tone.durationMs.toLong())
                        }
                    }

                    if (loopbackBridgeEnabled && _isListening.value) {
                        pushLoopbackSamples(ShortArray((sampleRate * 120) / 1000))
                    }

                    // Wait 500ms (half a second) between bursts unless this was the last burst or reception succeeded
                    if (burstIdx < totalBursts && isActive) {
                        // Give loopback 140ms to finish decoding END_TONE before checking Single-Request Lock
                        delay(140L)
                        if (wasListeningAtStart && _singleRequestLockActive.value) {
                            _burstStatusTextAr.value =
                                "تم تأكيد الاستلام بنجاح من الدفعة $burstIdx/$totalBursts ✓ (توقف الإرسال المتسلسل تلقائياً)"
                            break
                        }
                        _activeTransmitTone.value = null
                        _burstStatusTextAr.value =
                            "فاصل الأمان (0.5 ثانية) قبل بث الدفعة ${burstIdx + 1} من $totalBursts لضمان عدم ضياع النبضة..."
                        delay(SMART_RETRY_INTERVAL_MS - 140L)
                    } else if (burstIdx == totalBursts) {
                        _burstStatusTextAr.value = if (totalBursts > 1) {
                            "اكتمل البث المتسلسل الذكي ($totalBursts/$totalBursts دفعات بفاصل 0.5 ثانية) ✓"
                        } else {
                            "اكتمل البث الصوتي ✓"
                        }
                    }
                }

                _transmitProgress.value = 1f
                delay(200)
                track.stop()
                track.release()
                activeAudioTrack = null
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                try {
                    activeAudioTrack?.release()
                } catch (_: Exception) {}
                activeAudioTrack = null
                _activeTransmitTone.value = null
                _isTransmitting.value = false
                _transmitProgress.value = 0f
                _currentBurstIndex.value = 0
                simulateFarCrowdSignalInLoopback = false
                if (!_isListening.value) {
                    _spectrumFrame.value = SpectrumFrame(
                        isHighPassFilterActive = highPassFilterEnabled,
                        highPassCutoffHz = activeHighPassCutoffHz()
                    )
                }
            }
        }
    }

    fun stopTransmitting() {
        transmitterJob?.cancel()
        transmitterJob = null
        simulateFarCrowdSignalInLoopback = false
        try {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
        } catch (_: Exception) {}
        activeAudioTrack = null
        _activeTransmitTone.value = null
        _isTransmitting.value = false
        _transmitProgress.value = 0f
        _currentBurstIndex.value = 0
        _burstStatusTextAr.value = null
        if (!_isListening.value) {
            _spectrumFrame.value = SpectrumFrame(
                isHighPassFilterActive = highPassFilterEnabled,
                highPassCutoffHz = activeHighPassCutoffHz()
            )
        }
    }


    private fun pushLoopbackSamples(samples: ShortArray) {
        synchronized(loopbackLock) {
            for (s in samples) {
                loopbackRingBuffer[loopbackWritePos] = s
                loopbackWritePos = (loopbackWritePos + 1) % loopbackRingBuffer.size
                if (loopbackWritePos == loopbackReadPos) {
                    loopbackReadPos = (loopbackReadPos + 1) % loopbackRingBuffer.size
                }
            }
        }
    }

    private fun popLoopbackSamples(count: Int): ShortArray {
        val out = ShortArray(count)
        synchronized(loopbackLock) {
            for (i in 0 until count) {
                if (loopbackReadPos != loopbackWritePos) {
                    out[i] = loopbackRingBuffer[loopbackReadPos]
                    loopbackReadPos = (loopbackReadPos + 1) % loopbackRingBuffer.size
                } else {
                    out[i] = 0
                }
            }
        }
        return out
    }

    @SuppressLint("MissingPermission")
    fun startListening(initialStatus: ReceiverStatus = ReceiverStatus.LISTENING) {
        if (_isListening.value) return
        _isListening.value = true
        _singleRequestLockActive.value = false
        _lastBlockedCrowdAlert.value = null
        _receiverStatus.value = initialStatus
        _liveAccumulatedHex.value = ""
        _livePreviewText.value = ""
        _liveDetectedSymbols.value = emptyList()

        receiverJob?.cancel()
        receiverJob = scope.launch(Dispatchers.Default) {
            val sampleRate = MfskProtocol.SAMPLE_RATE
            val fftSize = MfskProtocol.FFT_SIZE
            val stepSamples = (sampleRate * 20) / 1000

            var audioRecord: AudioRecord? = null
            var selectedAudioSource = MediaRecorder.AudioSource.MIC

            try {
                // Safe, crash-proof AudioRecord initialization without native AudioEffect hooks
                val sourcesToTry = listOf(
                    MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION
                )
                val ratesToTry = listOf(MfskProtocol.SAMPLE_RATE, 44100)

                outerLoop@ for (rate in ratesToTry) {
                    val rawMinBuf = try {
                        AudioRecord.getMinBufferSize(
                            rate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                    } catch (_: Throwable) {
                        -1
                    }
                    if (rawMinBuf <= 0) continue
                    val minBuf = rawMinBuf.coerceAtLeast(fftSize * 4)

                    for (source in sourcesToTry) {
                        try {
                            val candidate = AudioRecord(
                                source,
                                rate,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                minBuf
                            )
                            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                                audioRecord = candidate
                                selectedAudioSource = source
                                break@outerLoop
                            } else {
                                candidate.release()
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }

                var isHwRecording = false
                audioRecord?.let { rec ->
                    try {
                        rec.startRecording()
                        isHwRecording = rec.recordingState == AudioRecord.RECORDSTATE_RECORDING
                    } catch (_: Throwable) {
                        isHwRecording = false
                    }
                }

                // Calibrate mic preamp boost if UNPROCESSED was selected
                val micPreampBoost = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    selectedAudioSource == MediaRecorder.AudioSource.UNPROCESSED
                ) 4.5f else 2.2f

                val slidingWindow = FloatArray(fftSize)
                val hannWindow = FloatArray(fftSize) { i ->
                    (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
                }
                val real = FloatArray(fftSize)
                val imag = FloatArray(fftSize)
                val smoothedMagnitudes = FloatArray(fftSize / 2)
                val byteFrequencyData = IntArray(fftSize / 2)
                val readBuffer = ShortArray(stepSamples)
                val biquadHighPassFilter = BiquadHighPassFilter(sampleRate)

                var candidateSymbol: ToneSymbol? = null
                var candidateStreak = 0
                var farCrowdRejectionStreak = 0
                var lastRegisteredSymbol: ToneSymbol? = null
                var isPacketActive = false
                val hexBuffer = StringBuilder()
                val detectedList = mutableListOf<String>()

                while (isActive && _isListening.value) {
                    val loopStartMs = System.currentTimeMillis()
                    val readCount = audioRecord?.read(readBuffer, 0, stepSamples) ?: 0
                    val loopbackChunk = popLoopbackSamples(stepSamples)

                    val mode = currentMode
                    val isHpActive = highPassFilterEnabled
                    val cutoffHz = activeHighPassCutoffHz(mode)
                    biquadHighPassFilter.configureHighPass(cutoffHz)

                    System.arraycopy(slidingWindow, stepSamples, slidingWindow, 0, fftSize - stepSamples)
                    val tailStart = fftSize - stepSamples
                    var rawEnergySum = 0.0
                    var filteredEnergySum = 0.0

                    for (i in 0 until stepSamples) {
                        val micVal = if (i < readCount) {
                            (readBuffer[i].toFloat() / 32768f) * micPreampBoost
                        } else {
                            0f
                        }
                        val loopVal = loopbackChunk[i].toFloat() / 32768f
                        val rawSample = (micVal + loopVal).coerceIn(-1f, 1f)
                        rawEnergySum += (rawSample * rawSample).toDouble()

                        // Apply Web Audio API equivalent BiquadFilterNode (type = "highpass")
                        val processedSample = if (isHpActive) {
                            biquadHighPassFilter.processSample(rawSample)
                        } else {
                            rawSample
                        }
                        filteredEnergySum += (processedSample * processedSample).toDouble()
                        slidingWindow[tailStart + i] = processedSample
                    }

                    for (i in 0 until fftSize) {
                        real[i] = slidingWindow[i] * hannWindow[i]
                        imag[i] = 0f
                    }

                    computeRadix2Fft(real, imag, fftSize)

                    // Web Audio API AnalyserNode getByteFrequencyData mapping (0..255)
                    val smoothing = MfskProtocol.SMOOTHING_TIME_CONSTANT
                    val minDecibels = -96f
                    val maxDecibels = -32f
                    val dbRange = maxDecibels - minDecibels
                    val cutoffBin = MfskProtocol.frequencyToBinIndex(cutoffHz)

                    var roomPeakMag = 0
                    var roomPeakBin = 0

                    for (bin in 0 until fftSize / 2) {
                        val rawMag = sqrt(real[bin] * real[bin] + imag[bin] * imag[bin]) / (fftSize / 4f)
                        // Strictly block general room/voice frequencies below the device-to-device carrier band
                        val mag = if (isHpActive && bin < cutoffBin) {
                            0f
                        } else {
                            rawMag
                        }
                        val smoothed = smoothing * smoothedMagnitudes[bin] + (1f - smoothing) * mag
                        smoothedMagnitudes[bin] = smoothed

                        val db = if (smoothed > 1e-7f) 20f * (ln(smoothed) / ln(10f)) else minDecibels
                        val normalized = ((db - minDecibels) / dbRange).coerceIn(0f, 1f)
                        val byteVal = (normalized * 255f).roundToInt().coerceIn(0, 255)
                        byteFrequencyData[bin] = byteVal

                        if (bin >= cutoffBin && byteVal > roomPeakMag) {
                            roomPeakMag = byteVal
                            roomPeakBin = bin
                        }
                    }

                    val roomPeakFreqHz = if (roomPeakMag > 0) {
                        ((roomPeakBin.toDouble() * sampleRate) / fftSize).roundToInt()
                    } else 0

                    // Never listen to general room sound — only track the device-to-device band
                    val roomSpectrumBars = List(36) { 0 }

                    val activeThreshold = if (nfcProximityMode) {
                        maxOf(threshold, MfskProtocol.NFC_PROXIMITY_THRESHOLD)
                    } else {
                        threshold
                    }
                    val toneSpecs = mode.allProtocolTones()

                    var strongestSpec = toneSpecs.first()
                    var strongestMag = -1

                    val rawBars = toneSpecs.map { spec ->
                        val mag = MfskProtocol.readPeakBinMagnitude(byteFrequencyData, spec.frequencyHz)
                        if (mag > strongestMag) {
                            strongestMag = mag
                            strongestSpec = spec
                        }
                        ToneRadarBar(
                            label = spec.shortLabel,
                            frequencyHz = spec.frequencyHz,
                            binIndex = MfskProtocol.frequencyToBinIndex(spec.frequencyHz),
                            magnitude = mag,
                            isLocked = false,
                            isAboveThreshold = mag >= activeThreshold
                        )
                    }

                    val effectiveProximityMag = max(0, strongestMag)
                    val proximityLevel = MfskProtocol.evaluateProximityLevel(
                        magnitude = effectiveProximityMag,
                        threshold = activeThreshold
                    )
                    val estimatedDistanceCmText = when {
                        effectiveProximityMag >= activeThreshold ->
                            "< 10 سم (تلاصق مباشر آمن ✓)"
                        effectiveProximityMag >= 120 ->
                            "15 – 40 سم (مرفوض في الزحام: $effectiveProximityMag < $activeThreshold)"
                        else ->
                            "> 40 سم (بعيد)"
                    }

                    var lockedLabel: String? = null
                    if (strongestMag >= activeThreshold) {
                        farCrowdRejectionStreak = 0
                        if (candidateSymbol == strongestSpec.symbol) {
                            candidateStreak++
                        } else {
                            candidateSymbol = strongestSpec.symbol
                            candidateStreak = 1
                        }

                        if (candidateStreak >= MfskProtocol.DEBOUNCE_CONSECUTIVE_READS) {
                            val confirmedSymbol = strongestSpec.symbol
                            lockedLabel = strongestSpec.shortLabel

                            when (confirmedSymbol) {
                                is ToneSymbol.Start -> {
                                    if (!isPacketActive) {
                                        isPacketActive = true
                                        lastRegisteredSymbol = ToneSymbol.Start
                                        hexBuffer.clear()
                                        detectedList.clear()
                                        detectedList.add("START")
                                        _liveAccumulatedHex.value = ""
                                        _livePreviewText.value = ""
                                        _liveDetectedSymbols.value = detectedList.toList()
                                        _receiverStatus.value = ReceiverStatus.RECEIVING
                                    }
                                }
                                is ToneSymbol.Sync -> {
                                    if (isPacketActive && lastRegisteredSymbol != ToneSymbol.Sync) {
                                        lastRegisteredSymbol = ToneSymbol.Sync
                                        detectedList.add("SYNC")
                                        _liveDetectedSymbols.value = detectedList.toList()
                                    }
                                }
                                is ToneSymbol.Data -> {
                                    if (isPacketActive && lastRegisteredSymbol != confirmedSymbol) {
                                        lastRegisteredSymbol = confirmedSymbol
                                        hexBuffer.append(confirmedSymbol.hexChar)
                                        detectedList.add(confirmedSymbol.hexChar.toString())
                                        val currentHex = hexBuffer.toString()
                                        _liveAccumulatedHex.value = currentHex
                                        _livePreviewText.value = MfskProtocol.decodeLivePreview(
                                            accumulatedHex = currentHex,
                                            decrypt = encryptionEnabled,
                                            secretPin = receiverSecretPin
                                        )
                                        _liveDetectedSymbols.value = detectedList.takeLast(24)
                                    }
                                }
                                is ToneSymbol.End -> {
                                    if (isPacketActive && hexBuffer.isNotEmpty()) {
                                        isPacketActive = false
                                        lastRegisteredSymbol = ToneSymbol.End
                                        detectedList.add("END")
                                        _liveDetectedSymbols.value = detectedList.takeLast(24)
                                        val finalHex = hexBuffer.toString()
                                        val decoded = MfskProtocol.decodeHexPacket(
                                            rawHexReceived = finalHex,
                                            decrypt = encryptionEnabled,
                                            secretPin = receiverSecretPin
                                        )
                                        // SINGLE-REQUEST LOCK: Immediately stop & lock microphone
                                        // upon capturing the first valid packet to prevent queue interference!
                                        _isListening.value = false
                                        _singleRequestLockActive.value = true
                                        _receiverStatus.value = ReceiverStatus.PACKET_COMPLETE
                                        withContext(Dispatchers.Main) {
                                            onPacketDecoded(decoded, mode)
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        candidateSymbol = null
                        candidateStreak = 0
                        // Crowd Proximity Shield (< 10cm): Ignore & log any MFSK tone arriving from > 10cm (115..204)
                        if (strongestMag in 115 until activeThreshold) {
                            farCrowdRejectionStreak++
                            if (farCrowdRejectionStreak == 2) {
                                _blockedCrowdSignalsCount.value = _blockedCrowdSignalsCount.value + 1
                                _lastBlockedCrowdAlert.value =
                                    "تم حجب إشارة صوتية من مسافة أبعد في الزحام (القوة $strongestMag/255 < $activeThreshold • مسافة > 10 سم)"
                            }
                        } else {
                            farCrowdRejectionStreak = 0
                        }
                    }

                    val wavePreview = FloatArray(128) { idx ->
                        slidingWindow[fftSize - 128 + idx]
                    }

                    val updatedBars = rawBars.map { bar ->
                        bar.copy(isLocked = bar.label == lockedLabel)
                    }

                    val suppressedPercent = if (!isHpActive) {
                        0
                    } else if (rawEnergySum > 1e-6 && rawEnergySum > filteredEnergySum) {
                        (((rawEnergySum - filteredEnergySum) / rawEnergySum) * 100.0).roundToInt().coerceIn(75, 99)
                    } else {
                        96
                    }

                    _spectrumFrame.value = SpectrumFrame(
                        bars = updatedBars,
                        roomBars = roomSpectrumBars,
                        dominantLabel = if (strongestMag >= activeThreshold) strongestSpec.shortLabel else null,
                        dominantFreqHz = if (strongestMag >= activeThreshold) strongestSpec.frequencyHz else null,
                        dominantMagnitude = effectiveProximityMag,
                        roomPeakFreqHz = roomPeakFreqHz,
                        roomPeakMagnitude = roomPeakMag,
                        proximityLevel = proximityLevel,
                        estimatedDistanceCmText = estimatedDistanceCmText,
                        isHardwareMicActive = isHwRecording && readCount > 0,
                        isHighPassFilterActive = isHpActive,
                        highPassCutoffHz = cutoffHz,
                        suppressedNoisePercent = suppressedPercent,
                        waveformSamples = wavePreview
                    )

                    val elapsed = System.currentTimeMillis() - loopStartMs
                    val waitMs = (20L - elapsed).coerceAtLeast(4L)
                    delay(waitMs)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            } finally {
                try {
                    if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                    }
                } catch (_: Throwable) {
                }
                try {
                    audioRecord?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun stopListening(statusAfterStop: ReceiverStatus = ReceiverStatus.IDLE) {
        _isListening.value = false
        receiverJob?.cancel()
        receiverJob = null
        _receiverStatus.value = statusAfterStop
        _spectrumFrame.value = SpectrumFrame()
    }

    fun resetReceiverPacketState() {
        _liveAccumulatedHex.value = ""
        _livePreviewText.value = ""
        _liveDetectedSymbols.value = emptyList()
        _lastBlockedCrowdAlert.value = null
        if (_isListening.value) {
            _receiverStatus.value = ReceiverStatus.LISTENING
        } else {
            _receiverStatus.value = ReceiverStatus.IDLE
        }
    }


    fun playFinancialSuccessChime() {
        scope.launch(Dispatchers.Default) {
            playSynthesizedNotes(
                notes = listOf(880 to 70, 1175 to 70, 1760 to 120)
            )
        }
    }

    fun playSecurityErrorBuzzer() {
        scope.launch(Dispatchers.Default) {
            playSynthesizedNotes(
                notes = listOf(280 to 110, 190 to 160)
            )
        }
    }

    private fun playSynthesizedNotes(notes: List<Pair<Int, Int>>) {
        try {
            val sampleRate = MfskProtocol.SAMPLE_RATE
            val rampSamples = (8 * sampleRate) / 1000
            val totalSamples = notes.sumOf { (_, durMs) -> (durMs * sampleRate) / 1000 }
            val pcm = ShortArray(totalSamples)
            var offset = 0
            for ((freq, durMs) in notes) {
                val count = (durMs * sampleRate) / 1000
                val angular = 2.0 * PI * freq.toDouble() / sampleRate.toDouble()
                for (i in 0 until count) {
                    val gain = when {
                        i < rampSamples -> i.toDouble() / rampSamples.toDouble()
                        i > count - rampSamples -> (count - i).toDouble() / rampSamples.toDouble()
                        else -> 1.0
                    }.coerceIn(0.0, 1.0)
                    val sampleVal = sin(angular * i) * gain * 0.75
                    if (offset + i < pcm.size) {
                        pcm[offset + i] = (sampleVal * Short.MAX_VALUE).toInt().toShort()
                    }
                }
                offset += count
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(notes.sumOf { it.second }.toLong() + 40L)
            track.stop()
            track.release()
        } catch (_: Exception) {
        }
    }

    private fun computeRadix2Fft(real: FloatArray, imag: FloatArray, n: Int) {
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tr = real[i]
                real[i] = real[j]
                real[j] = tr
                val ti = imag[i]
                imag[i] = imag[j]
                imag[j] = ti
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = (-2.0 * PI / len.toDouble())
            val wLenR = cos(angle).toFloat()
            val wLenI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1f
                var wI = 0f
                for (k in 0 until halfLen) {
                    val uIdx = i + k
                    val vIdx = i + k + halfLen
                    val tR = wR * real[vIdx] - wI * imag[vIdx]
                    val tI = wR * imag[vIdx] + wI * real[vIdx]

                    real[vIdx] = real[uIdx] - tR
                    imag[vIdx] = imag[uIdx] - tI
                    real[uIdx] = real[uIdx] + tR
                    imag[uIdx] = imag[uIdx] + tI

                    val nextWR = wR * wLenR - wI * wLenI
                    wI = wR * wLenI + wI * wLenR
                    wR = nextWR
                }
                i += len
            }
            len = len shl 1
        }
    }
}
