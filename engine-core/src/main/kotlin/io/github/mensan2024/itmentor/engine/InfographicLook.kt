// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.cos
import kotlin.math.sin

/**
 * Format « Infographie » : chaque idée est expliquée par un schéma animé qui montre le mécanisme.
 * Par exemple, un cadenas assailli de mots de passe avec un compteur d'essais, des barres de robustesse,
 * une chaîne « mot de passe deviné › code MFA demandé › accès refusé », des tentatives qui se bloquent,
 * ou un graphique d'échecs avec une alerte.
 *
 * Mise en page :
 * - en haut, la rubrique et une pastille « ÉTAPE 2/4 » ;
 * - un titre sur deux lignes, la seconde en couleur ;
 * - le schéma au centre ;
 * - l'explication complète dans un cadre en bas ;
 * - à la fin, « Abonne-toi » et l'astuce suivante.
 */
internal class InfographicLook(private val sb: Storyboard) : LookRenderer {
    private val pal = listOf(rgb(0x37C6F4), rgb(0x3DDC97), rgb(0xFFC43D))[(Rand.of(sb.seed, 0x1F0).nextInt() ushr 1) % 3]
    override val ink = rgb(0xF4F6FA)
    override val muted = rgb(0x8E98AD)
    override val accent = pal
    override val accent2 = rgb(0xFF4D5E)
    private val danger = rgb(0xFF4D5E)
    private val warn = rgb(0xFFC43D)
    private val ok = rgb(0x3DDC97)
    private val panel = rgb(0x111A2E)
    private val cache = TextCache()
    private val domain by lazy { DomainDetector.detect(TextSanitizer.clean(sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body })) }
    private val astuceNo: Int? = (listOf(sb.title) + sb.scenes.take(1).flatMap { listOf(it.kicker ?: "", it.title) })
        .firstNotNullOfOrNull { Regex("astuce\\D{0,30}?0*(\\d{1,4})", RegexOption.IGNORE_CASE).find(Marked.strip(it))?.groupValues?.get(1)?.toIntOrNull() }

    enum class Diagram { HERO_ICON, STEPLIST, TARGETS, LOCK_ATTACK, SHIELD, BARS, CHAIN, ATTEMPTS, CHART, TERMINAL, NUMBER, VERSUS, CHECKS, TEXT, CTA }

    data class Beat(val chip: String, val l1: String, val l2: String, val caption: String, val diagram: Diagram,
                    val main: String = "", val detail: String? = null, val list: List<String> = emptyList(), val index: Int = 0, val good: Boolean = true, val icon: IconId = IconId.BULB)

    private val beatsCache = HashMap<String, List<Beat>>()

    private fun plain(s: String?) = Marked.strip(s ?: "").replace(' ', ' ').trim()

    /** Titre en deux lignes : début en blanc, fin en couleur (2 à 7 mots au total). */
    private fun split2(text: String): Pair<String, String> {
        val cut = plain(text).split(Regex(" : | \\(|, | — ")).first().trim().trimEnd('.', '!', ':')
        val w = cut.split(' ').filter { it.isNotBlank() }.take(9)
        if (w.size <= 2) return w.joinToString(" ") to ""
        val k = (w.size + 1) / 2
        return w.take(k).joinToString(" ") to w.drop(k).joinToString(" ")
    }

    private fun diagramFor(text: String, fallback: Diagram): Diagram {
        val t = text.lowercase()
        return when {
            Regex("surveill|journal|auth\\.log|alerte|échecs|echecs|détecte|detecte").containsMatchIn(t) -> Diagram.CHART
            Regex("limite|limitation|tentative|verrouill|bloque|bannir|fail2ban").containsMatchIn(t) -> Diagram.ATTEMPTS
            Regex("\\bmfa\\b|\\b2fa\\b|double authentification|authentification à deux|code reçu|application d'authentification").containsMatchIn(t) -> Diagram.CHAIN
            Regex("mots? de passe|passphrase|robuste").containsMatchIn(t) -> Diagram.BARS
            Regex("^(sudo|git|ls|df|ssh|apt|systemctl|docker|npm|show|vlan|ufw|chmod)\\b").containsMatchIn(t) -> Diagram.TERMINAL
            else -> fallback
        }
    }

    /** Explication courte et exacte du mécanisme montré, quand l'élément de liste se contente de nommer l'action. */
    private fun explain(d: Diagram, item: String): String {
        if (item.length > 70) return item
        val t = item.lowercase()
        val why = when (d) {
            Diagram.BARS -> if (Regex("mots? de passe|passphrase").containsMatchIn(t)) "plus il est long et imprévisible, plus il résiste aux essais automatiques." else null
            Diagram.CHAIN -> if (Regex("mfa|2fa|authentification").containsMatchIn(t)) "même si le mot de passe est deviné, le second facteur bloque l'accès." else null
            Diagram.ATTEMPTS -> "après quelques échecs, l'accès est bloqué : l'attaque devient beaucoup trop lente."
            Diagram.CHART -> "un pic soudain d'échecs de connexion signale une attaque en cours."
            else -> null
        } ?: return item
        return item.trimEnd('.', ' ') + " : " + why
    }

    /** « les comptes, les serveurs SSH ou les interfaces » → cibles affichées. */
    private fun targets(body: String): List<String> {
        val m = Regex("(?:cibler|viser|toucher|concerne[r]?)\\s+(.+)", RegexOption.IGNORE_CASE).find(body) ?: return emptyList()
        return m.groupValues[1].trimEnd('.').split(Regex(",\\s*|\\s+ou\\s+|\\s+et\\s+"))
            .map { it.trim().removePrefix("les ").removePrefix("des ").removePrefix("la ").removePrefix("le ").replaceFirstChar { c -> c.uppercase() } }
            .filter { it.length in 3..40 }.take(3).takeIf { it.size >= 2 } ?: emptyList()
    }

    private fun beats(sc: Scene): List<Beat> = beatsCache.getOrPut(sc.id) {
        val attack = Regex("attaque|force brute|pirate|hacker|phishing|ransomware|intrus").containsMatchIn((sc.title + " " + sc.body + " " + sc.subtitle).lowercase())
        val tag = (sc.kicker ?: "").uppercase()
        when (sc.layout) {
            Layout.HERO -> { val (a, b) = split2(sc.title); listOf(Beat(tag.ifEmpty { domain.kicker }, a, b, plain(sc.subtitle ?: sc.title),
                if (attack) Diagram.LOCK_ATTACK else Diagram.HERO_ICON, icon = sc.iconOrDefault(), list = sb.scenes.flatMap { it.items }.map { splitLabel(plain(it)).first }.filter { it.length <= 22 }.take(6))) }
            Layout.DEFINITION -> {
                val (a, b) = split2(sc.title); val body = plain(sc.body.ifEmpty { sc.subtitle })
                val tg = targets(body)
                val d = when { tg.isNotEmpty() -> Diagram.TARGETS; attack || Regex("attaque|essai").containsMatchIn(body.lowercase()) -> Diagram.LOCK_ATTACK; else -> diagramFor(body, Diagram.TEXT) }
                listOf(Beat(tag.ifEmpty { "LE PRINCIPE" }, a, b, body, d, main = body, list = tg))
            }
            Layout.STEPS -> {
                val word = if (domain == Domain.CYBER) "DÉFENSE" else "ÉTAPE"
                val labels = sc.items.map { splitLabel(plain(it)).first }
                sc.items.mapIndexed { i, it ->
                    val (label, detail) = splitLabel(plain(it))
                    val (a, b) = split2(label)
                    val cmd = Regex("^[a-z][a-z0-9._-]*(\\s+[-/a-zA-Z0-9.=_\"]+){0,4}$").matches(label) && label.length < 40
                    val d = if (cmd) Diagram.TERMINAL else diagramFor(plain(it), Diagram.STEPLIST)
                    Beat("$word ${i + 1}/${sc.items.size}", a, b, explain(d, plain(it)), d, main = label, detail = detail, list = labels, index = i)
                }
            }
            Layout.STAT -> { val (a, b) = split2(sc.title); listOf(Beat(tag.ifEmpty { "LE CHIFFRE" }, a, b, plain(sc.title) + (if (sc.body.isNotEmpty()) ". " + plain(sc.body) else ""), Diagram.NUMBER, main = sc.statValue ?: "")) }
            Layout.COMPARE -> sc.compare?.let { c -> listOf(
                Beat("À ÉVITER", plain(c.badLabel), "", "${c.badLabel} : ${c.badText()}", Diagram.VERSUS, main = c.badText(), good = false),
                Beat("À FAIRE", plain(c.goodLabel), "", "${c.goodLabel} : ${c.goodText()}", Diagram.VERSUS, main = c.goodText(), good = true),
            ) } ?: emptyList()
            Layout.CHECKLIST -> {
                val (a, b) = split2(sc.title)
                listOfNotNull(
                    Beat(tag.ifEmpty { "À RETENIR" }, a, b, plain(sc.subtitle).ifEmpty { "${sc.items.size} réflexes simples à appliquer dès aujourd'hui." }, Diagram.CHECKS, list = sc.items.map(::plain)),
                    Beat("", "", "", plain(sc.body).ifEmpty { "Abonne-toi pour la suite !" }, Diagram.CTA, main = plain(sc.body)),
                )
            }
            Layout.SCREEN -> emptyList()
        }
    }

    private fun beatAt(sc: Scene, t: Float): Triple<Int, Float, Float> {
        val bs = beats(sc)
        val w = bs.map { 0.7f + 0.3f * (it.caption.length.coerceIn(30, 180) / 100f) + (if (it.diagram == Diagram.CTA) -0.3f else 0f) }
        val total = w.sum()
        var acc = 0f
        for ((i, x) in w.withIndex()) {
            val len = sc.durationMs * x / total
            if (t < acc + len || i == bs.lastIndex) return Triple(i, (t - acc).coerceAtLeast(0f), len)
            acc += len
        }
        return Triple(0, t, sc.durationMs.toFloat())
    }

    // ───────────────────────────── Scène ─────────────────────────────

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, rgb(0x0B1224), rgb(0x1A1446))
        g.fillRadialGlow(540f, 1650f, 900f, rgb(0x3B2E9A).withAlpha(0.35f))
        g.fillRadialGlow(540f, 300f, 700f, accent.withAlpha(0.06f))
        val bs = beats(sc)
        if (bs.isEmpty()) return
        val (i, t, len) = beatAt(sc, localMs)
        val b = bs[i]
        // Rubrique en haut (fixe)
        val hf = FontSpec(FontFamily.TEXT, 800, 24f)
        val left = (astuceNo?.let { "ASTUCE ${Fmt.pad(it, 3)}" } ?: "CODJIA")
        g.drawTracked(left, Safe.LEFT, 172f, hf, accent, 2f)
        g.drawTracked(domain.label.uppercase(), Safe.LEFT + g.measureTracked(left, hf, 2f) + 30f, 172f, hf.copy(weight = 600), muted, 2f)
        val e = Easing.EXPO_OUT.ease(clamp01(t / 300f))
        if (b.diagram == Diagram.CTA) { g.withPose(Pose(alpha = e), 540f, 900f) { cta(g, b, t) }; return }
        var y = 230f
        // Pastille d'étape
        if (b.chip.isNotEmpty()) {
            val cf = FontSpec(FontFamily.TEXT, 800, 26f)
            val w = g.measureTracked(b.chip, cf, 2f) + 48f
            g.withPose(Pose(alpha = e, scale = lerp(0.8f, 1f, e)), 540f, y + 25f) {
                g.fillRoundRect(540f - w / 2, y, 540f + w / 2, y + 50f, 25f, accent.withAlpha(0.18f))
                g.strokeRoundRect(540f - w / 2, y, 540f + w / 2, y + 50f, 25f, 2f, accent.withAlpha(0.7f))
                g.drawTracked(b.chip, 540f - w / 2 + 24f, y + 25f + g.metrics(cf).ascent / 2, cf, accent, 2f)
            }
            y += 80f
        }
        // Titre sur deux lignes
        val tf = FontSpec(FontFamily.TEXT, 800, 72f)
        for ((k, line) in listOf(b.l1, b.l2).withIndex()) {
            if (line.isEmpty()) continue
            val bl = cache.fit(g, "t/${sc.id}/$i/$k", line, tf, Safe.WIDTH, 1)
            val a = Easing.EXPO_OUT.ease(clamp01((t - k * 120f) / 420f))
            val base = g.baselineOf(bl, y, 0)
            val text = bl.lines.first()
            g.withPose(Pose(alpha = a, dy = (1 - a) * 24f), 540f, base) { g.drawText(text, 540f - g.measureText(text, bl.font) / 2, base, bl.font, if (k == 0) ink else accent) }
            y += bl.height + 4f
        }
        val top = y + 40f
        val cap = captionBox(g, b, t, "${sc.id}/$i")
        val area = R(Safe.LEFT, top, Safe.RIGHT, cap - 40f)
        g.withPose(Pose(alpha = e), 540f, area.cy) { diagram(g, b, t, len, area, "${sc.id}/$i") }
    }

    data class R(val l: Float, val t: Float, val r: Float, val b: Float) {
        val cx get() = (l + r) / 2
        val cy get() = (t + b) / 2
        val w get() = r - l
        val h get() = b - t
    }

    /** Cadre d'explication en bas. Renvoie son bord supérieur. */
    private fun captionBox(g: Gfx, b: Beat, t: Float, key: String): Float {
        val bl = cache.fit(g, "c/$key", b.caption, FontSpec(FontFamily.TEXT, 600, 38f), Safe.WIDTH - 70f, 4, 1.3f)
        val h = bl.height + 56f
        val top = 1560f - h
        val a = Easing.EXPO_OUT.ease(clamp01((t - 150f) / 400f))
        g.withPose(Pose(alpha = a, dy = (1 - a) * 16f), 540f, top) {
            g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 16f, panel.withAlpha(0.92f))
            g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 16f, 1.5f, 0x22FFFFFF)
            g.fillRoundRect(Safe.LEFT, top, Safe.LEFT + 7f, top + h, 3f, accent)
            g.drawBlock(bl, Safe.LEFT + 36f, top + 28f, ink)
        }
        return top
    }

    // ───────────────────────────── Schémas ─────────────────────────────

    private fun diagram(g: Gfx, b: Beat, t: Float, len: Float, a: R, key: String) {
        when (b.diagram) {
            Diagram.TARGETS -> targetsDiagram(g, b, t, a)
            Diagram.HERO_ICON -> heroIcon(g, b, t, a)
            Diagram.STEPLIST -> stepList(g, b, t, a, key)
            Diagram.LOCK_ATTACK -> lockAttack(g, t, a)
            Diagram.SHIELD -> shield(g, t, a)
            Diagram.BARS -> bars(g, b, t, a, key)
            Diagram.CHAIN -> chain(g, b, t, a, key)
            Diagram.ATTEMPTS -> attempts(g, t, a)
            Diagram.CHART -> chart(g, t, a)
            Diagram.TERMINAL -> terminal(g, b, t, a)
            Diagram.NUMBER -> number(g, b, t, a)
            Diagram.VERSUS -> versus(g, b, t, a, key)
            Diagram.CHECKS -> checks(g, b, t, a, key)
            Diagram.TEXT -> textCard(g, b, t, a, key)
            Diagram.CTA -> {}
        }
    }

    private fun card(g: Gfx, l: Float, tp: Float, r: Float, bt: Float, border: Int = 0x26FFFFFF) {
        g.fillRoundRect(l, tp, r, bt, 16f, panel)
        g.strokeRoundRect(l, tp, r, bt, 16f, 2f, border)
    }

    private fun lockAttack(g: Gfx, t: Float, a: R) {
        val cx = a.cx; val cy = a.cy - 30f
        g.fillRadialGlow(cx, cy, 260f, danger.withAlpha(0.35f))
        g.fillCircle(cx, cy, 120f, danger.withAlpha(0.25f))
        g.fillCircle(cx, cy, 92f, danger)
        Icons.draw(g, IconId.LOCK, cx, cy, 100f, rgb(0xFFFFFF), 2.4f)
        // Essais qui fusent autour du cadenas
        val guesses = listOf("azerty123", "motdepasse", "123456", "soleil2024", "admin", "qwerty", "bonjour1", "liberte!")
        val mf = FontSpec(FontFamily.MONO, 600, 30f)
        for ((k, s) in guesses.withIndex()) {
            val ph = ((t / 1600f) + k / guesses.size.toFloat()) % 1f
            val ang = k * 0.785f + 0.3f
            val r = 140f + ph * 300f
            val x = cx + cos(ang) * r * 1.3f; val y = cy + sin(ang) * r * 0.75f
            val w = g.measureText(s, mf) + 24f
            val al = (1f - ph) * clamp01(ph * 5f)
            if (x - w / 2 < a.l || x + w / 2 > a.r || y < a.t + 20f || y > a.b - 20f) continue
            if (y > cy + 130f && y < cy + 220f) continue
            g.withPose(Pose(alpha = al), x, y) {
                g.fillRoundRect(x - w / 2, y - 22f, x + w / 2, y + 22f, 8f, danger.withAlpha(0.2f))
                g.strokeRoundRect(x - w / 2, y - 22f, x + w / 2, y + 22f, 8f, 1.5f, danger.withAlpha(0.8f))
                g.drawText(s, x - w / 2 + 12f, y + 9f, mf, rgb(0xFFB3BB))
            }
        }
        val n = (Easing.EASE_IN.ease(clamp01(t / 3200f)) * 8412).toInt()
        val cf = FontSpec(FontFamily.TEXT, 800, 42f)
        val label = "Essais : " + Fmt.thousands(n)
        g.drawText(label, cx - g.measureText(label, cf) / 2, cy + 190f, cf, danger)
    }

    /** Icône du sujet dans des anneaux, entourée des mots-clés du tutoriel qui s'allument un à un. */
    private fun heroIcon(g: Gfx, b: Beat, t: Float, a: R) {
        val cx = a.cx; val cy = a.cy
        val k = Easing.BACK_OUT.ease(clamp01(t / 600f))
        g.fillRadialGlow(cx, cy, 320f, accent.withAlpha(0.28f))
        for (r in 0 until 3) {
            val ph = ((t / 2400f) + r / 3f) % 1f
            g.strokeCircle(cx, cy, 110f + ph * 200f, 2f, accent.withAlpha(0.45f * (1 - ph)))
        }
        g.withPose(Pose(scale = lerp(0.5f, 1f, k), alpha = clamp01(k)), cx, cy) {
            g.fillCircle(cx, cy, 105f, accent.withAlpha(0.2f))
            g.strokeCircle(cx, cy, 105f, 3f, accent)
            Icons.draw(g, b.icon, cx, cy, 110f, accent, 2.4f)
        }
        val mf = FontSpec(FontFamily.TEXT, 700, 28f)
        val n = b.list.size
        for ((i, s) in b.list.withIndex()) {
            val ang = -1.5708f + i * 6.2832f / n.coerceAtLeast(1)
            val x = cx + cos(ang) * 320f; val y = cy + sin(ang) * (a.h / 2 - 50f).coerceAtMost(260f)
            val w = g.measureText(s, mf) + 36f
            val l = (x - w / 2).coerceIn(a.l, a.r - w)
            val al = clamp01((t - 500f - i * 220f) / 300f)
            if (al > 0f) g.withPose(Pose(alpha = al), l + w / 2, y) {
                g.fillRoundRect(l, y - 26f, l + w, y + 26f, 26f, panel)
                g.strokeRoundRect(l, y - 26f, l + w, y + 26f, 26f, 2f, accent.withAlpha(0.6f))
                g.drawText(s, l + 18f, y + 10f, mf, ink)
            }
        }
    }

    /** Toutes les étapes : passées cochées, courante mise en avant, suivantes estompées. */
    private fun stepList(g: Gfx, b: Beat, t: Float, a: R, key: String) {
        val items = b.list.take(6)
        val rows = items.mapIndexed { i, s ->
            val cur = i == b.index
            cache.fit(g, "s/$key/$i", s, FontSpec(FontFamily.TEXT, if (cur) 800 else 600, if (cur) 46f else 34f), a.w - 150f, if (cur) 3 else 2, 1.15f)
        }
        val hs = rows.mapIndexed { i, bl -> bl.height + if (i == b.index) 70f else 40f }
        var y = a.cy - hs.sum() / 2
        val k = Easing.EXPO_OUT.ease(clamp01((t - 150f) / 500f))
        for ((i, bl) in rows.withIndex()) {
            val cur = i == b.index; val done = i < b.index
            val h = hs[i]
            val col = if (cur) accent else if (done) ok else muted
            val al = if (cur) 1f else if (done) 0.85f else 0.45f
            g.withPose(Pose(alpha = al, scale = if (cur) lerp(0.92f, 1f, k) else 1f), a.cx, y + h / 2) {
                if (cur) card(g, a.l, y + 6f, a.r, y + h - 6f, accent)
                g.fillCircle(a.l + 52f, y + h / 2, if (cur) 30f else 22f, col.withAlpha(if (cur) 1f else 0.25f))
                if (done) Icons.draw(g, IconId.CHECK, a.l + 52f, y + h / 2, 26f, ok, 3f)
                else { val n = "${i + 1}"; val nf = FontSpec(FontFamily.TEXT, 800, if (cur) 32f else 24f); g.drawText(n, a.l + 52f - g.measureText(n, nf) / 2, y + h / 2 + g.metrics(nf).ascent / 2.6f, nf, if (cur) rgb(0x0B1224) else ink) }
                g.drawBlock(bl, a.l + 100f, y + h / 2 - bl.height / 2, if (cur) ink else ink)
            }
            if (i < rows.lastIndex) g.fillRect(a.l + 51f, y + h / 2 + 32f, a.l + 53f, y + h + hs[i + 1] / 2 - 32f, 0x33FFFFFF)
            y += h
        }
    }

    private fun targetsDiagram(g: Gfx, b: Beat, t: Float, a: R) {
        val icons = listOf(IconId.USER, IconId.TERMINAL, IconId.GEAR)
        val n = b.list.size
        val src = a.t + 90f
        g.fillRadialGlow(a.cx, src, 160f, danger.withAlpha(0.35f))
        g.fillCircle(a.cx, src, 62f, danger)
        Icons.draw(g, IconId.WARNING, a.cx, src, 64f, rgb(0xFFFFFF), 2.4f)
        val cw = (a.w - (n - 1) * 24f) / n
        val ty = (a.b - 260f).coerceAtLeast(src + 220f)
        for ((i, s) in b.list.withIndex()) {
            val x = a.l + i * (cw + 24f) + cw / 2
            val k = Easing.EXPO_OUT.ease(clamp01((t - 300f - i * 300f) / 500f))
            if (k <= 0f) continue
            // trait d'attaque qui part de la menace vers la cible
            val ex = lerp(a.cx, x, k); val ey = lerp(src + 62f, ty - 10f, k)
            g.line(a.cx, src + 62f, ex, ey, 4f, danger.withAlpha(0.8f))
            val hit = clamp01((t - 800f - i * 300f) / 300f)
            g.withPose(Pose(alpha = hit, scale = lerp(0.8f, 1f, hit)), x, ty + 110f) {
                card(g, x - cw / 2, ty, x + cw / 2, ty + 230f, danger.withAlpha(0.7f))
                Icons.draw(g, icons[i % icons.size], x, ty + 70f, 64f, ink, 2.4f)
                val bl = TextLayout.fit(g, s, FontSpec(FontFamily.TEXT, 700, 30f), cw - 30f, 3, lineHeightFactor = 1.15f, minSize = 20f)
                g.drawBlock(bl, x, ty + 120f, ink, Align.CENTER)
            }
        }
    }

    private fun shield(g: Gfx, t: Float, a: R) {
        val cx = a.cx; val cy = a.cy
        val k = Easing.BACK_OUT.ease(clamp01(t / 600f))
        g.fillRadialGlow(cx, cy, 300f, accent.withAlpha(0.3f))
        g.withPose(Pose(scale = lerp(0.5f, 1f, k), alpha = clamp01(k)), cx, cy) {
            Icons.draw(g, IconId.SHIELD, cx, cy, 300f, accent, 2.2f)
            Icons.draw(g, IconId.CHECK, cx, cy + 6f, 120f, accent, 3f, reveal = clamp01((t - 400f) / 500f))
        }
    }

    private fun bars(g: Gfx, b: Beat, t: Float, a: R, key: String) {
        val pw = Regex("mots? de passe|passphrase").containsMatchIn(b.caption.lowercase())
        val rows: List<Triple<String, String, Float>> = if (pw) listOf(
            Triple("azerty", "Faible : deviné en quelques secondes", 0.12f),
            Triple("Soleil2024", "Moyen : nom + année, prévisible", 0.42f),
            Triple("rivière-cactus-lampe-orbite", "Robuste : long et aléatoire", 0.96f),
        ) else b.list.take(3).mapIndexed { i, s -> Triple(s, if (i == b.index) "← ici" else "", 0.35f + 0.3f * i) }
        val rowH = 150f
        var y = a.cy - rows.size * rowH / 2
        for ((i, r) in rows.withIndex()) {
            val d = 200f + i * 350f
            val k = Easing.EXPO_OUT.ease(clamp01((t - d) / 900f))
            if (k <= 0f) { y += rowH; continue }
            val col = if (r.third < 0.3f) danger else if (r.third < 0.6f) warn else ok
            g.withPose(Pose(alpha = clamp01(k * 2)), a.cx, y) {
                card(g, a.l, y, a.r, y + rowH - 18f, if (!pw && i == b.index.coerceAtMost(rows.lastIndex)) accent else 0x26FFFFFF)
                val mf = FontSpec(if (pw) FontFamily.MONO else FontFamily.TEXT, 700, 36f)
                val name = TextLayout.fit(g, r.first, mf, a.w - 80f, 1, minSize = 24f)
                g.drawText(name.lines.first(), a.l + 30f, y + 50f, name.font, ink)
                if (r.second.isNotEmpty()) {
                    val sf = FontSpec(FontFamily.TEXT, 500, 24f)
                    val st = TextLayout.fit(g, r.second, sf, a.w - 60f, 1, minSize = 18f)
                    g.drawText(st.lines.first(), a.l + 30f, y + 86f, st.font, muted)
                }
                g.fillRoundRect(a.l + 30f, y + 104f, a.r - 30f, y + 114f, 5f, 0x22FFFFFF)
                g.fillRoundRect(a.l + 30f, y + 104f, a.l + 30f + (a.w - 60f) * r.third * k, y + 114f, 5f, col)
            }
            y += rowH
        }
    }

    private fun chain(g: Gfx, b: Beat, t: Float, a: R, key: String) {
        val mfa = Regex("mfa|2fa|double authentification|authentification").containsMatchIn(b.caption.lowercase())
        val steps: List<Triple<IconId, String, Int>> = if (mfa) listOf(
            Triple(IconId.KEY, "Mot de passe deviné", danger), Triple(IconId.SHIELD, "Code MFA demandé", accent), Triple(IconId.LOCK, "Accès refusé", ok),
        ) else {
            val l = b.list.ifEmpty { listOf(b.main) }
            val s = (b.index - 1).coerceAtLeast(0).coerceAtMost((l.size - 3).coerceAtLeast(0))
            l.drop(s).take(3).mapIndexed { i, x -> Triple(listOf(IconId.BULB, IconId.TARGET, IconId.CHECK)[i], x, if (s + i == b.index) accent else muted) }
        }
        val h = 130f; val gap = 70f
        val total = steps.size * h + (steps.size - 1) * gap
        var y = a.cy - total / 2
        for ((i, s) in steps.withIndex()) {
            val k = Easing.BACK_OUT.ease(clamp01((t - 200f - i * 650f) / 450f))
            if (k > 0f) g.withPose(Pose(alpha = clamp01(k), scale = lerp(0.85f, 1f, k)), a.cx, y + h / 2) {
                card(g, a.l + 40f, y, a.r - 40f, y + h, s.third.withAlpha(0.8f))
                g.fillCircle(a.l + 110f, y + h / 2, 38f, s.third.withAlpha(0.25f))
                Icons.draw(g, s.first, a.l + 110f, y + h / 2, 44f, s.third, 2.4f)
                val f = FontSpec(FontFamily.TEXT, 700, 36f)
                val bl = TextLayout.fit(g, s.second, f, a.w - 260f, 2, lineHeightFactor = 1.15f, minSize = 24f)
                g.drawBlock(bl, a.l + 175f, y + h / 2 - bl.height / 2, s.third.let { if (it == muted) ink else it })
            }
            if (i < steps.lastIndex) {
                val lk = clamp01((t - 550f - i * 650f) / 300f)
                g.fillRect(a.cx - 2f, y + h, a.cx + 2f, y + h + gap * lk, 0x66FFFFFF)
            }
            y += h + gap
        }
    }

    private fun attempts(g: Gfx, t: Float, a: R) {
        val cy = a.t + 80f
        val n = (clamp01((t - 200f) / 2200f) * 5).toInt().coerceAtMost(5)
        val tf = FontSpec(FontFamily.TEXT, 700, 32f)
        val label = "Tentatives : $n/5"
        g.drawText(label, a.cx - g.measureText(label, tf) / 2, cy - 30f, tf, warn)
        for (k in 0 until 5) {
            val x = a.cx + (k - 2) * 120f
            if (k < n) { g.fillCircle(x, cy + 50f, 40f, danger); Icons.draw(g, IconId.CROSS, x, cy + 50f, 40f, rgb(0xFFFFFF), 3.4f) }
            else g.fillCircle(x, cy + 50f, 40f, 0x22FFFFFF)
        }
        if (n >= 5) {
            val k = Easing.BACK_OUT.ease(clamp01((t - 2500f) / 400f))
            val bf = FontSpec(FontFamily.TEXT, 800, 32f)
            val s = "Bloqué pendant 1 h"
            val w = g.measureText(s, bf) + 90f
            g.withPose(Pose(alpha = clamp01(k), scale = lerp(1.5f, 1f, k)), a.cx, cy + 150f) {
                g.fillRoundRect(a.cx - w / 2, cy + 122f, a.cx + w / 2, cy + 178f, 28f, danger)
                Icons.draw(g, IconId.LOCK, a.cx - w / 2 + 36f, cy + 150f, 28f, rgb(0xFFFFFF), 2.4f)
                g.drawText(s, a.cx - w / 2 + 64f, cy + 162f, bf, rgb(0xFFFFFF))
            }
        }
        // Configuration type (fail2ban) si on parle d'un serveur Linux / SSH, sinon réglage générique
        val lines = if (Regex("ssh|linux|serveur|fail2ban").containsMatchIn(sb.scenes.joinToString(" ") { it.body + it.items.joinToString(" ") }.lowercase()))
            listOf("# /etc/fail2ban/jail.local", "[sshd]", "enabled  = true", "maxretry = 5", "findtime = 10m", "bantime  = 1h")
        else listOf("# Stratégie de verrouillage", "Seuil       = 5 échecs", "Fenêtre     = 10 min", "Verrouillage = 1 h")
        val top = cy + 220f
        val h = (lines.size * 40f + 70f).coerceAtMost(a.b - top)
        if (h > 120f) {
            card(g, a.l, top, a.r, top + h)
            listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(a.l + 30f + i * 22f, top + 26f, 7f, c) }
            val mf = FontSpec(FontFamily.MONO, 400, 28f)
            for ((i, l) in lines.withIndex()) {
                val y = top + 82f + i * 40f
                if (y > top + h - 10f) break
                val shown = clamp01((t - 400f - i * 180f) / 200f)
                if (shown > 0f) g.drawText(l, a.l + 30f, y, mf, if (l.startsWith("#")) muted else if (l.startsWith("[")) accent else ink)
            }
        }
    }

    private fun chart(g: Gfx, t: Float, a: R) {
        val h = (a.h * 0.55f).coerceAtMost(420f)
        val top = a.t + 10f
        card(g, a.l, top, a.r, top + h)
        val lf = FontSpec(FontFamily.TEXT, 700, 26f)
        g.drawText("Échecs de connexion / heure", a.l + 28f, top + 46f, lf, muted)
        val vals = listOf(3, 2, 4, 3, 2, 3, 41, 58, 47, 4, 3, 2, 3, 2)
        val bw = (a.w - 60f) / vals.size
        val base = top + h - 30f
        val maxH = h - 110f
        for ((i, v) in vals.withIndex()) {
            val k = Easing.EXPO_OUT.ease(clamp01((t - 200f - i * 60f) / 500f))
            val bh = maxH * v / 58f * k
            g.fillRoundRect(a.l + 30f + i * bw + 5f, base - bh, a.l + 30f + (i + 1) * bw - 5f, base, 4f, if (v > 10) danger else accent.withAlpha(0.6f))
        }
        val ak = Easing.BACK_OUT.ease(clamp01((t - 1300f) / 400f))
        if (ak > 0f) g.withPose(Pose(alpha = clamp01(ak), scale = lerp(1.3f, 1f, ak)), a.r - 180f, top + 100f) {
            val af = FontSpec(FontFamily.TEXT, 800, 26f)
            g.fillRoundRect(a.r - 330f, top + 70f, a.r - 26f, top + 130f, 10f, danger.withAlpha(0.22f))
            g.strokeRoundRect(a.r - 330f, top + 70f, a.r - 26f, top + 130f, 10f, 2f, danger)
            g.drawText("ALERTE : pic d'échecs", a.r - 314f, top + 109f, af, rgb(0xFFB3BB))
        }
        // Terminal : recherche des échecs dans le journal
        val tt = top + h + 26f
        val th = (a.b - tt).coerceAtMost(230f)
        if (th > 120f) {
            card(g, a.l, tt, a.r, tt + th)
            val mf = FontSpec(FontFamily.MONO, 400, 25f)
            val cmd = "grep \"Failed password\" /var/log/auth.log"
            val p = clamp01((t - 1700f) / 1200f)
            g.drawText("$ " + cmd.take((cmd.length * p).toInt()), a.l + 26f, tt + 52f, mf, ok)
            if (p >= 1f) {
                val outs = listOf("Failed password for root from 203.0.113.7", "Failed password for admin from 203.0.113.7")
                for ((i, o) in outs.withIndex()) if (tt + 96f + i * 38f < tt + th - 6f) g.drawText(TextLayout.fit(g, o, mf, a.w - 52f, 1, minSize = 18f).lines.first(), a.l + 26f, tt + 96f + i * 38f, mf, rgb(0xFFB3BB))
            }
        }
    }

    private fun terminal(g: Gfx, b: Beat, t: Float, a: R) {
        val h = 300f; val top = a.cy - h / 2
        card(g, a.l, top, a.r, top + h)
        listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(a.l + 30f + i * 22f, top + 26f, 7f, c) }
        val mf = FontSpec(FontFamily.MONO, 700, 34f)
        val cmd = TextLayout.fit(g, b.main, mf, a.w - 100f, 1, minSize = 22f)
        val line = cmd.lines.first()
        val p = clamp01((t - 300f) / (line.length * 50f).coerceAtLeast(300f))
        g.drawText("$ " + line.take((line.length * p).toInt()), a.l + 30f, top + 110f, cmd.font, ok)
        b.detail?.let { if (p >= 1f) g.drawBlock(TextLayout.fit(g, "# $it", FontSpec(FontFamily.MONO, 400, 28f), a.w - 60f, 3, lineHeightFactor = 1.3f, minSize = 20f), a.l + 30f, top + 150f, muted) }
    }

    private fun number(g: Gfx, b: Beat, t: Float, a: R) {
        val (num, unit) = statParts(b.main)
        val f = fitSize(g, num, FontSpec(FontFamily.DISPLAY, 900, 260f), a.w - 40f)
        val k = Easing.BACK_OUT.ease(clamp01(t / 600f))
        g.fillRadialGlow(a.cx, a.cy, 320f, accent.withAlpha(0.22f))
        g.withPose(Pose(alpha = clamp01(k), scale = lerp(0.6f, 1f, k)), a.cx, a.cy) {
            g.drawText(num, a.cx - g.measureText(num, f) / 2, a.cy + g.metrics(f).ascent / 2 - 30f, f, accent)
            if (unit.isNotEmpty()) {
                val uf = fitSize(g, unit, FontSpec(FontFamily.TEXT, 800, 48f), a.w - 40f)
                g.drawText(unit, a.cx - g.measureText(unit, uf) / 2, a.cy + 140f, uf, ink)
            }
        }
    }

    private fun versus(g: Gfx, b: Beat, t: Float, a: R, key: String) {
        val col = if (b.good) ok else danger
        val f = FontSpec(FontFamily.TEXT, 700, 48f)
        val bl = cache.fit(g, "v/$key", b.main, f, a.w - 120f, 4, 1.22f)
        val h = bl.height + 200f
        val top = a.cy - h / 2
        card(g, a.l, top, a.r, top + h, col)
        g.fillCircle(a.cx, top + 80f, 42f, col)
        Icons.draw(g, if (b.good) IconId.CHECK else IconId.CROSS, a.cx, top + 80f, 44f, rgb(0xFFFFFF), 3.4f)
        g.drawBlock(bl, a.cx, top + 150f, ink, Align.CENTER)
        if (!b.good) {
            val s = Easing.EASE_IN_OUT.ease(clamp01((t - 900f) / 500f))
            for (i in bl.lines.indices) {
                val base = g.baselineOf(bl, top + 150f, i)
                val lw = g.measureText(bl.lines[i], bl.font)
                if (s > 0f) g.fillRect(a.cx - lw / 2, base - bl.font.size * 0.3f, a.cx - lw / 2 + lw * s, base - bl.font.size * 0.3f + 5f, danger)
            }
        }
    }

    private fun checks(g: Gfx, b: Beat, t: Float, a: R, key: String) {
        val items = b.list.take(5)
        val f = FontSpec(FontFamily.TEXT, 700, 40f)
        val blocks = items.mapIndexed { i, s -> cache.fit(g, "k/$key/$i", s, f, a.w - 150f, 2, 1.18f) }
        val rows = blocks.map { it.height + 44f }
        var y = a.cy - rows.sum() / 2
        for ((i, bl) in blocks.withIndex()) {
            val k = Easing.BACK_OUT.ease(clamp01((t - 250f - i * 350f) / 350f))
            if (k > 0f) g.withPose(Pose(alpha = clamp01(k), dx = (1 - k) * 30f), a.l, y) {
                card(g, a.l, y + 4f, a.r, y + rows[i] - 8f, ok.withAlpha(0.5f))
                g.fillCircle(a.l + 52f, y + rows[i] / 2 - 2f, 24f, ok)
                Icons.draw(g, IconId.CHECK, a.l + 52f, y + rows[i] / 2 - 2f, 30f, rgb(0xFFFFFF), 3.4f, reveal = clamp01(k))
                g.drawBlock(bl, a.l + 100f, y + rows[i] / 2 - bl.height / 2 - 2f, ink)
            }
            y += rows[i]
        }
    }

    private fun textCard(g: Gfx, b: Beat, t: Float, a: R, key: String) = shield(g, t, a)

    private fun cta(g: Gfx, b: Beat, t: Float) {
        val cy = 820f
        val k = Easing.BACK_OUT.ease(clamp01(t / 600f))
        g.fillRadialGlow(540f, cy - 160f, 320f, accent.withAlpha(0.3f))
        g.withPose(Pose(scale = lerp(0.5f, 1f, k), alpha = clamp01(k)), 540f, cy - 160f) {
            Icons.draw(g, IconId.SHIELD, 540f, cy - 160f, 240f, accent, 2.2f)
            Icons.draw(g, IconId.CHECK, 540f, cy - 154f, 100f, accent, 3f)
        }
        val f = FontSpec(FontFamily.TEXT, 800, 56f)
        val text = "Abonne-toi"
        val w = g.measureText(text, f) + 110f
        val pulse = 1f + 0.035f * sin(t * 0.008f)
        g.withPose(Pose(scale = pulse), 540f, cy + 120f) {
            g.fillRoundRect(540f - w / 2, cy + 75f, 540f + w / 2, cy + 165f, 45f, danger)
            g.drawText(text, 540f - g.measureText(text, f) / 2, cy + 140f, f, rgb(0xFFFFFF))
        }
        astuceNo?.let {
            val nf = FontSpec(FontFamily.TEXT, 700, 38f)
            val s1 = "Prochaine : "; val s2 = "Astuce ${Fmt.pad(it + 1, 3)}"
            val w2 = g.measureText(s1, nf) + g.measureText(s2, nf)
            val a2 = clamp01((t - 500f) / 400f)
            g.withPose(Pose(alpha = a2), 540f, cy + 240f) {
                g.drawText(s1, 540f - w2 / 2, cy + 250f, nf, muted)
                g.drawText(s2, 540f - w2 / 2 + g.measureText(s1, nf), cy + 250f, nf, accent)
            }
        }
        val bl = cache.fit(g, "cta/${b.caption}", b.caption, FontSpec(FontFamily.TEXT, 600, 34f), Safe.WIDTH - 60f, 2, 1.3f)
        g.drawBlock(bl, 540f, cy + 320f, muted, Align.CENTER)
    }
}
