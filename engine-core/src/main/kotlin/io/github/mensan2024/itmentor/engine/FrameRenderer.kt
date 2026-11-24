// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Point d'entrée du moteur : `render(g, tMs)` dessine l'image complète de l'instant tMs.
 * Fonction déterministe : même storyboard, même format, même t ⇒ même image.
 * Aucune dépendance à Android, à l'interface ou à une horloge.
 */
class FrameRenderer(val storyboard: Storyboard, val format: VideoFormat) {

    val timeline: Timeline = Timeline.build(storyboard)
    val style: Style = Styles.byId(storyboard.styleId)
    private val scenes = SceneRenderer(style)

    private val W = VideoFormat.DESIGN_WIDTH
    private val H = VideoFormat.DESIGN_HEIGHT

    val frameCount: Int get() = timeline.frameCount(format.fps)

    fun timeOfFrame(i: Int): Float = timeline.frameTimeMs(i, format.fps)

    fun renderFrame(g: Gfx, frame: Int) = render(g, timeOfFrame(frame))

    fun render(g: Gfx, tMs: Float) {
        g.save()
        if (format.scale != 1f) g.scale(format.scale, 0f, 0f)
        g.clipRect(0f, 0f, W, H)
        when (val m = timeline.at(tMs)) {
            is Moment.Single -> scene(g, m.slot, m.localMs, tMs)
            is Moment.Blend -> transition(g, m, tMs)
        }
        overlay(g, tMs)
        g.restore()
    }

    private fun scene(g: Gfx, slot: Slot, local: Float, tGlobal: Float) {
        Backgrounds.draw(g, style, slot.scene.background, tGlobal)
        scenes.draw(g, slot.scene, local, tGlobal)
    }

    private fun transition(g: Gfx, m: Moment.Blend, tGlobal: Float) {
        val e = Easing.EASE_IN_OUT.ease(m.progress)
        when (m.transition.type) {
            TransitionType.CUT -> scene(g, m.to, m.toLocalMs, tGlobal)
            TransitionType.FADE -> {
                scene(g, m.from, m.fromLocalMs, tGlobal)
                g.beginLayer(e)
                scene(g, m.to, m.toLocalMs, tGlobal)
                g.endLayer()
            }
            TransitionType.SLIDE -> {
                // « Pousser » : la scène suivante entre par la droite et chasse la précédente.
                g.save(); g.translate(-W * e, 0f); scene(g, m.from, m.fromLocalMs, tGlobal); g.restore()
                g.save(); g.translate(W * (1f - e), 0f); scene(g, m.to, m.toLocalMs, tGlobal); g.restore()
                // Fine ombre portée sur le bord d'attaque.
                val edge = W * (1f - e)
                g.fillRect(edge - 18f, 0f, edge, H, 0x33000000)
            }
            TransitionType.ZOOM -> {
                g.save(); g.scale(1f + 0.22f * e, W / 2, H / 2); scene(g, m.from, m.fromLocalMs, tGlobal); g.restore()
                g.beginLayer(e)
                g.save(); g.scale(lerp(0.86f, 1f, e), W / 2, H / 2); scene(g, m.to, m.toLocalMs, tGlobal); g.restore()
                g.endLayer()
            }
        }
    }

    /** Éléments fixes : indicateur de progression par scène et pied de page. */
    private fun overlay(g: Gfx, tMs: Float) {
        val n = timeline.slots.size
        val gap = 12f
        val segW = (Safe.WIDTH - gap * (n - 1)) / n
        val y = 104f
        for ((i, s) in timeline.slots.withIndex()) {
            val l = Safe.LEFT + i * (segW + gap)
            g.fillRoundRect(l, y, l + segW, y + 8f, 4f, style.text.withAlpha(0.16f))
            val f = window(tMs, s.startMs.toFloat(), s.durationMs.toFloat())
            if (f > 0f) g.fillRoundRect(l, y, l + segW * f, y + 8f, 4f, style.text.withAlpha(0.9f))
        }
        // Pied de page : marque ▶_ + nom + compteur de scène.
        val fy = 1608f
        val play = PathData().moveTo(Safe.LEFT, fy - 26f).lineTo(Safe.LEFT, fy + 6f).lineTo(Safe.LEFT + 27f, fy - 10f).close()
        g.fillPath(play, style.text.withAlpha(0.9f))
        g.fillRoundRect(Safe.LEFT + 32f, fy + 2f, Safe.LEFT + 50f, fy + 8f, 3f, style.highlight)
        val f = FontSpec(FontFamily.TEXT, 700, 28f)
        g.drawText(storyboard.footer, Safe.LEFT + 66f, fy, f, style.text.withAlpha(0.9f))
        storyboard.hashtag?.let { h ->
            val hf = FontSpec(FontFamily.TEXT, 500, 26f)
            g.drawText(h, Safe.RIGHT - g.measureText(h, hf), fy, hf, style.textMuted)
        }
    }
}
