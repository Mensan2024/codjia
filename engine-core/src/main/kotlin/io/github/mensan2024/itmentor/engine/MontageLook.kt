// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Icône qui illustre le sens d'un texte (partagée par les formats à plans courts). */
internal fun iconForText(text: String, fallback: IconId): IconId {
    val t = text.lowercase()
    return when {
        Regex("mfa|double authentification|2fa|verrou|chiffr").containsMatchIn(t) -> IconId.LOCK
        Regex("mots? de passe|password|clé|cle ").containsMatchIn(t) -> IconId.KEY
        Regex("surveill|journal|log|alerte|repère|repere").containsMatchIn(t) -> IconId.EYE
        Regex("limit|bloqu|pare-feu|firewall|protég|proteg").containsMatchIn(t) -> IconId.SHIELD
        Regex("mail|courriel|message").containsMatchIn(t) -> IconId.MAIL
        Regex("wi-?fi|réseau|reseau|routeur|vlan|switch").containsMatchIn(t) -> IconId.ROUTER
        Regex("mise à jour|mises à jour|update|correctif").containsMatchIn(t) -> IconId.CLOCK
        Regex("code|git|api|script|commande|terminal").containsMatchIn(t) -> IconId.CODE
        Regex("sommeil|dormir|nuit").containsMatchIn(t) -> IconId.MOON
        Regex("eau|boire|hydrat").containsMatchIn(t) -> IconId.DROP
        Regex("marche|sport|cœur|coeur|santé|sante").containsMatchIn(t) -> IconId.HEART
        Regex("légume|fruit|aliment|manger").containsMatchIn(t) -> IconId.LEAF
        Regex("\\bia\\b|prompt|modèle|modele|chatgpt|claude").containsMatchIn(t) -> IconId.BRAIN
        Regex("audience|abonné|abonne|publication|post|linkedin").containsMatchIn(t) -> IconId.MEGAPHONE
        Regex("cherche|recherche|trouve").containsMatchIn(t) -> IconId.SEARCH
        Regex("objectif|cible|but\\b").containsMatchIn(t) -> IconId.TARGET
        Regex("anglais|english|parler|phrase|mot\\b").containsMatchIn(t) -> IconId.CHAT
        else -> fallback
    }
}

/**
 * Format « Montage dynamique » (tutoriel animé des réseaux sociaux) :
 * - fond sombre, sol en grille en perspective, repères « + » dans les coins ;
 * - en haut, un chapitre numéroté : petite étiquette, puis un titre dont le mot clé est surligné ;
 * - au centre, une carte qui montre l'action : page qui s'écrit, prompt qui se tape, terminal, téléphone
 *   qui coche les étapes, objet en relief avec étiquette, chiffre, à éviter / à faire, guide final ;
 * - dessous, les sous-titres mot à mot, le mot prononcé surligné ;
 * - en bas, la mascotte CODJIA qui « parle ».
 * Tout le texte de l'auteur défile en sous-titres : rien n'est résumé.
 */
internal class MontageLook(private val sb: Storyboard) : LookRenderer {
    private val pick = (Rand.of(sb.seed, 0x3A11).nextInt() ushr 1)
    override val ink = rgb(0xFFFFFF)
    override val muted = rgb(0x8A8A8F)
    override val accent = listOf(rgb(0xFF7A33), rgb(0x34D27A), rgb(0x4C8DFF))[pick % 3]
    override val accent2 = rgb(0xFFD23F)
    private val ok = rgb(0x34D27A)
    private val bad = rgb(0xFF4D5E)
    private val card = rgb(0x1E1E20)
    private val inner = rgb(0x262628)
    private val dark = rgb(0x111111)
    private val cache = TextCache()
    private val pageCache = HashMap<String, List<IntRange>>()
    private val domain by lazy { DomainDetector.detect(TextSanitizer.clean(sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body })) }
    private val astuceNo: Int? = (listOf(sb.title) + sb.scenes.take(1).flatMap { listOf(it.kicker ?: "", it.title) })
        .firstNotNullOfOrNull { Regex("(?i)astuce\\D{0,30}?0*(\\d{1,4})").find(Marked.strip(it))?.groupValues?.get(1)?.toIntOrNull() }

    enum class Kind { HOOK, KEYWORDS, DOC, PROMPT, TERMINAL, PHONE, OBJECT, NUMBER, VERSUS, END }

    data class Shot(
        val kind: Kind, val caption: String, val kicker: String, val title: String, val badge: String?,
        val items: List<String> = emptyList(), val icon: IconId = IconId.BULB, val main: String = "",
        val detail: String? = null, val index: Int = 0, val good: Boolean = true, val sticker: String? = null,
    ) {
        val words: List<String> = caption.replace('\n', ' ').split(' ').filter { it.isNotBlank() }
        val weight: Float get() = (words.sumOf { it.length + 3 } / 15f).coerceAtLeast(1.6f) + if (kind == Kind.HOOK || kind == Kind.END) 0.6f else 0f
    }

    private fun plain(s: String?) = Marked.strip(s ?: "").replace(' ', ' ').trim()

    private val stop = setOf("des", "les", "de", "la", "le", "en", "à", "du", "et", "un", "une", "pour", "sur", "avec", "par", "dans", "au", "aux", "qui", "que", "ou", "si", "tu", "ne", "il", "on", "je", "ce", "se", "ton", "ta", "tes", "the", "a", "to", "of", "and", "d'", "l'")

    /** Titre court : avant « : » ou « ( », 6 mots au plus, sans finir sur un petit mot (« des », « les »…). */
    private fun short(s: String, max: Int = 8): String {
        val cut = plain(s).split(Regex(" : | \\(|, | — ")).first().trim().trimEnd('.', '!', ':')
        val w = cut.split(' ').filter { it.isNotBlank() }.take(max).toMutableList()
        while (w.size > 1 && w.last().lowercase() in stop) w.removeAt(w.lastIndex)
        return w.joinToString(" ")
    }

    private fun isCommand(label: String) =
        Regex("^[a-z][a-z0-9._-]*(\\s+[-/a-zA-Z0-9.=_\"|]+){0,5}$").matches(label) && label.length < 44

    private fun commandIn(text: String): String? =
        Regex("(?:ex\\.|:)\\s*`?((?:sudo|git|ls|df|du|ssh|apt|systemctl|docker|npm|grep|ufw|chmod|top|free|ping|ip|show|vlan|netstat|ipconfig|winget|brew)\\b[^)`]*)")
            .find(plain(text))?.groupValues?.get(1)?.trim()?.trimEnd('.', ')')

    /** Mot clé d'un titre court : celui du domaine, sinon le plus long. */
    private fun keyOf(title: String): String? = title.split(' ').map { it.trim(',', '.', ':', '!', '?') }.filter { it.length >= 3 }
        .maxByOrNull { (if (DomainDetector.isKeyword(it, domain)) 100 else 0) + it.length }

    /** Découpe un texte en plans de 16 mots au plus, aux fins de phrase, puis aux virgules. */
    private fun splitCaption(text: String): List<String> {
        val sentences = Regex("(?<=[.!?;])\\s+").split(plain(text)).filter { it.isNotBlank() }
        val out = ArrayList<String>()
        for (sn in sentences) {
            val w = sn.split(' ').filter { it.isNotBlank() }
            if (w.size <= 18) { out += sn; continue }
            var cur = ArrayList<String>()
            for (x in w) {
                cur += x
                if (cur.size >= 14 || (cur.size >= 7 && x.endsWith(","))) { out += cur.joinToString(" "); cur = ArrayList() }
            }
            if (cur.isNotEmpty()) { if (cur.size < 4 && out.isNotEmpty()) out[out.lastIndex] = out.last() + " " + cur.joinToString(" ") else out += cur.joinToString(" ") }
        }
        // regroupe les morceaux trop courts
        val merged = ArrayList<String>()
        for (c in out) if (merged.isNotEmpty() && (merged.last().split(' ').size + c.split(' ').size) <= 9) merged[merged.lastIndex] = merged.last() + " " + c else merged += c
        return merged.ifEmpty { listOf(plain(text)) }
    }

    /** Mots clés d'une phrase (sigles, termes du domaine, mots longs), dans l'ordre où ils sont dits. */
    private fun keywords(text: String, max: Int = 4): List<Pair<Int, String>> {
        val words = text.replace('\n', ' ').split(' ').filter { it.isNotBlank() }
        val scored = words.mapIndexedNotNull { i, raw ->
            val w = raw.trim(',', '.', ':', ';', '!', '?', '(', ')', '«', '»', '"', '`')
            val core = w.substringAfter('\'')
            if (core.length < 3 || core.lowercase() in stop) return@mapIndexedNotNull null
            val acr = core.length in 2..6 && core.all { it.isUpperCase() || it.isDigit() }
            val score = (if (acr) 60 else 0) + (if (DomainDetector.isKeyword(core, domain)) 50 else 0) + core.length.coerceAtMost(14) * 3 - (if (core.length < 5 && !acr) 40 else 0)
            Triple(i, w, score)
        }
        return scored.sortedByDescending { it.third }.distinctBy { it.second.lowercase() }.take(max).sortedBy { it.first }.map { it.first to it.second }
    }

    private val shotsCache = HashMap<String, List<Shot>>()

    /** Numéro de chapitre de chaque scène (les étapes sont numérotées une par une, en continu). */
    private val firstChapter: Map<String, Int> by lazy {
        var n = 1
        sb.scenes.associate { s ->
            val start = n
            n += when (s.layout) { Layout.STEPS -> s.items.size; Layout.HERO, Layout.CHECKLIST, Layout.SCREEN -> 0; else -> 1 }
            s.id to start
        }
    }

    private fun shots(sc: Scene): List<Shot> = shotsCache.getOrPut(sc.id) {
        val r = Rand.of(sb.seed, sc.id.hashCode())
        val n0 = firstChapter[sc.id] ?: 1
        val out = ArrayList<Shot>()
        when (sc.layout) {
            Layout.HERO -> {
                val icon = iconForText(plain(sc.title + " " + sc.subtitle), sc.iconOrDefault())
                out += Shot(Kind.HOOK, plain(sc.title).trimEnd('.', '!', ' ') + ".", kicker = plain(sc.kicker).uppercase(), title = plain(sc.title), badge = null,
                    icon = icon, sticker = domain.label.split(' ', ':').first().uppercase())
                val sub = plain(sc.subtitle)
                if (sub.isNotEmpty()) for (c in splitCaption(sub)) out += Shot(Kind.KEYWORDS, c, kicker = "L'IDÉE", title = short(c, 5), badge = null, icon = icon)
            }
            Layout.DEFINITION -> {
                val body = plain(sc.body.ifEmpty { sc.subtitle })
                for ((k, c) in splitCaption(body).withIndex()) {
                    val kind = if ((k + (r.nextInt() ushr 1)) % 2 == 0) Kind.KEYWORDS else Kind.DOC
                    out += Shot(kind, c, kicker = plain(sc.kicker).ifEmpty { "LE PRINCIPE" }.uppercase(), title = short(plain(sc.title)), badge = "$n0",
                        main = c, icon = iconForText(c, sc.iconOrDefault()))
                }
            }
            Layout.STEPS -> {
                val labels = sc.items.map { splitLabel(plain(it)).first }
                val kick = (plain(sc.title).split(Regex(" \\(")).first().takeIf { it.length in 3..28 } ?: if (domain == Domain.CYBER) "Défense" else "Étape").uppercase()
                for ((i, it) in sc.items.withIndex()) {
                    val (label, detail) = splitLabel(plain(it))
                    val cmd = if (isCommand(label)) label else commandIn(plain(it))
                    val kind = if (cmd != null) Kind.TERMINAL else listOf(Kind.PROMPT, Kind.PHONE, Kind.OBJECT)[((r.nextInt() ushr 1) + i) % 3]
                    // Une commande se montre en entier dans le terminal : pas de découpage.
                    val parts = if (cmd != null) listOf(plain(it)) else splitCaption(plain(it))
                    for ((k, c) in parts.withIndex()) {
                        val kd = if (k == 0) kind else Kind.KEYWORDS
                        out += Shot(kd, c, kicker = kick, title = short(label), badge = "${n0 + i}", items = labels.map { l -> short(l, 6) },
                            icon = iconForText(plain(it), sc.iconOrDefault()), main = if (kd == Kind.TERMINAL) cmd!! else c,
                            detail = if (cmd == label) detail else null, index = i, sticker = keyOf(short(label))?.uppercase())
                    }
                }
            }
            Layout.STAT -> out += Shot(Kind.NUMBER, plain(sc.title).trimEnd(':', ' ', '.') + "." + (if (sc.body.isNotEmpty()) " " + plain(sc.body) else ""),
                kicker = "LE CHIFFRE", title = short(plain(sc.title), 4), badge = "$n0", main = sc.statValue ?: "")
            Layout.COMPARE -> sc.compare?.let { c ->
                val both = listOf(c.badText(), c.goodText())
                out += Shot(Kind.VERSUS, "${plain(c.badLabel)} : ${c.badText().trimEnd('.', ' ')}.", kicker = "À ÉVITER", title = short(c.badText(), 5), badge = "$n0",
                    main = c.badText(), good = false, items = both)
                out += Shot(Kind.VERSUS, "${plain(c.goodLabel)} : ${c.goodText().trimEnd('.', ' ')}.", kicker = "À FAIRE", title = short(c.goodText(), 5), badge = "$n0",
                    main = c.goodText(), good = true, items = both)
            }
            Layout.CHECKLIST -> {
                val items = sc.items.map(::plain)
                val said = (items.joinToString(". ") + (if (items.isNotEmpty()) ". " else "") + plain(sc.subtitle)).trim()
                val cap = said.ifEmpty { plain(sc.body) }.ifEmpty { "Abonne-toi pour la suite." }
                out += Shot(Kind.END, if (cap.endsWith(".") || cap.endsWith("!")) cap else "$cap.",
                    kicker = if (items.isEmpty()) "LA SUITE" else "À RETENIR", title = if (items.isEmpty()) "Abonne-toi" else short(plain(sc.title), 4), badge = null,
                    items = items.map { short(it, 6) })
            }
            Layout.SCREEN -> {}
        }
        out
    }

    private fun shotAt(sc: Scene, t: Float): Triple<Int, Float, Float> {
        val ss = shots(sc)
        val total = ss.sumOf { it.weight.toDouble() }.toFloat()
        var acc = 0f
        for ((i, s) in ss.withIndex()) {
            val len = sc.durationMs * s.weight / total
            if (t < acc + len || i == ss.lastIndex) return Triple(i, (t - acc).coerceAtLeast(0f), len)
            acc += len
        }
        return Triple(0, t, sc.durationMs.toFloat())
    }

    // ───────────────────────────── Plan ─────────────────────────────

    private companion object {
        const val CARD_L = 60f; const val CARD_R = 1020f
        const val CARD_T = 450f; const val CARD_B = 1150f
        const val CAP_Y = 1262f
        const val FLOOR = 1380f
        const val MASCOT_Y = 1462f
    }

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        background(g, tGlobalMs)
        val ss = shots(sc)
        if (ss.isEmpty()) return
        val (i, t, len) = shotAt(sc, localMs)
        val s = ss[i]
        val key = "${sc.id}/$i"
        val (wi, speaking) = wordAt(s, t, len)
        mascot(g, tGlobalMs, speaking, t)
        val sameHeader = i > 0 && ss[i - 1].badge == s.badge && ss[i - 1].title == s.title && ss[i - 1].kind != Kind.HOOK
        if (s.kind == Kind.HOOK) hookHeader(g, s, wi, key) else header(g, s, if (sameHeader) 2000f else t, key)
        // Entrée de la carte : elle monte avec un léger rebond, puis la caméra pousse doucement.
        val e = Easing.BACK_OUT.ease(clamp01(t / 420f))
        val push = 1f + 0.02f * Easing.EASE_IN_OUT.ease(clamp01(t / len))
        val top = if (s.kind == Kind.HOOK) 720f else CARD_T
        g.withPose(Pose(alpha = clamp01(t / 160f), dy = (1 - e) * 90f, scale = lerp(0.9f, 1f, e) * push), 540f, (top + CARD_B) / 2) {
            cardFrame(g, top)
            val area = R(CARD_L + 40f, top + 40f, CARD_R - 40f, CARD_B - 40f)
            when (s.kind) {
                Kind.HOOK -> hookCard(g, s, t, area)
                Kind.KEYWORDS -> keywordsCard(g, s, t, wi, area)
                Kind.DOC -> doc(g, s, t, len, area, key)
                Kind.PROMPT -> prompt(g, s, t, len, area)
                Kind.TERMINAL -> terminal(g, s, t, len, area)
                Kind.PHONE -> phone(g, s, t, area)
                Kind.OBJECT -> objectCard(g, s, t, area)
                Kind.NUMBER -> number(g, s, t, area)
                Kind.VERSUS -> versus(g, s, t, area)
                Kind.END -> endCard(g, s, t, area)
            }
            sweep(g, top, t)
        }
        captions(g, s, wi, key, t)
    }

    data class R(val l: Float, val t: Float, val r: Float, val b: Float) {
        val cx get() = (l + r) / 2
        val cy get() = (t + b) / 2
        val w get() = r - l
        val h get() = b - t
    }

    // ───────────────────────────── Décor ─────────────────────────────

    private fun background(g: Gfx, tg: Float) {
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, rgb(0x1C1C1E), rgb(0x0B0B0C))
        for (x in listOf(90f, 990f)) { val y = 168f; g.fillRect(x - 14f, y - 1.5f, x + 14f, y + 1.5f, 0x40FFFFFF); g.fillRect(x - 1.5f, y - 14f, x + 1.5f, y + 14f, 0x40FFFFFF) }
        val r = Rand.of(sb.seed, 0x77D1)
        for (k in 0 until 26) {
            val x = r.next() * 1080f; val y = 200f + r.next() * 1600f; val ph = r.next() * 6.28f
            g.fillCircle(x, y + 10f * sin(tg * 0.0007f + ph), 2.6f, rgb(0xFFFFFF).withAlpha(0.12f + 0.12f * sin(tg * 0.002f + ph)))
        }
        // sol en grille, en perspective (point de fuite au-dessus de l'horizon)
        g.save(); g.clipRect(0f, FLOOR, 1080f, 1920f)
        g.fillRect(0f, FLOOR, 1080f, 1920f, rgb(0x0A0A0B))
        g.fillRect(0f, FLOOR, 1080f, FLOOR + 2f, 0x30FFFFFF)
        for (k in -9..9) g.line(540f + k * 70f, FLOOR, 540f + k * 330f, 1920f, 1.6f, 0x22FFFFFF)
        var d = 0f
        while (true) {
            val y = FLOOR + 16f * (1 + d) * (1 + d)
            if (y > 1920f) break
            g.fillRect(0f, y, 1080f, y + 1.6f, 0x20FFFFFF); d += 1f
        }
        g.restore()
        g.fillRadialGlow(540f, FLOOR + 40f, 520f, 0x22FFFFFF)
    }

    /** Mascotte CODJIA (originale) : une petite boule à antenne, qui sautille et parle avec les sous-titres. */
    private fun mascot(g: Gfx, tg: Float, speaking: Boolean, tShot: Float) {
        val jump = if (tShot < 380f) sin(tShot / 380f * 3.1416f) * 34f else 0f
        val bounce = abs(sin(tg * 0.0045f)) * 14f + jump
        val cx = 540f; val cy = MASCOT_Y - bounce
        g.fillRadialGlow(cx, MASCOT_Y + 70f, 150f, 0x66000000)
        g.fillRadialGlow(cx, cy, 170f, accent.withAlpha(0.35f))
        g.line(cx + 6f, cy - 58f, cx + 22f, cy - 96f, 6f, darken(accent, 0.2f))
        g.fillCircle(cx + 24f, cy - 100f, 12f, accent2)
        g.fillCircle(cx, cy, 66f, darken(accent, 0.25f))
        g.fillCircle(cx - 4f, cy - 6f, 60f, accent)
        g.fillRadialGlow(cx - 22f, cy - 30f, 50f, 0x88FFFFFF.toInt())
        val blink = ((tg / 2900f) % 1f) > 0.96f
        for (dx in listOf(-20f, 20f)) {
            if (blink) g.fillRoundRect(cx + dx - 8f, cy - 10f, cx + dx + 8f, cy - 6f, 2f, rgb(0x1A1A1A))
            else { g.fillCircle(cx + dx, cy - 8f, 9f, rgb(0x1A1A1A)); g.fillCircle(cx + dx + 3f, cy - 11f, 3f, rgb(0xFFFFFF)) }
        }
        g.fillCircle(cx - 38f, cy + 12f, 9f, rgb(0xFF5C8A).withAlpha(0.5f))
        g.fillCircle(cx + 38f, cy + 12f, 9f, rgb(0xFF5C8A).withAlpha(0.5f))
        val open = if (speaking) 3f + 7f * abs(sin(tg * 0.025f)) else 2.5f
        g.fillRoundRect(cx - 10f, cy + 14f, cx + 10f, cy + 16f + open, 6f, rgb(0x3A1010))
    }

    /** Reflet lumineux qui traverse la carte à son arrivée (effet « verre »). */
    private fun sweep(g: Gfx, top: Float, t: Float) {
        val p = clamp01((t - 120f) / 650f)
        if (p <= 0f || p >= 1f) return
        g.save()
        g.clipPath(roundPath(CARD_L, top, CARD_R, CARD_B, 30f))
        val x = lerp(CARD_L - 400f, CARD_R + 400f, Easing.EASE_IN_OUT.ease(p))
        g.rotate(18f, x, (top + CARD_B) / 2)
        g.fillRect(x - 70f, top - 400f, x + 70f, CARD_B + 400f, 0x14FFFFFF)
        g.fillRect(x - 18f, top - 400f, x + 18f, CARD_B + 400f, 0x18FFFFFF)
        g.restore()
    }

    /** Les mots clés de la phrase apparaissent autour de l'objet au moment exact où ils sont dits. */
    private fun keywordsCard(g: Gfx, s: Shot, t: Float, wi: Int, a: R) {
        object3d(g, s.icon, a.cx, a.cy, 210f, t)
        val kws = keywords(s.caption)
        val spots = listOf(a.l + 170f to a.t + 90f, a.r - 170f to a.t + 150f, a.l + 190f to a.b - 150f, a.r - 180f to a.b - 90f)
        val f = FontSpec(FontFamily.TEXT, 800, 40f)
        for ((k, kw) in kws.withIndex()) {
            val (idx, word) = kw
            if (wi < idx) continue
            val since = clamp01(((wi - idx) * 300f + 200f) / 300f)
            val kk = Easing.BACK_OUT.ease(since)
            val (x0, y) = spots[k % spots.size]
            val lab = TextLayout.fit(g, word, f, 360f, 1, minSize = 24f)
            val text = lab.lines.first()
            val w = g.measureText(text, lab.font) + 44f
            val x = x0.coerceIn(a.l + w / 2, a.r - w / 2)
            // fil vers l'objet
            g.line(a.cx, a.cy, x, y, 2f, accent.withAlpha(0.35f * kk))
            val isNow = wi == idx
            g.withPose(Pose(scale = lerp(0.3f, 1f, kk) * (if (isNow) 1.06f else 1f), alpha = clamp01(kk * 2)), x, y) {
                g.fillRoundRect(x - w / 2, y - 36f, x + w / 2, y + 36f, 36f, if (isNow) rgb(0xFFFFFF) else rgb(0x2C2C30))
                g.strokeRoundRect(x - w / 2, y - 36f, x + w / 2, y + 36f, 36f, 2f, if (isNow) rgb(0xFFFFFF) else accent.withAlpha(0.7f))
                g.drawText(text, x - w / 2 + 22f, y + 14f, lab.font, if (isNow) dark else ink)
            }
        }
    }

    private fun cardFrame(g: Gfx, top: Float) {
        g.fillRoundRect(CARD_L - 18f, top - 18f, CARD_R + 18f, CARD_B + 18f, 40f, rgb(0x141415))
        g.strokeRoundRect(CARD_L - 18f, top - 18f, CARD_R + 18f, CARD_B + 18f, 40f, 2f, 0x1CFFFFFF)
        g.fillRoundRect(CARD_L, top, CARD_R, CARD_B, 30f, card)
        g.strokeRoundRect(CARD_L, top, CARD_R, CARD_B, 30f, 2f, 0x26FFFFFF)
    }

    // ───────────────────────────── En-têtes ─────────────────────────────

    /** Badge numéroté en relief + étiquette espacée + titre dont le mot clé est surligné. */
    private fun header(g: Gfx, s: Shot, t: Float, key: String) {
        val a = Easing.EXPO_OUT.ease(clamp01(t / 300f))
        var x = Safe.LEFT
        val by = 255f
        s.badge?.let { b ->
            val k = Easing.BACK_OUT.ease(clamp01(t / 420f))
            g.withPose(Pose(scale = lerp(0.4f, 1f, k), alpha = clamp01(k * 2)), x + 52f, by + 52f) {
                g.fillRoundRect(x + 4f, by + 10f, x + 108f, by + 112f, 26f, 0x66000000)
                g.fillRoundRect(x, by, x + 104f, by + 104f, 26f, rgb(0xFFFFFF))
                val nf = fitSize(g, b, FontSpec(FontFamily.DISPLAY, 900, 60f), 80f)
                g.drawText(b, x + 52f - g.measureText(b, nf) / 2, by + 52f + g.metrics(nf).ascent / 2.4f, nf, dark)
            }
            x += 132f
        }
        val kf = FontSpec(FontFamily.MONO, 700, 26f)
        val maxW = Safe.RIGHT - x
        val kick = TextLayout.fit(g, s.kicker, kf, maxW - 60f, 1, minSize = 18f)
        g.withPose(Pose(alpha = a), x, by) { g.drawTracked(kick.lines.firstOrNull() ?: "", x, by + 26f, kick.font, muted, 5f) }
        // Titre sur une ligne si possible, sinon deux lignes en assez grand (jamais minuscule).
        val one = TextLayout.fit(g, s.title, FontSpec(FontFamily.TEXT, 800, 66f), maxW - 40f, 1, minSize = 50f)
        val fits = one.lines.size == 1 && !one.lines[0].endsWith("…") && g.measureText(one.lines[0], one.font) <= maxW - 40f
        val tl = if (fits) one else TextLayout.fit(g, s.title, FontSpec(FontFamily.TEXT, 800, 50f), maxW - 40f, 2, lineHeightFactor = 1.08f, minSize = 34f)
        val kw = keyOf(s.title)
        var hit = false
        for ((li, line) in tl.lines.withIndex()) {
            val k = if (!hit && kw != null && line.split(' ').any { it.trim(',', '.', ':', '!', '?') == kw }) { hit = true; kw } else null
            highlightLine(g, line, k, x, by + 94f + li * tl.font.size * 1.12f, tl.font, a)
        }
    }

    /** Une ligne de titre, avec le mot clé dans un cadre blanc (texte noir). */
    private fun highlightLine(g: Gfx, line: String, kw: String?, x0: Float, base: Float, f: FontSpec, a: Float) {
        var x = x0
        val words = line.split(' ')
        var hit = false
        g.withPose(Pose(alpha = a, dx = (1 - a) * 24f), x0, base) {
            for ((wi, w) in words.withIndex()) {
                val isKey = !hit && kw != null && w.trim(',', '.', ':', '!', '?') == kw
                val ww = g.measureText(w, f)
                if (isKey) {
                    hit = true
                    g.fillRoundRect(x - 10f, base - f.size * 0.86f, x + ww + 10f, base + f.size * 0.22f, 10f, rgb(0xFFFFFF))
                    g.drawText(w, x, base, f, dark)
                } else g.drawText(w, x, base, f, ink)
                if (wi < words.lastIndex) x += ww + g.measureText(" ", f) + if (isKey) 12f else 0f
            }
        }
    }

    /** Accroche : le titre en grand, aligné à gauche, mot à mot ; le mot clé surligné. */
    private fun hookHeader(g: Gfx, s: Shot, wi: Int, key: String) {
        val bl = cache.fit(g, "hk/$key", s.title, FontSpec(FontFamily.TEXT, 800, 84f), Safe.WIDTH - 40f, 4, 1.1f)
        val kw = keyOf(s.title)
        val titleWords = bl.lines.sumOf { l -> l.split(' ').count { it.isNotBlank() } }
        val shown = (wi + 1).coerceIn(0, titleWords)
        var count = 0
        val top = 230f
        for ((li, line) in bl.lines.withIndex()) {
            val base = g.baselineOf(bl, top, li)
            var x = Safe.LEFT
            for (w in line.split(' ')) {
                if (w.isBlank()) continue
                count++
                val ww = g.measureText(w, bl.font)
                val vis = count <= shown || wi >= titleWords
                val isKey = kw != null && w.trim(',', '.', ':', '!', '?') == kw
                if (isKey && vis) {
                    g.fillRoundRect(x - 10f, base - bl.font.size * 0.86f, x + ww + 10f, base + bl.font.size * 0.22f, 12f, rgb(0xFFFFFF))
                    g.drawText(w, x, base, bl.font, dark)
                } else g.drawText(w, x, base, bl.font, if (vis) ink else rgb(0x3A3A3E))
                x += ww + g.measureText(" ", bl.font) + if (isKey) 12f else 0f
            }
        }
        if (s.kicker.isNotEmpty()) {
            val kf = FontSpec(FontFamily.MONO, 700, 26f)
            val k = TextLayout.fit(g, s.kicker, kf, Safe.WIDTH - 60f, 1, minSize = 18f)
            g.drawTracked(k.lines.first(), Safe.LEFT, top + bl.height + 50f, k.font, muted, 4f)
        }
    }

    // ───────────────────────────── Sous-titres ─────────────────────────────

    /** Index du mot prononcé et « la mascotte parle-t-elle ? ». */
    private fun wordAt(s: Shot, t: Float, len: Float): Pair<Int, Boolean> {
        if (s.words.isEmpty()) return -1 to false
        val start = 180f; val end = (len - 250f).coerceAtLeast(start + 300f)
        if (t < start) { wordFrac = 0f; return -1 to false }
        val wts = s.words.map { it.length + 3f }
        val p = ((t - start) / (end - start)).coerceIn(0f, 1f) * wts.sum()
        var acc = 0f
        for ((i, w) in wts.withIndex()) { if (p < acc + w) { wordFrac = (p - acc) / w; return i to true }; acc += w }
        wordFrac = 1f
        return s.words.lastIndex to (t < end)
    }

    /** Avancement dans le mot prononcé (0 au début du mot), pour le petit « pop » du surlignage. */
    private var wordFrac = 0f

    /** Une ou deux lignes de sous-titres ; le mot prononcé est dans un cadre blanc, les suivants en gris. */
    private fun captions(g: Gfx, s: Shot, wi: Int, key: String, t: Float) {
        if (s.words.isEmpty() || wi < 0) return
        val f = FontSpec(FontFamily.TEXT, 800, 56f)
        val maxW = Safe.WIDTH - 60f
        val pages = pageCache.getOrPut(key) {
            val out = ArrayList<IntRange>()
            var start = 0
            while (start < s.words.size) {
                var lines = 1; var w = 0f; var end = start
                while (end < s.words.size) {
                    val ww = g.measureText(s.words[end] + " ", f) + 16f
                    if (w + ww > maxW) { if (lines == 2 || end == start) break; lines++; w = 0f }
                    w += ww; end++
                    if (end - start >= 3 && Regex("[.!?:;]$").containsMatchIn(s.words[end - 1])) break
                    if (end - start >= 7) break
                }
                if (end == start) end = start + 1
                out += start until end; start = end
            }
            out
        }
        val page = pages.firstOrNull { wi in it } ?: pages.last()
        val lines = ArrayList<List<Int>>(); var cur = ArrayList<Int>(); var w = 0f
        for (k in page) {
            val ww = g.measureText(s.words[k] + " ", f) + 16f
            if (cur.isNotEmpty() && w + ww > maxW) { lines += cur; cur = ArrayList(); w = 0f }
            cur += k; w += ww
        }
        if (cur.isNotEmpty()) lines += cur
        val lh = 78f
        var y = CAP_Y - (lines.size - 1) * lh / 2 + 20f
        val sp = g.measureText(" ", f)
        for (ln in lines) {
            val total = ln.sumOf { g.measureText(s.words[it], f).toDouble() }.toFloat() + sp * (ln.size - 1) + 16f
            var x = 540f - total / 2
            for (k in ln) {
                val word = s.words[k]
                val ww = g.measureText(word, f)
                if (k == wi) {
                    val pop = 1f + 0.1f * (1f - Easing.EXPO_OUT.ease(clamp01(wordFrac * 3f)))
                    g.withPose(Pose(scale = pop), x + ww / 2, y - 17f) {
                        g.fillRoundRect(x - 8f, y - 48f, x + ww + 8f, y + 14f, 10f, rgb(0xFFFFFF))
                        g.drawText(word, x, y, f, dark)
                    }
                    x += 16f
                } else g.drawText(word, x, y, f, if (k < wi) ink else rgb(0x5A5A60))
                x += ww + sp
            }
            y += lh
        }
    }

    // ───────────────────────────── Cartes ─────────────────────────────

    private fun roundPath(l: Float, t: Float, r: Float, b: Float, rad: Float): PathData {
        val k = 0.5522848f * rad
        return PathData().moveTo(l + rad, t).lineTo(r - rad, t).cubicTo(r - rad + k, t, r, t + rad - k, r, t + rad)
            .lineTo(r, b - rad).cubicTo(r, b - rad + k, r - rad + k, b, r - rad, b)
            .lineTo(l + rad, b).cubicTo(l + rad - k, b, l, b - rad + k, l, b - rad)
            .lineTo(l, t + rad).cubicTo(l, t + rad - k, l + rad - k, t, l + rad, t).close()
    }

    private fun darken(c: Int, f: Float): Int {
        val r = ((c shr 16) and 0xFF) * (1 - f); val gg = ((c shr 8) and 0xFF) * (1 - f); val b = (c and 0xFF) * (1 - f)
        return (0xFF shl 24) or (r.toInt() shl 16) or (gg.toInt() shl 8) or b.toInt()
    }

    private fun lighten(c: Int, f: Float): Int {
        fun ch(v: Int) = (v + (255 - v) * f).toInt()
        return (0xFF shl 24) or (ch((c shr 16) and 0xFF) shl 16) or (ch((c shr 8) and 0xFF) shl 8) or ch(c and 0xFF)
    }

    /** Objet « en relief » : tuile épaisse avec dégradé, reflet et ombre portée, qui surgit puis flotte. */
    private fun object3d(g: Gfx, icon: IconId, cx: Float, cy: Float, size: Float, t: Float, color: Int = accent) {
        val k = Easing.BACK_OUT.ease(clamp01(t / 480f))
        if (k <= 0f) return
        val fy = cy + 10f * sin(t * 0.003f)
        val h = size / 2
        g.fillRadialGlow(cx, cy + h + 30f, size * 0.7f, 0x77000000)
        g.fillRadialGlow(cx, fy, size * 1.05f, color.withAlpha(0.3f))
        g.save()
        g.rotate(3f * sin(t * 0.0022f), cx, fy)
        g.withPose(Pose(scale = lerp(0.3f, 1f, k), alpha = clamp01(k * 2)), cx, fy) {
            g.fillRoundRect(cx - h, fy - h + 20f, cx + h, fy + h + 20f, size * 0.24f, darken(color, 0.45f))
            g.save()
            g.clipPath(roundPath(cx - h, fy - h, cx + h, fy + h, size * 0.24f))
            g.fillVerticalGradient(cx - h, fy - h, cx + h, fy + h, lighten(color, 0.25f), darken(color, 0.12f))
            g.fillRadialGlow(cx - h * 0.4f, fy - h * 0.6f, size * 0.7f, 0x55FFFFFF)
            g.restore()
            g.strokeRoundRect(cx - h, fy - h, cx + h, fy + h, size * 0.24f, 3f, 0x44FFFFFF)
            Icons.draw(g, icon, cx, fy, size * 0.5f, rgb(0xFFFFFF), 3.2f)
        }
        g.restore()
    }

    /** Étiquette inclinée (« FAIT », « À ÉVITER »…), qui tombe avec un rebond. */
    private fun sticker(g: Gfx, text: String, x: Float, y: Float, t: Float, color: Int, delay: Float = 500f, deg: Float = 10f) {
        val k = Easing.BACK_OUT.ease(clamp01((t - delay) / 380f))
        if (k <= 0f) return
        val f = fitSize(g, text, FontSpec(FontFamily.TECH, 700, 32f), 300f)
        val w = g.measureText(text, f) + 36f
        val cx = x.coerceIn(Safe.LEFT + w / 2 + 10f, Safe.RIGHT - w / 2 - 10f)
        g.save()
        g.rotate(deg, cx, y)
        g.withPose(Pose(scale = lerp(1.6f, 1f, k), alpha = clamp01(k * 2)), cx, y) {
            g.fillRoundRect(cx - w / 2, y - 28f, cx + w / 2, y + 28f, 10f, darken(color, 0.7f))
            g.strokeRoundRect(cx - w / 2, y - 28f, cx + w / 2, y + 28f, 10f, 3f, color)
            g.drawText(text, cx - w / 2 + 18f, y + 12f, f, lighten(color, 0.3f))
        }
        g.restore()
    }

    private fun hookCard(g: Gfx, s: Shot, t: Float, a: R) {
        object3d(g, s.icon, a.cx, a.cy - 6f, 220f, t)
        val cols = listOf(rgb(0xFF4D7A), ok, rgb(0x4C8DFF), accent2)
        for ((k, c) in cols.withIndex()) {
            val ang = k * 1.57f + 0.6f
            val x = a.cx + cos(ang) * 300f; val y = a.cy + sin(ang) * 110f + 8f * sin(t * 0.003f + k)
            val kk = Easing.BACK_OUT.ease(clamp01((t - 300f - k * 120f) / 300f))
            if (kk > 0f) g.withPose(Pose(scale = kk), x, y) { g.fillRoundRect(x - 22f, y - 30f, x + 22f, y + 30f, 10f, rgb(0x0E0E10)); g.fillRoundRect(x - 13f, y - 13f, x + 13f, y + 13f, 6f, c) }
        }
        s.sticker?.let { sticker(g, it, a.r - 120f, a.t + 40f, t, ok) }
    }

    /** Page blanche penchée, comme un guide, où l'explication s'écrit. */
    private fun doc(g: Gfx, s: Shot, t: Float, len: Float, a: R, key: String) {
        val pl = a.l + 30f; val pr = a.r - 30f; val pt = a.t + 10f; val pb = a.b - 10f
        g.save(); g.rotate(-1.5f, a.cx, a.cy)
        g.fillRoundRect(pl + 10f, pt + 14f, pr + 10f, pb + 14f, 18f, 0x66000000)
        g.fillRoundRect(pl, pt, pr, pb, 18f, rgb(0xF4F4F2))
        val head = TextLayout.fit(g, s.title.uppercase(), FontSpec(FontFamily.HEAVY, 400, 44f), pr - pl - 80f, 1, minSize = 24f)
        g.drawText(head.lines.first(), pl + 40f, pt + 84f, head.font, dark)
        g.fillRect(pl + 40f, pt + 108f, pl + 160f, pt + 114f, accent)
        val bl = cache.fit(g, "d/$key", s.main, FontSpec(FontFamily.TEXT, 600, 36f), pr - pl - 80f, 9, 1.32f)
        val p = clamp01((t - 300f) / (len * 0.7f).coerceAtLeast(800f))
        var chars = (bl.lines.sumOf { it.length } * p).toInt()
        for ((i, line) in bl.lines.withIndex()) {
            val base = g.baselineOf(bl, pt + 146f, i)
            if (base > pb - 24f || chars <= 0) break
            g.drawText(line.take(chars), pl + 40f, base, bl.font, rgb(0x26262A))
            chars -= line.length
        }
        g.restore()
    }

    /** Fenêtre d'assistant : onglets, champ où l'étape se tape, puis la suite proposée. */
    private fun prompt(g: Gfx, s: Shot, t: Float, len: Float, a: R) {
        val tabs = listOf("Texte", "Étapes", "Action")
        val tf = FontSpec(FontFamily.TEXT, 700, 30f)
        val tw = tabs.map { g.measureText(it, tf) + 48f }
        var x = a.cx - tw.sum() / 2
        g.fillRoundRect(x - 8f, a.t + 6f, x + tw.sum() + 8f, a.t + 70f, 32f, rgb(0x2E2E31))
        for ((k, tab) in tabs.withIndex()) {
            if (k == 2) g.fillRoundRect(x, a.t + 12f, x + tw[k], a.t + 64f, 26f, rgb(0x4A4A4E))
            g.drawText(tab, x + 24f, a.t + 49f, tf, if (k == 2) ink else muted)
            x += tw[k]
        }
        val bt = a.t + 110f; val bb = a.t + 420f
        val focus = clamp01((t - 200f) / 300f)
        g.fillRoundRect(a.l, bt, a.r, bb, 22f, inner)
        g.strokeRoundRect(a.l, bt, a.r, bb, 22f, 3f, accent.withAlpha(0.15f + 0.6f * focus))
        val full = TextLayout.fit(g, s.main, FontSpec(FontFamily.TEXT, 500, 38f), a.w - 140f, 5, lineHeightFactor = 1.3f, minSize = 24f)
        val typeMs = (len * 0.55f).coerceAtLeast(700f)
        val p = clamp01((t - 450f) / typeMs)
        var chars = (full.lines.sumOf { it.length } * p).toInt()
        var cx = a.l + 36f; var cyb = g.baselineOf(full, bt + 30f, 0)
        for ((i, line) in full.lines.withIndex()) {
            if (chars <= 0) break
            val base = g.baselineOf(full, bt + 30f, i)
            val part = line.take(chars)
            g.drawText(part, a.l + 36f, base, full.font, ink)
            cx = a.l + 36f + g.measureText(part, full.font); cyb = base
            chars -= line.length
        }
        if (p < 1f || (t / 420f).toInt() % 2 == 0) g.fillRect(cx + 4f, cyb - full.font.size * 0.85f, cx + 7f, cyb + 8f, accent)
        g.fillCircle(a.r - 50f, bb - 46f, 26f, if (p >= 1f) accent else accent.withAlpha(0.25f))
        Icons.draw(g, IconId.CHECK, a.r - 50f, bb - 46f, 26f, rgb(0xFFFFFF), 3f)
        if (p >= 1f && s.index + 1 < s.items.size) {
            val kk = Easing.BACK_OUT.ease(clamp01((t - 450f - typeMs - 150f) / 300f))
            val lab = TextLayout.fit(g, "Ensuite : " + s.items[s.index + 1], FontSpec(FontFamily.TEXT, 700, 30f), a.w - 80f, 1, minSize = 20f)
            val w = g.measureText(lab.lines.first(), lab.font) + 44f
            val y = bb + 70f
            if (kk > 0f) g.withPose(Pose(scale = kk, alpha = clamp01(kk)), a.cx, y + 28f) {
                g.fillRoundRect(a.cx - w / 2, y, a.cx + w / 2, y + 58f, 29f, rgb(0xFFFFFF))
                g.drawText(lab.lines.first(), a.cx - w / 2 + 22f, y + 40f, lab.font, dark)
            }
        }
    }

    private fun terminal(g: Gfx, s: Shot, t: Float, len: Float, a: R) {
        g.fillRoundRect(a.l, a.t, a.r, a.b, 20f, rgb(0x101012))
        listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(a.l + 34f + i * 26f, a.t + 34f, 8f, c) }
        val cmd = TextLayout.fit(g, s.main, FontSpec(FontFamily.MONO, 700, 36f), a.w - 110f, 3, lineHeightFactor = 1.3f, minSize = 22f)
        val all = cmd.lines.sumOf { it.length }
        val typeMs = (all * 45f).coerceIn(500f, len * 0.5f)
        val p = clamp01((t - 400f) / typeMs)
        var chars = (all * p).toInt()
        var y = a.t + 120f
        for ((i, line) in cmd.lines.withIndex()) {
            g.drawText((if (i == 0) "$ " else "  ") + line.take(chars.coerceAtLeast(0)), a.l + 36f, y, cmd.font, ok)
            chars -= line.length; y += cmd.font.size * 1.3f
        }
        // Sortie typique de la commande (exemples génériques, adresses IP réservées à la documentation).
        val lines = (sampleOutput(s.main) + listOfNotNull(s.detail?.let { "# $it" })).take(6)
        val of = FontSpec(FontFamily.MONO, 400, 28f)
        var oy = y + 16f
        for ((k, l) in lines.withIndex()) {
            val oa = clamp01((t - 400f - typeMs - 250f - k * 220f) / 200f)
            if (oa <= 0f || oy > a.b - 90f) break
            val ob = TextLayout.fit(g, l, of, a.w - 80f, 1, minSize = 18f)
            val col = when { l.startsWith("#") -> muted; Regex("Failed|refus|denied|erreur", RegexOption.IGNORE_CASE).containsMatchIn(l) -> rgb(0xFF9AA5); else -> rgb(0xD6D6DA) }
            g.withPose(Pose(alpha = oa), a.l, oy) { g.drawText(ob.lines.first(), a.l + 36f, oy + 24f, ob.font, col) }
            oy += 42f
        }
        sticker(g, "FAIT", a.r - 90f, a.b - 60f, t, ok, 400f + typeMs + 500f, -8f)
    }

    private fun sampleOutput(cmd: String): List<String> {
        val c = cmd.lowercase()
        return when {
            c.contains("apt install") -> listOf("Lecture des listes de paquets… Fait", "Les NOUVEAUX paquets suivants seront installés :", "  " + c.substringAfter("install").trim().split(' ').first(), "Paramétrage en cours… Fait")
            c.startsWith("grep") && c.contains("failed") -> listOf("Failed password for root from 203.0.113.7", "Failed password for admin from 203.0.113.7", "Failed password for root from 198.51.100.23")
            c.startsWith("ufw") -> listOf("Règles mises à jour", "État : actif")
            c.startsWith("systemctl") -> listOf("● service actif (running)")
            c.startsWith("git status") -> listOf("Sur la branche main", "rien à valider, la copie de travail est propre")
            c.startsWith("df") -> listOf("Sys. fichiers  Taille Utilisé Dispo Uti%", "/dev/sda1        50G     21G   27G  44%")
            c.startsWith("free") -> listOf("        total  utilisé  libre", "Mem:    7,7Gi   3,1Gi   2,9Gi")
            c.startsWith("show vlan") -> listOf("VLAN Name        Status", "10   COMPTA      active")
            else -> emptyList()
        }
    }

    /** Téléphone qui coche les étapes une à une ; l'étape courante est mise en avant. */
    private fun phone(g: Gfx, s: Shot, t: Float, a: R) {
        val pw = 400f; val ph = a.h - 10f
        val px = a.r - pw - 10f; val py = a.t + 5f
        val k = Easing.EXPO_OUT.ease(clamp01(t / 500f))
        g.withPose(Pose(dy = (1 - k) * 120f, alpha = k), px + pw / 2, py + ph / 2) {
            g.fillRoundRect(px - 10f, py - 10f, px + pw + 10f, py + ph + 10f, 56f, rgb(0x0B0B0C))
            g.fillRoundRect(px, py, px + pw, py + ph, 48f, rgb(0xF7F7F5))
            g.fillRoundRect(px + pw / 2 - 60f, py + 16f, px + pw / 2 + 60f, py + 46f, 15f, rgb(0x0B0B0C))
            g.drawText("Étapes", px + 30f, py + 100f, FontSpec(FontFamily.TEXT, 800, 32f), dark)
            val done = s.index + if (t > 900f) 1 else 0
            g.drawText("$done sur ${s.items.size} faites", px + 30f, py + 136f, FontSpec(FontFamily.TEXT, 500, 24f), rgb(0x777777))
            var y = py + 172f
            val rowH = ((ph - 200f) / s.items.size.coerceAtLeast(1)).coerceAtMost(100f)
            val rf = FontSpec(FontFamily.TEXT, 600, 25f)
            for ((i, it) in s.items.withIndex()) {
                val cur = i == s.index
                val checked = i < s.index || (cur && t > 900f)
                if (cur) g.fillRoundRect(px + 16f, y - 4f, px + pw - 16f, y + rowH - 10f, 16f, accent.withAlpha(0.15f))
                val ccx = px + 46f; val ccy = y + rowH / 2 - 7f
                if (checked) { g.fillCircle(ccx, ccy, 15f, if (cur) accent else ok); Icons.draw(g, IconId.CHECK, ccx, ccy, 16f, rgb(0xFFFFFF), 3f) }
                else g.strokeCircle(ccx, ccy, 15f, 2.5f, rgb(0xBBBBBB))
                val lb = TextLayout.fit(g, it, rf, pw - 110f, 2, lineHeightFactor = 1.1f, minSize = 16f)
                g.drawBlock(lb, px + 76f, ccy - lb.height / 2, if (cur) dark else rgb(0x666666))
                y += rowH
            }
        }
        val lx = a.l + (px - a.l) / 2
        object3d(g, s.icon, lx, a.cy - 60f, 200f, t)
        s.sticker?.let { sticker(g, it, lx, a.cy + 150f, t, accent, 1000f, -6f) }
    }

    private fun objectCard(g: Gfx, s: Shot, t: Float, a: R) {
        object3d(g, s.icon, a.cx, a.cy - 30f, 280f, t)
        s.sticker?.let { sticker(g, it, a.cx + 200f, a.t + 70f, t, ok) }
        val n = s.items.size.coerceAtLeast(1)
        val bw = a.w - 160f
        val y = a.b - 40f
        val pr = (s.index + clamp01((t - 400f) / 700f)) / n
        g.fillRoundRect(a.cx - bw / 2, y, a.cx + bw / 2, y + 12f, 6f, 0x26FFFFFF)
        g.fillRoundRect(a.cx - bw / 2, y, a.cx - bw / 2 + bw * pr, y + 12f, 6f, accent)
    }

    private fun number(g: Gfx, s: Shot, t: Float, a: R) {
        val (num, unit) = statParts(s.main)
        val digits = Regex("\\d+").find(num)?.value?.toIntOrNull()
        val p = Easing.EXPO_OUT.ease(clamp01(t / 1200f))
        val shown = if (digits != null && digits < 100_000) num.replaceFirst(digits.toString(), (digits * p).toInt().toString()) else num
        val f = fitSize(g, num, FontSpec(FontFamily.DISPLAY, 900, 260f), a.w - 60f)
        g.fillRadialGlow(a.cx, a.cy, 360f, accent.withAlpha(0.25f))
        g.drawText(shown, a.cx - g.measureText(shown, f) / 2, a.cy + 40f, f, ink)
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit, FontSpec(FontFamily.TEXT, 800, 60f), a.w - 80f)
            val w = g.measureText(unit, uf)
            g.fillRoundRect(a.cx - w / 2 - 14f, a.cy + 100f, a.cx + w / 2 + 14f, a.cy + 180f, 12f, rgb(0xFFFFFF))
            g.drawText(unit, a.cx - w / 2, a.cy + 162f, uf, dark)
        }
    }

    /** « À éviter » puis « à faire » : la seconde carte arrive au deuxième plan. */
    private fun versus(g: Gfx, s: Shot, t: Float, a: R) {
        val rows = if (s.good) 2 else 1
        val rowH = 230f
        var y = a.cy - (rows * rowH + (rows - 1) * 50f) / 2
        for (k in 0 until rows) {
            val good = k == 1
            val col = if (good) ok else bad
            val kk = if (k == rows - 1) Easing.BACK_OUT.ease(clamp01(t / 420f)) else 1f
            g.withPose(Pose(scale = lerp(0.8f, 1f, kk), alpha = clamp01(kk * 2)), a.cx, y + rowH / 2) {
                g.fillRoundRect(a.l + 10f, y, a.r - 10f, y + rowH, 22f, darken(col, 0.82f))
                g.strokeRoundRect(a.l + 10f, y, a.r - 10f, y + rowH, 22f, 3f, col)
                g.fillCircle(a.l + 90f, y + rowH / 2, 40f, darken(col, 0.6f))
                Icons.draw(g, if (good) IconId.CHECK else IconId.CROSS, a.l + 90f, y + rowH / 2, 40f, col, 3.4f)
                val bl = TextLayout.fit(g, s.items.getOrElse(k) { s.main }, FontSpec(FontFamily.TEXT, 700, 38f), a.w - 220f, 3, lineHeightFactor = 1.2f, minSize = 24f)
                g.drawBlock(bl, a.l + 160f, y + rowH / 2 - bl.height / 2, ink)
            }
            if (k == rows - 1) sticker(g, if (good) "À FAIRE" else "À ÉVITER", a.r - 110f, y - 6f, t, col, 350f, 6f)
            y += rowH + 50f
        }
    }

    /** Fin : un guide en relief, la conclusion de l'auteur s'il en a écrit une, puis « Abonne-toi ». */
    private fun endCard(g: Gfx, s: Shot, t: Float, a: R) {
        val gw = 320f; val gh = 420f
        val gx = if (s.items.isEmpty()) a.cx else a.l + gw / 2 + 10f
        val gy = a.cy - 20f
        val k = Easing.BACK_OUT.ease(clamp01(t / 520f))
        g.save(); g.rotate(-6f + 2f * sin(t * 0.002f), gx, gy)
        g.withPose(Pose(scale = lerp(0.5f, 1f, k), alpha = clamp01(k * 2)), gx, gy) {
            g.fillRoundRect(gx - gw / 2 + 14f, gy - gh / 2 + 18f, gx + gw / 2 + 14f, gy + gh / 2 + 18f, 20f, 0x77000000)
            g.fillRoundRect(gx - gw / 2, gy - gh / 2, gx + gw / 2, gy + gh / 2, 20f, rgb(0xF4F4F2))
            g.drawText("GUIDE", gx - gw / 2 + 36f, gy - gh / 2 + 90f, FontSpec(FontFamily.HEAVY, 400, 52f), dark)
            val sub = astuceNo?.let { "ASTUCE %03d".format(it) } ?: "CODJIA"
            g.drawTracked(sub, gx - gw / 2 + 38f, gy - gh / 2 + 150f, FontSpec(FontFamily.MONO, 700, 22f), rgb(0x555555), 3f)
            for (i in 0 until 5) {
                val ly = gy - gh / 2 + 205f + i * 44f
                Icons.draw(g, IconId.LOCK, gx - gw / 2 + 50f, ly, 22f, rgb(0x333333), 2.4f)
                g.fillRoundRect(gx - gw / 2 + 80f, ly - 7f, gx + gw / 2 - 40f - (i % 2) * 60f, ly + 7f, 7f, rgb(0xD5D5D2))
            }
        }
        g.restore()
        if (s.items.isNotEmpty()) {
            val rx = gx + gw / 2 + 80f
            var y = a.t + 40f
            val rf = FontSpec(FontFamily.TEXT, 700, 30f)
            for ((i, it) in s.items.take(5).withIndex()) {
                val bl = TextLayout.fit(g, it, rf, a.r - rx - 50f, 2, lineHeightFactor = 1.12f, minSize = 20f)
                if (y + bl.height > a.b - 120f) break
                val kk = Easing.BACK_OUT.ease(clamp01((t - 400f - i * 300f) / 300f))
                if (kk > 0f) g.withPose(Pose(alpha = clamp01(kk), dx = (1 - kk) * 30f), rx, y) {
                    g.fillCircle(rx + 16f, y + bl.height / 2, 16f, ok)
                    Icons.draw(g, IconId.CHECK, rx + 16f, y + bl.height / 2, 18f, rgb(0xFFFFFF), 3f)
                    g.drawBlock(bl, rx + 46f, y, ink)
                }
                y += bl.height + 34f
            }
        }
        sticker(g, "ABONNE-TOI", if (s.items.isEmpty()) a.cx + 200f else a.r - 160f, a.b - 50f, t, bad, 700f, -5f)
        astuceNo?.let {
            if (s.items.isNotEmpty()) return@let
            val nf = FontSpec(FontFamily.MONO, 700, 26f)
            val txt = "PROCHAINE : ASTUCE %03d".format(it + 1)
            val al = clamp01((t - 1100f) / 300f)
            if (al > 0f) g.withPose(Pose(alpha = al), a.cx, a.t + 10f) { g.drawTracked(txt, a.cx - g.measureTracked(txt, nf, 3f) / 2, a.t + 20f, nf, accent, 3f) }
        }
    }
}
