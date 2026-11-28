// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Phase D.2 — scènes 3D de réseaux et de systèmes, construites sur le moteur 3D logiciel ([Render3D]) : routeurs, commutateurs, serveurs,
 * postes, mur de pare-feu, fichiers et paquets. Une [View] assemble les solides d'UNE image (les paquets bougent, donc le modèle est
 * reconstruit à chaque image : quelques centaines de solides, négligeable) ; les liaisons et la grille du sol sont tracées en 2D sous les
 * solides, projetées par la même caméra, pour éviter tout défaut d'ordre de tri entre un grand plan et les objets qu'il porte.
 *
 * Repère : x vers la droite, y vers le haut, z vers le fond (la caméra regarde depuis z < 0).
 */
internal object Net3D {
    class View(val c: StageCtx, yaw: Float, pitch: Float, scale: Float, cy: Float = 380f, cx: Float = 500f) {
        val th = c.th
        val cam = Cam3D(yaw, pitch, 18f, scale, cx, cy, 0.4f)
        val parts = ArrayList<Part>()
        val body = mix(th.bgBottom, th.ink, 0.62f)
        val dark = mix(th.bgBottom, th.ink, 0.26f)

        fun add(p: Part): Int { parts += p; return parts.lastIndex }
        fun pt(x: Float, y: Float, z: Float): FloatArray = Render3D.project(cam, x, y, z)

        /** Grille du sol (carrés de 1 unité) : donne l'échelle et la profondeur. */
        fun grid(half: Float, depth: Float, a: Float = 0.16f) {
            var x = -half
            while (x <= half + 0.01f) { seg(x, -depth, x, depth, 1.2f, th.line.withAlpha(a)); x += 1f }
            var z = -depth
            while (z <= depth + 0.01f) { seg(-half, z, half, z, 1.2f, th.line.withAlpha(a)); z += 1f }
        }

        /** Segment posé sur le sol, tracé en 2D. */
        fun seg(x0: Float, z0: Float, x1: Float, z1: Float, w: Float, color: Int, y: Float = 0.02f) {
            val a = pt(x0, y, z0); val b = pt(x1, y, z1)
            c.g.line(a[0], a[1], b[0], b[1], w, color)
        }

        /** Liaison entre deux appareils : trait épais avec un liseré. */
        fun link(x0: Float, z0: Float, x1: Float, z1: Float, color: Int, alpha: Float = 1f, width: Float = 7f) {
            val a = pt(x0, 0.03f, z0); val b = pt(x1, 0.03f, z1)
            c.g.line(a[0], a[1], b[0], b[1], width + 4f, 0xFF000000.toInt().withAlpha(0.35f * alpha))
            c.g.line(a[0], a[1], b[0], b[1], width, color.withAlpha(alpha))
        }

        fun draw() {
            val m = Model3D(parts, 0f, 6f, 0.4f, 0.4f, 8f, 8f)
            Render3D.draw(c.g, m, cam, 0f, c.t)
        }

        /** Étiquette posée au-dessus d'un point du monde. */
        fun tag(text: String, x: Float, y: Float, z: Float, color: Int = th.ink, size: Float = 20f, a: Float = 1f) {
            if (a <= 0.01f) return
            val p = pt(x, y, z)
            SK.pill(c.g, text, p[0], p[1], color, th.bgTop.withAlpha(0.88f), size, a)
        }
    }

    fun router(v: View, x: Float, z: Float, glow: Float = 0.3f, accent: Int = v.th.accent): Int {
        val base = v.add(Solids.cylinder("routeur", x, 0.22f, z, 0.95f, 0.44f, 18, v.body))
        v.add(Solids.cylinder("routeur-d", x, 0.47f, z, 0.80f, 0.06f, 18, mix(v.dark, accent, 0.55f), glow, decalOf = base))
        v.add(Solids.box("fl-x", x, 0.51f, z, 1.15f, 0.04f, 0.15f, accent, 0.5f + 0.5f * glow, decalOf = base))
        v.add(Solids.box("fl-z", x, 0.51f, z, 0.15f, 0.04f, 1.15f, accent, 0.5f + 0.5f * glow, decalOf = base))
        return base
    }

    fun switchBox(v: View, x: Float, z: Float): Int {
        val b = v.add(Solids.box("switch", x, 0.2f, z, 2.1f, 0.4f, 1.0f, v.body))
        for (i in 0 until 8) v.add(Solids.box("port", x - 0.85f + i * 0.24f, 0.41f, z - 0.2f, 0.17f, 0.03f, 0.2f, v.dark, 0f, decalOf = b))
        for (i in 0 until 4) v.add(Solids.box("led", x - 0.85f + i * 0.24f, 0.41f, z + 0.25f, 0.1f, 0.03f, 0.1f, v.th.good, 0.9f, decalOf = b, blink = i * 0.25f))
        return b
    }

    fun server(v: View, x: Float, z: Float, accent: Int = v.th.accent, tall: Float = 1.9f, glow: Float = 0.4f): Int {
        val b = v.add(Solids.box("serveur", x, tall / 2f, z, 1.05f, tall, 0.95f, v.body))
        var k = 0
        var y = 0.28f
        while (y < tall - 0.2f) {
            v.add(Solids.box("baie", x, y, z - 0.48f, 0.9f, 0.2f, 0.03f, v.dark, 0f, decalOf = b))
            v.add(Solids.box("voyant", x + 0.32f, y, z - 0.5f, 0.07f, 0.07f, 0.03f, accent, glow + 0.5f, decalOf = b, blink = (k++ % 4) * 0.25f))
            y += 0.34f
        }
        return b
    }

    fun pc(v: View, x: Float, z: Float, screen: Int = v.th.accent, glow: Float = 0.6f): Int {
        val base = v.add(Solids.box("clavier", x, 0.07f, z - 0.2f, 1.0f, 0.14f, 0.55f, v.dark))
        v.add(Solids.box("pied", x, 0.35f, z + 0.2f, 0.18f, 0.5f, 0.12f, v.dark, 0f, decalOf = base))
        val mon = v.add(Solids.box("ecran", x, 0.78f, z + 0.2f, 1.0f, 0.68f, 0.09f, v.body, 0f, decalOf = base))
        v.add(Solids.box("dalle", x, 0.78f, z + 0.14f, 0.88f, 0.56f, 0.03f, screen, glow, decalOf = mon))
        return base
    }

    /** Document : plaque fine ; [state] 0 = sain, 1 = chiffré (rouge, cadenas). */
    fun file(v: View, x: Float, y: Float, z: Float, state: Float, accent: Int = v.th.good) {
        val col = if (state > 0.5f) v.th.bad else accent
        val b = v.add(Solids.box("fichier", x, y, z, 0.34f, 0.05f, 0.44f, mix(v.body, col, 0.5f), if (state > 0.5f) 0.7f else 0.25f))
        v.add(Solids.box("fichier-l", x, y + 0.03f, z + 0.1f, 0.22f, 0.02f, 0.05f, col, 0.8f, decalOf = b))
        v.add(Solids.box("fichier-l2", x, y + 0.03f, z - 0.02f, 0.22f, 0.02f, 0.05f, col, 0.8f, decalOf = b))
        if (state > 0.5f) v.add(Solids.box("cadenas", x, y + 0.1f, z - 0.05f, 0.14f, 0.1f, 0.12f, v.th.warn, 1f, decalOf = b))
    }

    fun packet(v: View, x: Float, y: Float, z: Float, color: Int, s: Float = 0.28f) {
        v.add(Solids.box("paquet", x, y, z, s, s, s, color, 0.9f))
    }

    /** Mur de briques le long de l'axe z (en x = [x]) ; [gap] : demi-largeur de la porte (0 = mur plein). */
    fun wall(v: View, x: Float, half: Float, gap: Float, heat: Float = 0f) {
        var z = -half
        var i = 0
        while (z <= half + 0.01f) {
            if (kotlin.math.abs(z) >= gap || gap <= 0f) {
                for (r in 0 until 3) {
                    val col = mix(mix(v.th.bad, v.dark, 0.55f), v.th.warn, if ((r + i) % 2 == 0) 0.12f else 0f)
                    v.add(Solids.box("brique", x, 0.22f + r * 0.44f, z, 0.5f, 0.42f, 0.54f, col, 0.18f + 0.3f * heat))
                }
            }
            z += 0.56f; i++
        }
        // liseré lumineux sur le haut du mur
        v.add(Solids.box("crete", x, 1.5f, 0f, 0.1f, 0.06f, half * 2f + 0.5f, v.th.warn, 0.9f))
    }
}
