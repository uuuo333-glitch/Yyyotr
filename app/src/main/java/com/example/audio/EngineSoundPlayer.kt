package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.EngineAlertSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class EngineSoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    private val _playingSound = MutableStateFlow<EngineAlertSound?>(null)
    val playingSound: StateFlow<EngineAlertSound?> = _playingSound.asStateFlow()

    fun play(sound: EngineAlertSound) {
        activeJob?.cancel()
        activeJob = scope.launch {
            _playingSound.value = sound
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val durationSec = 1.75
                val totalSamples = (sampleRate * durationSec).toInt()
                val pcm = ShortArray(totalSamples)

                var phase1 = 0.0
                var phase2 = 0.0
                var phaseTurbo = 0.0

                for (i in 0 until totalSamples) {
                    if (!isActive) break
                    val t = i.toDouble() / sampleRate
                    val progress = t / durationSec
                    val envelope = when {
                        progress < 0.05 -> progress / 0.05
                        progress > 0.88 -> (1.0 - progress) / 0.12
                        else -> 1.0
                    }.coerceIn(0.0, 1.0)

                    val sampleValue = when (sound) {
                        EngineAlertSound.POPS_BANGS -> {
                            // Exhaust rev followed by 4 loud backfire pops & bangs
                            val baseFreq = 95.0 + (1.0 - progress) * 140.0
                            phase1 += 2.0 * PI * baseFreq / sampleRate
                            val popTimes = doubleArrayOf(0.32, 0.54, 0.72, 0.86)
                            var popBurst = 0.0
                            for (pt in popTimes) {
                                val dt = progress - pt
                                if (dt in 0.0..0.09) {
                                    val decay = exp(-dt * 45.0)
                                    popBurst += decay * ((Random.nextDouble() - 0.5) * 1.9 + sin(2.0 * PI * 75.0 * dt))
                                }
                            }
                            (0.45 * sin(phase1) + popBurst) * envelope
                        }

                        EngineAlertSound.REV_LIMITER -> {
                            // Rapid engine rev limiter cut-off (taktat / tqattee3)
                            val cutGate = if (sin(2.0 * PI * 14.5 * t) > -0.15) 1.0 else 0.12
                            val freq = 145.0 + 185.0 * progress + 30.0 * sin(2.0 * PI * 14.5 * t)
                            phase1 += 2.0 * PI * freq / sampleRate
                            phase2 += 2.0 * PI * (freq * 2.0) / sampleRate
                            val pop = if (cutGate < 0.2) (Random.nextDouble() - 0.5) * 0.5 else 0.0
                            ((0.6 * sin(phase1) + 0.35 * sin(phase2)) * cutGate + pop) * envelope
                        }

                        EngineAlertSound.ROCKET -> {
                            // Supersonic rocket ignition roar + Doppler whoosh
                            val roarFreq = 70.0 + progress * 520.0
                            phase1 += 2.0 * PI * roarFreq / sampleRate
                            phaseTurbo += 2.0 * PI * (400.0 + progress * 1900.0) / sampleRate
                            val noise = (Random.nextDouble() - 0.5) * (0.45 + 0.45 * progress)
                            val doppler = sin(progress * PI)
                            (0.45 * sin(phase1) + 0.3 * sin(phaseTurbo) + noise) * (0.4 + 0.6 * doppler) * envelope
                        }

                        EngineAlertSound.SIREN_SCREAM -> {
                            // Dramatic high-pitched scream/whine sweep
                            val screamFreq = 620.0 + 980.0 * sin(progress * PI * 1.8) + 120.0 * sin(2.0 * PI * 12.0 * t)
                            phase1 += 2.0 * PI * screamFreq / sampleRate
                            phase2 += 2.0 * PI * (screamFreq * 1.5) / sampleRate
                            (0.6 * sin(phase1) + 0.3 * sin(phase2)) * envelope
                        }

                        EngineAlertSound.DRAG_LAUNCH -> {
                            // 2-step launch stutter for first 0.45s then explosive full-throttle acceleration
                            if (progress < 0.45) {
                                val pulse = if (sin(2.0 * PI * 16.0 * t) > 0) 1.0 else 0.25
                                phase1 += 2.0 * PI * 130.0 / sampleRate
                                (0.75 * sin(phase1) + (Random.nextDouble() - 0.5) * 0.25) * pulse * envelope
                            } else {
                                val p = (progress - 0.45) / 0.55
                                val freq = 130.0 + p * 340.0
                                phase1 += 2.0 * PI * freq / sampleRate
                                phase2 += 2.0 * PI * (freq * 2.0) / sampleRate
                                (0.65 * sin(phase1) + 0.35 * sin(phase2)) * envelope
                            }
                        }

                        EngineAlertSound.MOTORCYCLE -> {
                            val gearProgress = if (progress < 0.55) progress / 0.55 else (progress - 0.58).coerceAtLeast(0.0) / 0.42
                            val baseFreq = 115.0 + gearProgress * 260.0
                            phase1 += 2.0 * PI * baseFreq / sampleRate
                            phase2 += 2.0 * PI * (baseFreq * 2.0) / sampleRate
                            val crackle = if (progress in 0.54..0.59) (Random.nextDouble() - 0.5) * 0.45 else 0.0
                            (0.6 * sin(phase1) + 0.3 * sin(phase2) + 0.15 * sin(phase1 * 3.0) + crackle) * envelope
                        }

                        EngineAlertSound.SPORTBIKE -> {
                            val freq = 180.0 + sin(progress * PI) * 340.0
                            phase1 += 2.0 * PI * freq / sampleRate
                            phase2 += 2.0 * PI * (freq * 1.5) / sampleRate
                            (0.55 * sin(phase1) + 0.35 * sin(phase2) + 0.2 * sin(phase1 * 2.0)) * envelope
                        }

                        EngineAlertSound.TURBO -> {
                            val spoolFreq = if (progress < 0.65) 80.0 + (progress / 0.65) * 150.0 else 110.0 - (progress - 0.65) * 80.0
                            val whistleFreq = 1400.0 + progress * 1100.0
                            phase1 += 2.0 * PI * spoolFreq / sampleRate
                            phaseTurbo += 2.0 * PI * whistleFreq / sampleRate
                            val engine = 0.65 * sin(phase1) + 0.25 * sin(phase1 * 2.0)
                            val blowOff = if (progress > 0.64) {
                                val flutterEnv = 0.5 + 0.5 * sin((progress - 0.64) * 55.0)
                                flutterEnv * 0.35 * sin(phaseTurbo) + (Random.nextDouble() - 0.5) * 0.22 * flutterEnv
                            } else {
                                0.12 * sin(phaseTurbo) * progress
                            }
                            (engine + blowOff) * envelope
                        }

                        EngineAlertSound.V8 -> {
                            val rumbleFreq = 58.0 + sin(progress * PI) * 85.0
                            val chop = 0.7 + 0.3 * sin(2.0 * PI * 14.0 * t)
                            phase1 += 2.0 * PI * rumbleFreq / sampleRate
                            phase2 += 2.0 * PI * (rumbleFreq * 1.5) / sampleRate
                            (0.7 * sin(phase1) + 0.35 * sin(phase2) + 0.2 * sin(phase1 * 2.0)) * chop * envelope
                        }
                    }

                    pcm[i] = (sampleValue.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.82).toInt().toShort()
                }

                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ).coerceAtLeast(pcm.size * 2)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(minBuf.coerceAtLeast(pcm.size * 2))
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                kotlinx.coroutines.delay((durationSec * 1000).toLong())
            } catch (_: Throwable) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
                if (_playingSound.value == sound) {
                    _playingSound.value = null
                }
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        _playingSound.value = null
    }
}
