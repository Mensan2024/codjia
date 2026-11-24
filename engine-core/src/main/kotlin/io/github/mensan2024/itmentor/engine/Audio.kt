// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Son PCM 16 bits, échantillons entrelacés (G, D, G, D… en stéréo). */
class Pcm(val sampleRate: Int, val channels: Int, val samples: ShortArray) {
    /** Son mono. */
    constructor(sampleRate: Int, samples: ShortArray) : this(sampleRate, 1, samples)

    init { require(channels in 1..2 && sampleRate > 0 && samples.size % channels == 0) }

    val frames: Int get() = samples.size / channels
    val durationMs: Long get() = frames * 1000L / sampleRate
}

/** Lecture et écriture WAV (PCM 16 bits, mono ou stéréo). */
object Wav {
    fun read(bytes: ByteArray): Pcm {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(bytes.size >= 12 && tag(bytes, 0) == "RIFF" && tag(bytes, 8) == "WAVE") { "Fichier WAV invalide" }
        var pos = 12
        var channels = 0
        var rate = 0
        var bits = 0
        var format = 0
        while (pos + 8 <= bytes.size) {
            val id = tag(bytes, pos)
            var size = b.getInt(pos + 4)
            val body = pos + 8
            // Certains moteurs laissent une taille provisoire (0 ou -1) : on prend alors le reste du fichier.
            if (size <= 0 || body + size > bytes.size) size = bytes.size - body
            when (id) {
                "fmt " -> {
                    format = b.getShort(body).toInt() and 0xFFFF
                    channels = b.getShort(body + 2).toInt()
                    rate = b.getInt(body + 4)
                    bits = b.getShort(body + 14).toInt()
                }
                "data" -> {
                    require(format == 1 && bits == 16) { "Seul le PCM 16 bits est pris en charge (format $format, $bits bits)" }
                    require(channels in 1..2 && rate > 0) { "En-tête WAV incohérent" }
                    val count = (size / (2 * channels)) * channels
                    val out = ShortArray(count) { b.getShort(body + it * 2) }
                    return Pcm(rate, channels, out)
                }
            }
            pos = body + size + (size and 1)
        }
        error("Aucune donnée audio dans le WAV")
    }

    fun write(pcm: Pcm): ByteArray {
        val dataSize = pcm.samples.size * 2
        val b = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(pcm.channels.toShort()).putInt(pcm.sampleRate)
            .putInt(pcm.sampleRate * 2 * pcm.channels).putShort((2 * pcm.channels).toShort()).putShort(16)
        b.put("data".toByteArray()).putInt(dataSize)
        for (s in pcm.samples) b.putShort(s)
        return b.array()
    }

    private fun tag(bytes: ByteArray, at: Int) = String(bytes, at, 4, Charsets.US_ASCII)
}

object AudioMix {
    /** Convertit en mono (moyenne des canaux). */
    fun toMono(p: Pcm): Pcm = if (p.channels == 1) p
    else Pcm(p.sampleRate, 1, ShortArray(p.frames) { ((p.samples[2 * it].toInt() + p.samples[2 * it + 1]) / 2).toShort() })

    /** Rééchantillonnage linéaire, canal par canal. */
    fun resample(p: Pcm, rate: Int): Pcm {
        if (p.sampleRate == rate || p.samples.isEmpty()) return Pcm(rate, p.channels, p.samples)
        val c = p.channels
        val n = (p.frames.toLong() * rate / p.sampleRate).toInt()
        val out = ShortArray(n * c)
        val ratio = p.sampleRate.toDouble() / rate
        for (i in 0 until n) {
            val x = i * ratio
            val i0 = x.toInt().coerceAtMost(p.frames - 1)
            val i1 = min(i0 + 1, p.frames - 1)
            val f = x - i0
            for (ch in 0 until c) {
                out[i * c + ch] = (p.samples[i0 * c + ch] * (1 - f) + p.samples[i1 * c + ch] * f).roundToInt().toShort()
            }
        }
        return Pcm(rate, c, out)
    }

    /**
     * Place chaque extrait mono à son instant (ms) sur une piste mono de `totalMs`, puis normalise :
     * crête ramenée vers −1 dBFS, gain limité à ×2,5 pour ne pas amplifier le souffle.
     */
    fun place(totalMs: Long, clips: List<Pair<Long, Pcm>>): Pcm {
        val rate = clips.firstOrNull()?.second?.sampleRate ?: 24_000
        val total = IntArray((totalMs * rate / 1000).toInt())
        for ((startMs, clip) in clips) {
            val c = resample(toMono(clip), rate)
            val off = (startMs * rate / 1000).toInt()
            for (i in c.samples.indices) {
                val j = off + i
                if (j < 0 || j >= total.size) continue
                total[j] += c.samples[i].toInt()
            }
        }
        var peak = 1
        for (v in total) peak = max(peak, abs(v))
        val gain = min(2.5, 29_000.0 / peak)
        return Pcm(rate, 1, ShortArray(total.size) { (total[it] * gain).roundToInt().coerceIn(-32768, 32767).toShort() })
    }

    /**
     * Fond musical de la vidéo : coupé exactement à `totalMs`, fondu d'entrée court, fondu de sortie
     * en courbe douce qui se termine pile sur la dernière image. Si la musique est plus courte
     * que la vidéo, elle est rebouclée avec un fondu enchaîné.
     */
    fun musicBed(music: Pcm, totalMs: Long, gain: Float = 1f, fadeInMs: Long = 250, fadeOutMs: Long = 1800): Pcm {
        val c = music.channels
        val rate = music.sampleRate
        val frames = (totalMs * rate / 1000).toInt()
        val out = ShortArray(frames * c)
        // Boucle : la piste se répète sur [0, L) ; à chaque raccord, la fin [L, L + xfade) s'efface
        // pendant que le début revient (fondu enchaîné d'au plus 1 s).
        val xfade = min(rate, music.frames / 4)
        val loopLen = music.frames - xfade
        val fi = (fadeInMs * rate / 1000).toInt().coerceAtLeast(1)
        val fo = (fadeOutMs * rate / 1000).toInt().coerceAtLeast(1)
        for (i in 0 until frames) {
            val p = if (i < loopLen) i else i % loopLen
            val wrapped = i >= loopLen && p < xfade
            var g = gain.toDouble()
            if (i < fi) g *= i.toDouble() / fi
            val toEnd = frames - 1 - i
            if (toEnd < fo) { val k = toEnd.toDouble() / fo; g *= k * k }
            for (ch in 0 until c) {
                var v = music.samples[p * c + ch].toDouble()
                if (wrapped) {
                    val k = p.toDouble() / xfade
                    v = v * k + music.samples[(loopLen + p) * c + ch] * (1 - k)
                }
                out[i * c + ch] = (v * g).roundToInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return Pcm(rate, c, out)
    }
}
