// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Son PCM 16 bits mono. */
class Pcm(val sampleRate: Int, val samples: ShortArray) {
    val durationMs: Long get() = samples.size * 1000L / sampleRate
}

/** Lecture et écriture WAV (PCM 16 bits). Suffisant pour les fichiers produits par la synthèse vocale d'Android. */
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
                    val frames = size / (2 * channels)
                    val out = ShortArray(frames)
                    for (i in 0 until frames) {
                        val o = body + i * 2 * channels
                        out[i] = if (channels == 1) b.getShort(o)
                        else ((b.getShort(o).toInt() + b.getShort(o + 2).toInt()) / 2).toShort()
                    }
                    return Pcm(rate, out)
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
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(pcm.sampleRate)
            .putInt(pcm.sampleRate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(dataSize)
        for (s in pcm.samples) b.putShort(s)
        return b.array()
    }

    private fun tag(bytes: ByteArray, at: Int) = String(bytes, at, 4, Charsets.US_ASCII)
}

object AudioMix {
    /** Rééchantillonnage linéaire (les voix d'un même moteur ont en pratique la même fréquence). */
    fun resample(p: Pcm, rate: Int): Pcm {
        if (p.sampleRate == rate || p.samples.isEmpty()) return Pcm(rate, p.samples)
        val n = (p.samples.size.toLong() * rate / p.sampleRate).toInt()
        val out = ShortArray(n)
        val ratio = p.sampleRate.toDouble() / rate
        for (i in 0 until n) {
            val x = i * ratio
            val i0 = x.toInt().coerceAtMost(p.samples.lastIndex)
            val i1 = min(i0 + 1, p.samples.lastIndex)
            val f = x - i0
            out[i] = (p.samples[i0] * (1 - f) + p.samples[i1] * f).roundToInt().toShort()
        }
        return Pcm(rate, out)
    }

    /**
     * Place chaque extrait à son instant (ms) sur une piste de `totalMs`, puis normalise le volume :
     * crête ramenée vers −1 dBFS, avec un gain limité à ×2,5 pour ne pas amplifier le souffle.
     */
    fun place(totalMs: Long, clips: List<Pair<Long, Pcm>>): Pcm {
        val rate = clips.firstOrNull()?.second?.sampleRate ?: 24_000
        val total = IntArray((totalMs * rate / 1000).toInt())
        for ((startMs, clip) in clips) {
            val c = resample(clip, rate)
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
        val out = ShortArray(total.size) { (total[it] * gain).roundToInt().coerceIn(-32768, 32767).toShort() }
        return Pcm(rate, out)
    }
}
