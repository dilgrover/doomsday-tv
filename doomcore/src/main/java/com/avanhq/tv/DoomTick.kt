package com.avanhq.tv

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The countdown's soft clock tick, made here: a short
 * click of noise and a high wooden knock, about 30 ms, played quietly (a tenth of full volume,
 * -20 dB) once a second. No sound file, nothing to license. Whoever shows the countdown decides
 * when it may play (never at night, never in the screensaver).
 */
class DoomTick {
    private companion object {
        const val RATE = 22_050
        const val VOLUME = 0.1f
    }

    private val track: AudioTrack? = runCatching {
        val n = RATE * 30 / 1000
        val rnd = Random(7)
        val pcm = ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val env = exp(-t * 180)
            val knock = sin(2 * PI * 2_400 * t) * 0.6 + sin(2 * PI * 1_150 * t) * 0.3
            val click = (rnd.nextDouble() * 2 - 1) * exp(-t * 900) * 0.5
            ((knock * env + click) * 0.8 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(n * 2)
            .build()
            .apply { write(pcm, 0, n); setVolume(VOLUME) }
    }.getOrNull()

    /** How many ticks have played (for tests). */
    var played = 0
        private set

    fun tick() {
        val t = track ?: return
        runCatching {
            t.stop()
            t.reloadStaticData()
            t.play()
            played++
        }
    }

    fun release() {
        runCatching { track?.release() }
    }
}
