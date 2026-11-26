// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.sin

/**
 * Formats « réseaux sociaux » (vidéos explicatives courtes au rythme rapide) : chaque scène est découpée
 * en *temps forts* de 2 à 4 s. Un temps fort = un titre percutant en haut (un mot mis en valeur), un
 * visuel concret au centre (téléphone, fenêtre, terminal, carte, chiffre) et des sous-titres qui
 * défilent mot à mot en bas, comme une voix off écrite.
 *
 * Trois habillages partagent cette grammaire : [ReelStyle.NEON] (explicatif sombre, titres condensés),
 * [ReelStyle.COLLAGE] (fond bleu, cartes en papier épinglées, étiquettes), [ReelStyle.CINEMA]
 * (dégradé nocturne, serif italique, téléphone réaliste).
 */
internal enum class ReelStyle { NEON, COLLAGE, CINEMA }

internal class ReelLook(private val sb: Storyboard, private val template: Template, private val rs: ReelStyle) : LookRenderer {
    private val accentPick = Rand.of(sb.seed, 0x5EE1).let { (it.nextInt() ushr 1) % 3 }
    override val ink = when (rs) { ReelStyle.COLLAGE -> rgb(0xFFFFFF); else -> rgb(0xF4F5F7) }
    override val muted = when (rs) { ReelStyle.COLLAGE -> rgb(0xC9D6FF); ReelStyle.CINEMA -> rgb(0xA9A6C8); else -> rgb(0x8C93A1) }
    override val accent: Int = when (rs) {
        ReelStyle.NEON -> listOf(rgb(0xF5B301), rgb(0x3DDC97), rgb(0xFF5C7A))[accentPick]
        ReelStyle.COLLAGE -> listOf(rgb(0x8FD3FF), rgb(0xFFD166), rgb(0x7CF5C2))[accentPick]
        ReelStyle.CINEMA -> listOf(rgb(0x8EA2FF), rgb(0xC9A7FF), rgb(0x7FD1FF))[accentPick]
    }
    override val accent2: Int = when (rs) { ReelStyle.COLLAGE -> rgb(0xFF6B1A); ReelStyle.CINEMA -> rgb(0xFF4D6D); else -> rgb(0x1C2230) }
    private val danger = rgb(0xFF4D4D)
    private val ok = rgb(0x34D27B)
    private val cache = TextCache()
    private val domain by lazy { DomainDetector.detect(TextSanitizer.clean(sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body })) }

    // ───────────────────────────── Temps forts ─────────────────────────────

    enum class Visual { ICON, PHONE_LOCK, PHONE_SETTINGS, PHONE_KEYPAD, WINDOW_TEXT, TERMINAL, NUMBER, BAD, GOOD, CHECKS, CTA, HOOK }

    data class Beat(val headline: String, val caption: String, val visual: Visual, val main: String = "", val detail: String? = null,
                    val list: List<String> = emptyList(), val number: Int = 0, val icon: IconId = IconId.BULB, val weight: Float = 1f)

    private val beatsCache = HashMap<String, List<Beat>>()

    /** Icône qui correspond au sens de l'élément (mot de passe → clé, surveiller → œil…). */
    private fun iconFor(text: String, fallback: IconId): IconId {
        val t = text.lowercase()
        return when {
            Regex("mfa|double authentification|2fa|verrou|chiffr").containsMatchIn(t) -> IconId.LOCK
            Regex("mot de passe|password|clé|cle ").containsMatchIn(t) -> IconId.KEY
            Regex("surveill|journal|\\blogs?\\b|alerte|repère|repere").containsMatchIn(t) -> IconId.EYE
            Regex("limit|bloqu|pare-feu|firewall|protég|proteg").containsMatchIn(t) -> IconId.SHIELD
            Regex("mail|courriel|message").containsMatchIn(t) -> IconId.MAIL
            Regex("wi-?fi|réseau|reseau|routeur|vlan").containsMatchIn(t) -> IconId.ROUTER
            Regex("mise à jour|mises à jour|update|correctif").containsMatchIn(t) -> IconId.CLOCK
            Regex("code|git|api|script|commande").containsMatchIn(t) -> IconId.CODE
            Regex("sommeil|dormir|nuit").containsMatchIn(t) -> IconId.MOON
            Regex("\\beaux?\\b|boire|hydrat").containsMatchIn(t) -> IconId.DROP
            Regex("marche|sport|cœur|coeur|santé|sante").containsMatchIn(t) -> IconId.HEART
            Regex("légume|fruit|aliment|manger").containsMatchIn(t) -> IconId.LEAF
            Regex("ia|prompt|modèle|modele|chatgpt|claude").containsMatchIn(t) -> IconId.BRAIN
            Regex("audience|abonné|abonne|publication|post|linkedin").containsMatchIn(t) -> IconId.MEGAPHONE
            else -> fallback
        }
    }

    /** Titre percutant : 2 à 6 mots, coupé avant « : », « ( » ou une virgule, mot clé mis en valeur. */
    private fun punch(text: String, maxWords: Int = 5): String {
        val plain = Marked.strip(text).replace('\u00A0', ' ')
        val cut = plain.split(Regex(" : | \\(|, | — | - ")).first().trim().trimEnd('.', '!', ':')
        val words = cut.split(' ').filter { it.isNotBlank() }
        val kept = if (words.size <= maxWords + 1) words else words.take(maxWords)
        val markedOrig = Regex("\\*([^*]+)\\*").find(text)?.groupValues?.get(1)?.split(' ')?.firstOrNull()
        val key = kept.firstOrNull { it == markedOrig } ?: kept.filter { it.length > 3 }.maxByOrNull { (if (DomainDetector.isKeyword(it, domain)) 100 else 0) + it.length }
        return kept.joinToString(" ") { if (it == key) "*$it*" else it } + (if (kept.size < words.size) "…" else "")
    }

    private fun beats(sc: Scene): List<Beat> = beatsCache.getOrPut(sc.id) {
        val r = Rand.of(sb.seed xor sc.id.hashCode().toLong(), 0x2B1)
        val icon = sc.iconOrDefault()
        val all = (sb.title + " " + sc.title + " " + sc.body + " " + sc.items.joinToString(" ")).lowercase()
        val phoneish = Regex("téléphone|telephone|smartphone|android|iphone|appli|application|code|mfa|notification|wi-?fi").containsMatchIn(all)
        fun plain(s: String) = Marked.strip(s)
        when (sc.layout) {
            Layout.HERO -> listOf(
                Beat(punch(sc.title, 6), plain(sc.title), if (rs == ReelStyle.COLLAGE) Visual.HOOK else Visual.ICON, icon = icon, weight = 0.9f),
                Beat(sc.kicker?.let { punch(it.lowercase().replaceFirstChar { c -> c.uppercase() }, 4) } ?: punch(sc.title),
                    plain(sc.subtitle ?: sc.body).ifEmpty { plain(sc.title) },
                    if (phoneish) Visual.PHONE_LOCK else Visual.WINDOW_TEXT, main = plain(sc.title), detail = sc.subtitle?.let(::plain), icon = icon),
            )
            Layout.DEFINITION -> {
                val body = plain(sc.body.ifEmpty { sc.subtitle ?: sc.title })
                val cap = if (sc.authorTitle) plain(sc.title).trimEnd(':') + ". " + body else body
                listOf(Beat(punch(sc.title), cap, if (rs == ReelStyle.CINEMA || r.next() < 0.5f) Visual.WINDOW_TEXT else Visual.ICON, main = body, icon = icon))
            }
            Layout.STEPS -> (if (sc.authorTitle) listOf(Beat(punch(sc.title, 4), plain(sc.title), Visual.ICON, main = plain(sc.title), icon = icon, weight = 0.8f)) else emptyList()) + sc.items.mapIndexed { i, it ->
                val (label, detail) = splitLabel(plain(it))
                val cmd = Regex("^(sudo|git|ls|df|du|ps|ssh|ping|ip|apt|systemctl|docker|npm|show|vlan|interface|ufw|chmod|netstat|nmap|get-|set-)", RegexOption.IGNORE_CASE).containsMatchIn(label)
                val v = when {
                    cmd -> Visual.TERMINAL
                    phoneish && i % 2 == 0 -> if (Regex("code|mot de passe|pin|mfa").containsMatchIn(it.lowercase())) Visual.PHONE_KEYPAD else Visual.PHONE_SETTINGS
                    else -> listOf(Visual.ICON, Visual.WINDOW_TEXT, Visual.PHONE_SETTINGS)[(i + accentPick) % 3]
                }
                Beat("${Fmt.pad(i + 1, 2)}  ${punch(label, 4)}", plain(it), v, main = label, detail = detail, list = sc.items.map { x -> splitLabel(plain(x)).first },
                    number = i, icon = iconFor(it, listOf(IconId.SHIELD, IconId.CHECK, IconId.TARGET, IconId.BULB)[(i + icon.ordinal) % 4]))
            }
            Layout.STAT -> listOf(Beat(punch(sc.title), plain(sc.title) + (if (sc.body.isNotEmpty()) " " + plain(sc.body) else ""), Visual.NUMBER, main = sc.statValue ?: "", icon = icon))
            Layout.COMPARE -> sc.compare?.let { c -> listOf(
                Beat("*${c.badLabel}*", "${c.badLabel} : ${c.badText()}", Visual.BAD, main = c.badText(), icon = IconId.CROSS),
                Beat("*${c.goodLabel}*", "${c.goodLabel} : ${c.goodText()}" + (sc.subtitle?.let { " " + plain(it) } ?: ""), Visual.GOOD, main = c.goodText(), icon = IconId.CHECK),
            ) } ?: listOf(Beat(sc.title, plain(sc.title), Visual.ICON, icon = icon))
            Layout.CHECKLIST -> listOfNotNull(
                Beat(punch(sc.title), plain(sc.subtitle ?: sc.items.joinToString(", ") { plain(it) }), Visual.CHECKS, list = sc.items.map(::plain), weight = 1.4f),
                if (sc.body.isNotEmpty()) Beat(punch(sc.body), plain(sc.body), Visual.CTA, main = plain(sc.body), weight = 0.8f) else null,
            )
            Layout.SCREEN -> emptyList()
        }
    }

    /** Temps fort courant et progression locale (0..1) dans ce temps fort. */
    private fun beatAt(sc: Scene, t: Float): Triple<Int, Float, Float> {
        val bs = beats(sc)
        val weights = bs.map { it.weight * (0.6f + 0.4f * (it.caption.length.coerceIn(20, 160) / 90f)) }
        val total = weights.sum()
        var acc = 0f
        for ((i, w) in weights.withIndex()) {
            val len = sc.durationMs * w / total
            if (t < acc + len || i == bs.lastIndex) return Triple(i, (t - acc).coerceAtLeast(0f), len)
            acc += len
        }
        return Triple(0, t, sc.durationMs.toFloat())
    }

    // ───────────────────────────── Scène ─────────────────────────────

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        background(g, tGlobalMs)
        val bs = beats(sc)
        if (bs.isEmpty()) return
        val (i, bt, len) = beatAt(sc, localMs)
        val b = bs[i]
        // Entrée de chaque temps fort : coupe rapide (petit zoom + fondu de 220 ms).
        val e = Easing.EXPO_OUT.ease(clamp01(bt / 260f))
        g.withPose(Pose(alpha = e, scale = lerp(1.04f, 1f, e)), 540f, 900f) {
            val top = header(g, sc, b, bt)
            visual(g, sc, b, i, bt, len, top)
        }
        captions(g, b, bt, len, "${sc.id}/$i")
    }

    private fun background(g: Gfx, t: Float) {
        when (rs) {
            ReelStyle.NEON -> {
                g.fillRect(0f, 0f, 1080f, 1920f, rgb(0x0B0F17))
                g.fillRadialGlow(540f, 820f, 760f, accent.withAlpha(0.06f))
            }
            ReelStyle.COLLAGE -> {
                g.fillVerticalGradient(0f, 0f, 1080f, 1920f, rgb(0x2463EB), rgb(0x0B2A8A))
                g.fillRadialGlow(300f, 400f, 700f, rgb(0x5B8CFF).withAlpha(0.35f))
                val r = Rand(0xB1E)
                repeat(220) { g.fillCircle(r.range(0f, 1080f), r.range(0f, 1920f), r.range(0.8f, 2.4f), 0x14FFFFFF) }
                repeat(160) { g.fillCircle(r.range(0f, 1080f), r.range(0f, 1920f), r.range(0.8f, 2.4f), 0x10000000) }
            }
            ReelStyle.CINEMA -> {
                g.fillVerticalGradient(0f, 0f, 1080f, 1920f, rgb(0x0A0820), rgb(0x05040F))
                g.fillRadialGlow(220f + 80f * sin(t * 0.0003f), 1250f, 760f, rgb(0x3B2BFF).withAlpha(0.38f))
                g.fillRadialGlow(900f, 900f + 120f * sin(t * 0.00025f), 620f, rgb(0xFF2D55).withAlpha(0.22f))
                g.fillRadialGlow(560f, 300f, 600f, rgb(0x6A3DFF).withAlpha(0.18f))
            }
        }
    }

    // ───────────────────────────── Titre ─────────────────────────────

    /** Titre du temps fort, révélé mot à mot. Renvoie le bas du titre. */
    private fun header(g: Gfx, sc: Scene, b: Beat, t: Float): Float {
        var y = 200f
        // Étiquette (rubrique) en haut à gauche, comme une émission.
        val tag = (sc.kicker ?: sb.domainTag()).uppercase()
        val tf = FontSpec(FontFamily.TEXT, 800, 24f)
        when (rs) {
            ReelStyle.COLLAGE -> {
                val w = g.measureTracked(tag, tf, 2f) + 36f
                g.strokeRoundRect(Safe.LEFT, y - 8f, Safe.LEFT + w, y + 36f, 22f, 2f, 0xCCFFFFFF.toInt())
                g.drawTracked(tag, Safe.LEFT + 18f, y + 22f, tf, rgb(0xFFFFFF), 2f)
                y += 90f
            }
            ReelStyle.CINEMA -> y += 30f
            ReelStyle.NEON -> {}
        }
        val marked = Marked.parse(b.headline)
        val font = when (rs) {
            ReelStyle.NEON -> FontSpec(FontFamily.CONDENSED, 700, 132f)
            ReelStyle.COLLAGE -> FontSpec(FontFamily.TEXT, 800, 70f)
            ReelStyle.CINEMA -> FontSpec(FontFamily.TEXT, 700, 84f)
        }
        val text = if (rs == ReelStyle.NEON) marked.plain.uppercase() else marked.plain
        val block = cache.fit(g, "h/${sc.id}/${b.headline}", text, font, Safe.WIDTH, 3, if (rs == ReelStyle.NEON) 1.0f else 1.12f)
        val runs = MarkedText.runs(block, marked)
        val markFont = if (rs == ReelStyle.CINEMA) FontSpec(FontFamily.CINEMA_ITALIC, 400, block.font.size * 1.08f) else block.font
        val center = rs != ReelStyle.NEON
        if (rs == ReelStyle.COLLAGE) {
            // Bandeau de papier épinglé, comme une étiquette de classeur.
            val w = (block.lines.maxOfOrNull { g.measureText(it, block.font) } ?: 400f) + 70f
            val l = 540f - w / 2
            g.save(); g.rotate(-1.2f, 540f, y + block.height / 2)
            g.fillRect(l + 8f, y - 16f, l + w + 8f, y + block.height + 34f, 0x33000000)
            g.fillRect(l, y - 24f, l + w, y + block.height + 26f, rgb(0xF4EFE4))
            clip(g, 540f, y - 26f)
            var wi = 0
            for (li in block.lines.indices) {
                val base = g.baselineOf(block, y, li)
                val lw = MarkedText.lineWidth(g, runs[li], block.font)
                MarkedText.drawLine(g, runs[li], 540f - lw / 2, base, block.font, rgb(0x14203A), rgb(0x1E5BFF))
                wi++
            }
            g.restore()
            return y + block.height + 40f
        }
        var word = 0
        for (li in block.lines.indices) {
            val base = g.baselineOf(block, y, li)
            val lw = MarkedText.lineWidth(g, runs[li], block.font, markFont)
            var x = if (center) 540f - lw / 2 else Safe.LEFT
            // Révélation mot à mot (60 ms par mot).
            for ((txt, m) in runs[li]) {
                for (piece in Regex("\\S+\\s*").findAll(txt).map { it.value }) {
                    val a = Easing.EXPO_OUT.ease(clamp01((t - word * 60f) / 240f))
                    val f = if (m) markFont else block.font
                    val col = if (m) accent else ink
                    if (a > 0f) g.withPose(Pose(alpha = a, dy = (1 - a) * 26f), x, base) { g.drawText(piece, x, base, f, col) }
                    x += g.measureText(piece, f)
                    word++
                }
            }
        }
        return y + block.height
    }

    // ───────────────────────────── Visuels ─────────────────────────────

    private fun visual(g: Gfx, sc: Scene, b: Beat, idx: Int, t: Float, len: Float, top: Float) {
        val cy = ((top + 1300f) / 2).coerceIn(780f, 900f)
        val a = Easing.BACK_OUT.ease(clamp01((t - 120f) / 450f))
        g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.88f, 1f, a), dy = (1 - a) * 30f), 540f, cy) {
            when (b.visual) {
                Visual.ICON -> iconCard(g, b, cy, t)
                Visual.HOOK -> hook(g, b, cy, t)
                Visual.PHONE_LOCK, Visual.PHONE_SETTINGS, Visual.PHONE_KEYPAD -> {
                    // Le téléphone occupe l'espace entre le titre et les sous-titres (réduit si le titre est long).
                    val pivot = top + 34f
                    val k = ((1335f - pivot) / 930f).coerceAtMost(1f)
                    g.save(); g.scale(k, 540f, pivot)
                    phone(g, b, pivot + 410f, t)
                    g.restore()
                }
                Visual.WINDOW_TEXT -> windowText(g, b, cy, t, "${sc.id}/$idx")
                Visual.TERMINAL -> terminal(g, b, cy, t)
                Visual.NUMBER -> number(g, b, cy, t)
                Visual.BAD, Visual.GOOD -> verdict(g, b, cy, t, "${sc.id}/$idx")
                Visual.CHECKS -> checks(g, b, cy, t, "${sc.id}/$idx")
                Visual.CTA -> cta(g, b, cy, t)
            }
        }
    }

    /** Carte de base selon l'habillage : fenêtre sombre, papier épinglé ou verre dépoli. */
    private fun card(g: Gfx, l: Float, tp: Float, r: Float, bt: Float, title: String? = null, tilt: Float = 0f) {
        when (rs) {
            ReelStyle.NEON -> {
                g.fillRoundRect(l, tp, r, bt, 14f, rgb(0x151B26))
                g.strokeRoundRect(l, tp, r, bt, 14f, 1.5f, 0x22FFFFFF)
                g.fillRoundRect(l, tp, r, tp + 46f, 14f, rgb(0x1C2331)); g.fillRect(l, tp + 32f, r, tp + 46f, rgb(0x1C2331))
                for (k in 0 until 3) g.fillCircle(l + 26f + k * 22f, tp + 23f, 6f, 0x55FFFFFF)
                title?.let { g.drawText(it, l + 100f, tp + 31f, FontSpec(FontFamily.MONO, 400, 20f), 0x88FFFFFF.toInt()) }
            }
            ReelStyle.COLLAGE -> {
                g.save(); g.rotate(tilt, (l + r) / 2, (tp + bt) / 2)
                g.fillRect(l + 10f, tp + 14f, r + 10f, bt + 14f, 0x40000000)
                g.fillRect(l, tp, r, bt, rgb(0xF4EFE4))
                clip(g, (l + r) / 2, tp - 2f)
                g.restore()
            }
            ReelStyle.CINEMA -> {
                g.fillRoundRect(l, tp, r, bt, 28f, 0x1EFFFFFF)
                g.strokeRoundRect(l, tp, r, bt, 28f, 1.5f, 0x30FFFFFF)
            }
        }
    }

    private val cardInk get() = if (rs == ReelStyle.COLLAGE) rgb(0x14203A) else ink
    private val cardMuted get() = if (rs == ReelStyle.COLLAGE) rgb(0x5B6478) else muted

    /** Pince à dessin (style collage). */
    private fun clip(g: Gfx, cx: Float, y: Float) {
        g.fillRoundRect(cx - 34f, y - 26f, cx + 34f, y + 14f, 6f, rgb(0x1B1B1F))
        g.fillRoundRect(cx - 26f, y - 46f, cx + 26f, y - 22f, 10f, 0x00000000)
        g.strokeRoundRect(cx - 22f, y - 52f, cx + 22f, y - 20f, 10f, 5f, rgb(0x9AA0A8))
    }

    /** Étiquette autocollante inclinée (style collage) ou pastille (autres). */
    private fun sticker(g: Gfx, text: String, x: Float, y: Float, color: Int, t: Float, delay: Float) {
        val a = Easing.BACK_OUT.ease(clamp01((t - delay) / 380f))
        if (a <= 0f) return
        val f = FontSpec(if (rs == ReelStyle.NEON) FontFamily.CONDENSED else FontFamily.TEXT, if (rs == ReelStyle.NEON) 700 else 800, if (rs == ReelStyle.NEON) 44f else 34f)
        val label0 = text.uppercase()
        val label = TextLayout.fit(g, label0, f, Safe.WIDTH - 80f, 1).lines.firstOrNull() ?: label0
        val w = g.measureText(label, f) + 44f
        val x = x.coerceAtMost(Safe.RIGHT - 20f - w).coerceAtLeast(Safe.LEFT + 20f)
        g.withPose(Pose(alpha = clamp01(a), scale = lerp(1.6f, 1f, a)), x + w / 2, y + 30f) {
            g.save(); g.rotate(if (rs == ReelStyle.COLLAGE) -5f else 0f, x + w / 2, y + 30f)
            g.fillRoundRect(x, y, x + w, y + 62f, if (rs == ReelStyle.CINEMA) 31f else 8f, color)
            g.drawText(label, x + 22f, y + 31f + g.metrics(f).ascent / 2, f, if (luma(color) > 0.6f) rgb(0x111111) else rgb(0xFFFFFF))
            g.restore()
        }
    }

    private fun iconCard(g: Gfx, b: Beat, cy: Float, t: Float) {
        val s = 380f
        val l = 540f - s / 2
        if (rs == ReelStyle.COLLAGE) {
            g.save(); g.rotate(-3f, 540f, cy)
            g.fillRoundRect(l + 10f, cy - s / 2 + 14f, l + s + 10f, cy + s / 2 + 14f, 46f, 0x40000000)
            g.fillRoundRect(l, cy - s / 2, l + s, cy + s / 2, 46f, rgb(0xFFFFFF))
            Icons.draw(g, b.icon, 540f, cy, 220f, rgb(0x14203A), 1.6f, reveal = clamp01(t / 700f))
            g.restore()
        } else {
            g.fillCircle(540f, cy, 240f, accent.withAlpha(if (rs == ReelStyle.CINEMA) 0.22f else 0.12f))
            g.strokeCircle(540f, cy, 240f, 3f, accent.withAlpha(0.6f))
            Icons.draw(g, b.icon, 540f, cy, 260f, accent, 1.6f, reveal = clamp01(t / 700f))
        }
        b.detail?.let { sticker(g, it.take(28), 560f, cy + 200f, accent2.takeIf { rs != ReelStyle.NEON } ?: accent, t, 600f) }
    }

    /** Accroche « lettres découpées » (style collage) : chaque lettre sur sa propre étiquette. */
    private fun hook(g: Gfx, b: Beat, cy: Float, t: Float) {
        // Les mots qui portent le sens (on saute les petits mots), 3 au plus.
        val words = Marked.strip(b.headline).trimEnd('…').uppercase().split(' ').filter { it.length > 3 }.take(3)
        val r = Rand(b.headline.hashCode())
        var y = cy - (words.size - 1) * 85f
        val colors = listOf(rgb(0xFFFFFF), rgb(0x111111), rgb(0xFFD166), rgb(0xFF6B1A), rgb(0x8FD3FF))
        var n = 0
        for (w in words) {
            val fonts = listOf(FontFamily.HEAVY, FontFamily.CINEMA, FontFamily.CONDENSED, FontFamily.DISPLAY, FontFamily.SERIF)
            val sizes = w.map { 84f }
            val widths = w.mapIndexed { k, ch -> g.measureText(ch.toString(), FontSpec(fonts[(k + n) % fonts.size], 400, sizes[k])) + 26f }
            var x = 540f - widths.sum() / 2
            for ((k, ch) in w.withIndex()) {
                val a = Easing.BACK_OUT.ease(clamp01((t - n * 45f) / 300f))
                val bg = colors[(k + n) % colors.size]
                val f = FontSpec(fonts[(k + n) % fonts.size], if (fonts[(k + n) % fonts.size] == FontFamily.DISPLAY) 900 else if (fonts[(k + n) % fonts.size] == FontFamily.CONDENSED) 700 else 400, 84f)
                val rot = r.range(-7f, 7f)
                if (a > 0f) g.withPose(Pose(alpha = clamp01(a), scale = lerp(1.8f, 1f, a)), x + widths[k] / 2, y) {
                    g.save(); g.rotate(rot, x + widths[k] / 2, y)
                    g.fillRect(x + 3f, y - 66f, x + widths[k] - 3f, y + 50f, bg)
                    g.drawText(ch.toString(), x + 13f, y + 28f, f, if (luma(bg) > 0.6f) rgb(0x111111) else rgb(0xFFFFFF))
                    g.restore()
                }
                x += widths[k]; n++
            }
            y += 170f
        }
    }

    private fun windowText(g: Gfx, b: Beat, cy: Float, t: Float, key: String) {
        val text = b.main.ifEmpty { b.caption }
        val f = FontSpec(FontFamily.TEXT, 600, 50f)
        val block = cache.fit(g, "w/$key", text, f, 790f, 7, 1.3f)
        val h = block.height + 150f
        val l = Safe.LEFT; val r = Safe.RIGHT
        card(g, l, cy - h / 2, r, cy + h / 2, title = sb.footer.lowercase() + ".app", tilt = 1.5f)
        val p = clamp01((t - 300f) / (block.charCount * 18f).coerceAtLeast(400f))
        g.save(); if (rs == ReelStyle.COLLAGE) g.rotate(1.5f, 540f, cy)
        g.drawBlock(block, l + 60f, cy - h / 2 + (if (rs == ReelStyle.NEON) 90f else 70f), cardInk, reveal = p)
        g.restore()
    }

    private fun terminal(g: Gfx, b: Beat, cy: Float, t: Float) {
        val l = 100f; val r = 980f
        val h = 300f
        val top = cy - h / 2
        g.fillRoundRect(l + 8f, top + 14f, r + 8f, top + h + 14f, 16f, 0x55000000)
        g.fillRoundRect(l, top, r, top + h, 16f, rgb(0x0D1117))
        g.fillRoundRect(l, top, r, top + 48f, 16f, rgb(0x1F242C)); g.fillRect(l, top + 32f, r, top + 48f, rgb(0x1F242C))
        listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(l + 28f + i * 24f, top + 24f, 7f, c) }
        g.drawText("Terminal", 540f - 40f, top + 31f, FontSpec(FontFamily.TEXT, 600, 19f), 0x99FFFFFF.toInt())
        val f = FontSpec(FontFamily.MONO, 700, 34f)
        val cmd = TextLayout.fit(g, b.main, f, r - l - 120f, 1, minSize = 22f)
        val line = cmd.lines.firstOrNull() ?: ""
        val p = clamp01((t - 300f) / (line.length * 50f).coerceAtLeast(300f))
        val shown = line.take((line.length * p).toInt())
        g.drawText("$", l + 36f, top + 120f, cmd.font, ok)
        g.drawText(shown, l + 80f, top + 120f, cmd.font, rgb(0xE6EDF3))
        val cx = l + 82f + g.measureText(shown, cmd.font)
        if ((t / 400f).toInt() % 2 == 0) g.fillRect(cx, top + 92f, cx + 16f, top + 128f, accent)
        b.detail?.let { d ->
            val db = TextLayout.fit(g, "# $d", FontSpec(FontFamily.MONO, 400, 28f), r - l - 80f, 2, lineHeightFactor = 1.3f, minSize = 22f)
            if (p >= 1f) g.drawBlock(db, l + 36f, top + 170f, 0x99FFFFFF.toInt())
        }
    }

    private fun number(g: Gfx, b: Beat, cy: Float, t: Float) {
        val (num, unit) = statParts(b.main)
        val f = fitSize(g, num, FontSpec(if (rs == ReelStyle.CINEMA) FontFamily.CINEMA else if (rs == ReelStyle.NEON) FontFamily.CONDENSED else FontFamily.DISPLAY,
            if (rs == ReelStyle.CINEMA) 400 else if (rs == ReelStyle.NEON) 700 else 900, 300f), 860f)
        val target = num.filter { it.isDigit() }.toLongOrNull()
        val k = Easing.EXPO_OUT.ease(clamp01((t - 150f) / 1300f))
        val shown = if (target != null && num.all { it.isDigit() } && target in 2..100000 && !b.caption.lowercase().contains("port")) (target * k).toLong().toString() else num
        if (rs == ReelStyle.COLLAGE) card(g, 150f, cy - 230f, 930f, cy + 200f, tilt = -2f)
        val col = if (rs == ReelStyle.COLLAGE) rgb(0x1E5BFF) else accent
        g.drawText(shown, 540f - g.measureText(num, f) / 2, cy + g.metrics(f).ascent / 2 - 40f, f, col)
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit.uppercase(), FontSpec(FontFamily.TEXT, 800, 46f), 820f)
            g.drawTracked(unit.uppercase(), 540f - g.measureTracked(unit.uppercase(), uf, 3f) / 2, cy + 130f, uf, if (rs == ReelStyle.COLLAGE) rgb(0x14203A) else ink, 3f)
        }
    }

    private fun verdict(g: Gfx, b: Beat, cy: Float, t: Float, key: String) {
        val good = b.visual == Visual.GOOD
        val col = if (good) ok else danger
        val text = b.main
        val f = FontSpec(FontFamily.TEXT, 700, 54f)
        val block = cache.fit(g, "v/$key", text, f, 780f, 4, 1.22f)
        val h = block.height + 220f
        val l = Safe.LEFT; val r = Safe.RIGHT
        val shake = if (!good && t in 700f..1100f) sin(t * 0.1f) * 10f else 0f
        g.save(); g.translate(shake, 0f)
        card(g, l, cy - h / 2, r, cy + h / 2, tilt = if (good) 1.5f else -1.5f)
        g.save(); if (rs == ReelStyle.COLLAGE) g.rotate(if (good) 1.5f else -1.5f, 540f, cy)
        g.fillCircle(540f, cy - h / 2 + (if (rs == ReelStyle.NEON) 110f else 80f), 40f, col)
        Icons.draw(g, if (good) IconId.CHECK else IconId.CROSS, 540f, cy - h / 2 + (if (rs == ReelStyle.NEON) 110f else 80f), 44f, rgb(0xFFFFFF), 3.4f)
        val top = cy - h / 2 + (if (rs == ReelStyle.NEON) 165f else 140f)
        g.drawBlock(block, 540f, top, cardInk, Align.CENTER)
        if (!good) {
            val s = Easing.EASE_IN_OUT.ease(clamp01((t - 900f) / 500f))
            for (i in block.lines.indices) {
                val base = g.baselineOf(block, top, i)
                val lw = g.measureText(block.lines[i], block.font)
                if (s > 0f) g.fillRect(540f - lw / 2, base - block.font.size * 0.3f, 540f - lw / 2 + lw * s, base - block.font.size * 0.3f + 5f, danger)
            }
        }
        g.restore(); g.restore()
        sticker(g, if (good) "À faire" else "À éviter", 600f, cy + h / 2 - 20f, col, t, 500f)
    }

    private fun checks(g: Gfx, b: Beat, cy: Float, t: Float, key: String) {
        val items = b.list.take(5)
        val f = FontSpec(FontFamily.TEXT, 700, 46f)
        val blocks = items.mapIndexed { i, s -> cache.fit(g, "c/$key/$i", s, f, 720f, 2, 1.18f) }
        val rowH = blocks.map { it.height + 48f }
        val h = rowH.sum() + 90f
        val l = Safe.LEFT; val r = Safe.RIGHT
        var y = cy - h / 2
        card(g, l, y, r, y + h, title = "checklist.md", tilt = -1f)
        y += if (rs == ReelStyle.NEON) 70f else 44f
        g.save(); if (rs == ReelStyle.COLLAGE) g.rotate(-1f, 540f, cy)
        for ((i, bl) in blocks.withIndex()) {
            val on = Easing.BACK_OUT.ease(clamp01((t - 300f - i * 380f) / 300f))
            val ccy = y + rowH[i] / 2
            g.fillCircle(l + 66f, ccy, 30f, if (on > 0f) ok.withAlpha(clamp01(on)) else 0x33FFFFFF)
            if (on > 0f) Icons.draw(g, IconId.CHECK, l + 66f, ccy, 38f, rgb(0xFFFFFF), 3.4f, reveal = clamp01(on))
            g.drawBlock(bl, l + 124f, ccy - bl.height / 2, cardInk)
            y += rowH[i]
        }
        g.restore()
    }

    private fun cta(g: Gfx, b: Beat, cy: Float, t: Float) {
        val f = FontSpec(if (rs == ReelStyle.NEON) FontFamily.CONDENSED else FontFamily.TEXT, if (rs == ReelStyle.NEON) 700 else 800, if (rs == ReelStyle.NEON) 84f else 54f)
        val text = if (rs == ReelStyle.NEON) b.main.uppercase() else b.main
        val bl = cache.fit(g, "cta/${b.main}", text, f, 820f, 2, 1.0f)
        val w = (bl.lines.maxOfOrNull { g.measureText(it, bl.font) } ?: 400f) + 100f
        val h = bl.height + 70f
        val pulse = 1f + 0.03f * sin(t * 0.008f)
        g.withPose(Pose(scale = pulse), 540f, cy) {
            g.fillRoundRect(540f - w / 2, cy - h / 2, 540f + w / 2, cy + h / 2, if (rs == ReelStyle.CINEMA) h / 2 else 14f, if (rs == ReelStyle.COLLAGE) accent2 else accent)
            g.drawBlock(bl, 540f, cy - bl.height / 2, if (rs == ReelStyle.COLLAGE) rgb(0xFFFFFF) else rgb(0x111111), Align.CENTER)
        }
        if (rs == ReelStyle.CINEMA) {
            g.fillCircle(540f, cy - h / 2 - 170f, 100f, accent.withAlpha(0.35f))
            Icons.draw(g, IconId.SHIELD, 540f, cy - h / 2 - 170f, 110f, rgb(0xFFFFFF), 2f)
        }
    }

    // ───────────────────────────── Téléphone ─────────────────────────────

    /** Téléphone réaliste (générique, sans marque) avec un écran adapté au temps fort. */
    private fun phone(g: Gfx, b: Beat, cy: Float, t: Float) {
        val w = 480f; val h = 900f
        val l = 540f - w / 2; val tp = cy - h / 2 + 40f
        val tilt = if (rs == ReelStyle.COLLAGE) 3f else 0f
        g.save(); g.rotate(tilt, 540f, cy)
        g.fillRoundRect(l + 14f, tp + 24f, l + w + 14f, tp + h + 24f, 70f, 0x66000000)
        g.fillRoundRect(l - 12f, tp - 12f, l + w + 12f, tp + h + 12f, 74f, rgb(0x2A2C33))
        g.fillRoundRect(l, tp, l + w, tp + h, 62f, rgb(0x0E0F13))
        g.save(); g.clipRect(l + 10f, tp + 10f, l + w - 10f, tp + h - 10f)
        val sl = l + 10f; val sr = l + w - 10f; val st = tp + 10f; val sbm = tp + h - 10f
        val light = b.visual == Visual.PHONE_SETTINGS
        if (light) g.fillRoundRect(sl, st, sr, sbm, 54f, rgb(0xF2F2F7))
        else g.fillVerticalGradient(sl, st, sr, sbm, rgb(0x4B2C8F), rgb(0x16123A))
        val sf = FontSpec(FontFamily.TEXT, 700, 22f)
        val fg = if (light) rgb(0x111111) else rgb(0xFFFFFF)
        g.drawText("14:32", sl + 40f, st + 44f, sf, fg)
        g.fillRoundRect(540f - 60f, st + 18f, 540f + 60f, st + 52f, 17f, rgb(0x000000))
        g.fillRoundRect(sr - 70f, st + 30f, sr - 34f, st + 46f, 4f, fg)
        when (b.visual) {
            Visual.PHONE_LOCK -> {
                val tf = FontSpec(FontFamily.DISPLAY, 800, 104f)
                g.drawText("14:32", 540f - g.measureText("14:32", tf) / 2, st + 230f, tf, rgb(0xFFFFFF))
                val df = FontSpec(FontFamily.TEXT, 500, 22f)
                val date = "mardi 24 novembre"
                g.drawText(date, 540f - g.measureText(date, df) / 2, st + 110f, df, 0xCCFFFFFF.toInt())
                // Notification qui descend
                val a = Easing.BACK_OUT.ease(clamp01((t - 500f) / 450f))
                val ny = st + 300f + (1 - a) * -80f
                if (a > 0f) g.withPose(Pose(alpha = clamp01(a)), 540f, ny) {
                    g.fillRoundRect(sl + 20f, ny, sr - 20f, ny + 200f, 28f, 0xDDFFFFFF.toInt())
                    g.fillRoundRect(sl + 40f, ny + 22f, sl + 84f, ny + 66f, 12f, accent2.takeIf { it != rgb(0x1C2230) } ?: rgb(0xFF4D6D))
                    Icons.draw(g, b.icon, sl + 62f, ny + 44f, 28f, rgb(0xFFFFFF), 2.4f)
                    g.drawText("CODJIA · maintenant", sl + 100f, ny + 50f, FontSpec(FontFamily.TEXT, 600, 18f), rgb(0x555555))
                    val nb = cache.fit(g, "n/${b.main}", b.main, FontSpec(FontFamily.TEXT, 700, 28f), sr - sl - 80f, 3, 1.2f)
                    g.drawBlock(nb, sl + 40f, ny + 78f, rgb(0x111111))
                }
            }
            Visual.PHONE_SETTINGS -> {
                g.drawText("‹ Réglages", sl + 30f, st + 100f, FontSpec(FontFamily.TEXT, 500, 22f), rgb(0x0A84FF))
                val title = cache.fit(g, "pt/${b.headline}", "Sécurité", FontSpec(FontFamily.TEXT, 800, 46f), sr - sl - 60f, 1)
                g.drawBlock(title, sl + 30f, st + 120f, rgb(0x111111))
                val all = b.list.ifEmpty { listOf(b.main) }
                val start = (b.number - 3).coerceAtLeast(0).coerceAtMost((all.size - 4).coerceAtLeast(0))
                val rows = all.drop(start).take(4)
                val hiIdx = (b.number - start).coerceIn(0, rows.lastIndex)
                val RH = 128f
                var y = st + 200f
                g.fillRoundRect(sl + 20f, y, sr - 20f, y + rows.size * RH, 18f, rgb(0xFFFFFF))
                for ((i, row) in rows.withIndex()) {
                    val hi = i == hiIdx
                    val ry = y + i * RH
                    if (hi) g.strokeRoundRect(sl + 24f, ry + 4f, sr - 24f, ry + RH - 4f, 14f, 3.5f, rgb(0xFF3B30))
                    g.fillRoundRect(sl + 38f, ry + RH / 2 - 22f, sl + 82f, ry + RH / 2 + 22f, 10f, listOf(rgb(0x0A84FF), rgb(0x34C759), rgb(0xFF9500), rgb(0xAF52DE), rgb(0xFF2D55))[(i + start) % 5])
                    val rb = TextLayout.fit(g, row, FontSpec(FontFamily.TEXT, 600, 28f), sr - sl - 224f, 2, lineHeightFactor = 1.12f, minSize = 20f)
                    g.drawBlock(rb, sl + 98f, ry + RH / 2 - rb.height / 2, rgb(0x111111))
                    val on = if (hi) Easing.EXPO_OUT.ease(clamp01((t - 900f) / 300f)) else 0f
                    val tx = sr - 104f; val tcy = ry + RH / 2
                    g.fillRoundRect(tx, tcy - 18f, tx + 64f, tcy + 18f, 18f, mix(rgb(0xD1D1D6), rgb(0x34C759), on))
                    g.fillCircle(lerp(tx + 18f, tx + 46f, on), tcy, 15f, rgb(0xFFFFFF))
                    if (i < rows.lastIndex) g.fillRect(sl + 98f, ry + RH - 1f, sr - 20f, ry + RH, rgb(0xE5E5EA))
                }
                // Doigt / curseur qui touche la ligne
                val tap = clamp01((t - 700f) / 300f)
                val hiY = y + hiIdx * RH + RH / 2
                if (tap > 0f && tap < 1f) g.strokeCircle(sr - 72f, hiY, 20f + 30f * tap, 4f * (1 - tap), rgb(0x0A84FF))
            }
            else -> { // Clavier de code
                val lf = FontSpec(FontFamily.TEXT, 500, 24f)
                g.drawText("Saisir le code", 540f - g.measureText("Saisir le code", lf) / 2, st + 140f, lf, rgb(0xFFFFFF))
                val filled = (clamp01((t - 300f) / 1400f) * 6).toInt()
                for (k in 0 until 6) {
                    val x = 540f - 100f + k * 40f
                    if (k < filled) g.fillCircle(x, st + 190f, 9f, rgb(0xFFFFFF)) else g.strokeCircle(x, st + 190f, 9f, 2f, rgb(0xFFFFFF))
                }
                val kf = FontSpec(FontFamily.TEXT, 400, 40f)
                for (k in 0 until 10) {
                    val row = if (k == 9) 3 else k / 3
                    val col = if (k == 9) 1 else k % 3
                    val x = 540f + (col - 1) * 112f; val y = st + 330f + row * 112f
                    val pressed = k == filled % 10
                    g.fillCircle(x, y, 44f, if (pressed) 0x66FFFFFF else 0x2AFFFFFF)
                    val d = if (k == 9) "0" else "${k + 1}"
                    g.drawText(d, x - g.measureText(d, kf) / 2, y + 14f, kf, rgb(0xFFFFFF))
                }
            }
        }
        g.restore()
        g.restore()
        b.detail?.takeIf { b.visual == Visual.PHONE_SETTINGS }?.let { sticker(g, it.take(26), 560f, tp + h - 90f, if (rs == ReelStyle.COLLAGE) accent2 else rgb(0xFF3B30), t, 1100f) }
    }

    // ───────────────────────────── Sous-titres mot à mot ─────────────────────────────

    /**
     * Sous-titres type « voix off » : groupes de 3 à 5 mots qui s'enchaînent sur la durée du temps fort ;
     * le mot important du groupe est coloré.
     */
    private fun captions(g: Gfx, b: Beat, t: Float, len: Float, key: String) {
        val words = b.caption.replace('\n', ' ').split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return
        val chunks = ArrayList<List<String>>()
        var cur = ArrayList<String>()
        for (w in words) {
            cur += w
            if (cur.size >= 5 || (cur.size >= 3 && (w.endsWith(",") || w.endsWith(".") || w.endsWith(":")))) { chunks += cur; cur = ArrayList() }
        }
        if (cur.isNotEmpty()) chunks += cur
        val usable = (len - 200f).coerceAtLeast(400f)
        val per = usable / chunks.size
        val ci = ((t - 100f) / per).toInt().coerceIn(0, chunks.lastIndex)
        val local = (t - 100f) - ci * per
        val chunk = chunks[ci]
        val font = when (rs) {
            ReelStyle.NEON -> FontSpec(FontFamily.CONDENSED, 700, 58f)
            ReelStyle.COLLAGE -> FontSpec(FontFamily.TEXT, 800, 50f)
            ReelStyle.CINEMA -> FontSpec(FontFamily.TEXT, 700, 46f)
        }
        val text = chunk.joinToString(" ").let { if (rs == ReelStyle.NEON) it.uppercase() else it }
        val block = cache.fit(g, "cap/$key/$ci", text, font, Safe.WIDTH - 40f, 2, 1.15f)
        val keyword = chunk.maxByOrNull { w -> (if (DomainDetector.isKeyword(w, domain)) 100 else 0) + w.length }?.let { if (rs == ReelStyle.NEON) it.uppercase() else it }
        val y = if (rs == ReelStyle.CINEMA) 1400f else 1380f
        val a = Easing.EXPO_OUT.ease(clamp01(local / 160f))
        g.withPose(Pose(alpha = a, scale = lerp(0.92f, 1f, a)), 540f, y + block.height / 2) {
            for ((li, line) in block.lines.withIndex()) {
                val base = g.baselineOf(block, y, li)
                val lw = g.measureText(line, block.font)
                var x = 540f - lw / 2
                for (piece in Regex("\\S+\\s*").findAll(line).map { it.value }) {
                    val isKey = keyword != null && piece.trim().trimEnd(',', '.', ':', '!', '?') == keyword.trimEnd(',', '.', ':', '!', '?')
                    if (rs != ReelStyle.CINEMA) g.drawText(piece, x + 3f, base + 4f, block.font, 0x55000000)
                    g.drawText(piece, x, base, block.font, if (isKey) accent else rgb(0xFFFFFF))
                    x += g.measureText(piece, block.font)
                }
            }
        }
    }

    private fun Storyboard.domainTag(): String = domain.label

    private fun luma(c: Int): Float = (0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
}
