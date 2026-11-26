// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Point d'entrée du moteur : `render(g, tMs)` dessine l'image complète de l'instant tMs.
 * Fonction déterministe : même storyboard, même template, même format, même t ⇒ même image.
 * Aucune dépendance à Android, à l'interface ou à une horloge.
 *
 * Le storyboard doit déjà être habillé (`Templates.apply`) et, s'il y a une musique, calé sur son tempo
 * (`Rhythm.align`) : voir [Composer].
 */
class FrameRenderer(val storyboard: Storyboard, val format: VideoFormat, baseTemplate: Template = Templates.byId(storyboard.templateId)) {

    val timeline: Timeline = Timeline.build(storyboard)
    /** Template effectivement utilisé : le template choisi, décliné selon la graine de la génération. */
    val template: Template = Templates.vary(baseTemplate, storyboard.seed)
    private val style = template.style
    private val scenes = SceneRenderer(template)
    /** Format autre que Motion design : il dessine les scènes à sa manière. */
    private val look: LookRenderer? = Looks.renderer(storyboard.look, template, storyboard)
    private val ink get() = look?.ink ?: style.text
    private val muted get() = look?.muted ?: style.textMuted
    private val accentA get() = look?.accent ?: style.accent
    private val accentB get() = look?.accent2 ?: style.accent2

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
        if (look == null) Backgrounds.overlayFx(g, template, (tMs / 33.333f).toInt()) else look.overlayFx(g, (tMs / 33.333f).toInt())
        overlay(g, tMs)
        g.restore()
    }

    private val screens = ScreenSceneRenderer(look?.accent ?: style.accent)

    private fun scene(g: Gfx, slot: Slot, local: Float, tGlobal: Float) {
        if (slot.scene.layout == Layout.SCREEN) { screens.draw(g, slot.scene, local); return }
        look?.let { it.scene(g, slot, local, tGlobal); return }
        Backgrounds.draw(g, template, slot.index, slot.scene.background, tGlobal)
        scenes.draw(g, slot.scene, local, tGlobal)
    }

    private fun transition(g: Gfx, m: Moment.Blend, tGlobal: Float) {
        val e = Easing.EASE_IN_OUT.ease(m.progress)
        when (m.transition.type) {
            TransitionType.CUT -> scene(g, m.to, m.toLocalMs, tGlobal)
            TransitionType.FADE -> {
                scene(g, m.from, m.fromLocalMs, tGlobal)
                g.beginLayer(e); scene(g, m.to, m.toLocalMs, tGlobal); g.endLayer()
            }
            TransitionType.SLIDE -> {
                // « Pousser » : la scène suivante entre par la droite et chasse la précédente.
                g.save(); g.translate(-W * e, 0f); scene(g, m.from, m.fromLocalMs, tGlobal); g.restore()
                g.save(); g.translate(W * (1f - e), 0f); scene(g, m.to, m.toLocalMs, tGlobal); g.restore()
                val edge = W * (1f - e)
                if (look == null) g.fillRect(edge - 18f, 0f, edge, H, 0x33000000)
            }
            TransitionType.ZOOM -> {
                g.save(); g.scale(1f + 0.22f * e, W / 2, H / 2); scene(g, m.from, m.fromLocalMs, tGlobal); g.restore()
                g.beginLayer(e)
                g.save(); g.scale(lerp(0.86f, 1f, e), W / 2, H / 2); scene(g, m.to, m.toLocalMs, tGlobal); g.restore()
                g.endLayer()
            }
            TransitionType.WIPE -> {
                // Volets de couleur en diagonale : ils couvrent l'écran à mi-parcours, la scène change dessous.
                if (m.progress < 0.5f) scene(g, m.from, m.fromLocalMs, tGlobal) else scene(g, m.to, m.toLocalMs, tGlobal)
                panel(g, m.progress, 0.00f, accentB, 1.9f)
                panel(g, m.progress, 0.07f, accentA, 1.9f)
                panel(g, m.progress, 0.12f, ink.withAlpha(0.9f), 0.10f)
            }
            TransitionType.CIRCLE -> {
                // Ouverture circulaire depuis le centre, avec un anneau d'accent sur le bord.
                scene(g, m.from, m.fromLocalMs, tGlobal)
                val r = 1150f * e
                if (r > 1f) {
                    g.save()
                    g.clipPath(PathData().circle(W / 2, H / 2, r))
                    scene(g, m.to, m.toLocalMs, tGlobal)
                    g.restore()
                    g.strokeCircle(W / 2, H / 2, r, 14f * (1f - e) + 2f, accentA)
                }
            }
            TransitionType.DIP -> {
                // Passage par le noir, comme au cinéma.
                if (m.progress < 0.5f) {
                    scene(g, m.from, m.fromLocalMs, tGlobal)
                    g.fillRect(0f, 0f, W, H, (0xFF000000.toInt()).withAlpha(Easing.EASE_IN_OUT.ease(m.progress * 2f)))
                } else {
                    scene(g, m.to, m.toLocalMs, tGlobal)
                    g.fillRect(0f, 0f, W, H, (0xFF000000.toInt()).withAlpha(1f - Easing.EASE_IN_OUT.ease(m.progress * 2f - 1f)))
                }
            }
        }
    }

    /** Volet en parallélogramme qui traverse l'écran de gauche à droite. `width` en largeurs d'écran. */
    private fun panel(g: Gfx, progress: Float, delay: Float, color: Int, width: Float) {
        val u = Easing.EASE_IN_OUT.ease(clamp01((progress - delay) / (1f - 0.12f)))
        val left = lerp(-2.3f * W, 1.35f * W, u)
        val skew = 0.35f * W
        val p = PathData().moveTo(left + skew, 0f).lineTo(left + skew + width * W, 0f).lineTo(left + width * W, H).lineTo(left, H).close()
        g.fillPath(p, color)
    }

    /** Éléments fixes : une seule ligne de progression continue (pas de segments par scène) et pied de page. */
    private fun overlay(g: Gfx, tMs: Float) {
        val y = 104f
        val progress = clamp01(tMs / timeline.totalMs.coerceAtLeast(1))
        g.fillRoundRect(Safe.LEFT, y, Safe.RIGHT, y + 6f, 3f, ink.withAlpha(0.14f))
        if (progress > 0f) g.fillRoundRect(Safe.LEFT, y, Safe.LEFT + Safe.WIDTH * progress, y + 6f, 3f, ink.withAlpha(0.9f))
        val fy = 1608f
        val play = PathData().moveTo(Safe.LEFT, fy - 26f).lineTo(Safe.LEFT, fy + 6f).lineTo(Safe.LEFT + 27f, fy - 10f).close()
        g.fillPath(play, ink.withAlpha(0.9f))
        g.fillRoundRect(Safe.LEFT + 32f, fy + 2f, Safe.LEFT + 50f, fy + 8f, 3f, rgb(0xF5B301))
        val f = FontSpec(FontFamily.TEXT, 800, 28f)
        g.drawTracked(storyboard.footer, Safe.LEFT + 66f, fy, f, ink.withAlpha(0.9f), 2f)
    }
}

/**
 * Prépare un storyboard pour le rendu : habillage par le template, puis calage sur le tempo de la musique.
 * C'est la seule porte d'entrée de l'application : l'aperçu et l'export passent tous deux par ici.
 */
object Composer {
    /** Durée visée après habillage : une marge sous le maximum de 59 s. */
    private const val TARGET_MS = 57_500L

    /**
     * Applique le modèle, le format (ses transitions) et le calage sur le tempo de la musique.
     * Un format en coupes franches (sans fondus qui se chevauchent) ou le calage sur le tempo peuvent
     * allonger la vidéo : si elle dépasse la limite, les scènes sont resserrées proportionnellement
     * (rien n'est retiré) et le calage est refait.
     */
    fun prepare(content: Storyboard, template: Template, music: MusicTrack?): Storyboard {
        var src = content
        var out = dress(src, template, music)
        repeat(5) {
            val total = totalMs(out)
            if (total <= TARGET_MS + 1_000) return out
            val k = TARGET_MS.toDouble() / total
            src = src.copy(scenes = src.scenes.map { it.copy(durationMs = (it.durationMs * k).toInt().coerceAtLeast(2_000)) })
            out = dress(src, template, music)
        }
        return out
    }

    private fun dress(content: Storyboard, template: Template, music: MusicTrack?): Storyboard {
        val dressed = Looks.apply(Templates.apply(content, template), content.look)
        return if (music != null) Rhythm.align(dressed, music.beatMs) else dressed
    }

    /** Durée totale, calculée comme `Timeline.build` mais sans limite. */
    internal fun totalMs(sb: Storyboard): Long {
        var cursor = 0L
        var end = 0L
        for ((i, sc) in sb.scenes.withIndex()) {
            end = cursor + sc.durationMs
            val overlap = if (i == sb.scenes.lastIndex || sc.transitionOut.type == TransitionType.CUT) 0 else sc.transitionOut.durationMs
            cursor = end - overlap
        }
        return end
    }
}
