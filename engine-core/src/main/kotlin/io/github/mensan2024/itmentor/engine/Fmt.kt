// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Mise en forme des nombres sans `String.format` (absent hors JVM) : le moteur tourne aussi dans le
 * navigateur (Kotlin/JS).
 */
internal object Fmt {
    /** Entier complété par des zéros à gauche : `pad(7, 3)` → « 007 ». */
    fun pad(n: Int, width: Int): String = n.toString().padStart(width, '0')

    /** Un chiffre après la virgule, point décimal : `one(60.43)` → « 60.4 ». */
    fun one(x: Double): String {
        val r = kotlin.math.round(x * 10).toLong()
        val sign = if (r < 0) "-" else ""
        val a = kotlin.math.abs(r)
        return "$sign${a / 10}.${a % 10}"
    }

    /** Milliers séparés par une espace : `thousands(8412)` → « 8 412 ». */
    fun thousands(n: Int): String {
        val s = kotlin.math.abs(n).toString()
        val out = StringBuilder()
        for ((i, c) in s.withIndex()) { if (i > 0 && (s.length - i) % 3 == 0) out.append(' '); out.append(c) }
        return (if (n < 0) "-" else "") + out
    }
}

/** Degrés → radians et l'inverse (sans java.lang.Math, pour tourner aussi dans le navigateur). */
internal fun rad(deg: Double): Double = deg * kotlin.math.PI / 180.0
internal fun deg(rad: Double): Double = rad * 180.0 / kotlin.math.PI
