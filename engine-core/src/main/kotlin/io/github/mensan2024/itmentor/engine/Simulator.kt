// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Scène de démonstration : un bureau simulé (Windows, macOS, Ubuntu, console réseau) filmé comme un
 * tutoriel : la caméra zoome sur la zone utile, le curseur se déplace et clique, le texte se tape au
 * clavier, les interrupteurs basculent. En haut, le titre de l'étape ; en bas, l'explication.
 *
 * Le bureau est dessiné dans un espace virtuel de 1600 × 1000, puis cadré par la caméra dans la vidéo.
 * Interfaces génériques dessinées en vectoriel : aucune capture d'écran, aucun logo de marque.
 */
internal class ScreenSceneRenderer(private val accent: Int) {
    private val cache = TextCache()

    // Fenêtre de la vidéo où l'écran est filmé (dans la zone sûre).
    private val vl = Safe.LEFT
    private val vr = Safe.RIGHT
    private val vb = 1330f

    data class R(val l: Float, val t: Float, val r: Float, val b: Float) {
        val cx get() = (l + r) / 2
        val cy get() = (t + b) / 2
        val w get() = r - l
        val h get() = b - t
        fun inset(d: Float) = R(l + d, t + d, r - d, b - d)
    }

    private data class Cam(val cx: Float, val cy: Float, val s: Float)

    // ───────────────────────────── Scène complète ─────────────────────────────

    fun draw(g: Gfx, sc: Scene, t: Float) {
        val step = sc.demo ?: return
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, rgb(0x0A1020), rgb(0x121C33))
        g.fillRadialGlow(540f, 900f, 900f, accent.withAlpha(0.10f))
        val top = header(g, sc, t)
        val vt = max(top + 36f, 430f)
        val view = R(vl, vt, vr, vb)
        // Cadre de l'écran
        g.fillRoundRect(view.l - 6f, view.t - 6f, view.r + 6f, view.b + 6f, 22f, rgb(0x050811))
        g.strokeRoundRect(view.l - 6f, view.t - 6f, view.r + 6f, view.b + 6f, 22f, 2f, 0x30FFFFFF)
        g.save()
        g.clipRect(view.l, view.t, view.r, view.b)
        g.fillRect(view.l, view.t, view.r, view.b, rgb(0x050811))
        val cam = camera(step, t, view)
        g.save()
        g.translate(view.cx, view.cy)
        g.scale(cam.s, 0f, 0f)
        g.translate(-cam.cx, -cam.cy)
        desktop(g, step, t)
        cursor(g, step, t)
        g.restore()
        g.restore()
        caption(g, sc, t)
    }

    private fun header(g: Gfx, sc: Scene, t: Float): Float {
        var y = 190f
        sc.kicker?.let { k ->
            val f = FontSpec(FontFamily.TEXT, 800, 28f)
            val text = k.uppercase()
            val w = g.measureTracked(text, f, 2f) + 44f
            val a = Easing.EXPO_OUT.ease(window(t, 0f, 450f))
            g.withPose(Pose(alpha = a), Safe.LEFT, y) {
                g.fillRoundRect(Safe.LEFT, y, Safe.LEFT + w, y + 54f, 27f, accent.withAlpha(0.18f))
                g.strokeRoundRect(Safe.LEFT, y, Safe.LEFT + w, y + 54f, 27f, 2f, accent.withAlpha(0.7f))
                g.drawTracked(text, Safe.LEFT + 22f, y + 27f + g.metrics(f).ascent / 2, f, accent, 2f)
            }
            y += 80f
        }
        val marked = Marked.parse(sc.title)
        val b = cache.fit(g, "${sc.id}/t", marked.plain, FontSpec(FontFamily.TEXT, 800, 60f), Safe.WIDTH, 2, 1.1f)
        val runs = MarkedText.runs(b, marked)
        for (i in b.lines.indices) {
            val a = Easing.EXPO_OUT.ease(window(t, 80f + i * 90f, 500f))
            val base = g.baselineOf(b, y, i)
            g.withPose(Pose(alpha = a, dy = (1 - a) * 24f), Safe.LEFT, base) {
                MarkedText.drawLine(g, runs[i], Safe.LEFT, base, b.font, rgb(0xF5F7FA), accent)
            }
        }
        return y + b.height
    }

    private fun caption(g: Gfx, sc: Scene, t: Float) {
        val text = Marked.strip(sc.subtitle ?: return)
        val b = cache.fit(g, "${sc.id}/c", text, FontSpec(FontFamily.TEXT, 600, 36f), Safe.WIDTH - 70f, 4, 1.3f)
        val top = 1372f
        val h = b.height + 50f
        val a = Easing.EXPO_OUT.ease(window(t, 250f, 500f))
        g.withPose(Pose(alpha = a, dy = (1 - a) * 20f), Safe.LEFT, top) {
            g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 18f, rgb(0x0B1324))
            g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 18f, 2f, 0x24FFFFFF)
            g.fillRoundRect(Safe.LEFT, top, Safe.LEFT + 8f, top + h, 4f, accent)
            g.drawBlock(b, Safe.LEFT + 40f, top + 25f, rgb(0xF5F7FA))
        }
    }

    // ───────────────────────────── Caméra ─────────────────────────────

    private val DW = 1600f
    private val DH = 1000f

    private fun overview(view: R) = Cam(DW / 2, DH / 2, min(view.w / DW, view.h / DH))

    private fun focusCam(step: DemoStep, view: R): Cam {
        val f = focusRect(step) ?: return overview(view)
        val s = min(view.w / (f.w + 60f), view.h / (f.h + 60f)).coerceIn(overview(view).s, 1.2f)
        val halfW = view.w / s / 2
        val halfH = view.h / s / 2
        val cx = if (halfW * 2 >= DW) DW / 2 else f.cx.coerceIn(halfW, DW - halfW)
        val cy = if (halfH * 2 >= DH) DH / 2 else f.cy.coerceIn(halfH, DH - halfH)
        return Cam(cx, cy, s)
    }

    private fun camera(step: DemoStep, t: Float, view: R): Cam {
        val from = step.previous?.let { focusCam(it, view) } ?: overview(view)
        val to = focusCam(step, view)
        val k = Easing.EASE_IN_OUT.ease(window(t, if (step.previous == null) 900f else 250f, 1100f))
        return Cam(lerp(from.cx, to.cx, k), lerp(from.cy, to.cy, k), lerp(from.s, to.s, k))
    }

    private fun focusRect(step: DemoStep): R? = when (val s = step.screen) {
        DemoScreen.Desktop -> null
        is DemoScreen.Search -> searchPanel(step.os)
        is DemoScreen.Settings -> {
            val win = settingsWindow(step.os)
            when (val a = step.action) {
                is DemoAction.ClickRow -> {
                    val row = rowRect(step.os, s, a.row)
                    R(row.l - 20f, row.t - 150f, row.r + 20f, row.b + 70f)
                }
                else -> R(win.l, win.t, win.r, min(win.b, rowRect(step.os, s, (s.rows.size - 1).coerceAtLeast(0)).b + 50f))
            }
        }
        is DemoScreen.Terminal -> {
            val w = terminalWindow(step.os)
            val last = terminalBottom(step, s)
            R(w.l, max(w.t, last - 560f), w.r, min(w.b, max(last + 40f, w.t + 300f)))
        }
    }

    // ───────────────────────────── Minutage des actions ─────────────────────────────

    /** Instant du clic dans l'étape (ms), ou `null` s'il n'y a pas de clic. */
    private fun clickAt(step: DemoStep): Float? = when (val s = step.screen) {
        is DemoScreen.Search -> if (step.action == DemoAction.OpenResult) 900f + s.query.length * 70f + 900f else null
        is DemoScreen.Settings -> if (step.action is DemoAction.ClickRow) 1900f else null
        else -> null
    }

    private fun clickPoint(step: DemoStep): Pair<Float, Float>? = when (val s = step.screen) {
        is DemoScreen.Search -> searchResult(step.os).let { it.l + 120f to it.cy }
        is DemoScreen.Settings -> (step.action as? DemoAction.ClickRow)?.let { a ->
            val r = rowRect(step.os, s, a.row)
            if (s.rows[a.row].control is DemoControl.Toggle || s.rows[a.row].control is DemoControl.Button) controlRect(step.os, r, s.rows[a.row].control).let { it.cx to it.cy }
            else r.l + 160f to r.cy
        }
        else -> null
    }

    private fun restingCursor(step: DemoStep?): Pair<Float, Float> = step?.let { clickPoint(it) } ?: (1240f to 760f)

    private fun cursor(g: Gfx, step: DemoStep, t: Float) {
        if (step.screen is DemoScreen.Terminal) return
        val from = restingCursor(step.previous)
        val target = clickPoint(step)
        val at = clickAt(step)
        var x = from.first; var y = from.second
        if (target != null && at != null) {
            val k = Easing.EASE_IN_OUT.ease(window(t, at - 800f, 750f))
            x = lerp(from.first, target.first, k); y = lerp(from.second, target.second, k) - sin(k * Math.PI.toFloat()) * 30f
            val c = window(t, at, 420f)
            if (c > 0f && c < 1f) g.strokeCircle(target.first, target.second, 10f + 34f * c, 4f * (1 - c), accent.withAlpha(1 - c))
        }
        val p = PathData().moveTo(x, y).lineTo(x, y + 34f).lineTo(x + 9f, y + 26f).lineTo(x + 15f, y + 40f)
            .lineTo(x + 21f, y + 37f).lineTo(x + 15f, y + 24f).lineTo(x + 26f, y + 24f).close()
        g.fillPath(p.transformed(1f, 3f, 4f), 0x50000000)
        g.fillPath(p, rgb(0xFFFFFF)); g.strokePath(p, 2f, rgb(0x111111))
    }

    // ───────────────────────────── Bureau ─────────────────────────────

    private fun desktop(g: Gfx, step: DemoStep, t: Float) {
        when (step.os) {
            DemoOs.WINDOWS, DemoOs.CISCO -> winDesktop(g)
            DemoOs.MAC -> macDesktop(g, step)
            DemoOs.UBUNTU -> ubuntuDesktop(g)
        }
        when (val s = step.screen) {
            DemoScreen.Desktop -> {}
            is DemoScreen.Search -> search(g, step, s, t)
            is DemoScreen.Settings -> settings(g, step, s, t)
            is DemoScreen.Terminal -> terminal(g, step, s, t)
        }
    }

    private fun winDesktop(g: Gfx) {
        g.fillVerticalGradient(0f, 0f, DW, DH, rgb(0x0B3D91), rgb(0x5AA9E6))
        g.fillRadialGlow(1150f, 420f, 650f, rgb(0xBFE3FF).withAlpha(0.55f))
        g.fillRadialGlow(500f, 760f, 520f, rgb(0x2E6FD8).withAlpha(0.5f))
        // Barre des tâches
        g.fillRect(0f, 952f, DW, DH, rgb(0xEEF2F7).withAlpha(0.94f))
        val icons = listOf(rgb(0x2B7DE9), rgb(0xF8C33A), rgb(0x1DA1F2), rgb(0x6E6E6E), rgb(0x16A34A))
        val x0 = DW / 2 - icons.size * 30f - 120f
        // Bouton de démarrage générique + champ de recherche
        g.fillRoundRect(x0, 962f, x0 + 30f, 992f, 7f, rgb(0x2B7DE9))
        g.fillPath(PathData().moveTo(x0 + 15f, 968f).lineTo(x0 + 24f, 977f).lineTo(x0 + 15f, 986f).lineTo(x0 + 6f, 977f).close(), rgb(0xFFFFFF))
        g.fillRoundRect(x0 + 44f, 962f, x0 + 224f, 992f, 15f, rgb(0xFFFFFF))
        g.drawText("Rechercher", x0 + 74f, 983f, FontSpec(FontFamily.TEXT, 400, 16f), rgb(0x5F6B7A))
        icons.forEachIndexed { i, c -> g.fillRoundRect(x0 + 244f + i * 48f, 962f, x0 + 274f + i * 48f, 992f, 7f, c) }
        val tf = FontSpec(FontFamily.TEXT, 500, 15f)
        g.drawText("14:32", DW - 92f, 973f, tf, rgb(0x1B1B1B))
        g.drawText("24/11/2026", DW - 112f, 992f, tf, rgb(0x1B1B1B))
    }

    private fun macDesktop(g: Gfx, step: DemoStep) {
        g.fillVerticalGradient(0f, 0f, DW, DH, rgb(0x1E2B6F), rgb(0xE9875A))
        g.fillRadialGlow(400f, 300f, 700f, rgb(0x8E4FC9).withAlpha(0.6f))
        g.fillRadialGlow(1250f, 800f, 600f, rgb(0xF5B25C).withAlpha(0.55f))
        // Barre de menus
        g.fillRect(0f, 0f, DW, 30f, rgb(0xFFFFFF).withAlpha(0.28f))
        g.fillCircle(26f, 15f, 8f, rgb(0x111111))
        val app = if (step.screen is DemoScreen.Settings) "Réglages Système" else if (step.screen is DemoScreen.Terminal) "Terminal" else "Finder"
        val mf = FontSpec(FontFamily.TEXT, 700, 16f)
        g.drawText(app, 50f, 21f, mf, rgb(0x111111))
        var x = 60f + g.measureText(app, mf) + 20f
        for (m in listOf("Fichier", "Édition", "Présentation", "Fenêtre", "Aide")) {
            g.drawText(m, x, 21f, mf.copy(weight = 400), rgb(0x111111)); x += g.measureText(m, mf) + 22f
        }
        g.drawText("lun. 24 nov.  14:32", DW - 180f, 21f, mf.copy(weight = 500), rgb(0x111111))
        // Dock
        g.fillRoundRect(DW / 2 - 330f, 922f, DW / 2 + 330f, 992f, 22f, rgb(0xFFFFFF).withAlpha(0.35f))
        val cols = listOf(rgb(0x3D8BFF), rgb(0xF5F5F5), rgb(0x34C759), rgb(0xFF9F0A), rgb(0x8E8E93), rgb(0x111111), rgb(0xFF375F), rgb(0x5E5CE6), rgb(0x64D2FF), rgb(0xA2845E))
        cols.forEachIndexed { i, c -> g.fillRoundRect(DW / 2 - 310f + i * 62f, 930f, DW / 2 - 258f + i * 62f, 982f, 12f, c) }
    }

    private fun ubuntuDesktop(g: Gfx) {
        g.fillVerticalGradient(0f, 0f, DW, DH, rgb(0x2C001E), rgb(0xE95420))
        g.fillRadialGlow(1100f, 300f, 700f, rgb(0x77216F).withAlpha(0.7f))
        g.fillRect(0f, 0f, DW, 32f, rgb(0x000000))
        val f = FontSpec(FontFamily.TEXT, 600, 16f)
        g.fillRoundRect(14f, 7f, 74f, 25f, 9f, rgb(0x3A3A3A))
        g.drawText("24 nov.  14:32", DW / 2 - 50f, 22f, f, rgb(0xFFFFFF))
        // Dock à gauche
        g.fillRect(0f, 32f, 76f, DH, rgb(0x1D1D1D).withAlpha(0.85f))
        val cols = listOf(rgb(0x3D8BFF), rgb(0xE95420), rgb(0xF6D32D), rgb(0x2EC27E), rgb(0x241F31), rgb(0x9141AC))
        cols.forEachIndexed { i, c -> g.fillRoundRect(14f, 54f + i * 66f, 62f, 102f + i * 66f, 12f, c) }
        for (i in 0 until 3) for (j in 0 until 3) g.fillCircle(28f + i * 10f, 940f + j * 10f, 2.6f, rgb(0xFFFFFF))
    }

    // ───────────────────────────── Recherche (Démarrer, Spotlight, Activités) ─────────────────────────────

    private fun searchPanel(os: DemoOs): R = when (os) {
        DemoOs.WINDOWS, DemoOs.CISCO -> R(470f, 200f, 1130f, 930f)
        DemoOs.MAC -> R(450f, 200f, 1150f, 470f)
        DemoOs.UBUNTU -> R(450f, 80f, 1150f, 470f)
    }

    private fun searchResult(os: DemoOs): R = when (os) {
        DemoOs.WINDOWS, DemoOs.CISCO -> R(500f, 330f, 1100f, 430f)
        DemoOs.MAC -> R(460f, 300f, 1140f, 370f)
        DemoOs.UBUNTU -> R(700f, 230f, 900f, 420f)
    }

    private fun search(g: Gfx, step: DemoStep, s: DemoScreen.Search, t: Float) {
        val p = searchPanel(step.os)
        val typed = window(t, 900f, s.query.length * 70f)
        val q = s.query.take((s.query.length * typed).toInt())
        val showResult = typed > 0.4f
        val pressed = clickAt(step)?.let { window(t, it, 200f) } ?: 0f
        val open = Easing.EXPO_OUT.ease(window(t, 400f, 400f))
        val caret = (t / 450f).toInt() % 2 == 0
        val res = searchResult(step.os)
        when (step.os) {
            DemoOs.WINDOWS, DemoOs.CISCO -> {
                g.withPose(Pose(alpha = open, dy = (1 - open) * 60f), p.cx, p.b) {
                    g.fillRoundRect(p.l + 6f, p.t + 10f, p.r + 6f, p.b + 10f, 16f, 0x40000000)
                    g.fillRoundRect(p.l, p.t, p.r, p.b, 16f, rgb(0xF3F4F6))
                    g.fillRoundRect(p.l + 30f, p.t + 30f, p.r - 30f, p.t + 86f, 28f, rgb(0xFFFFFF))
                    g.strokeRoundRect(p.l + 30f, p.t + 30f, p.r - 30f, p.t + 86f, 28f, 2f, rgb(0x2B7DE9))
                    Icons.draw(g, IconId.SEARCH, p.l + 66f, p.t + 58f, 26f, rgb(0x5F6B7A), 2f)
                    val qf = FontSpec(FontFamily.TEXT, 500, 32f)
                    g.drawText(q, p.l + 96f, p.t + 67f, qf, rgb(0x111111))
                    if (caret && typed < 1f) { val cx = p.l + 98f + g.measureText(q, qf); g.fillRect(cx, p.t + 44f, cx + 2f, p.t + 74f, rgb(0x111111)) }
                    if (showResult) {
                        g.drawText("Meilleure correspondance", p.l + 36f, p.t + 118f, FontSpec(FontFamily.TEXT, 700, 18f), rgb(0x3B4452))
                        resultRow(g, res, s, rgb(0xDCE8F8).let { if (pressed > 0f) rgb(0xC3D7F3) else it }, rgb(0x111111), rgb(0x5F6B7A))
                    }
                }
            }
            DemoOs.MAC -> {
                g.withPose(Pose(alpha = open, scale = lerp(0.96f, 1f, open)), p.cx, p.t + 35f) {
                    g.fillRoundRect(p.l + 4f, p.t + 8f, p.r + 4f, p.t + 78f, 16f, 0x40000000)
                    g.fillRoundRect(p.l, p.t, p.r, p.t + 70f, 16f, rgb(0xF2F2F5).withAlpha(0.96f))
                    Icons.draw(g, IconId.SEARCH, p.l + 40f, p.t + 35f, 28f, rgb(0x6B6B70), 2f)
                    val qf = FontSpec(FontFamily.TEXT, 400, 30f)
                    g.drawText(q, p.l + 76f, p.t + 46f, qf, rgb(0x111111))
                    if (caret && typed < 1f) { val cx = p.l + 78f + g.measureText(q, qf); g.fillRect(cx, p.t + 18f, cx + 2f, p.t + 54f, rgb(0x0A84FF)) }
                    if (showResult) {
                        g.fillRoundRect(p.l, p.t + 84f, p.r, p.b, 16f, rgb(0xF2F2F5).withAlpha(0.96f))
                        resultRow(g, res, s, rgb(0x0A64D8), rgb(0xFFFFFF), rgb(0xDCE6F5))
                    }
                }
            }
            DemoOs.UBUNTU -> {
                g.fillRect(0f, 32f, DW, DH, 0x99000000.toInt())
                g.withPose(Pose(alpha = open), p.cx, p.t) {
                    g.fillRoundRect(p.l + 100f, p.t, p.r - 100f, p.t + 56f, 28f, rgb(0x3A3A3A))
                    Icons.draw(g, IconId.SEARCH, p.l + 134f, p.t + 28f, 24f, rgb(0xDDDDDD), 2f)
                    val qf = FontSpec(FontFamily.TEXT, 500, 32f)
                    g.drawText(q, p.l + 164f, p.t + 37f, qf, rgb(0xFFFFFF))
                    if (caret && typed < 1f) { val cx = p.l + 166f + g.measureText(q, qf); g.fillRect(cx, p.t + 14f, cx + 2f, p.t + 44f, rgb(0xFFFFFF)) }
                    if (showResult) {
                        g.fillRoundRect(res.l, res.t, res.r, res.b, 18f, if (pressed > 0f) rgb(0x5A5A5A) else rgb(0x464646))
                        g.fillRoundRect(res.cx - 48f, res.t + 22f, res.cx + 48f, res.t + 118f, 22f, rgb(0x2B2B2B))
                        Icons.draw(g, s.icon, res.cx, res.t + 70f, 60f, rgb(0xFFFFFF), 2.2f)
                        val nf = FontSpec(FontFamily.TEXT, 600, 20f)
                        g.drawText(s.result, res.cx - g.measureText(s.result, nf) / 2, res.t + 160f, nf, rgb(0xFFFFFF))
                    }
                }
            }
        }
    }

    private fun resultRow(g: Gfx, r: R, s: DemoScreen.Search, bg: Int, ink: Int, sub: Int) {
        g.fillRoundRect(r.l, r.t, r.r, r.b, 10f, bg)
        g.fillRoundRect(r.l + 20f, r.cy - 28f, r.l + 76f, r.cy + 28f, 12f, rgb(0x2B7DE9))
        Icons.draw(g, s.icon, r.l + 48f, r.cy, 34f, rgb(0xFFFFFF), 2.2f)
        g.drawText(s.result, r.l + 96f, r.cy - 2f, FontSpec(FontFamily.TEXT, 700, 32f), ink)
        g.drawText(s.resultKind, r.l + 96f, r.cy + 30f, FontSpec(FontFamily.TEXT, 400, 22f), sub)
    }

    // ───────────────────────────── Fenêtre de réglages ─────────────────────────────

    private fun settingsWindow(os: DemoOs): R = when (os) {
        DemoOs.MAC -> R(230f, 60f, 1370f, 905f)
        DemoOs.UBUNTU -> R(170f, 70f, 1430f, 920f)
        else -> R(140f, 40f, 1460f, 935f)
    }

    private fun sidebarW(os: DemoOs) = if (os == DemoOs.MAC) 330f else 350f

    private fun pane(os: DemoOs): R {
        val w = settingsWindow(os)
        return R(w.l + sidebarW(os) + 40f, w.t + 70f, w.r - 40f, w.b - 30f)
    }

    private fun rowTop(os: DemoOs) = pane(os).t + 120f

    private fun rowH(row: DemoRow) = if (row.detail != null) 132f else 96f

    fun rowRect(os: DemoOs, s: DemoScreen.Settings, i: Int): R {
        val p = pane(os)
        var y = rowTop(os)
        for (k in 0 until i) y += rowH(s.rows[k]) + 10f
        return R(p.l, y, p.r, y + rowH(s.rows.getOrElse(i) { DemoRow("") }))
    }

    private fun controlRect(os: DemoOs, r: R, c: DemoControl): R = when (c) {
        is DemoControl.Toggle -> R(r.r - 120f, r.cy - 22f, r.r - 30f, r.cy + 22f)
        is DemoControl.Button -> R(r.r - 340f, r.cy - 30f, r.r - 24f, r.cy + 30f)
        else -> R(r.r - 60f, r.cy - 16f, r.r - 24f, r.cy + 16f)
    }

    private fun settings(g: Gfx, step: DemoStep, s: DemoScreen.Settings, t: Float) {
        val os = step.os
        val w = settingsWindow(os)
        val fresh = step.previous?.screen.let { it !is DemoScreen.Settings || it.app != s.app }
        val open = if (fresh) Easing.EXPO_OUT.ease(window(t, 150f, 500f)) else 1f
        val light = true
        val winBg = when (os) { DemoOs.MAC -> rgb(0xF5F5F7); DemoOs.UBUNTU -> rgb(0xFAFAFA); else -> rgb(0xF3F3F3) }
        val side = when (os) { DemoOs.MAC -> rgb(0xE6E6EA); DemoOs.UBUNTU -> rgb(0xEBEBED); else -> rgb(0xF3F3F3) }
        val ink = rgb(0x1B1B1F)
        val sub = rgb(0x5F6470)
        val blue = when (os) { DemoOs.MAC -> rgb(0x0A84FF); DemoOs.UBUNTU -> rgb(0x3584E4); else -> rgb(0x005FB8) }
        g.withPose(Pose(alpha = open, scale = lerp(0.95f, 1f, open)), w.cx, w.cy) {
            g.fillRoundRect(w.l + 8f, w.t + 16f, w.r + 8f, w.b + 16f, 14f, 0x44000000)
            g.fillRoundRect(w.l, w.t, w.r, w.b, 12f, winBg)
            g.fillRoundRect(w.l, w.t, w.l + sidebarW(os), w.b, 12f, side)
            if (os != DemoOs.MAC) g.fillRect(w.l + sidebarW(os) - 12f, w.t, w.l + sidebarW(os), w.b, side)
            // Barre de titre
            val tf = FontSpec(FontFamily.TEXT, 600, 18f)
            val appName = when (s.app) { DemoApp.WIN_SECURITY -> "Sécurité Windows"; DemoApp.MAC_SETTINGS -> ""; else -> "Paramètres" }
            when (os) {
                DemoOs.MAC -> listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(w.l + 26f + i * 24f, w.t + 26f, 7.5f, c) }
                DemoOs.UBUNTU -> {
                    g.drawText(appName, w.l + sidebarW(os) / 2 - g.measureText(appName, tf) / 2, w.t + 32f, tf.copy(weight = 700), ink)
                    g.fillCircle(w.r - 30f, w.t + 26f, 13f, rgb(0xDADADA))
                    g.line(w.r - 35f, w.t + 21f, w.r - 25f, w.t + 31f, 2f, ink); g.line(w.r - 35f, w.t + 31f, w.r - 25f, w.t + 21f, 2f, ink)
                }
                else -> {
                    g.drawText(appName, w.l + 20f, w.t + 30f, tf, ink)
                    g.line(w.r - 140f, w.t + 24f, w.r - 126f, w.t + 24f, 1.5f, ink)
                    g.strokeRoundRect(w.r - 90f, w.t + 17f, w.r - 77f, w.t + 30f, 1f, 1.5f, ink)
                    g.line(w.r - 40f, w.t + 17f, w.r - 26f, w.t + 31f, 1.5f, ink); g.line(w.r - 40f, w.t + 31f, w.r - 26f, w.t + 17f, 1.5f, ink)
                }
            }
            // Navigation
            var ny = w.t + 70f
            if (os == DemoOs.WINDOWS || os == DemoOs.CISCO) {
                if (s.app == DemoApp.WIN_SETTINGS) {
                    g.fillCircle(w.l + 50f, ny + 30f, 28f, rgb(0x8B5CF6))
                    g.drawText("C", w.l + 41f, ny + 40f, FontSpec(FontFamily.TEXT, 700, 26f), rgb(0xFFFFFF))
                    g.drawText("Codjia", w.l + 92f, ny + 24f, FontSpec(FontFamily.TEXT, 700, 19f), ink)
                    g.drawText("Compte local", w.l + 92f, ny + 48f, FontSpec(FontFamily.TEXT, 400, 15f), sub)
                    ny += 80f
                }
                g.fillRoundRect(w.l + 18f, ny, w.l + sidebarW(os) - 18f, ny + 40f, 6f, rgb(0xFFFFFF))
                g.drawText(if (s.app == DemoApp.WIN_SECURITY) "Rechercher" else "Rechercher un paramètre", w.l + 34f, ny + 26f, FontSpec(FontFamily.TEXT, 400, 15f), sub)
                ny += 60f
            } else if (os == DemoOs.MAC) {
                g.fillRoundRect(w.l + 16f, ny - 14f, w.l + sidebarW(os) - 16f, ny + 20f, 8f, rgb(0xD9D9DE))
                g.drawText("Rechercher", w.l + 44f, ny + 8f, FontSpec(FontFamily.TEXT, 400, 15f), sub)
                ny += 40f
            }
            val nf = FontSpec(FontFamily.TEXT, 500, 21f)
            val palette = listOf(rgb(0x2B7DE9), rgb(0x16A34A), rgb(0xF59E0B), rgb(0xEF4444), rgb(0x8B5CF6), rgb(0x0EA5E9), rgb(0x64748B), rgb(0xEC4899))
            for ((i, item) in s.navItems.withIndex()) {
                if (ny > w.b - 40f) break
                val sel = item == s.nav
                if (sel) {
                    g.fillRoundRect(w.l + 12f, ny, w.l + sidebarW(os) - 12f, ny + 46f, 7f, if (os == DemoOs.MAC) blue else rgb(0xE1E4EA))
                    if (os != DemoOs.MAC) g.fillRoundRect(w.l + 14f, ny + 12f, w.l + 19f, ny + 34f, 2f, blue)
                }
                val c = palette[i % palette.size]
                if (os == DemoOs.MAC) g.fillRoundRect(w.l + 26f, ny + 9f, w.l + 54f, ny + 37f, 7f, c) else g.fillCircle(w.l + 40f, ny + 23f, 10f, c)
                val lab = TextLayout.fit(g, item, nf, sidebarW(os) - 86f, 1, minSize = 16f)
                g.drawText(lab.lines.firstOrNull() ?: "", w.l + 68f, ny + 30f, lab.font, if (sel && os == DemoOs.MAC) rgb(0xFFFFFF) else ink)
                ny += 52f
            }
            // Contenu
            val p = pane(os)
            val title = cache.fit(g, "set/${s.title}", s.title, FontSpec(FontFamily.TEXT, 700, if (os == DemoOs.MAC) 36f else 44f), p.w, 1)
            g.drawText(title.lines.first(), p.l, p.t + 46f, title.font, ink)
            for ((i, row) in s.rows.withIndex()) {
                val r = rowRect(os, s, i)
                if (r.b > w.b - 10f) break
                drawRow(g, step, s, i, row, r, t, ink, sub, blue)
            }
        }
    }

    private fun drawRow(g: Gfx, step: DemoStep, s: DemoScreen.Settings, i: Int, row: DemoRow, r: R, t: Float, ink: Int, sub: Int, blue: Int) {
        val act = step.action as? DemoAction.ClickRow
        val clicked = act != null && act.row == i
        val at = clickAt(step) ?: 0f
        val pressed = if (clicked) window(t, at, 120f) * (1 - window(t, at + 120f, 200f)) else 0f
        val highlight = if (clicked) Easing.EXPO_OUT.ease(window(t, at - 500f, 400f)) else 0f
        g.fillRoundRect(r.l, r.t, r.r, r.b, 8f, mix(rgb(0xFFFFFF), rgb(0xEAF1FB), highlight * 0.7f + pressed * 0.3f))
        g.strokeRoundRect(r.l, r.t, r.r, r.b, 8f, 1f, mix(rgb(0xE2E4E8), blue, highlight))
        var tx = r.l + 24f
        row.icon?.let { ic -> Icons.draw(g, ic, r.l + 44f, r.cy, 38f, ink, 1.8f); tx = r.l + 86f }
        val lf = FontSpec(FontFamily.TEXT, 600, 30f)
        val df = FontSpec(FontFamily.TEXT, 400, 23f)
        val ctrl = controlRect(step.os, r, row.control)
        val statusW = (row.control as? DemoControl.Status)?.let { g.measureText(it.text, df) + 60f } ?: 0f
        val maxW = when (val c = row.control) {
            is DemoControl.Button -> r.r - 48f - (g.measureText(c.label, FontSpec(FontFamily.TEXT, 600, 23f)) + 48f) - tx - 24f
            else -> ctrl.l - tx - statusW - (if (c is DemoControl.Toggle) 150f else 24f)
        }
        val lbB = TextLayout.fit(g, row.label, lf, maxW, 1, minSize = 22f)
        val lb = lbB.lines.firstOrNull() ?: ""
        if (row.detail != null) {
            g.drawText(lb, tx, r.cy - 8f, lbB.font, ink)
            val dbB = TextLayout.fit(g, row.detail, df, maxW, 1, minSize = 18f)
            g.drawText(dbB.lines.firstOrNull() ?: "", tx, r.cy + 30f, dbB.font, sub)
        } else g.drawText(lb, tx, r.cy + 11f, lbB.font, ink)
        val after = window(t, at + 60f, 260f)
        when (val c = row.control) {
            is DemoControl.Toggle -> {
                val target = if (clicked) act?.toggleTo else null
                val on = if (target != null) lerp(if (c.on) 1f else 0f, if (target) 1f else 0f, Easing.EXPO_OUT.ease(after)) else if (c.on) 1f else 0f
                val state = if (on > 0.5f) (if (step.os == DemoOs.WINDOWS || step.os == DemoOs.CISCO) "Activé" else "") else (if (step.os == DemoOs.WINDOWS || step.os == DemoOs.CISCO) "Désactivé" else "")
                if (state.isNotEmpty()) g.drawText(state, ctrl.l - 18f - g.measureText(state, df), r.cy + 8f, df, ink)
                toggle(g, ctrl, on, blue, step.os)
            }
            is DemoControl.Button -> {
                val bf = FontSpec(FontFamily.TEXT, 600, 23f)
                val bw = g.measureText(c.label, bf) + 48f
                val br = R(r.r - 24f - bw, r.cy - 28f, r.r - 24f, r.cy + 28f)
                g.fillRoundRect(br.l, br.t, br.r, br.b, 8f, mix(blue, rgb(0x003E92), pressed))
                g.drawText(c.label, br.l + 24f, r.cy + 8f, bf, rgb(0xFFFFFF))
            }
            is DemoControl.Status -> {
                val txt = if (clicked && act?.statusAfter != null && after > 0.5f) act.statusAfter else c.text
                val col = if (c.ok) rgb(0x0F7B0F) else rgb(0xC42B1C)
                g.fillCircle(ctrl.r - 16f, r.cy, 15f, col)
                Icons.draw(g, if (c.ok) IconId.CHECK else IconId.CROSS, ctrl.r - 16f, r.cy, 20f, rgb(0xFFFFFF), 3f)
                val sw = g.measureText(txt, df)
                g.drawText(txt, ctrl.r - 44f - sw, r.cy + 8f, df, col)
            }
            DemoControl.Chevron -> g.strokePath(PathData().moveTo(r.r - 40f, r.cy - 9f).lineTo(r.r - 31f, r.cy).lineTo(r.r - 40f, r.cy + 9f), 2.2f, sub)
            DemoControl.None -> {}
        }
    }

    private fun toggle(g: Gfx, r: R, on: Float, blue: Int, os: DemoOs) {
        val win = os == DemoOs.WINDOWS || os == DemoOs.CISCO
        val h = r.h
        if (win) {
            g.fillRoundRect(r.l, r.t, r.r, r.b, h / 2, mix(rgb(0xFFFFFF), blue, on))
            g.strokeRoundRect(r.l, r.t, r.r, r.b, h / 2, 1.5f, mix(rgb(0x5F6470), blue, on))
            g.fillCircle(lerp(r.l + h / 2, r.r - h / 2, on), r.cy, h / 2 - 7f + on * 1f, mix(rgb(0x5F6470), rgb(0xFFFFFF), on))
        } else {
            g.fillRoundRect(r.l, r.t, r.r, r.b, h / 2, mix(rgb(0xD5D5DA), blue, on))
            g.fillCircle(lerp(r.l + h / 2, r.r - h / 2, on), r.cy, h / 2 - 3f, rgb(0xFFFFFF))
        }
    }

    // ───────────────────────────── Terminal ─────────────────────────────

    private fun terminalWindow(os: DemoOs): R = when (os) {
        DemoOs.MAC -> R(260f, 70f, 1340f, 900f)
        DemoOs.CISCO -> R(250f, 50f, 1350f, 930f)
        DemoOs.UBUNTU -> R(240f, 70f, 1360f, 920f)
        DemoOs.WINDOWS -> R(250f, 50f, 1350f, 930f)
    }

    private val termFont = FontSpec(FontFamily.MONO, 400, 30f)
    private val lineH = 42f

    private fun terminalBody(os: DemoOs) = terminalWindow(os).let { R(it.l + 26f, it.t + 66f, it.r - 26f, it.b - 20f) }

    private fun schedule(lines: List<TermLine>) = termSchedule(lines)

    private fun terminalBottom(step: DemoStep, s: DemoScreen.Terminal): Float {
        val body = terminalBody(step.os)
        val maxRows = (body.h / lineH).toInt()
        val n = (s.history.size + s.lines.size).coerceAtMost(maxRows)
        return body.t + n * lineH
    }

    private fun terminal(g: Gfx, step: DemoStep, s: DemoScreen.Terminal, t: Float) {
        val os = step.os
        val w = terminalWindow(os)
        val fresh = step.previous?.screen !is DemoScreen.Terminal
        val open = if (fresh) Easing.EXPO_OUT.ease(window(t, 150f, 500f)) else 1f
        val (bar, bg, fg, user, path) = when (os) {
            DemoOs.UBUNTU -> listOf(rgb(0x2B2B2B), rgb(0x300A24), rgb(0xFFFFFF), rgb(0x8AE234), rgb(0x729FCF))
            DemoOs.MAC -> listOf(rgb(0x3A3A3C), rgb(0x1E1E1E), rgb(0xF2F2F2), rgb(0xF2F2F2), rgb(0x64D2FF))
            DemoOs.WINDOWS -> listOf(rgb(0x1F1F1F), rgb(0x0C0C0C), rgb(0xCCCCCC), rgb(0xCCCCCC), rgb(0xCCCCCC))
            DemoOs.CISCO -> listOf(rgb(0x1F1F1F), rgb(0x000000), rgb(0xD0D0D0), rgb(0xD0D0D0), rgb(0xD0D0D0))
        }
        g.withPose(Pose(alpha = open, scale = lerp(0.95f, 1f, open)), w.cx, w.cy) {
            g.fillRoundRect(w.l + 8f, w.t + 16f, w.r + 8f, w.b + 16f, 14f, 0x55000000)
            g.fillRoundRect(w.l, w.t, w.r, w.b, 12f, bg)
            g.fillRoundRect(w.l, w.t, w.r, w.t + 48f, 12f, bar)
            g.fillRect(w.l, w.t + 36f, w.r, w.t + 48f, bar)
            val tf = FontSpec(FontFamily.TEXT, 600, 18f)
            if (os == DemoOs.MAC) listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(w.l + 24f + i * 24f, w.t + 24f, 7.5f, c) }
            else {
                g.line(w.r - 40f, w.t + 17f, w.r - 26f, w.t + 31f, 1.6f, rgb(0xDDDDDD)); g.line(w.r - 40f, w.t + 31f, w.r - 26f, w.t + 17f, 1.6f, rgb(0xDDDDDD))
            }
            g.drawText(s.title, w.cx - g.measureText(s.title, tf) / 2, w.t + 30f, tf, rgb(0xE6E6E6))
            val body = terminalBody(os)
            val sched = schedule(s.lines)
            // Lignes visibles : historique + nouvelles lignes déjà commencées ; défilement vers le haut si besoin.
            data class Shown(val line: TermLine, val reveal: Float, val typing: Boolean)
            val shown = ArrayList<Shown>()
            s.history.forEach { shown += Shown(it, 1f, false) }
            for ((i, l) in s.lines.withIndex()) {
                val (st, d) = sched[i]
                if (t < st) {
                    // Invite vide avant la prochaine commande
                    if (l.command && (i == 0 || t >= sched[i - 1].first + sched[i - 1].second)) shown += Shown(l.copy(text = ""), 1f, true)
                    break
                }
                val p = if (d <= 0f) 1f else clamp01((t - st) / d)
                shown += Shown(l, p, l.command && p < 1f)
            }
            if (shown.isEmpty() || shown.last().line.command.not() && s.lines.isNotEmpty() && t >= sched.last().first) shown += Shown(TermLine("", true), 1f, true)
            val maxRows = (body.h / lineH).toInt()
            val visible = shown.takeLast(maxRows)
            val caretOn = (t / 450f).toInt() % 2 == 0
            for ((k, sh) in visible.withIndex()) {
                val base = body.t + 30f + k * lineH
                var x = body.l
                if (sh.line.command) {
                    val prompt = sh.line.prompt ?: s.prompt
                    if (os == DemoOs.UBUNTU && prompt.contains(':')) {
                        val u = prompt.substringBefore(':') + ":"
                        val rest = prompt.substringAfter(':')
                        g.drawText(u, x, base, termFont.copy(weight = 700), user); x += g.measureText(u, termFont.copy(weight = 700))
                        val pth = rest.trimEnd('$', ' ', '#')
                        g.drawText(pth, x, base, termFont.copy(weight = 700), path); x += g.measureText(pth, termFont.copy(weight = 700))
                        val tail = rest.substring(pth.length)
                        g.drawText(tail, x, base, termFont, fg); x += g.measureText(tail, termFont)
                    } else { g.drawText(prompt, x, base, termFont, user); x += g.measureText(prompt, termFont) }
                    val txt = sh.line.text.take((sh.line.text.length * sh.reveal).toInt())
                    g.drawText(txt, x, base, termFont, fg); x += g.measureText(txt, termFont)
                    if (sh.typing && (caretOn || sh.reveal in 0.001f..0.999f)) g.fillRect(x + 2f, base - 24f, x + 16f, base + 6f, fg.withAlpha(0.85f))
                } else {
                    // Sortie brute : les espaces d'alignement des tableaux sont conservés, coupure à la largeur.
                    var txt = sh.line.text
                    if (g.measureText(txt, termFont) > body.w) {
                        while (txt.isNotEmpty() && g.measureText("$txt…", termFont) > body.w) txt = txt.dropLast(1)
                        txt += "…"
                    }
                    g.drawText(txt, x, base, termFont, fg.withAlpha(0.92f))
                }
            }
        }
    }
}

/** Instants d'apparition des lignes d'un terminal : (début, durée de frappe). Partagé avec le planificateur. */
internal fun termSchedule(lines: List<TermLine>): List<Pair<Float, Float>> {
    var t = 700f
    return lines.map { l ->
        if (l.command) { val d = 250f + l.text.length * 45f; val s = t; t += d + 400f; s to d }
        else { val s = t; t += 80f; s to 0f }
    }
}

internal fun termDuration(lines: List<TermLine>): Float = termSchedule(lines).lastOrNull()?.let { it.first + it.second } ?: 0f
