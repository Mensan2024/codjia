// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.sin

/**
 * Format « Démo à l'écran » : la scène se joue dans une fenêtre de système (terminal Linux, PowerShell,
 * terminal macOS, console d'équipement réseau) ou sur un téléphone, avec frappe au clavier, curseur
 * de souris et clics. Interfaces génériques dessinées en vectoriel : aucun logo ni marque.
 *
 * Règle d'honnêteté : on ne montre que des commandes présentes dans le texte de l'utilisateur ; on
 * n'invente jamais de sortie de commande.
 */
internal class DemoLook(private val sb: Storyboard, private val template: Template) : LookRenderer {
    enum class Os { LINUX, WINDOWS, MAC, NETWORK, PHONE }

    private val os: Os = detectOs()
    override val ink = rgb(0xF2F4F8)
    override val muted = rgb(0x9AA4B2)
    override val accent: Int = template.style.accent.let { if (luma(it) < 0.35f) rgb(0x4CC2FF) else it }
    override val accent2: Int = template.style.accent2
    private val ok = rgb(0x3FB950)
    private val danger = rgb(0xF85149)
    private val cache = TextCache()

    private data class Theme(val bar: Int, val body: Int, val text: Int, val prompt: Int, val promptText: String, val title: String)
    private val theme = when (os) {
        Os.LINUX -> Theme(rgb(0x2D2F36), rgb(0x16181D), rgb(0xE6EDF3), rgb(0x7EE787), "codjia@linux:~$ ", "Terminal — bash")
        Os.WINDOWS -> Theme(rgb(0x1F1F1F), rgb(0x0C1E3A), rgb(0xF0F0F0), rgb(0xF0F0F0), "PS C:\\Users\\codjia> ", "Windows PowerShell")
        Os.MAC -> Theme(rgb(0x37373A), rgb(0x1C1C1E), rgb(0xF2F2F7), rgb(0x64D2FF), "codjia@mac ~ % ", "Terminal — zsh")
        Os.NETWORK -> Theme(rgb(0x23282E), rgb(0x0D1117), rgb(0xC9F7D0), rgb(0x56D364), "SW1# ", "Console — SW1 (9600 bauds)")
        Os.PHONE -> Theme(rgb(0xF2F2F7), rgb(0xFFFFFF), rgb(0x1C1C1E), rgb(0x0A84FF), "~ $ ", "Paramètres")
    }
    private val light = os == Os.PHONE

    private fun detectOs(): Os {
        val t = (sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body + " " + it.items.joinToString(" ") }).lowercase()
        return when {
            Regex("android|smartphone|téléphone|telephone|mobile").containsMatchIn(t) -> Os.PHONE
            Regex("powershell|windows|regedit|active directory|get-|winget|\\brdp\\b|bureau à distance").containsMatchIn(t) -> Os.WINDOWS
            Regex("macos|mac os|finder|zsh|homebrew|\\bbrew\\b").containsMatchIn(t) -> Os.MAC
            Regex("vlan|ospf|eigrp|bgp|switchport|cisco|routeur|router|\\bswitch\\b|ccna|ccnp|trunk|hsrp").containsMatchIn(t) -> Os.NETWORK
            else -> Os.LINUX
        }
    }

    // ───────────────────────────── Cadre ─────────────────────────────

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        background(g, tGlobalMs)
        val capBottom = caption(g, sc, localMs)
        val top = (capBottom + 50f).coerceAtLeast(520f)
        val win = frame(g, sc, top, localMs)
        g.save(); g.clipRect(win.l, win.t, win.r, win.b)
        val targets = when (sc.layout) {
            Layout.HERO -> hero(g, sc, win, localMs)
            Layout.DEFINITION -> definition(g, sc, win, localMs)
            Layout.STEPS -> steps(g, sc, win, localMs)
            Layout.STAT -> stat(g, sc, win, localMs)
            Layout.COMPARE -> compare(g, sc, win, localMs)
            Layout.CHECKLIST -> checklist(g, sc, win, localMs)
            Layout.SCREEN -> emptyList()
        }
        g.restore()
        cursor(g, targets, localMs, win)
    }

    private fun background(g: Gfx, t: Float) {
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, rgb(0x0B0E14), rgb(0x151A23))
        g.fillRadialGlow(540f + 120f * sin(t * 0.0004f), 1100f, 900f, accent.withAlpha(0.13f))
        for (i in 0 until 12) for (j in 0 until 22) g.fillCircle(60f + i * 88f, 40f + j * 88f, 1.6f, 0x14FFFFFF)
    }

    /** Légende au-dessus de la fenêtre : petit badge + titre de la scène. Renvoie le bas de la légende. */
    private fun caption(g: Gfx, sc: Scene, t: Float): Float {
        var y = 190f
        sc.kicker?.let { k ->
            val f = FontSpec(FontFamily.MONO, 700, 30f)
            val text = "> " + k.uppercase()
            val w = g.measureText(text, f) + 40f
            val a = Easing.EXPO_OUT.ease(window(t, 0f, 500f))
            g.withPose(Pose(alpha = a, dx = (1 - a) * -30f), Safe.LEFT, y) {
                g.fillRoundRect(Safe.LEFT, y, Safe.LEFT + w, y + 56f, 10f, accent.withAlpha(0.16f))
                g.strokeRoundRect(Safe.LEFT, y, Safe.LEFT + w, y + 56f, 10f, 2f, accent.withAlpha(0.6f))
                g.drawText(text, Safe.LEFT + 20f, y + 28f + g.metrics(f).ascent / 2, f, accent)
            }
            y += 84f
        }
        val marked = Marked.parse(sc.title)
        val b = cache.fit(g, "${sc.id}/cap", marked.plain, FontSpec(FontFamily.TEXT, 800, 66f), Safe.WIDTH, 3, 1.12f)
        val runs = MarkedText.runs(b, marked)
        for (i in b.lines.indices) {
            val a = Easing.EXPO_OUT.ease(window(t, 120f + i * 110f, 600f))
            val base = g.baselineOf(b, y, i)
            g.withPose(Pose(alpha = a, dy = (1 - a) * 34f), Safe.LEFT, base) {
                MarkedText.drawLine(g, runs[i], Safe.LEFT, base, b.font, ink, accent)
            }
        }
        return y + b.height
    }

    data class Box(val l: Float, val t: Float, val r: Float, val b: Float) {
        val w get() = r - l
        val h get() = b - t
    }

    /** Fenêtre (ou téléphone) qui apparaît avec un léger zoom. Renvoie la zone de contenu. */
    private fun windowTitle(sc: Scene): String = when (sc.layout) {
        Layout.DEFINITION -> theme.title
        Layout.STEPS -> if (terminalMode(sc)) theme.title else if (settingsMode(sc)) "Paramètres" else "Liste d'actions"
        Layout.HERO -> "Notification"
        Layout.STAT -> "Moniteur"
        Layout.COMPARE -> "Analyse de sécurité"
        Layout.CHECKLIST -> "Liste de contrôle"
        Layout.SCREEN -> theme.title
    }

    /** Interrupteurs seulement si les étapes parlent vraiment de réglages ; sinon, cases à cocher (pas de faux « Paramètres »). */
    private fun settingsMode(sc: Scene): Boolean {
        val t = sc.items.joinToString(" ") { Marked.strip(it) }.lowercase()
        return Regex("\\b(active|activer|désactive|désactiver|desactive|autorise|autoriser|bloque|bloquer|paramètre|paramètres|réglage|réglages|option|options|mode)\\b").containsMatchIn(t)
    }

    private fun terminalMode(sc: Scene): Boolean {
        val parts = sc.items.map { splitLabel(Marked.strip(it)) }
        val commands = parts.count { (l, d) -> isCommand(l) || (d != null && isCommand(d)) }
        return commands * 2 >= parts.size && parts.isNotEmpty() && !light
    }

    private fun frame(g: Gfx, sc: Scene, top: Float, t: Float): Box {
        val title = if (os == Os.PHONE) theme.title else windowTitle(sc)
        val e = Easing.EXPO_OUT.ease(window(t, 150f, 700f))
        val bottom = 1555f
        if (os == Os.PHONE) {
            val l = 250f; val r = 830f
            val tt = top.coerceAtMost(620f)
            g.withPose(Pose(alpha = e, scale = lerp(0.94f, 1f, e)), 540f, (tt + bottom) / 2) {
                g.fillRoundRect(l - 22f + 10f, tt - 22f + 24f, r + 22f + 10f, bottom + 22f + 24f, 70f, 0x55000000)
                g.fillRoundRect(l - 22f, tt - 22f, r + 22f, bottom + 22f, 70f, rgb(0x0E0F12))
                g.strokeRoundRect(l - 22f, tt - 22f, r + 22f, bottom + 22f, 70f, 3f, rgb(0x3A3D44))
                g.fillRoundRect(l, tt, r, bottom, 52f, theme.body)
                // Barre d'état
                val f = FontSpec(FontFamily.TEXT, 700, 26f)
                g.drawText("09:41", l + 44f, tt + 52f, f, theme.text)
                g.fillRoundRect(r - 110f, tt + 34f, r - 62f, tt + 54f, 5f, theme.text)
                g.fillRoundRect(500f, tt + 22f, 580f, tt + 50f, 14f, rgb(0x0E0F12))
                val hf = FontSpec(FontFamily.TEXT, 800, 44f)
                g.drawText(theme.title, l + 40f, tt + 140f, hf, theme.text)
            }
            return Box(l, tt + 170f, r, bottom - 30f)
        }
        val l = 66f; val r = 1014f
        g.withPose(Pose(alpha = e, scale = lerp(0.94f, 1f, e)), 540f, (top + bottom) / 2) {
            g.fillRoundRect(l + 12f, top + 26f, r + 12f, bottom + 30f, 22f, 0x66000000)
            g.fillRoundRect(l, top, r, bottom, 18f, theme.body)
            g.fillRoundRect(l, top, r, top + 64f, 18f, theme.bar)
            g.fillRect(l, top + 40f, r, top + 64f, theme.bar)
            g.strokeRoundRect(l, top, r, bottom, 18f, 2f, 0x33FFFFFF)
            val f = FontSpec(FontFamily.TEXT, 600, 26f)
            when (os) {
                Os.MAC, Os.LINUX -> {
                    val cols = if (os == Os.MAC) listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)) else listOf(rgb(0x6E7681), rgb(0x6E7681), rgb(0xE5534B))
                    cols.forEachIndexed { i, c -> g.fillCircle(l + 36f + i * 34f, top + 32f, 10f, c) }
                    g.drawText(title, 540f - g.measureText(title, f) / 2, top + 41f, f, rgb(0xB8BDC6))
                }
                else -> {
                    g.drawText(title, l + 28f, top + 41f, f, rgb(0xD0D4DA))
                    val c = rgb(0xD0D4DA)
                    g.line(r - 160f, top + 34f, r - 140f, top + 34f, 2f, c)
                    g.strokeRoundRect(r - 102f, top + 24f, r - 84f, top + 42f, 2f, 2f, c)
                    g.line(r - 46f, top + 24f, r - 28f, top + 42f, 2f, c); g.line(r - 46f, top + 42f, r - 28f, top + 24f, 2f, c)
                }
            }
        }
        return Box(l + 36f, top + 96f, r - 36f, bottom - 30f)
    }

    // ───────────────────────────── Outils de contenu ─────────────────────────────

    private val fMono = FontSpec(FontFamily.MONO, 400, 34f)
    private val fMonoB = FontSpec(FontFamily.MONO, 700, 34f)
    private val fUi = FontSpec(FontFamily.TEXT, 600, 36f)
    private val fUiS = FontSpec(FontFamily.TEXT, 500, 30f)
    private val textCol get() = theme.text
    private val dim get() = if (light) rgb(0x6B7280) else rgb(0x8B949E)
    private val card get() = if (light) rgb(0xF2F2F7) else 0x14FFFFFF
    private val lineCol get() = if (light) rgb(0xD1D5DB) else 0x22FFFFFF

    /** Ligne tapée au clavier. Renvoie le bas du texte. */
    private fun typed(g: Gfx, key: String, text: String, font: FontSpec, x: Float, top: Float, w: Float, lines: Int, color: Int,
                      t: Float, start: Float, msPerChar: Float = 38f, caret: Boolean = true): Float {
        val b = cache.fit(g, key, text, font, w, lines, 1.3f)
        val dur = (b.charCount * msPerChar).coerceIn(250f, 2600f)
        val p = window(t, start, dur)
        if (p > 0f) g.drawBlock(b, x, top, color, reveal = p, caretColor = accent, caretVisible = caret && p < 1f || (caret && ((t / 420f).toInt() % 2 == 0)))
        return top + b.height
    }

    private fun typeDuration(g: Gfx, key: String, text: String, font: FontSpec, w: Float, lines: Int, msPerChar: Float = 38f): Float =
        (cache.fit(g, key, text, font, w, lines, 1.3f).charCount * msPerChar).coerceIn(250f, 2600f)

    /** Interrupteur (on/off) animé. */
    private fun toggle(g: Gfx, x: Float, cy: Float, on: Float) {
        val w = 92f; val h = 52f
        g.fillRoundRect(x, cy - h / 2, x + w, cy + h / 2, h / 2, mix(if (light) rgb(0xD1D5DB) else rgb(0x3A3F47), ok, on))
        g.fillCircle(lerp(x + h / 2, x + w - h / 2, on), cy, h / 2 - 5f, rgb(0xFFFFFF))
    }

    private fun checkbox(g: Gfx, x: Float, cy: Float, on: Float) {
        val s = 44f
        g.fillRoundRect(x, cy - s / 2, x + s, cy + s / 2, 9f, mix(card, accent, on))
        g.strokeRoundRect(x, cy - s / 2, x + s, cy + s / 2, 9f, 2.5f, mix(dim, accent, on))
        if (on > 0f) Icons.draw(g, IconId.CHECK, x + s / 2, cy, 34f, rgb(0xFFFFFF).withAlpha(on), 3f, reveal = on)
    }

    private fun button(g: Gfx, text: String, x: Float, y: Float, color: Int, pressed: Float): Box {
        val f = FontSpec(FontFamily.TEXT, 700, 34f)
        val w = (g.measureText(text, f) + 70f).coerceAtMost(860f)
        val sc = 1f - 0.05f * pressed
        g.withPose(Pose(scale = sc), x + w / 2, y + 40f) {
            g.fillRoundRect(x, y, x + w, y + 80f, 16f, color)
            g.drawText(text, x + 35f, y + 40f + g.metrics(f).ascent / 2, f, if (luma(color) > 0.6f) rgb(0x111111) else rgb(0xFFFFFF))
        }
        return Box(x, y, x + w, y + 80f)
    }

    /** Cible du curseur : à l'instant `at`, le curseur est sur (x, y) ; `click` = clic à cet instant. */
    private data class Target(val at: Float, val x: Float, val y: Float, val click: Boolean = true)

    private fun cursor(g: Gfx, targets: List<Target>, t: Float, win: Box) {
        if (targets.isEmpty()) return
        val start = Target(targets.first().at - 900f, win.r - 60f, win.b + 120f, false)
        val all = listOf(start) + targets
        var x = start.x; var y = start.y
        for (i in 1 until all.size) {
            val a = all[i - 1]; val b = all[i]
            val moveStart = maxOf(a.at + 150f, b.at - 650f)
            if (t >= b.at) { x = b.x; y = b.y; continue }
            val k = Easing.EASE_IN_OUT.ease(window(t, moveStart, b.at - moveStart))
            x = lerp(a.x, b.x, k); y = lerp(a.y, b.y, k) - sin(k * kotlin.math.PI.toFloat()) * 40f
            break
        }
        val alpha = window(t, start.at, 300f)
        if (alpha <= 0f) return
        // Onde de clic
        for (tg in targets) if (tg.click) {
            val c = window(t, tg.at, 450f)
            if (c > 0f && c < 1f) g.strokeCircle(tg.x, tg.y, 14f + 46f * c, 4f * (1 - c), accent.withAlpha(1 - c))
        }
        val p = PathData().moveTo(x, y).lineTo(x, y + 46f).lineTo(x + 12f, y + 35f).lineTo(x + 21f, y + 55f)
            .lineTo(x + 29f, y + 51f).lineTo(x + 20f, y + 32f).lineTo(x + 35f, y + 32f).close()
        g.save(); g.multiplyAlpha(alpha)
        g.fillPath(p.transformed(1f, 4f, 6f), 0x55000000)
        g.fillPath(p, rgb(0xFFFFFF)); g.strokePath(p, 2.5f, rgb(0x111111))
        g.restore()
    }

    private fun isCommand(s: String): Boolean {
        val first = s.trim().substringBefore(' ').lowercase()
        return first in COMMANDS || first.startsWith("get-") || first.startsWith("set-") || first.startsWith("./") ||
            Regex("^[a-z][a-z0-9-]*\\s+-{1,2}[a-z]").containsMatchIn(s.trim())
    }

    // ───────────────────────────── Scènes ─────────────────────────────

    private fun hero(g: Gfx, sc: Scene, w: Box, t: Float): List<Target> {
        // Notification système : icône, message tapé, bouton « Voir l'astuce ».
        val cx = (w.l + w.r) / 2
        val iy = w.t + 150f
        val a = Easing.BACK_OUT.ease(window(t, 600f, 600f))
        g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.6f, 1f, a)), cx, iy) {
            g.fillCircle(cx, iy, 104f, accent.withAlpha(0.18f))
            g.strokeCircle(cx, iy, 104f, 3f, accent.withAlpha(0.7f))
            Icons.draw(g, sc.iconOrDefault(), cx, iy, 110f, accent, 2f)
        }
        var y = iy + 170f
        val msg = Marked.strip(sc.subtitle ?: sc.body)
        if (msg.isNotEmpty()) {
            y = typed(g, "${sc.id}/m", msg, fUi.copy(weight = 500), w.l + 30f, y, w.w - 60f, 4, textCol, t, 1100f, 26f, caret = false) + 50f
        }
        val btnAt = 1100f + typeDuration(g, "${sc.id}/m", msg, fUi.copy(weight = 500), w.w - 60f, 4, 26f) + 600f
        val pressed = window(t, btnAt, 160f) * (1 - window(t, btnAt + 160f, 200f))
        val ba = window(t, btnAt - 700f, 400f)
        var tg = emptyList<Target>()
        if (ba > 0f) g.withPose(Pose(alpha = ba), cx, y) {
            val label = "Voir l'astuce  →"
            val bw = g.measureText(label, FontSpec(FontFamily.TEXT, 700, 34f)) + 70f
            val b = button(g, label, cx - bw / 2, y, accent, pressed)
            tg = listOf(Target(btnAt, (b.l + b.r) / 2 + 30f, b.t + 40f))
        }
        return tg.ifEmpty { listOf(Target(btnAt, cx + 30f, y + 40f)) }
    }

    private fun definition(g: Gfx, sc: Scene, w: Box, t: Float): List<Target> {
        var y = w.t + 10f
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        if (light) {
            y = typed(g, "${sc.id}/b", body, fUi.copy(weight = 500), w.l + 30f, y + 10f, w.w - 60f, 9, textCol, t, 600f, 24f)
            return emptyList()
        }
        // Une note dans le terminal : commentaire tapé, ligne par ligne.
        val pf = fMonoB.withSize(40f)
        val bf = fMono.withSize(40f)
        val pw = g.measureText("# ", pf)
        g.drawText("#", w.l, y + 44f, pf, dim)
        y = typed(g, "${sc.id}/b", body, bf, w.l + pw, y, w.w - pw, 12, textCol, t, 600f, 24f) + 36f
        val end = 600f + typeDuration(g, "${sc.id}/b", body, bf, w.w - pw, 12, 24f)
        if (t > end) {
            g.drawText(theme.promptText, w.l, y + 30f, fMonoB, theme.prompt)
            if ((t / 420f).toInt() % 2 == 0) {
                val x = w.l + g.measureText(theme.promptText, fMonoB) + 4f
                g.fillRect(x, y, x + 18f, y + 38f, accent)
            }
        }
        return emptyList()
    }

    private fun steps(g: Gfx, sc: Scene, w: Box, t: Float): List<Target> {
        val parts = sc.items.map { splitLabel(Marked.strip(it)) }
        return if (terminalMode(sc)) terminalSteps(g, sc, parts, w, t) else toggleSteps(g, sc, parts, w, t)
    }

    /** Les étapes sont des commandes : commentaire (#), puis la commande tapée. */
    private fun terminalSteps(g: Gfx, sc: Scene, parts: List<Pair<String, String?>>, w: Box, t: Float): List<Target> {
        var y = w.t
        var start = 700f
        val pw = g.measureText(theme.promptText, fMonoB)
        val budget = (sc.durationMs * 0.82f - 700f) / parts.size.coerceAtLeast(1)
        for ((i, p) in parts.withIndex()) {
            val (label, detail) = p
            val (cmd, note) = if (isCommand(label)) label to detail else (detail ?: label) to label
            if (note != null) {
                if (t >= start) y = typed(g, "${sc.id}/n$i", "# $note", fMono, w.l, y, w.w, 2, dim, t, start, 14f, caret = false)
                else y += cache.fit(g, "${sc.id}/n$i", "# $note", fMono, w.w, 2, 1.3f).height
            }
            if (t >= start + 200f) {
                g.drawText(theme.promptText, w.l, y + 34f, fMonoB, theme.prompt)
            }
            val cmdStart = start + 450f
            y = typed(g, "${sc.id}/c$i", cmd, fMonoB, w.l + pw, y, w.w - pw, 3, textCol, t, cmdStart, (budget * 0.55f / cmd.length.coerceAtLeast(1)).coerceIn(22f, 60f),
                caret = t < start + budget) + 28f
            start += budget
        }
        return emptyList()
    }

    /** Les étapes sont des réglages : chaque ligne a un interrupteur que le curseur active. */
    private fun toggleSteps(g: Gfx, sc: Scene, parts: List<Pair<String, String?>>, w: Box, t: Float): List<Target> {
        val n = parts.size.coerceAtLeast(1)
        val rowH = ((w.h - 20f) / n).coerceIn(120f, 210f)
        val targets = ArrayList<Target>()
        val settings = settingsMode(sc)
        val step = ((sc.durationMs * 0.8f - 900f) / n).coerceAtLeast(500f)
        for ((i, p) in parts.withIndex()) {
            val (label, detail) = p
            val top = w.t + i * rowH
            val appear = Easing.EXPO_OUT.ease(window(t, 300f + i * 120f, 500f))
            val clickAt = 900f + i * step + step * 0.6f
            val on = Easing.EXPO_OUT.ease(window(t, clickAt, 260f))
            g.withPose(Pose(alpha = appear, dy = (1 - appear) * 20f), w.l, top) {
                g.fillRoundRect(w.l, top + 6f, w.r, top + rowH - 10f, 18f, mix(card, ok.withAlpha(0.18f), on))
                val tw = w.w - 160f
                val lb = cache.fit(g, "${sc.id}/l$i", label, fUi, tw, if (detail == null) 2 else 1, 1.2f)
                val db = detail?.let { cache.fit(g, "${sc.id}/d$i", it, fUiS, tw, 2, 1.2f) }
                val blockH = lb.height + (db?.height?.plus(6f) ?: 0f)
                var ty = top + (rowH - 4f - blockH) / 2
                g.drawBlock(lb, w.l + 28f, ty, textCol)
                ty += lb.height + 6f
                db?.let { g.drawBlock(it, w.l + 28f, ty, dim) }
                if (settings) toggle(g, w.r - 120f, top + rowH / 2 - 2f, on)
                else checkbox(g, w.r - 100f, top + rowH / 2 - 2f, on)
            }
            targets += Target(clickAt, if (settings) w.r - 120f + 66f else w.r - 78f, top + rowH / 2)
        }
        return targets
    }

    private fun stat(g: Gfx, sc: Scene, w: Box, t: Float): List<Target> {
        val (num, unit) = statParts(sc.statValue ?: "")
        val cx = (w.l + w.r) / 2
        val target = num.filter { it.isDigit() }.toLongOrNull()
        val p = Easing.EXPO_OUT.ease(window(t, 500f, 1600f))
        // Le compteur n'anime que les nombres « ronds » ; un numéro de port reste affiché tel quel.
        val shown = if (target != null && num.all { it.isDigit() } && target in 2..100000 && sc.title.contains("port").not()) (target * p).toLong().toString() else num
        val f = fitSize(g, num, FontSpec(FontFamily.MONO, 700, 200f), w.w - 80f)
        val cy = w.t + 230f
        val a = window(t, 400f, 400f)
        g.withPose(Pose(alpha = a), cx, cy) {
            g.fillRoundRect(w.l + 10f, w.t + 20f, w.r - 10f, cy + 210f, 22f, card)
            val lift = if (unit.isNotEmpty()) 40f else 0f
            g.drawText(shown, cx - g.measureText(num, f) / 2, cy + g.metrics(f).ascent / 2 - lift, f, accent)
            if (unit.isNotEmpty()) {
                val uf = fitSize(g, unit, FontSpec(FontFamily.TEXT, 700, 44f), w.w - 80f)
                g.drawText(unit, cx - g.measureText(unit, uf) / 2, cy + 175f, uf, dim)
            }
        }
        // Mini graphique d'activité (décor, pas une donnée)
        val gy = cy + 280f
        val bars = 14
        val bw = (w.w - 40f) / bars
        for (i in 0 until bars) {
            val h = (60f + 140f * ((sin(i * 1.7f) + 1f) / 2f)) * Easing.EXPO_OUT.ease(window(t, 900f + i * 50f, 600f))
            g.fillRoundRect(w.l + 20f + i * bw + 6f, gy + 220f - h, w.l + 20f + (i + 1) * bw - 6f, gy + 220f, 6f, if (i == bars - 3) accent else lineCol)
        }
        return emptyList()
    }

    private fun compare(g: Gfx, sc: Scene, w: Box, t: Float): List<Target> {
        val cmp = sc.compare ?: return hero(g, sc, w, t)
        val cardH = ((w.h - 60f) / 2).coerceAtMost(380f)
        fun panel(top: Float, label: String, value: String, color: Int, icon: IconId, start: Float, key: String): Box {
            val a = Easing.EXPO_OUT.ease(window(t, start, 600f))
            g.withPose(Pose(alpha = a, dy = (1 - a) * 30f), w.l, top) {
                g.fillRoundRect(w.l, top, w.r, top + cardH, 20f, color.withAlpha(0.12f))
                g.strokeRoundRect(w.l, top, w.r, top + cardH, 20f, 3f, color.withAlpha(0.8f))
                g.fillCircle(w.l + 60f, top + 64f, 30f, color)
                Icons.draw(g, icon, w.l + 60f, top + 64f, 34f, rgb(0xFFFFFF), 3f)
                g.drawText(label, w.l + 110f, top + 64f + g.metrics(fUi).ascent / 2, fUi.copy(weight = 800), color)
                val vb = cache.fit(g, key, value, fMonoB, w.w - 60f, 3, 1.3f)
                g.drawBlock(vb, w.l + 30f, top + 124f, textCol)
            }
            return Box(w.l, top, w.r, top + cardH)
        }
        val shake = if (t in 1500f..1900f) sin(t * 0.09f) * 10f else 0f
        g.save(); g.translate(shake, 0f)
        val bad = panel(w.t, cmp.badLabel, cmp.badText(), danger, IconId.WARNING, 300f, "${sc.id}/bv")
        g.restore()
        val blocked = window(t, 1900f, 300f)
        if (blocked > 0f) {
            val f = FontSpec(FontFamily.TEXT, 800, 30f)
            val label = "BLOQUÉ"
            val bw = g.measureText(label, f) + 40f
            g.withPose(Pose(alpha = blocked, scale = lerp(1.4f, 1f, blocked)), bad.r - 30f - bw / 2, bad.t + 64f) {
                g.fillRoundRect(bad.r - 30f - bw, bad.t + 40f, bad.r - 30f, bad.t + 88f, 10f, danger)
                g.drawText(label, bad.r - 10f - bw, bad.t + 64f + g.metrics(f).ascent / 2, f, rgb(0xFFFFFF))
            }
        }
        val goodTop = w.t + cardH + 40f
        panel(goodTop, cmp.goodLabel, cmp.goodText(), ok, IconId.CHECK, 2300f, "${sc.id}/gv")
        val applyAt = 3300f
        val pressed = window(t, applyAt, 150f) * (1 - window(t, applyAt + 150f, 200f))
        val ba = window(t, 2700f, 400f)
        var tg = listOf(Target(1500f, bad.r - 140f, bad.t + 64f), Target(applyAt, w.r - 150f, goodTop + cardH - 70f))
        if (ba > 0f) g.withPose(Pose(alpha = ba), w.r, goodTop) {
            val done = window(t, applyAt + 200f, 200f)
            val label = if (done > 0f) "Appliqué" else "Appliquer"
            val f = FontSpec(FontFamily.TEXT, 700, 34f)
            val bw = g.measureText("Appliquer", f) + 70f
            val b = button(g, label, w.r - 30f - bw, goodTop + cardH - 110f, ok, pressed)
            tg = listOf(tg[0], Target(applyAt, (b.l + b.r) / 2, b.t + 40f))
        }
        return tg
    }

    private fun checklist(g: Gfx, sc: Scene, w: Box, t: Float): List<Target> {
        val n = sc.items.size.coerceAtLeast(1)
        val ctaH = if (sc.body.isNotEmpty()) 130f else 0f
        val rowH = ((w.h - ctaH - 20f) / n).coerceIn(100f, 170f)
        val step = ((sc.durationMs * 0.7f - 900f) / (n + 1)).coerceAtLeast(450f)
        val targets = ArrayList<Target>()
        for ((i, item) in sc.items.withIndex()) {
            val top = w.t + i * rowH
            val a = Easing.EXPO_OUT.ease(window(t, 300f + i * 110f, 500f))
            val clickAt = 900f + i * step
            val on = Easing.EXPO_OUT.ease(window(t, clickAt, 250f))
            g.withPose(Pose(alpha = a, dy = (1 - a) * 20f), w.l, top) {
                checkbox(g, w.l + 10f, top + rowH / 2, on)
                val b = cache.fit(g, "${sc.id}/i$i", Marked.strip(item), fUi, w.w - 100f, 2, 1.2f)
                g.drawBlock(b, w.l + 80f, top + (rowH - b.height) / 2, mix(textCol, dim, on * 0.3f))
                if (i < n - 1) g.fillRect(w.l + 80f, top + rowH - 1f, w.r, top + rowH, lineCol)
            }
            targets += Target(clickAt, w.l + 32f, top + rowH / 2)
        }
        if (sc.body.isNotEmpty()) {
            val at = 900f + n * step
            val pressed = window(t, at, 150f) * (1 - window(t, at + 150f, 200f))
            val y = w.t + n * rowH + 30f
            val a = window(t, at - 600f, 400f)
            if (a > 0f) g.withPose(Pose(alpha = a), w.l, y) {
                val b = button(g, Marked.strip(sc.body), w.l, y, accent, pressed)
                targets += Target(at, (b.l + b.r) / 2, b.t + 40f)
            }
        }
        return targets
    }

    private companion object {
        val COMMANDS = setOf("sudo", "git", "ls", "cd", "df", "du", "ps", "top", "htop", "chmod", "chown", "ssh", "scp", "ping", "ip",
            "ifconfig", "ipconfig", "netstat", "ss", "nmap", "apt", "dnf", "yum", "systemctl", "journalctl", "docker", "kubectl", "npm",
            "npx", "node", "python", "python3", "pip", "curl", "wget", "tail", "cat", "grep", "find", "tar", "ufw", "iptables", "nslookup",
            "dig", "tracert", "traceroute", "show", "conf", "configure", "interface", "vlan", "switchport", "router", "enable", "copy",
            "write", "hostname", "brew", "winget", "sfc", "dism", "chkdsk", "netsh", "whoami", "passwd", "useradd", "usermod", "crontab",
            "mkdir", "rm", "cp", "mv", "nano", "vim", "echo", "export", "history", "free", "uname", "lsblk", "mount", "fail2ban-client")
        fun luma(c: Int): Float = (0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
    }
}
