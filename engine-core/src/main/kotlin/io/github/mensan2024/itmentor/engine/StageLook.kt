// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Familles de formats à mise en scène propre, chacune avec sa composition, ses animations et son rythme :
 * - JOURNAL : journal télévisé (incrustation, bandeau de titre, bandeau défilant) ;
 * - FLASH : flash d'information (alerte rouge, titre qui claque) ;
 * - TERMINAL : terminal « hacker » (le texte est tapé, commande par commande) ;
 * - BD : bande dessinée (cases, bulles, onomatopées).
 * Le texte de l'auteur est toujours montré en entier (frappé, lu dans la bulle ou dans le bandeau).
 */
internal class StageLook(private val sb: Storyboard, private val style: Style) : LookRenderer {
    enum class Style { JOURNAL, FLASH, TERMINAL, BD }

    private class Th(val bg1: Int, val bg2: Int, val a: Int, val b: Int, val ink: Int, val dark: Int)
    private val th = when (style) {
        Style.JOURNAL -> Th(rgb(0x0B1E4A), rgb(0x040B22), rgb(0xE5202E), rgb(0x1F5FD6), rgb(0xFFFFFF), rgb(0x0A1128))
        Style.FLASH -> Th(rgb(0x2A0508), rgb(0x0C0102), rgb(0xFF2D3B), rgb(0xFFD23F), rgb(0xFFFFFF), rgb(0x16090A))
        Style.TERMINAL -> Th(rgb(0x04100A), rgb(0x010504), rgb(0x2DFF8A), rgb(0x22D3EE), rgb(0xC9FFE2), rgb(0x06150D))
        Style.BD -> Th(rgb(0xFFF1CC), rgb(0xFFE3A3), rgb(0xE5202E), rgb(0x1F5FD6), rgb(0x141414), rgb(0xFFFFFF))
    }
    override val ink = th.ink
    override val muted = if (style == Style.BD) rgb(0x6B5B3A) else th.ink.withAlpha(0.6f)
    override val accent = th.a
    override val accent2 = th.b
    private val cache = TextCache()
    private fun txt(w: Int, s: Float) = FontSpec(FontFamily.TEXT, w, s)

    private data class Beat(val tag: String, val head: String, val body: String, val icon: IconId, val list: List<String> = emptyList(), val big: String? = null, val section: String = "")

    private fun plain(s: String?) = Marked.strip(s ?: "").replace(' ', ' ').trim()
    private fun short(text: String): String {
        val c = plain(text).split(Regex(" : | \\(|, | — |\\. ")).first().trim().trimEnd('.', ':', '!')
        var out = ""
        for (w in c.split(' ')) { val n = (out + " " + w).trim(); if (n.length > 30) break; out = n }
        return out.ifEmpty { c.take(30) }.replaceFirstChar { it.uppercase() }
    }
    private val totalSteps by lazy { sb.scenes.filter { it.layout == Layout.STEPS }.sumOf { it.items.size } }
    private val firstStep: Map<String, Int> by lazy {
        var n = 0
        sb.scenes.filter { it.layout == Layout.STEPS }.associate { sc -> val s0 = n; n += sc.items.size; sc.id to s0 }
    }
    private val cacheBeats = HashMap<String, List<Beat>>()

    private fun beats(sc: Scene): List<Beat> = cacheBeats.getOrPut(sc.id) {
        when (sc.layout) {
            Layout.HERO -> listOf(Beat("À LA UNE", short(sc.title), plain(sc.title) + (sc.subtitle?.let { "\n" + plain(it) } ?: ""), sc.iconOrDefault()))
            Layout.DEFINITION -> listOf(Beat("LE PRINCIPE", short(sc.title), plain(sc.body.ifEmpty { sc.subtitle }), sc.iconOrDefault(), section = plain(sc.title)))
            Layout.STEPS -> {
                val n0 = firstStep[sc.id] ?: 0
                val total = totalSteps.coerceAtLeast(sc.items.size)
                sc.items.mapIndexed { i, it -> Beat("POINT ${n0 + i + 1}/$total", short(splitLabel(plain(it)).first), plain(it), IconId.CHECK, big = "${n0 + i + 1}", section = plain(sc.title)) }
            }
            Layout.STAT -> listOf(Beat(sc.kicker?.uppercase() ?: "LE CHIFFRE", short(sc.title), plain(sc.title) + (if (sc.body.isNotEmpty()) ". " + plain(sc.body) else ""), IconId.CHART, big = sc.statValue))
            Layout.COMPARE -> sc.compare?.let { c -> listOf(
                Beat("À ÉVITER", short(c.badLabel), "${plain(c.badLabel)} : ${c.badText()}", IconId.CROSS),
                Beat("À FAIRE", short(c.goodLabel), "${plain(c.goodLabel)} : ${c.goodText()}", IconId.CHECK)) } ?: emptyList()
            Layout.CHECKLIST -> listOfNotNull(
                if (sc.items.isEmpty()) null else Beat("À RETENIR", short(sc.title), plain(sc.subtitle), IconId.SHIELD, list = sc.items.map(::plain), section = plain(sc.title)),
                Beat("MERCI", "À bientôt", plain(sc.body).ifEmpty { "Partage cette astuce !" }, IconId.HEART))
            Layout.SCREEN -> emptyList()
        }
    }

    private fun beatAt(sc: Scene, t: Float): Triple<Int, Float, Float> {
        val bs = beats(sc)
        val w = bs.map { 0.7f + 0.3f * ((it.body.length + it.list.sumOf { l -> l.length }).coerceIn(30, 260) / 100f) }
        val total = w.sum(); var acc = 0f
        for ((i, x) in w.withIndex()) {
            val len = sc.durationMs * x / total
            if (t < acc + len || i == bs.lastIndex) return Triple(i, (t - acc).coerceAtLeast(0f), len)
            acc += len
        }
        return Triple(0, t, sc.durationMs.toFloat())
    }

    private fun pop(t: Float, d: Float = 0f, dur: Float = 420f) = Easing.EXPO_OUT.ease(clamp01((t - d) / dur))

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        val bs = beats(sc)
        background(g, tGlobalMs)
        if (bs.isEmpty()) return
        val (i, t, len) = beatAt(sc, localMs)
        val b = bs[i]; val key = "${sc.id}/$i"
        when (style) {
            Style.JOURNAL -> journal(g, b, t, len, key, tGlobalMs)
            Style.FLASH -> flash(g, b, t, len, key, tGlobalMs)
            Style.TERMINAL -> terminal(g, b, t, len, key, tGlobalMs)
            Style.BD -> comic(g, b, t, len, key)
        }
    }

    private fun background(g: Gfx, tg: Float) {
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, th.bg1, th.bg2)
        when (style) {
            Style.JOURNAL -> {
                g.fillRadialGlow(540f, 520f, 900f, th.b.withAlpha(0.25f))
                for (k in 0..6) g.line(0f, 260f + k * 120f, 1080f, 260f + k * 120f, 1.5f, 0x10FFFFFF)
                g.strokeCircle(540f, 560f, 420f + (tg / 40f) % 40f, 2f, 0x14FFFFFF)
            }
            Style.FLASH -> {
                g.fillRadialGlow(540f, 700f, 1000f, th.a.withAlpha(0.28f))
                val off = (tg / 20f) % 120f
                for (k in -2..14) g.line(-200f + k * 120f + off, 0f, 400f + k * 120f + off, 1920f, 40f, 0x08FF2D3B)
            }
            Style.TERMINAL -> {
                val r = Rand.of(sb.seed, 0x7E1)
                for (k in 0 until 18) {
                    val x = 40f + k * 60f; val sp = 0.06f + r.next() * 0.12f; val ph = r.next() * 1900f
                    val y = ((tg * sp + ph) % 2100f) - 150f
                    g.fillRoundRect(x, y, x + 6f, y + 90f + r.next() * 120f, 3f, th.a.withAlpha(0.07f))
                }
                var y = 0f; while (y < 1920f) { g.fillRect(0f, y, 1080f, y + 2f, 0x14000000); y += 6f }
            }
            Style.BD -> {
                var yy = 0f; var row = 0
                while (yy < 1920f) { var xx = if (row % 2 == 0) 0f else 20f; while (xx < 1080f) { g.fillCircle(xx, yy, 5f, th.a.withAlpha(0.10f)); xx += 40f }; yy += 40f; row++ }
            }
        }
    }

    private fun sectionLine(g: Gfx, b: Beat, x: Float, y: Float, maxW: Float, font: FontSpec, color: Int) {
        if (b.section.isEmpty()) return
        g.drawText(b.section, x, y, fitSize(g, b.section, font, maxW), color)
    }

    private fun typed(g: Gfx, key: String, text: String, font: FontSpec, x: Float, top: Float, w: Float, lines: Int, color: Int, t: Float, from: Float, dur: Float, caret: Int? = null) {
        val bl = cache.fit(g, key, text, font, w, lines, 1.3f)
        g.drawBlock(bl, x, top, color, reveal = clamp01((t - from) / dur), caretColor = caret, caretVisible = ((t / 450f).toInt() % 2 == 0))
    }

    // ───────────────────────────── Journal télévisé ─────────────────────────────

    private fun journal(g: Gfx, b: Beat, t: Float, len: Float, key: String, tg: Float) {
        val a = pop(t)
        // En direct
        g.fillRoundRect(Safe.LEFT, 150f, Safe.LEFT + 210f, 214f, 10f, th.a)
        g.fillCircle(Safe.LEFT + 34f, 182f, 9f, if ((tg / 500f).toInt() % 2 == 0) th.ink else th.a)
        g.drawTracked("DIRECT", Safe.LEFT + 58f, 194f, txt(800, 28f), th.ink, 2f)
        g.drawTracked("INFO", Safe.LEFT + 240f, 194f, txt(700, 28f), th.ink.withAlpha(0.85f), 3f)
        // Incrustation
        val dy = (1 - a) * 40f
        g.withPose(Pose(alpha = a, dy = dy), 540f, 520f) {
            g.fillRoundRect(Safe.LEFT, 260f, Safe.RIGHT, 880f, 28f, th.dark.withAlpha(0.9f))
            g.strokeRoundRect(Safe.LEFT, 260f, Safe.RIGHT, 880f, 28f, 4f, th.b)
            for (r in 0..3) g.strokeCircle(540f, 570f, 130f + r * 60f + ((t / 30f) % 60f), 2f, th.b.withAlpha(0.30f - r * 0.06f))
            if (b.big != null) {
                val f = fitSize(g, b.big, FontSpec(FontFamily.CONDENSED, 700, 380f), 700f)
                g.drawText(b.big, 540f - g.measureText(b.big, f) / 2, 570f + g.metrics(f).ascent / 2, f, th.ink)
            } else Icons.draw(g, b.icon, 540f, 570f, 230f, th.ink, 5f)
        }
        // Titre incrusté
        val a2 = pop(t, 200f)
        g.withPose(Pose(alpha = a2, dx = (1 - a2) * -160f), 540f, 1000f) {
            val tf = txt(800, 26f)
            val tw = g.measureTracked(b.tag, tf, 2f) + 48f
            g.fillRoundRect(Safe.LEFT, 940f, Safe.LEFT + tw, 996f, 8f, th.a)
            g.drawTracked(b.tag, Safe.LEFT + 24f, 978f, tf, th.ink, 2f)
            sectionLine(g, b, Safe.LEFT + tw + 24f, 978f, Safe.WIDTH - tw - 24f, txt(700, 28f), th.ink.withAlpha(0.9f))
            g.fillRoundRect(Safe.LEFT, 996f, Safe.RIGHT, 1100f, 10f, th.ink)
            val hf = fitSize(g, b.head.uppercase(), FontSpec(FontFamily.CONDENSED, 700, 74f), Safe.WIDTH - 70f)
            g.drawText(b.head.uppercase(), Safe.LEFT + 30f, 1048f + g.metrics(hf).ascent / 2, hf, th.dark)
        }
        // Texte complet
        g.fillRoundRect(Safe.LEFT, 1100f, Safe.RIGHT, 1500f, 10f, th.b.withAlpha(0.92f))
        val body = if (b.list.isEmpty()) b.body else b.body + "\n" + b.list.joinToString("\n") { "• $it" }
        typed(g, "j/$key", body, txt(600, 38f), Safe.LEFT + 30f, 1124f, Safe.WIDTH - 60f, 9, th.ink, t, len * 0.08f, len * 0.5f)
        // Bandeau défilant
        g.fillRect(0f, 1530f, 1080f, 1590f, th.dark)
        g.fillRect(0f, 1530f, 1080f, 1536f, th.a)
        val line = "●  INFO  ●  " + b.tag
        g.drawText(line, Safe.LEFT, 1573f, txt(700, 30f), th.ink.withAlpha(if ((tg / 600f).toInt() % 2 == 0) 0.95f else 0.7f))
    }

    // ───────────────────────────── Flash d'information ─────────────────────────────

    private fun flash(g: Gfx, b: Beat, t: Float, len: Float, key: String, tg: Float) {
        val a = pop(t, 0f, 260f)
        val blink = (tg / 400f).toInt() % 2 == 0
        g.fillRect(0f, 150f, 1080f, 250f, th.a)
        g.drawTracked("FLASH INFO", Safe.LEFT, 218f, FontSpec(FontFamily.CONDENSED, 700, 74f), th.ink, 8f)
        g.fillCircle(Safe.RIGHT - 30f, 200f, 18f, if (blink) th.ink else th.a)
        val shake = if (t < 500f) sin(t / 18f) * (1 - t / 500f) * 14f else 0f
        g.withPose(Pose(alpha = a, dx = shake, scale = 1.25f - 0.25f * a), 540f, 520f) {
            val hf = fitSize(g, b.head.uppercase(), FontSpec(FontFamily.CONDENSED, 700, 190f), Safe.WIDTH)
            val bl = cache.fit(g, "f/h/$key", b.head.uppercase(), hf, Safe.WIDTH, 3, 1.0f)
            g.drawBlock(bl, 540f, 380f, th.ink, align = Align.CENTER)
        }
        val tf = txt(800, 30f)
        val tw = g.measureTracked(b.tag, tf, 3f) + 60f
        g.fillRoundRect(540f - tw / 2, 860f, 540f + tw / 2, 926f, 12f, th.b)
        g.drawTracked(b.tag, 540f - tw / 2 + 30f, 905f, tf, th.dark, 3f)
        b.section.takeIf { it.isNotEmpty() }?.let { sct -> val sf = fitSize(g, sct, txt(700, 32f), Safe.WIDTH); g.drawText(sct, 540f - g.measureText(sct, sf) / 2, 962f, sf, th.ink.withAlpha(0.9f)) }
        val a2 = pop(t, 250f)
        g.withPose(Pose(alpha = a2, dy = (1 - a2) * 60f), 540f, 1100f) {
            g.fillRoundRect(Safe.LEFT, 980f, Safe.RIGHT, 1520f, 22f, th.dark.withAlpha(0.92f))
            g.fillRect(Safe.LEFT, 980f, Safe.LEFT + 14f, 1520f, th.a)
            val body = if (b.list.isEmpty()) b.body else b.body + "\n" + b.list.joinToString("\n") { "• $it" }
            typed(g, "f/$key", body, txt(700, 42f), Safe.LEFT + 44f, 1012f, Safe.WIDTH - 80f, 9, th.ink, t, len * 0.08f, len * 0.5f)
        }
    }

    // ───────────────────────────── Terminal ─────────────────────────────

    private fun terminal(g: Gfx, b: Beat, t: Float, len: Float, key: String, tg: Float) {
        val a = pop(t, 0f, 300f)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 30f), 540f, 800f) {
            g.fillRoundRect(Safe.LEFT, 180f, Safe.RIGHT, 1540f, 24f, th.dark)
            g.strokeRoundRect(Safe.LEFT, 180f, Safe.RIGHT, 1540f, 24f, 3f, th.a.withAlpha(0.7f))
            g.fillRect(Safe.LEFT, 180f, Safe.RIGHT, 250f, th.a.withAlpha(0.12f))
            g.fillCircle(Safe.LEFT + 40f, 215f, 11f, rgb(0xFF5F56)); g.fillCircle(Safe.LEFT + 76f, 215f, 11f, rgb(0xFFBD2E)); g.fillCircle(Safe.LEFT + 112f, 215f, 11f, rgb(0x27C93F))
            g.drawText("codjia@lab: ~", Safe.LEFT + 160f, 226f, FontSpec(FontFamily.MONO, 400, 28f), th.ink.withAlpha(0.7f))
        }
        val mono = FontSpec(FontFamily.MONO, 400, 34f)
        val prompt = "codjia@lab:~$ "
        val cmd = "explain --" + b.tag.lowercase().replace(' ', '-').replace('/', '-').replace('à', 'a').replace('é', 'e')
        val cmdShown = cmd.take((clamp01((t - 150f) / 700f) * cmd.length).toInt())
        g.drawText(prompt, Safe.LEFT + 40f, 330f, mono, th.a)
        g.drawText(cmdShown, Safe.LEFT + 40f + g.measureText(prompt, mono), 330f, mono, th.ink)
        if (t > 900f) {
            val hf = fitSize(g, "> " + b.head.uppercase(), FontSpec(FontFamily.MONO, 700, 46f), Safe.WIDTH - 80f)
            g.drawText("> " + b.head.uppercase(), Safe.LEFT + 40f, 420f, hf, th.b)
            sectionLine(g, b, Safe.LEFT + 40f, 372f, Safe.WIDTH - 80f, FontSpec(FontFamily.MONO, 400, 28f), th.a.withAlpha(0.7f))
            val body = if (b.list.isEmpty()) b.body else b.body + "\n" + b.list.joinToString("\n") { "[ok] $it" }
            typed(g, "t/$key", body, FontSpec(FontFamily.MONO, 400, 36f), Safe.LEFT + 40f, 470f, Safe.WIDTH - 80f, 20, th.a, t, len * 0.15f, len * 0.5f, caret = th.a)
        } else if ((t / 400f).toInt() % 2 == 0) g.fillRect(Safe.LEFT + 44f + g.measureText(prompt + cmdShown, mono), 300f, Safe.LEFT + 62f + g.measureText(prompt + cmdShown, mono), 340f, th.a)
    }

    // ───────────────────────────── Bande dessinée ─────────────────────────────

    private fun comic(g: Gfx, b: Beat, t: Float, len: Float, key: String) {
        val a = pop(t, 0f, 350f)
        val bw = 8f
        // Case 1 : l'image
        g.withPose(Pose(alpha = a, scale = 0.94f + 0.06f * Easing.BACK_OUT.ease(a)), 540f, 560f) {
            g.fillRoundRect(Safe.LEFT - 6f, 174f, Safe.RIGHT + 6f, 906f, 12f, th.ink)
            g.fillRoundRect(Safe.LEFT, 180f, Safe.RIGHT, 900f, 8f, mix(th.bg2, th.b, 0.18f))
            // éclats
            val cx = 540f; val cy = 540f
            val p = PathData()
            val n = 18
            for (k in 0 until n * 2) {
                val ang = (k * PI / n).toFloat() - 1.5708f
                val r = if (k % 2 == 0) 300f + sin(t / 140f + k) * 8f else 190f
                if (k == 0) p.moveTo(cx + cos(ang) * r, cy + sin(ang) * r) else p.lineTo(cx + cos(ang) * r, cy + sin(ang) * r)
            }
            p.close()
            g.fillPath(p, rgb(0xFFD23F)); g.strokePath(p, bw, th.ink)
            if (b.big != null) {
                val f = fitSize(g, b.big, FontSpec(FontFamily.CONDENSED, 700, 260f), 330f)
                g.drawText(b.big, cx - g.measureText(b.big, f) / 2, cy + g.metrics(f).ascent / 2, f, th.a)
            } else Icons.draw(g, b.icon, cx, cy, 190f, th.ink, 7f)
            val tf = txt(800, 28f)
            val tw = g.measureTracked(b.tag, tf, 2f) + 44f
            g.fillRoundRect(Safe.LEFT + 24f, 204f, Safe.LEFT + 24f + tw, 262f, 6f, th.a)
            g.strokeRoundRect(Safe.LEFT + 24f, 204f, Safe.LEFT + 24f + tw, 262f, 6f, 4f, th.ink)
            g.drawTracked(b.tag, Safe.LEFT + 46f, 244f, tf, th.dark, 2f)
            if (b.section.isNotEmpty()) { val sf = fitSize(g, b.section, txt(800, 30f), Safe.WIDTH - 60f); val sw = g.measureText(b.section, sf) + 36f
                g.fillRoundRect(Safe.RIGHT - 12f - sw, 836f, Safe.RIGHT - 12f, 888f, 8f, th.dark); g.strokeRoundRect(Safe.RIGHT - 12f - sw, 836f, Safe.RIGHT - 12f, 888f, 8f, 4f, th.ink)
                g.drawText(b.section, Safe.RIGHT - 12f - sw + 18f, 874f, sf, th.ink) }
        }
        // Titre en légende jaune
        val a2 = pop(t, 200f)
        g.withPose(Pose(alpha = a2, dy = (1 - a2) * 40f, scale = 0.96f + 0.04f * a2), 540f, 980f) {
            val hf = fitSize(g, b.head.uppercase(), FontSpec(FontFamily.CONDENSED, 700, 84f), Safe.WIDTH - 70f)
            g.fillRoundRect(Safe.LEFT, 940f, Safe.RIGHT, 1052f, 8f, rgb(0xFFD23F))
            g.strokeRoundRect(Safe.LEFT, 940f, Safe.RIGHT, 1052f, 8f, 6f, th.ink)
            g.drawText(b.head.uppercase(), 540f - g.measureText(b.head.uppercase(), hf) / 2, 996f + g.metrics(hf).ascent / 2, hf, th.ink)
        }
        // Case 2 : la bulle avec tout le texte
        val a3 = pop(t, 380f)
        g.withPose(Pose(alpha = a3, dy = (1 - a3) * 50f), 540f, 1300f) {
            g.fillRoundRect(Safe.LEFT - 6f, 1094f, Safe.RIGHT + 6f, 1536f, 40f, th.ink)
            g.fillRoundRect(Safe.LEFT, 1100f, Safe.RIGHT, 1530f, 34f, th.dark)
            val tail = PathData().moveTo(220f, 1528f).lineTo(150f, 1600f).lineTo(300f, 1528f).close()
            g.fillPath(tail, th.dark)
            val body = if (b.list.isEmpty()) b.body else b.body + "\n" + b.list.joinToString("\n") { "• $it" }
            typed(g, "b/$key", body, FontSpec(FontFamily.HAND, 700, 40f), Safe.LEFT + 36f, 1130f, Safe.WIDTH - 72f, 9, th.ink, t, len * 0.1f, len * 0.5f)
        }
    }
}
