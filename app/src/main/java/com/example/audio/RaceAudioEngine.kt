package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight procedural PCM engine sound + tactile haptics controller.
 * Produces real dynamic RPM engine hum, nitro whine, and collision/near-miss audio
 * completely offline with zero external media asset dependencies.
 */
class RaceAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engineJob: Job? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var targetRpmNorm: Float = 0.15f

    @Volatile
    private var isNitroActive: Boolean = false

    @Volatile
    private var isMuted: Boolean = true

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }.getOrNull()
    }

    fun startEngine(enabled: Boolean) {
        isMuted = !enabled
        if (!enabled || engineJob?.isActive == true) return
        engineJob = scope.launch {
            runCatching {
                val sampleRate = 22050
                val bufferSamples = 1024
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(bufferSamples * 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val buffer = ShortArray(bufferSamples)
                var phase1 = 0.0
                var phase2 = 0.0
                var phaseTurbo = 0.0

                while (isActive) {
                    if (isMuted) {
                        buffer.fill(0)
                        track.write(buffer, 0, buffer.size)
                        continue
                    }
                    val rpm = targetRpmNorm.coerceIn(0f, 1f)
                    val baseFreq = 58.0 + rpm * 155.0 + if (isNitroActive) 32.0 else 0.0
                    val harmonicFreq = baseFreq * 2.0
                    val turboFreq = 420.0 + rpm * 680.0
                    val gain = 1800.0 + rpm * 1400.0

                    for (i in 0 until bufferSamples) {
                        phase1 += 2.0 * PI * baseFreq / sampleRate
                        phase2 += 2.0 * PI * harmonicFreq / sampleRate
                        phaseTurbo += 2.0 * PI * turboFreq / sampleRate

                        if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                        if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                        if (phaseTurbo > 2.0 * PI) phaseTurbo -= 2.0 * PI

                        val engineWave = 0.65 * sin(phase1) + 0.35 * sin(phase2)
                        val turboWave = if (isNitroActive) 0.28 * sin(phaseTurbo) else 0.0
                        val sample = ((engineWave + turboWave) * gain).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        buffer[i] = sample.toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
            }
        }
    }

    fun updateTelemetry(speedRatio: Float, nitro: Boolean, soundEnabled: Boolean) {
        isMuted = !soundEnabled
        targetRpmNorm = speedRatio
        isNitroActive = nitro
        if (soundEnabled && engineJob?.isActive != true) {
            startEngine(true)
        }
    }

    fun stopEngine() {
        engineJob?.cancel()
        engineJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
    }

    fun pulseHaptic(hapticsEnabled: Boolean, heavy: Boolean = false) {
        if (!hapticsEnabled) return
        runCatching {
            val vib = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (heavy) 70L else 28L
                val amp = if (heavy) 210 else 110
                vib.vibrate(VibrationEffect.createOneShot(duration, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(if (heavy) 70L else 28L)
            }
        }
    }
}
