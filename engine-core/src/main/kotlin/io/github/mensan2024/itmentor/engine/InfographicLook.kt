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
internal class InfographicLook(private val sb: Storyboard, private val kit: StyleKit = StyleKit.INFOGRAPHIE, private val smart: Boolean = false) : LookRenderer {
    private val pal = kit.palette
    private val classic = kit == StyleKit.INFOGRAPHIE
    override val ink = pal.ink
    override val muted = pal.muted
    /** Style d'origine : une des trois couleurs de la référence ; sinon la couleur du style. */
    override val accent = if (classic) listOf(rgb(0x37C6F4), rgb(0x3DDC97), rgb(0xFFC43D))[(Rand.of(sb.seed, 0x1F0).nextInt() ushr 1) % 3] else pal.accent
    override val accent2 = rgb(0xFF4D5E)
    private val danger = rgb(0xFF4D5E)
    private val warn = rgb(0xFFC43D)
    private val ok = rgb(0x3DDC97)
    private val panel = pal.card
    private val cache = TextCache()
    private val domain by lazy { DomainDetector.detect(TextSanitizer.clean(sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body })) }
    private val astuceNo: Int? = (listOf(sb.title) + sb.scenes.take(1).flatMap { listOf(it.kicker ?: "", it.title) })
        .firstNotNullOfOrNull { Regex("astuce\\D{0,30}?0*(\\d{1,4})", RegexOption.IGNORE_CASE).find(Marked.strip(it))?.groupValues?.get(1)?.toIntOrNull() }

    enum class Diagram { CHAPTER, HERO_ICON, STEPLIST, TARGETS, LOCK_ATTACK, SHIELD, BARS, CHAIN, ATTEMPTS, CHART, TERMINAL, NUMBER, VERSUS, CHECKS, TEXT, CTA, PACKETS, PROMPT, EDITOR, PULSE, PHISHING, VLAN, FIREWALL, FUNNEL, API, STREAK }

    data class Beat(val chip: String, val l1: String, val l2: String, val caption: String, val diagram: Diagram,
                    val main: String = "", val detail: String? = null, val list: List<String> = emptyList(), val index: Int = 0, val good: Boolean = true, val icon: IconId = IconId.BULB, val attack: Boolean = false)

    private val beatsCache = HashMap<String, List<Beat>>()

    /** Nombre total d'étapes du texte (toutes les listes), pour « DÉFENSE 2/4 ». */
    private val totalSteps: Int by lazy { sb.scenes.filter { it.layout == Layout.STEPS }.sumOf { it.items.size } }
    private val firstStep: Map<String, Int> by lazy {
        var n = 0
        sb.scenes.filter { it.layout == Layout.STEPS }.associate { sc -> val s0 = n; n += sc.items.size; sc.id to s0 }
    }

    /** Titre d'étape plus direct : « Utilise des mots de passe… » → « Mots de passe… ». */
    private fun direct(label: String): String {
        val m = Regex("^(utilise|choisis|adopte|mets en place|installe|crée|configure)\\s+(des|un|une|les|le|la|l')\\s*", RegexOption.IGNORE_CASE).find(label)
        val out = if (m != null && label.length - m.value.length >= 8) label.substring(m.value.length) else label
        return out.replaceFirstChar { it.uppercase() }
    }

    private fun plain(s: String?) = Marked.strip(s ?: "").replace(' ', ' ').trim()

    /** Titre en deux lignes : début en blanc, fin en couleur (2 à 7 mots au total). */
    private fun split2(text: String): Pair<String, String> {
        val full = plain(text).trim().trimEnd('.', '!', ':')
        // Un « titre : suite » se coupe naturellement aux deux-points : rien n'est perdu, la suite passe en couleur.
        val colon = full.indexOf(" : ")
        if (colon in 3..60) {
            val a = full.substring(0, colon).trim(); val b = full.substring(colon + 3).trim()
            if (a.isNotEmpty() && b.isNotEmpty()) return a to b
        }
        val w = full.split(' ').filter { it.isNotBlank() }.take(20)
        if (w.size <= 2) return w.joinToString(" ") to ""
        val k = (w.size + 1) / 2
        return w.take(k).joinToString(" ") to w.drop(k).joinToString(" ")
    }

    private fun diagramFor(text: String, fallback: Diagram): Diagram {
        val t = text.lowercase()
        return when {
            Regex("surveill|journal|auth\\.log|alerte|échecs|echecs|détecte|detecte").containsMatchIn(t) -> Diagram.CHART
            Regex("limite|limitation|tentative|verrouill|bloque|bannir|fail2ban").containsMatchIn(t) -> Diagram.ATTEMPTS
            Regex("pare-?feu|firewall|\\bufw\\b|iptables|filtrage").containsMatchIn(t) && domain in setOf(Domain.RESEAUX, Domain.CYBER, Domain.SYSTEMES) -> Diagram.FIREWALL
            domain == Domain.MARKETING && Regex("tunnel|entonnoir|conversion|prospect|convertis|capte|fidélis|fidelis").containsMatchIn(t) -> Diagram.FUNNEL
            domain == Domain.FULLSTACK && Regex("\\bapi\\b|\\brest\\b|requ[êe]te|\\bhttp\\b|json|endpoint|client-serveur|\\bget\\b|\\bpost\\b|statut|status|\\b200\\b|\\b404\\b").containsMatchIn(t) -> Diagram.API
            Regex("\\bmfa\\b|\\b2fa\\b|double authentification|authentification à deux|code reçu|application d'authentification").containsMatchIn(t) -> Diagram.CHAIN
            (Regex("phishing|hameçon|hamecon|sms piégé|sms pieg|lien suspect|faux site|usurp|cliquer sur un lien").containsMatchIn(t) ||
                phishingContext && Regex("message|sms|lien|expéditeur|expediteur|demande de|signale|banque|colis").containsMatchIn(t)) -> Diagram.PHISHING
            Regex("mots? de passe|passphrase|robuste").containsMatchIn(t) -> Diagram.BARS
            Regex("^(sudo|git|ls|df|ssh|apt|systemctl|docker|npm|show|vlan|ufw|chmod)\\b").containsMatchIn(t) -> Diagram.TERMINAL
            domain == Domain.RESEAUX && Regex("vlan").containsMatchIn(t) -> Diagram.VLAN
            domain == Domain.RESEAUX && Regex("paquet|ttl|traceroute|routage|\\bping\\b|passerelle").containsMatchIn(t) -> Diagram.PACKETS
            domain == Domain.IA && Regex("prompt|consigne|précis|vague|exactement|chatgpt").containsMatchIn(t) -> Diagram.PROMPT
            domain == Domain.FULLSTACK && Regex("code|fonction|bug|erreur|variable|python|javascript|\\bapi\\b|programme|test").containsMatchIn(t) -> Diagram.EDITOR
            // Habitudes et discipline : progression jour après jour. Santé : pouls, sauf sujets apaisants (santé mentale).
            (domain == Domain.GENERAL || domain == Domain.SANTE) && Regex("habitude|chaque jour|discipline|routine|régularité|regularite|quotidien|par jour|jour après jour").containsMatchIn(t) -> Diagram.STREAK
            domain == Domain.SANTE && sb.mood == Mood.ANY -> Diagram.PULSE
            // Démonstration intelligente : chaque étape sans schéma propre reçoit la simulation de son domaine.
            smart && fallback == Diagram.STEPLIST && domain == Domain.RESEAUX -> Diagram.PACKETS
            smart && fallback == Diagram.STEPLIST && domain == Domain.FULLSTACK -> Diagram.EDITOR
            smart && fallback == Diagram.STEPLIST && domain == Domain.MARKETING -> Diagram.FUNNEL
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
            Layout.HERO -> { val (a, b) = split2(sc.title); listOf(Beat(if (attack) "L'ATTAQUE" else if (smart) "DÉMONSTRATION" else "L'ASTUCE", a, b, plain(sc.subtitle ?: sc.title),
                if (attack) Diagram.LOCK_ATTACK else Diagram.HERO_ICON, icon = sc.iconOrDefault(), attack = attack,
                list = sb.scenes.flatMap { it.items }.map { splitLabel(plain(it)).first }.filter { it.length <= 22 }.take(6))) }
            Layout.DEFINITION -> {
                val (a, b) = split2(sc.title); val body = plain(sc.body.ifEmpty { sc.subtitle })
                val tg = targets(body)
                val d = when { tg.isNotEmpty() -> Diagram.TARGETS; attack || Regex("attaque|essai").containsMatchIn(body.lowercase()) -> Diagram.LOCK_ATTACK; else -> diagramFor(body, Diagram.TEXT) }
                listOf(Beat("LE PRINCIPE", a, b, if (sc.authorTitle) plain(sc.title).trimEnd(':') + ". " + body else body, d, main = body, list = tg, attack = attack || d == Diagram.LOCK_ATTACK))
            }
            Layout.STEPS -> {
                val defend = domain == Domain.CYBER
                val word = if (defend) "DÉFENSE" else "ÉTAPE"
                val labels = sc.items.map { splitLabel(plain(it)).first }
                val n0 = firstStep[sc.id] ?: 0
                val total = totalSteps.coerceAtLeast(sc.items.size)
                val steps = sc.items.mapIndexed { i, it ->
                    val (label, detail) = splitLabel(plain(it))
                    val (a, b) = split2(direct(label))
                    val cmd = Regex("^[a-z][a-z0-9._-]*(\\s+[-/a-zA-Z0-9.=_\"]+){0,4}$").matches(label) && label.length < 40
                    val d = if (cmd) Diagram.TERMINAL else diagramFor(plain(it), Diagram.STEPLIST)
                    Beat("$word ${n0 + i + 1}/$total", a, b, explain(d, plain(it)), d, main = label, detail = detail, list = labels, index = i)
                }
                // Écran de chapitre avant la première liste : « Comment se défendre ? 4 réflexes ».
                if (sc.authorTitle && !(n0 == 0 && total >= 2 && plain(sc.title).lowercase().startsWith("comment"))) {
                    val (c1, c2) = split2(plain(sc.title))
                    listOf(Beat("${sc.items.size} ${if (defend) "RÉFLEXES" else "ÉTAPES"}", c1, c2, plain(sc.title).trimEnd(':') + ".", Diagram.CHAPTER)) + steps
                } else if (n0 == 0 && total >= 2) {
                    val header = plain(sc.title).trimEnd('?', ' ', ':').takeIf { it.lowercase().startsWith("comment") }
                    val (c1, c2) = if (header != null) split2("$header ?") else if (defend) "Comment" to "se défendre ?" else "Comment" to "faire ?"
                    val unit = if (defend) "RÉFLEXES" else "ÉTAPES"
                    listOf(Beat("$total $unit", c1, c2, if (header != null) plain(sc.title).trimEnd(':') + "." else "${(c1 + " " + c2).trim()} $total ${unit.lowercase()} simples.", Diagram.CHAPTER)) + steps
                } else steps
            }
            Layout.STAT -> { val (a, b) = split2(sc.title); listOf(Beat(tag.ifEmpty { "LE CHIFFRE" }, a, b, plain(sc.title) + (if (sc.body.isNotEmpty()) ". " + plain(sc.body) else ""), Diagram.NUMBER, main = sc.statValue ?: "")) }
            Layout.COMPARE -> sc.compare?.let { c -> listOf(
                Beat("À ÉVITER", plain(c.badLabel), "", "${c.badLabel} : ${c.badText()}", Diagram.VERSUS, main = c.badText(), good = false),
                Beat("À FAIRE", plain(c.goodLabel), "", "${c.goodLabel} : ${c.goodText()}", Diagram.VERSUS, main = c.goodText(), good = true),
            ) } ?: emptyList()
            Layout.CHECKLIST -> {
                val (a, b) = split2(sc.title)
                listOfNotNull(
                    if (sc.items.isEmpty()) null else Beat(tag.ifEmpty { "À RETENIR" }, a, b, plain(sc.subtitle).ifEmpty { "${sc.items.size} réflexes simples à appliquer dès aujourd'hui." }, Diagram.CHECKS, list = sc.items.map(::plain)),
                    Beat("", "", "", astuceNo?.let { "Abonne-toi pour l'astuce ${Fmt.pad(it + 1, 3)} !" } ?: plain(sc.body).ifEmpty { "Abonne-toi pour la suite !" }, Diagram.CTA, main = plain(sc.body)),
                )
            }
            Layout.SCREEN -> emptyList()
        }
    }

    private fun beatAt(sc: Scene, t: Float): Triple<Int, Float, Float> {
        val bs = beats(sc)
        // Les schémas riches (graphique + terminal, fail2ban, MFA, mots de passe) ont besoin de plus de temps pour se dérouler.
        val rich = setOf(Diagram.PACKETS, Diagram.VLAN, Diagram.PROMPT, Diagram.EDITOR, Diagram.PULSE, Diagram.PHISHING, Diagram.FIREWALL, Diagram.FUNNEL, Diagram.API, Diagram.STREAK, Diagram.CHART, Diagram.ATTEMPTS, Diagram.CHAIN, Diagram.BARS, Diagram.LOCK_ATTACK)
        val w = bs.map {
            (0.7f + 0.3f * (it.caption.length.coerceIn(30, 180) / 100f)) * (if (it.diagram in rich) 1.4f else 1f) +
                (if (it.diagram == Diagram.CTA) -0.3f else 0f) + (if (it.diagram == Diagram.CHAPTER) -0.25f else 0f)
        }
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
        if (classic) {
            g.fillVerticalGradient(0f, 0f, 1080f, 1920f, rgb(0x0B1224), rgb(0x1A1446))
            g.fillRadialGlow(540f, 1650f, 900f, rgb(0x3B2E9A).withAlpha(0.35f))
        } else KitDecor.background(g, kit, sb.seed, tGlobalMs, corners = false)
        val bs = beats(sc)
        if (bs.isEmpty()) return
        val (i, t, len) = beatAt(sc, localMs)
        val b = bs[i]
        // Rouge pendant l'attaque, bleu pour la défense.
        if (b.attack) { g.fillRadialGlow(540f, 520f, 1000f, danger.withAlpha(0.22f)); g.fillRadialGlow(540f, 1300f, 700f, rgb(0x7A1F4A).withAlpha(0.25f)) }
        else if (classic) g.fillRadialGlow(540f, 300f, 760f, rgb(0x1E6FD9).withAlpha(0.12f))
        // Rubrique en haut (fixe) : le numéro de l'astuce s'il y en a un, sinon la marque.
        val hf = FontSpec(FontFamily.TEXT, 800, 24f)
        val left = (astuceNo?.let { "ASTUCE ${Fmt.pad(it, 3)}" } ?: "ASTUCE")
        g.drawTracked(left, Safe.LEFT, 172f, hf, accent, 2f)
        val e = Easing.EXPO_OUT.ease(clamp01(t / 300f))
        if (b.diagram == Diagram.CTA) { g.withPose(Pose(alpha = e), 540f, 900f) { cta(g, b, t) }; captionBox(g, b, t, "${sc.id}/$i"); return }
        if (b.diagram == Diagram.CHAPTER) { chapter(g, b, t, "${sc.id}/$i"); captionBox(g, b, t, "${sc.id}/$i"); return }
        var y = 230f
        // Pastille d'étape
        if (b.chip.isNotEmpty()) {
            val cf = FontSpec(FontFamily.TEXT, 800, 26f)
            val w = g.measureTracked(b.chip, cf, 2f) + 48f
            g.withPose(Pose(alpha = e, scale = lerp(0.8f, 1f, e)), 540f, y + 25f) {
                when (kit.header) {
                    KitHeader.BADGE -> {
                        g.fillRoundRect(540f - w / 2, y, 540f + w / 2, y + 50f, 10f, accent)
                        g.drawTracked(b.chip, 540f - w / 2 + 24f, y + 25f + g.metrics(cf).ascent / 2, cf, pal.onHighlight.takeIf { !classic } ?: rgb(0x0B1224), 2f)
                    }
                    KitHeader.BAR -> {
                        g.fillRect(540f - w / 2 + 10f, y + 46f, 540f + w / 2 - 10f, y + 50f, accent)
                        g.drawTracked(b.chip, 540f - w / 2 + 24f, y + 25f + g.metrics(cf).ascent / 2, cf, accent, 2f)
                    }
                    KitHeader.CHIP -> {
                        g.fillRoundRect(540f - w / 2, y, 540f + w / 2, y + 50f, 25f, accent.withAlpha(0.18f))
                        g.strokeRoundRect(540f - w / 2, y, 540f + w / 2, y + 50f, 25f, 2f, accent.withAlpha(0.7f))
                        g.drawTracked(b.chip, 540f - w / 2 + 24f, y + 25f + g.metrics(cf).ascent / 2, cf, accent, 2f)
                    }
                }
            }
            y += 80f
        }
        // Titre sur deux lignes
        val tf = titleFont(72f)
        for ((k, line) in listOf(b.l1, b.l2).withIndex()) {
            if (line.isEmpty()) continue
            // Le titre de l'auteur n'est jamais coupé : jusqu'à 3 lignes, la taille baisse si besoin.
            val bl = cache.fit(g, "t/${sc.id}/$i/$k", line, tf, Safe.WIDTH - 20f, 3)
            val a = Easing.EXPO_OUT.ease(clamp01((t - k * 120f) / 420f))
            for ((j, text) in bl.lines.withIndex()) {
                val base = g.baselineOf(bl, y, j)
                val tw = g.measureText(text, bl.font)
                val x0 = 540f - tw / 2
                g.withPose(Pose(alpha = a, dy = (1 - a) * 24f), 540f, base) {
                    if (k == 1) when (kit.titleFx) {
                        KitTitleFx.HIGHLIGHT -> {
                            val m = g.metrics(bl.font); val p = Easing.EXPO_OUT.ease(clamp01((t - 250f) / 450f))
                            g.fillRoundRect(x0 - 16f, base - m.ascent - 6f, x0 - 16f + (tw + 32f) * p, base + m.descent + 4f, 12f, pal.highlight)
                            g.drawText(text, x0, base, bl.font, pal.onHighlight)
                        }
                        KitTitleFx.MARKER -> {
                            val p = Easing.EXPO_OUT.ease(clamp01((t - 250f) / 450f))
                            g.fillRoundRect(x0 - 6f, base - 8f, x0 - 6f + (tw + 12f) * p, base + 12f, 8f, accent.withAlpha(0.45f))
                            g.drawText(text, x0, base, bl.font, ink)
                        }
                        else -> g.drawText(text, x0, base, bl.font, accent)
                    } else g.drawText(text, x0, base, bl.font, ink)
                }
            }
            y += bl.height + 4f
        }
        val top = y + 40f
        val cap = captionBox(g, b, t, "${sc.id}/$i")
        val area = R(Safe.LEFT, top, Safe.RIGHT, cap - 40f)
        g.withPose(entrance(t), 540f, area.cy) { diagram(g, b, t, len, area, "${sc.id}/$i") }
        if (kit.flash && t < 120f) g.fillRect(0f, 0f, 1080f, 1920f, (((1f - t / 120f) * 0.18f * 255).toInt() shl 24) or 0xFFFFFF)
    }

    /** Police des titres du style (taille de référence : 72 px en police « texte »). */
    private fun titleFont(size: Float) = if (classic) FontSpec(FontFamily.TEXT, 800, size)
        else FontSpec(kit.titleFont.family, kit.titleFont.weight, size * kit.titleFont.size / 66f)

    /** Entrée du schéma selon le style. */
    private fun entrance(t: Float): Pose = when (kit.motion) {
        KitMotion.RISE -> { val e = Easing.EXPO_OUT.ease(clamp01(t / 380f)); Pose(alpha = e, dy = (1 - e) * 60f) }
        KitMotion.DROP -> { val e = Easing.BACK_OUT.ease(clamp01(t / 440f)); Pose(alpha = clamp01(t / 160f), dy = -(1 - e) * 70f) }
        KitMotion.ZOOM -> { val e = Easing.EXPO_OUT.ease(clamp01(t / 380f)); Pose(alpha = clamp01(t / 140f), scale = lerp(0.82f, 1f, e)) }
        KitMotion.POP -> { val e = Easing.BACK_OUT.ease(clamp01(t / 360f)); Pose(alpha = clamp01(t / 100f), scale = lerp(0.7f, 1f, e).coerceAtMost(1.01f)) }
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
            when (kit.frame) {
                KitFrame.FLAT -> {
                    g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 16f, panel.withAlpha(0.92f))
                    g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 16f, 1.5f, 0x22FFFFFF)
                    g.fillRoundRect(Safe.LEFT, top, Safe.LEFT + 7f, top + h, 3f, accent)
                }
                KitFrame.DOUBLE -> {
                    g.fillRoundRect(Safe.LEFT - 10f, top - 10f, Safe.RIGHT + 10f, top + h + 10f, 26f, KitDecor.darken(panel, 0.35f))
                    g.strokeRoundRect(Safe.LEFT - 10f, top - 10f, Safe.RIGHT + 10f, top + h + 10f, 26f, 1.5f, 0x1CFFFFFF)
                    g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 18f, panel)
                    g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 18f, 1.5f, 0x26FFFFFF)
                    g.fillRoundRect(Safe.LEFT, top, Safe.LEFT + 7f, top + h, 3f, accent)
                }
                KitFrame.GLASS -> {
                    g.fillRoundRect(Safe.LEFT + 6f, top + 12f, Safe.RIGHT + 6f, top + h + 12f, 22f, 0x44000000)
                    g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 22f, panel.withAlpha(0.78f))
                    g.fillVerticalGradient(Safe.LEFT + 16f, top + 2f, Safe.RIGHT - 16f, top + 60f, 0x22FFFFFF, 0x00FFFFFF)
                    g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 22f, 2f, 0x33FFFFFF)
                }
                KitFrame.NEON -> {
                    g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 18f, panel.withAlpha(0.94f))
                    g.strokeRoundRect(Safe.LEFT - 5f, top - 5f, Safe.RIGHT + 5f, top + h + 5f, 22f, 7f, accent.withAlpha(0.22f))
                    g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, top + h, 18f, 2.5f, accent)
                }
            }
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
            Diagram.LOCK_ATTACK -> lockAttack(g, t, len, a)
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
            Diagram.PACKETS -> domainDiagram(g, a) { l, tp, r, bt -> demos.packets(g, t, l, tp, r, bt) }
            Diagram.PROMPT -> domainDiagram(g, a) { l, tp, r, bt -> demos.prompt(g, t, l, tp, r, bt) }
            Diagram.EDITOR -> domainDiagram(g, a) { l, tp, r, bt -> demos.editor(g, t, l, tp, r, bt) }
            Diagram.VLAN -> domainDiagram(g, a) { l, tp, r, bt -> demos.vlan(g, t, l, tp, r, bt) }
            Diagram.PULSE -> domainDiagram(g, a) { l, tp, r, bt -> demos.pulse(g, t, l, tp, r, bt) }
            Diagram.PHISHING -> domainDiagram(g, a) { l, tp, r, bt -> demos.phishing(g, t, l, tp, r, bt) }
            Diagram.FIREWALL -> domainDiagram(g, a) { l, tp, r, bt -> demos.firewall(g, t, l, tp, r, bt) }
            Diagram.FUNNEL -> domainDiagram(g, a) { l, tp, r, bt -> demos.funnel(g, t, l, tp, r, bt) }
            Diagram.API -> domainDiagram(g, a) { l, tp, r, bt -> demos.api(g, t, l, tp, r, bt) }
            Diagram.STREAK -> domainDiagram(g, a) { l, tp, r, bt -> demos.streak(g, t, l, tp, r, bt) }
            Diagram.CTA, Diagram.CHAPTER -> {}
        }
    }

    private val phishingContext by lazy {
        Regex("phishing|hameçon|hamecon|sms piégé|sms pieg|lien suspect|faux site", RegexOption.IGNORE_CASE)
            .containsMatchIn(sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body })
    }

    private val demos by lazy { DomainDiagrams(ink, muted, accent, danger, ok, warn) }

    /** Les démonstrations sont dessinées pour 560 px de haut ; réduites si la zone est plus basse. */
    private fun domainDiagram(g: Gfx, a: R, draw: (Float, Float, Float, Float) -> Unit) {
        val s = (a.h / 560f).coerceIn(0.5f, 1f)
        g.withPose(Pose(scale = s), a.cx, a.t) { draw(a.l, a.t, a.r, a.t + 560f) }
    }

    private fun card(g: Gfx, l: Float, tp: Float, r: Float, bt: Float, border: Int = 0x26FFFFFF) {
        g.fillRoundRect(l, tp, r, bt, 16f, panel)
        g.strokeRoundRect(l, tp, r, bt, 16f, 2f, border)
    }

    /** Bouclier « glitch » du chapitre, puis le titre en grand. */
    private fun chapter(g: Gfx, b: Beat, t: Float, key: String) {
        val cx = 540f; val cy = 760f
        val k = Easing.BACK_OUT.ease(clamp01(t / 520f))
        val glitch = t < 700f && ((t / 60f).toInt() % 3 != 0)
        g.fillRadialGlow(cx, cy, 380f, accent.withAlpha(0.28f))
        g.withPose(Pose(scale = lerp(0.6f, 1f, k), alpha = clamp01(k * 1.5f)), cx, cy) {
            if (glitch) {
                Icons.draw(g, IconId.SHIELD, cx - 9f, cy, 300f, rgb(0xFF3B6B).withAlpha(0.7f), 2.6f)
                Icons.draw(g, IconId.SHIELD, cx + 9f, cy, 300f, rgb(0x22D3EE).withAlpha(0.7f), 2.6f)
            }
            Icons.draw(g, IconId.SHIELD, cx, cy, 300f, accent, 2.6f)
            Icons.draw(g, IconId.CHECK, cx, cy + 8f, 130f, accent, 3.4f, reveal = clamp01((t - 300f) / 400f))
        }
        val cf = FontSpec(FontFamily.TEXT, 800, 26f)
        val w = g.measureTracked(b.chip, cf, 2f) + 48f
        val y0 = 1010f
        g.fillRoundRect(cx - w / 2, y0, cx + w / 2, y0 + 50f, 25f, accent.withAlpha(0.18f))
        g.strokeRoundRect(cx - w / 2, y0, cx + w / 2, y0 + 50f, 25f, 2f, accent.withAlpha(0.7f))
        g.drawTracked(b.chip, cx - w / 2 + 24f, y0 + 25f + g.metrics(cf).ascent / 2, cf, accent, 2f)
        val tf = FontSpec(FontFamily.TEXT, 900, 96f)
        var y = y0 + 90f
        for ((li, line) in listOf(b.l1, b.l2).withIndex()) {
            if (line.isEmpty()) continue
            val bl = cache.fit(g, "ch/$key/$li", line, tf, Safe.WIDTH, 3)
            val a = Easing.EXPO_OUT.ease(clamp01((t - 200f - li * 150f) / 400f))
            for ((j, text) in bl.lines.withIndex()) {
                val base = g.baselineOf(bl, y, j)
                g.withPose(Pose(alpha = a, dy = (1 - a) * 30f), cx, base) { g.drawText(text, cx - g.measureText(text, bl.font) / 2, base, bl.font, if (li == 0) ink else accent) }
            }
            y += bl.height + 6f
        }
    }

    /** Petites étiquettes alignées et centrées (types d'attaque, conseils). */
    private fun chips(g: Gfx, labels: List<String>, y: Float, t: Float, delay: Float, col: Int, a: R): Float {
        val f = FontSpec(FontFamily.TEXT, 700, 24f)
        val rows = ArrayList<List<Pair<String, Float>>>(); var cur = ArrayList<Pair<String, Float>>(); var w = 0f
        for (l in labels) {
            val lw = g.measureText(l, f) + 36f
            if (cur.isNotEmpty() && w + lw + 12f > a.w) { rows += cur; cur = ArrayList(); w = 0f }
            cur += l to lw; w += lw + 12f
        }
        if (cur.isNotEmpty()) rows += cur
        var yy = y
        var idx = 0
        for (row in rows) {
            var x = a.cx - (row.sumOf { it.second.toDouble() }.toFloat() + 12f * (row.size - 1)) / 2
            for ((l, lw) in row) {
                val k = Easing.BACK_OUT.ease(clamp01((t - delay - idx * 120f) / 300f))
                if (k > 0f) g.withPose(Pose(alpha = clamp01(k * 2), scale = lerp(0.7f, 1f, k)), x + lw / 2, yy + 22f) {
                    g.fillRoundRect(x, yy, x + lw, yy + 44f, 22f, col.withAlpha(0.16f))
                    g.strokeRoundRect(x, yy, x + lw, yy + 44f, 22f, 1.5f, col.withAlpha(0.8f))
                    g.drawText(l, x + 18f, yy + 30f, f, col)
                }
                x += lw + 12f; idx++
            }
            yy += 56f
        }
        return yy
    }

    /** Cadenas assailli d'essais, compteur qui s'emballe, puis le mot de passe trouvé : le cadenas s'ouvre. */
    private fun lockAttack(g: Gfx, t: Float, len: Float, a: R) {
        val cx = a.cx; val cy = a.cy - 70f
        val found = t > len * 0.62f
        val col = if (found) ok else danger
        g.fillRadialGlow(cx, cy, 260f, col.withAlpha(0.35f))
        g.fillCircle(cx, cy, 120f, col.withAlpha(0.25f))
        g.fillCircle(cx, cy, 92f, col)
        Icons.draw(g, IconId.LOCK, cx, cy, 100f, rgb(0xFFFFFF), 2.4f)
        val mf = FontSpec(FontFamily.MONO, 600, 28f)
        if (!found) {
            val guesses = listOf("azerty123", "motdepasse", "123456", "liberte2023", "admin", "qwerty", "bonjour1", "000000", "iloveyou", "marseille")
            for ((k, s) in guesses.withIndex()) {
                val ph = ((t / 1600f) + k / guesses.size.toFloat()) % 1f
                val ang = k * 0.63f + 0.3f
                val r = 150f + ph * 280f
                val x = cx + cos(ang) * r * 1.3f; val y = cy + sin(ang) * r * 0.7f
                val w = g.measureText(s, mf) + 24f
                val al = (1f - ph) * clamp01(ph * 5f)
                if (x - w / 2 < a.l || x + w / 2 > a.r || y < a.t + 20f || y > cy + 150f) continue
                g.withPose(Pose(alpha = al), x, y) {
                    g.fillRoundRect(x - w / 2, y - 21f, x + w / 2, y + 21f, 8f, danger.withAlpha(0.2f))
                    g.strokeRoundRect(x - w / 2, y - 21f, x + w / 2, y + 21f, 8f, 1.5f, danger.withAlpha(0.8f))
                    g.drawText(s, x - w / 2 + 12f, y + 9f, mf, rgb(0xFFB3BB))
                    g.fillCircle(x + w / 2 - 2f, y - 19f, 7f, danger)
                }
            }
        } else {
            // le mot de passe trouvé, au-dessus du cadenas
            val s = "Soleil2024"
            val w = g.measureText(s, mf) + 30f
            val k = Easing.BACK_OUT.ease(clamp01((t - len * 0.62f) / 350f))
            g.withPose(Pose(scale = lerp(0.5f, 1f, k), alpha = clamp01(k * 2)), cx, cy - 160f) {
                g.fillRoundRect(cx - w / 2, cy - 182f, cx + w / 2, cy - 138f, 10f, ok.withAlpha(0.22f))
                g.strokeRoundRect(cx - w / 2, cy - 182f, cx + w / 2, cy - 138f, 10f, 2f, ok)
                g.drawText(s, cx - w / 2 + 15f, cy - 150f, mf, ok)
            }
        }
        val n = (Easing.EASE_IN.ease(clamp01(t / (len * 0.62f))) * 8412).toInt().coerceAtLeast(27)
        val cf = FontSpec(FontFamily.TEXT, 800, 42f)
        val label = "Essais : " + Fmt.thousands(n)
        g.drawText(label, cx - g.measureText(label, cf) / 2, cy + 190f, cf, col)
        chips(g, listOf("Force brute pure", "Dictionnaire", "Password spraying", "Credential stuffing"), cy + 230f, t, 900f, warn, a)
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

    /** Nombre de combinaisons lisible : « 309 millions », « 3 600 000 milliards ». */
    private fun bigCount(x: Double): String = when {
        x >= 1e9 -> { val m = x / 1e9; val mag = kotlin.math.floor(kotlin.math.log10(m)).toInt(); val p = kotlin.math.round(m / 10.0.pow(mag - 1)) * 10.0.pow(mag - 1); Fmt.thousands(p.toInt()) + " milliards" }
        x >= 1e6 -> "${kotlin.math.round(x / 1e6).toInt()} millions"
        else -> Fmt.thousands(x.toInt())
    }

    private fun Double.pow(e: Int): Double { var r = 1.0; repeat(kotlin.math.abs(e)) { r *= this }; return if (e < 0) 1 / r else r }

    /**
     * Robustesse : combinaisons qu'un attaquant doit tester dans le pire cas.
     * - azerty : 6 minuscules → 26⁶ ≈ 309 millions, et il figure en tête de tous les dictionnaires ;
     * - Soleil2024 : mot courant + année → environ 10 millions de candidats avec les règles de dictionnaire ;
     * - 4 mots tirés au hasard dans une liste de 7 776 (Diceware) → 7 776⁴ ≈ 3,7 × 10¹⁵.
     */
    private fun bars(g: Gfx, b: Beat, t: Float, a: R, key: String) {
        val pw = Regex("mots? de passe|passphrase").containsMatchIn(b.caption.lowercase())
        if (!pw) { stepList(g, b, t, a, key); return }
        data class Row(val pwd: String, val sub: String, val count: String, val fill: Float, val col: Int)
        val rows = listOf(
            Row("azerty", "6 minuscules · en tête de tous les dictionnaires", bigCount(26.0.pow(6)), 0.12f, danger),
            Row("Soleil2024", "10 caractères… mais mot courant + année", "≈ 10 millions", 0.38f, warn),
            Row("rivière-cactus-lampe-orbite", "4 mots tirés au hasard", "≈ " + bigCount(7776.0.pow(4)), 0.97f, ok),
        )
        val lf = FontSpec(FontFamily.TEXT, 600, 24f)
        g.drawText("Combinaisons à tester (pire cas pour l'attaquant)", a.l + 4f, a.t + 26f, lf, muted)
        val rowH = 150f
        var y = a.t + 50f
        val mf = FontSpec(FontFamily.MONO, 700, 34f)
        val sf = FontSpec(FontFamily.TEXT, 500, 22f)
        val cf = FontSpec(FontFamily.TEXT, 800, 26f)
        for ((i, r) in rows.withIndex()) {
            val k = Easing.EXPO_OUT.ease(clamp01((t - 150f - i * 330f) / 700f))
            if (k <= 0f) { y += rowH; continue }
            g.withPose(Pose(alpha = clamp01(k * 2)), a.cx, y) {
                card(g, a.l, y, a.r, y + rowH - 16f)
                val cw = g.measureText(r.count, cf)
                val name = TextLayout.fit(g, r.pwd, mf, a.w - cw - 90f, 1, minSize = 22f)
                g.drawText(name.lines.first(), a.l + 26f, y + 48f, name.font, ink)
                if (k > 0.5f) g.drawText(r.count, a.r - 26f - cw, y + 46f, cf, r.col)
                g.drawText(TextLayout.fit(g, r.sub, sf, a.w - 52f, 1, minSize = 16f).lines.first(), a.l + 26f, y + 82f, sf, muted)
                g.fillRoundRect(a.l + 26f, y + 104f, a.r - 26f, y + 114f, 5f, 0x22FFFFFF)
                g.fillRoundRect(a.l + 26f, y + 104f, a.l + 26f + (a.w - 52f) * r.fill * k, y + 114f, 5f, r.col)
            }
            y += rowH
        }
        if (Regex("unique").containsMatchIn(b.caption.lowercase()))
            chips(g, listOf("Unique : une fuite ne doit jamais ouvrir tes autres comptes"), y + 6f, t, 1300f, accent, a)
    }

    private fun chain(g: Gfx, b: Beat, t: Float, a: R, key: String) {
        val mfa = Regex("mfa|2fa|double authentification|authentification").containsMatchIn(b.caption.lowercase())
        data class Step(val icon: IconId, val title: String, val sub: String, val col: Int)
        val steps: List<Step> = if (mfa) listOf(
            Step(IconId.KEY, "Mot de passe deviné", "l'attaquant a trouvé « Soleil2024 »", danger),
            Step(IconId.SHIELD, "Code MFA demandé", "sur ton téléphone ou ta clé", accent),
            Step(IconId.LOCK, "Accès refusé", "l'attaquant reste dehors", ok),
        ) else {
            val l = b.list.ifEmpty { listOf(b.main) }
            val s0 = (b.index - 1).coerceAtLeast(0).coerceAtMost((l.size - 3).coerceAtLeast(0))
            l.drop(s0).take(3).mapIndexed { i, x -> Step(listOf(IconId.BULB, IconId.TARGET, IconId.CHECK)[i], x, "", if (s0 + i == b.index) accent else muted) }
        }
        val h = 132f; val gap = 54f
        var y = a.t + 10f
        val tf = FontSpec(FontFamily.TEXT, 800, 32f)
        val sf = FontSpec(FontFamily.TEXT, 500, 22f)
        for ((i, s) in steps.withIndex()) {
            val start = 150f + i * 550f
            val k = Easing.BACK_OUT.ease(clamp01((t - start) / 420f))
            if (k > 0f) g.withPose(Pose(alpha = clamp01(k), scale = lerp(0.88f, 1f, k)), a.cx, y + h / 2) {
                card(g, a.l + 10f, y, a.r - 10f, y + h, s.col.withAlpha(0.85f))
                g.fillCircle(a.l + 74f, y + h / 2, 34f, s.col.withAlpha(0.22f))
                Icons.draw(g, s.icon, a.l + 74f, y + h / 2, 36f, s.col, 2.6f)
                val title = TextLayout.fit(g, s.title, tf, a.w - 400f, 1, minSize = 22f)
                g.drawText(title.lines.first(), a.l + 128f, y + (if (s.sub.isEmpty()) h / 2 + 12f else 58f), title.font, if (s.col == muted) ink else s.col)
                if (s.sub.isNotEmpty()) g.drawText(TextLayout.fit(g, s.sub, sf, a.w - 170f, 1, minSize = 16f).lines.first(), a.l + 128f, y + 94f, sf, muted)
                // code à 6 chiffres envoyé à l'utilisateur, que l'attaquant n'a pas
                if (mfa && i == 1) {
                    val digits = "482913"
                    val bw = 34f; val bx0 = a.r - 40f - 6 * (bw + 6f)
                    val df = FontSpec(FontFamily.MONO, 700, 26f)
                    val fail = t > start + 650f
                    for (d in 0 until 6) {
                        val x = bx0 + d * (bw + 6f)
                        g.fillRoundRect(x, y + 44f, x + bw, y + 88f, 6f, if (fail) danger.withAlpha(0.18f) else accent.withAlpha(0.15f))
                        g.strokeRoundRect(x, y + 44f, x + bw, y + 88f, 6f, 1.5f, if (fail) danger else accent)
                        val ch = if (fail) "•" else digits[d].toString()
                        if (t > start + 150f + d * 60f) g.drawText(ch, x + bw / 2 - g.measureText(ch, df) / 2, y + 76f, df, if (fail) danger else ink)
                    }
                    if (fail) { g.fillCircle(a.r - 34f, y + 40f, 15f, danger); Icons.draw(g, IconId.CROSS, a.r - 34f, y + 40f, 14f, rgb(0xFFFFFF), 3f) }
                }
            }
            if (i < steps.lastIndex) {
                val lk = clamp01((t - start - 450f) / 250f)
                g.fillRect(a.cx - 2f, y + h, a.cx + 2f, y + h + gap * lk, 0x66FFFFFF)
            }
            y += h + gap
        }
        if (mfa) chips(g, listOf("Préfère une appli d'authentification ou une clé FIDO2 au SMS"), y - gap + 24f, t, 1800f, accent, a)
    }

    private fun attempts(g: Gfx, t: Float, a: R) {
        val cy = a.t + 70f
        val n = (clamp01((t - 150f) / 1500f) * 5).toInt().coerceAtMost(5)
        val tf = FontSpec(FontFamily.TEXT, 800, 30f)
        val label = "Tentatives : $n/5"
        g.drawText(label, a.cx - g.measureText(label, tf) / 2, cy - 30f, tf, warn)
        for (k in 0 until 5) {
            val x = a.cx + (k - 2) * 110f
            if (k < n) { g.fillCircle(x, cy + 40f, 36f, danger); Icons.draw(g, IconId.CROSS, x, cy + 40f, 36f, rgb(0xFFFFFF), 3.4f) }
            else g.fillCircle(x, cy + 40f, 36f, 0x22FFFFFF)
        }
        val banned = n >= 5
        if (banned) {
            val k = Easing.BACK_OUT.ease(clamp01((t - 1700f) / 350f))
            val bf = FontSpec(FontFamily.TEXT, 800, 28f)
            val s = "IP bannie pendant 1 h"
            val w = g.measureText(s, bf) + 86f
            g.withPose(Pose(alpha = clamp01(k), scale = lerp(1.5f, 1f, k)), a.cx, cy + 130f) {
                g.fillRoundRect(a.cx - w / 2, cy + 104f, a.cx + w / 2, cy + 156f, 26f, danger)
                Icons.draw(g, IconId.LOCK, a.cx - w / 2 + 34f, cy + 130f, 26f, rgb(0xFFFFFF), 2.4f)
                g.drawText(s, a.cx - w / 2 + 60f, cy + 140f, bf, rgb(0xFFFFFF))
            }
        }
        val linux = Regex("ssh|linux|serveur|fail2ban").containsMatchIn(sb.scenes.joinToString(" ") { it.body + it.items.joinToString(" ") }.lowercase())
        val lines = if (linux) listOf("# /etc/fail2ban/jail.local", "[sshd]", "enabled  = true", "maxretry = 5", "findtime = 10m", "bantime  = 1h")
        else listOf("# Stratégie de verrouillage", "Seuil        = 5 échecs", "Fenêtre      = 10 min", "Verrouillage = 1 h")
        val top = cy + 190f
        val h = (lines.size * 38f + 120f).coerceAtMost(a.b - top - 130f)
        if (h > 140f) {
            terminalFrame(g, a.l, top, a.r, top + h, if (linux) "fail2ban · protection SSH" else "Stratégie de sécurité")
            val mf = FontSpec(FontFamily.MONO, 400, 26f)
            var yy = top + 92f
            for ((i, l) in lines.withIndex()) {
                if (yy > top + h - 50f) break
                if (t > 250f + i * 160f) g.drawText(l, a.l + 28f, yy, mf, if (l.startsWith("#")) muted else if (l.startsWith("[")) accent else ink)
                yy += 38f
            }
            if (banned && linux && t > 1900f) g.drawText("NOTICE  [sshd] Ban 203.0.113.7", a.l + 28f, (yy + 8f).coerceAtMost(top + h - 18f), mf, rgb(0xFF9AA5))
            chips(g, listOf("Windows : seuil de verrouillage de compte", "Web : délai progressif + CAPTCHA"), top + h + 18f, t, 2100f, accent, a)
        }
    }

    private fun terminalFrame(g: Gfx, l: Float, tp: Float, r: Float, bt: Float, title: String) {
        g.fillRoundRect(l, tp, r, bt, 14f, rgb(0x0A0F1C))
        g.strokeRoundRect(l, tp, r, bt, 14f, 1.5f, 0x2AFFFFFF)
        g.fillRect(l + 2f, tp + 46f, r - 2f, tp + 48f, 0x18FFFFFF)
        listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(l + 26f + i * 22f, tp + 24f, 7f, c) }
        val f = FontSpec(FontFamily.TEXT, 600, 20f)
        g.drawText(title, (l + r) / 2 - g.measureText(title, f) / 2, tp + 31f, f, muted)
    }

    /** Échecs de connexion sur 24 h avec seuil d'alerte, puis la commande qui trouve l'attaquant. */
    private fun chart(g: Gfx, t: Float, a: R) {
        val h = (a.h * 0.5f).coerceIn(300f, 400f)
        val top = a.t
        card(g, a.l, top, a.r, top + h)
        val lf = FontSpec(FontFamily.TEXT, 700, 24f)
        val rf = FontSpec(FontFamily.TEXT, 500, 20f)
        g.drawText("Échecs d'authentification / heure", a.l + 24f, top + 40f, lf, ink)
        val srv = "srv-01 · dernières 24 h"
        g.drawText(srv, a.r - 24f - g.measureText(srv, rf), top + 40f, rf, muted)
        // exemple : 24 heures, pic entre 2 h et 4 h
        val vals = listOf(6, 4, 52, 61, 44, 5, 4, 3, 5, 6, 4, 5, 6, 7, 5, 6, 5, 4, 6, 5, 7, 6, 4, 5)
        val maxV = 61f
        val base = top + h - 44f
        val maxH = h - 130f
        val bw = (a.w - 48f) / vals.size
        for ((i, v) in vals.withIndex()) {
            val k = Easing.EXPO_OUT.ease(clamp01((t - 100f - i * 25f) / 400f))
            val bh = maxH * v / maxV * k
            g.fillRoundRect(a.l + 24f + i * bw + 4f, base - bh, a.l + 24f + (i + 1) * bw - 4f, base, 3f, if (v > 20) danger else accent.withAlpha(0.75f))
        }
        val xf = FontSpec(FontFamily.TEXT, 500, 18f)
        for ((i, lab) in listOf(0 to "00h", 6 to "06h", 12 to "12h", 18 to "18h")) g.drawText(lab, a.l + 24f + i * bw, base + 28f, xf, muted)
        // seuil d'alerte (pointillés)
        val ty = base - maxH * 20f / maxV
        var x = a.l + 24f
        while (x < a.r - 24f) { g.fillRect(x, ty, x + 12f, ty + 2f, warn.withAlpha(0.8f)); x += 20f }
        g.drawText("seuil d'alerte", a.r - 24f - g.measureText("seuil d'alerte", xf), ty - 8f, xf, warn)
        val ak = Easing.BACK_OUT.ease(clamp01((t - 750f) / 350f))
        if (ak > 0f) g.withPose(Pose(alpha = clamp01(ak), scale = lerp(1.3f, 1f, ak)), a.r - 190f, top + 100f) {
            val bx = a.r - 360f
            g.fillRoundRect(bx, top + 62f, a.r - 24f, top + 132f, 10f, danger.withAlpha(0.22f))
            g.strokeRoundRect(bx, top + 62f, a.r - 24f, top + 132f, 10f, 2f, danger)
            g.fillCircle(bx + 30f, top + 97f, 13f, danger)
            g.drawText("!", bx + 26f, top + 105f, FontSpec(FontFamily.TEXT, 900, 22f), rgb(0xFFFFFF))
            g.drawText("ALERTE · 4 812 échecs", bx + 54f, top + 94f, FontSpec(FontFamily.TEXT, 800, 22f), rgb(0xFFB3BB))
            g.drawText("depuis 203.0.113.7 à 03 h", bx + 54f, top + 120f, FontSpec(FontFamily.TEXT, 500, 18f), rgb(0xFFB3BB))
        }
        // Terminal : qui m'attaque ?
        val tt = top + h + 22f
        val th = (a.b - tt).coerceAtMost(340f)
        if (th > 160f) {
            terminalFrame(g, a.l, tt, a.r, tt + th, "Qui m'attaque ?")
            val mf = FontSpec(FontFamily.MONO, 400, 22f)
            val cmd = listOf("$ grep \"Failed password\" /var/log/auth.log \\", "  | awk '{print \$(NF-3)}' | sort | uniq -c | sort -nr")
            val p = clamp01((t - 1000f) / 900f)
            var chars = (cmd.sumOf { it.length } * p).toInt()
            var yy = tt + 84f
            for (c in cmd) { g.drawText(TextLayout.fit(g, c.take(chars.coerceAtLeast(0)), mf, a.w - 50f, 1, minSize = 14f).lines.firstOrNull() ?: "", a.l + 24f, yy, mf, ok); chars -= c.length; yy += 32f }
            if (p >= 1f) {
                val out = listOf("   4812 203.0.113.7", "    934 198.51.100.23", "      2 10.0.0.25")
                for ((i, o) in out.withIndex()) {
                    if (yy > tt + th - 50f) break
                    if (t < 2000f + i * 150f) continue
                    if (i == 0) g.fillRect(a.l + 16f, yy - 24f, a.r - 16f, yy + 8f, danger.withAlpha(0.35f))
                    g.drawText(o, a.l + 24f, yy, mf, if (i == 0) rgb(0xFFFFFF) else ink)
                    yy += 32f
                }
                if (t > 2500f && yy < tt + th - 14f) g.drawText("# Windows : Observateur d'événements → ID 4625", a.l + 24f, yy + 4f, mf, muted)
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
        val tags = listOfNotNull(sb.hashtag?.let { if (it.startsWith("#")) it else "#$it" }, "#codjia").distinct().joinToString("  ")
        val tf = FontSpec(FontFamily.TEXT, 500, 26f)
        val tl = TextLayout.fit(g, tags, tf, Safe.WIDTH - 60f, 1, minSize = 16f)
        if (t > 800f) g.drawText(tl.lines.first(), 540f - g.measureText(tl.lines.first(), tl.font) / 2, cy + 310f, tl.font, muted)
    }

}
