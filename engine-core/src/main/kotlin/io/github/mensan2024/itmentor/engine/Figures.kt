// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Personnages 2D vectoriels du registre de scènes (6.1b) : une silhouette entière, pas un buste, avec une tête, un torse, deux bras à
 * deux segments, deux jambes, une expression (sourire) et une pose (debout, en marche, assis). Les couleurs de peau, de vêtement et de
 * pantalon restent fixes pendant toute la vidéo : c'est ce qui donne la continuité d'apparence d'un plan à l'autre.
 *
 * Limites assumées : personnage stylisé et plat (pas d'ombrage, pas de doigts, visage réduit à un œil et une bouche). Il remplace le simple
 * buste de 6.1, il ne prétend pas à l'expressivité d'une animation de personnage complète (étape 6.3).
 */
internal class FigStyle(val skin: Int, val cloth: Int, val pants: Int, val hair: Int) {
    companion object {
        /** Quatre apparences distinctes pour un même décor ; l'indice choisit, le même indice redonne toujours la même personne. */
        fun of(i: Int, accent: Int): FigStyle = when (((i % 4) + 4) % 4) {
            0 -> FigStyle(rgb(0x8D5A3C), accent, rgb(0x2F3A52), rgb(0x1B1410))
            1 -> FigStyle(rgb(0xE0B08C), rgb(0x5B8DEF), rgb(0x33384A), rgb(0x3A2418))
            2 -> FigStyle(rgb(0x6B4129), rgb(0xE8B04A), rgb(0x2B3F3A), rgb(0x120D0A))
            else -> FigStyle(rgb(0xC98F68), rgb(0xD96C6C), rgb(0x2E3350), rgb(0x24160F))
        }
    }
}

internal object Fig {
    /**
     * @param x abscisse du personnage, @param floor ordonnée des pieds, @param h hauteur totale
     * @param walk -1..1 : balancement des jambes (0 = à l'arrêt)
     * @param armF, armB angles (degrés) du bras avant / arrière : [épaule, coude], 0 = le long du corps, 90 = à l'horizontale vers l'avant
     * @param facing 1 vers la droite, -1 vers la gauche
     * @param smile -1 triste .. 1 sourire
     * @param seated assis (chaise invisible : le décor la dessine)
     */
    fun draw(
        g: Gfx, x: Float, floor: Float, h: Float, st: FigStyle, facing: Int = 1, walk: Float = 0f, armF: Pair<Float, Float> = 8f to 12f,
        armB: Pair<Float, Float> = 8f to 12f, smile: Float = 0.5f, seated: Boolean = false, lean: Float = 0f,
    ): Pair<Float, Float> {
        val f = facing.toFloat()
        val hipY = floor - (if (seated) 0.30f else 0.46f) * h
        val shY = hipY - 0.31f * h
        val lx = x + lean * h
        val hr = 0.085f * h
        val headY = shY - 0.11f * h
        val legW = 0.075f * h
        // jambes
        if (seated) {
            for (k in 0..1) {
                val dx = if (k == 0) 0f else 0.03f
                val kneeX = lx + f * (0.22f + dx) * h
                g.line(lx + f * dx * h, hipY, kneeX, hipY, legW, st.pants)
                g.line(kneeX, hipY, kneeX, floor - 0.02f * h, legW, st.pants)
                g.fillRoundRect(kneeX - 0.03f * h + (if (f > 0) 0f else -0.04f * h), floor - 0.04f * h, kneeX + 0.07f * h + (if (f > 0) 0f else -0.04f * h), floor, 0.02f * h, st.hair)
            }
        } else {
            for (k in 0..1) {
                val sgn = if (k == 0) 1f else -1f
                val swing = walk * sgn * 0.14f * h
                val fx = lx + f * swing
                val lift = if (walk != 0f && swing * f * f > 0.05f * h) 0.03f * h else 0f
                g.line(lx + (sgn * 0.03f) * h, hipY, fx + sgn * 0.02f * h, floor - 0.03f * h - lift, legW, st.pants)
                g.fillRoundRect(fx - 0.04f * h + sgn * 0.02f * h, floor - 0.045f * h - lift, fx + 0.07f * h * f + sgn * 0.02f * h, floor - lift, 0.02f * h, st.hair)
            }
        }
        // bras arrière (derrière le torse)
        arm(g, lx, shY, h, f, armB, st.cloth, st.skin, back = true)
        // torse
        g.fillRoundRect(lx - 0.095f * h, shY - 0.02f * h, lx + 0.095f * h, hipY + 0.03f * h, 0.07f * h, st.cloth)
        g.fillRect(lx - 0.095f * h, hipY - 0.01f * h, lx + 0.095f * h, hipY + 0.04f * h, st.pants)
        // cou et tête
        g.fillRoundRect(lx - 0.025f * h, shY - 0.06f * h, lx + 0.025f * h, shY + 0.01f * h, 0.01f * h, st.skin)
        g.fillCircle(lx - f * 0.1f * hr, headY - 0.22f * hr, hr * 1.06f, st.hair)
        g.fillCircle(lx, headY + 0.1f * hr, hr, st.skin)
        g.fillCircle(lx + f * 0.45f * hr, headY + 0.05f * hr, 0.1f * hr, rgb(0x1A1A1A))
        val sm = smile.coerceIn(-1f, 1f)
        if (sm >= 0f) g.strokeArc(lx + f * 0.35f * hr, headY + 0.35f * hr, 0.38f * hr, 25f, 40f + 100f * sm, 0.07f * hr + 1f, rgb(0x3A1F18))
        else g.strokeArc(lx + f * 0.35f * hr, headY + 0.75f * hr, 0.38f * hr, 205f, 70f, 0.07f * hr + 1f, rgb(0x3A1F18))
        // bras avant (devant le torse)
        val front = arm(g, lx, shY, h, f, armF, st.cloth, st.skin, back = false)
        return front
    }

    /** Bras à deux segments ; retourne la position de la main. */
    private fun arm(g: Gfx, x: Float, shY: Float, h: Float, f: Float, a: Pair<Float, Float>, cloth: Int, skin: Int, back: Boolean): Pair<Float, Float> {
        val l1 = 0.17f * h; val l2 = 0.16f * h
        val sx = x + (if (back) -f * 0.02f * h else f * 0.02f * h)
        val sy = shY + 0.03f * h
        val a1 = a.first * PI.toFloat() / 180f
        val a2 = (a.first + a.second) * PI.toFloat() / 180f
        val ex = sx + f * sin(a1) * l1; val ey = sy + cos(a1) * l1
        val hx = ex + f * sin(a2) * l2; val hy = ey + cos(a2) * l2
        val w = 0.06f * h
        g.line(sx, sy, ex, ey, w, if (back) mix(cloth, rgb(0x000000), 0.18f) else cloth)
        g.line(ex, ey, hx, hy, w * 0.9f, skin)
        g.fillCircle(hx, hy, w * 0.55f, skin)
        return hx to hy
    }
}
