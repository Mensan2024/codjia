// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Nettoyage du texte de l'utilisateur, avant toute analyse.
 * - longueur bornée (protection contre les textes géants) ;
 * - caractères de contrôle, émojis et symboles absents des polices supprimés (pas de « tofu » à l'écran) ;
 * - balises Markdown simples retirées ; espaces normalisées.
 */
object TextSanitizer {
    const val MAX_CHARS = 5_000
    private const val EXTRA = " .,;:!?'\"()[]{}-–—…«»’‘“”%€$£#@&+=/\\*_<>|~^°§ \n"

    fun isAllowed(c: Char): Boolean =
        (c.isLetterOrDigit() && c.code <= 0x017F) || c in EXTRA || c == 'Œ' || c == 'œ'

    fun clean(raw: String): String {
        val s = raw.take(MAX_CHARS)
            .replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ')
            .replace("→", " > ").replace("⇒", " > ").replace("•", "-").replace("·", "-")
            .replace(' ', ' ')
            .replace("**", "").replace("__", "").replace("`", "")
        val out = StringBuilder(s.length)
        for (c in s) if (isAllowed(c)) out.append(c)
        return out.toString().lines().joinToString("\n") { line ->
            line.replace(Regex("^#{1,6}\\s+"), "").replace(Regex(" {2,}"), " ").trim()
        }.replace(Regex("\n{3,}"), "\n\n").trim()
    }
}

/** Ce que le planificateur a compris du texte (utile pour l'interface et les tests). */
data class TextAnalysis(
    val title: String,
    val number: String?,
    val intro: List<String>,
    val lists: List<Pair<String?, List<String>>>,
    val bad: List<String>,
    val good: List<String>,
    val conclusion: String?,
    val closing: String?,
    val hashtags: List<String>,
    val domain: Domain,
    /** Toutes les parties du texte, dans l'ordre de l'auteur (rien n'est perdu). */
    val sections: List<TextSection> = emptyList(),
)

/** Une partie du texte : un en-tête facultatif (« Comment l'éviter »), des phrases d'explication et/ou une liste. */
data class TextSection(val header: String?, val prose: List<String>, val items: List<String>)

/**
 * Transforme un texte libre (astuce, post LinkedIn, notes de cours) en storyboard prêt à habiller.
 * Aucune IA : des règles explicables, testées sur de vrais textes. La graine fait varier la mise en scène :
 * ordre de certaines scènes, variantes de compositions, formulations des titres de section.
 */
object StoryboardPlanner {
    /** Durée cible : selon le texte, de 30 s à 59 s au plus (58 s visés pour garder une marge). */
    const val MIN_MS = 30_000
    const val SOFT_MAX_MS = 58_000
    private const val NB = ' '

    private val TITLE_RE = Regex("^(?:astuce|tip|conseil)\\b[^\\d\\n]{0,40}?0*(\\d{1,4})\\s*[—–:\\-]+\\s*(.+)$", RegexOption.IGNORE_CASE)
    /** « Astuce santé — … », « English tip — … » : préfixe sans numéro. */
    private val PREFIX_RE = Regex("^(?:astuce|tip|english tip|conseil|dev tip)\\b[^—–:\\n]{0,30}\\s[—–:]\\s*(.+)$", RegexOption.IGNORE_CASE)
    /** « Comment l'éviter : texte… » sur une seule ligne : en-tête de partie suivi du contenu. */
    private val SECTION_RE = Regex("^((?:comment|pourquoi|que faire|quoi faire|les bons? réflexes?|le bon réflexe|bons? réflexes?|conseils?|astuces?|actions?|à faire|a faire|à éviter|a eviter|signes?|exemples?|attention|important|risques?|conséquences?|consequences?|how to|why|what to do|tips?)[^:]{0,45})\\s:\\s*(.{3,})$", RegexOption.IGNORE_CASE)
    private val BULLET_RE = Regex("^(?:[-*–+]|\\d{1,2}[.)])\\s+(.+)$")
    private val CONCLUSION_RE = Regex("^(?:à retenir|a retenir|en résumé|en resume|conclusion|retenez|retiens|l'essentiel|to remember|remember|key takeaway|takeaway)\\s*[:!]?\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val HASHTAG_RE = Regex("#([\\p{L}0-9_]{2,40})")
    private val BAD_MARKERS = listOf("❌", "✖", "🚫", "⛔", "✗")
    private val GOOD_MARKERS = listOf("✅", "✔", "☑", "✓", "👍")

    fun analyze(raw: String): TextAnalysis {
        require(raw.isNotBlank()) { "Le texte est vide" }
        // Les marqueurs ❌/✅ portent du sens : on les lit AVANT de retirer les émojis.
        val bad = ArrayList<String>()
        val good = ArrayList<String>()
        for (line in raw.take(TextSanitizer.MAX_CHARS).lines()) {
            val l = line.trim()
            when {
                BAD_MARKERS.any { l.startsWith(it) } -> bad += TextSanitizer.clean(l).trim('-', ' ')
                GOOD_MARKERS.any { l.startsWith(it) } -> good += TextSanitizer.clean(l).trim('-', ' ')
            }
        }
        val text = TextSanitizer.clean(raw)
        val hashtags = HASHTAG_RE.findAll(text).map { "#" + it.groupValues[1] }.distinct().toList()
        val lines = text.lines().map { HASHTAG_RE.replace(it, "").trim() }.filter { it.isNotEmpty() && it.any(Char::isLetterOrDigit) }
        require(lines.isNotEmpty()) { "Le texte ne contient rien d'exploitable" }

        // Titre.
        var number: String? = null
        var title: String
        val first = lines.first()
        val m = TITLE_RE.find(first)
        var rest = lines.drop(1)
        val pm = PREFIX_RE.find(first)
        if (m != null) {
            number = m.groupValues[1]
            title = m.groupValues[2]
        } else if (pm != null) {
            title = pm.groupValues[1]
        } else if (first.length <= 90) {
            title = first
        } else {
            title = sentences(first).first()
            rest = listOf(first.removePrefix(title).trim()).filter { it.isNotEmpty() } + rest
        }
        title = title.trim().trimEnd('.', ':').trim()

        // Sections dans l'ordre de l'auteur : en-têtes « … : » (seuls sur leur ligne ou suivis du texte),
        // puces, lignes courtes, paragraphes. Rien n'est jeté : un paragraphe après une liste devient une
        // explication à sa place, et la partie « À retenir » garde toutes ses lignes.
        class Raw(val header: String?) { val entries = ArrayList<Pair<Boolean, String>>() }
        val raws = ArrayList<Raw>()
        var cur: Raw? = null
        val conclusionParts = ArrayList<String>()
        var inConclusion = false
        for (line0 in rest) {
            var line = line0
            val c = CONCLUSION_RE.find(line)
            if (c != null) { inConclusion = true; cur = null; c.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { conclusionParts += it }; continue }
            if (bad.any { it == line } || good.any { it == line }) continue
            if (line.endsWith(":") && line.length <= 70 && BULLET_RE.find(line) == null) {
                inConclusion = false; cur = Raw(line.trimEnd(':').trim()); raws += cur; continue
            }
            val sp = SECTION_RE.find(line)
            if (sp != null && BULLET_RE.find(line) == null) {
                inConclusion = false; cur = Raw(sp.groupValues[1].trim()); raws += cur
                line = sp.groupValues[2].trim().replaceFirstChar { it.uppercase() }
            }
            val b = BULLET_RE.find(line)
            if (inConclusion) { conclusionParts += (b?.groupValues?.get(1) ?: line).trim(); continue }
            if (cur == null) { cur = Raw(null); raws += cur }
            cur.entries += (b != null) to (b?.groupValues?.get(1) ?: line).trim()
        }
        fun isShort(l: String) = l.length <= 150 && sentences(l).size <= 2
        val sections = ArrayList<TextSection>()
        for (rw in raws) {
            if (rw.entries.isEmpty()) continue
            val texts = rw.entries.map { it.second }
            val section = when {
                rw.header != null && looksExplanatory(rw.header) -> TextSection(rw.header, texts.flatMap { sentences(it) }, emptyList())
                rw.entries.any { it.first } -> {
                    // Puces + lignes courtes = la liste ; les paragraphes longs restent des explications.
                    val items = rw.entries.filter { it.first || isShort(it.second) }.map { it.second }
                    val prose = rw.entries.filter { !it.first && !isShort(it.second) }.flatMap { sentences(it.second) }
                    TextSection(rw.header, prose, items)
                }
                rw.header != null && texts.size >= 2 && texts.all(::isShort) -> TextSection(rw.header, emptyList(), texts)
                else -> TextSection(rw.header, texts.flatMap { sentences(it) }, emptyList())
            }
            // Une liste d'un seul élément est en fait une explication.
            sections += if (section.items.size == 1) TextSection(section.header, section.prose + sentences(section.items.first()), emptyList()) else section
        }
        val intro = sections.flatMap { it.prose }
        val realLists = sections.filter { it.items.isNotEmpty() }.map { it.header to it.items }
        val conclusion = conclusionParts.takeIf { it.isNotEmpty() }?.joinToString(" ") { p -> p.trim().let { if (it.endsWith(".") || it.endsWith("!") || it.endsWith("?")) it else "$it." } }
        val closing: String? = null
        val domain = DomainDetector.detect(text)
        return TextAnalysis(title, number, intro, realLists, bad, good, conclusion, closing, hashtags, domain, sections)
    }

    private fun looksExplanatory(header: String): Boolean {
        val h = DomainDetector.normalize(header)
        return listOf("apprentissage", "definition", "principe", "explication", "contexte", "pourquoi", "c'est quoi", "qu'est-ce").any { h.contains(it) }
    }

    /** Découpage en phrases (garde les abréviations courantes et les nombres décimaux). */
    fun sentences(p: String): List<String> {
        val out = ArrayList<String>()
        var start = 0
        var i = 0
        while (i < p.length) {
            val c = p[i]
            if ((c == '.' || c == '!' || c == '?') && (i + 1 == p.length || p[i + 1] == ' ' || p[i + 1] == ' ')) {
                val prevWord = p.substring(start, i).substringAfterLast(' ')
                val isAbbrev = prevWord.lowercase() in setOf("ex", "etc", "cf", "env", "p", "vs", "mr", "dr", "n°")
                if (!isAbbrev) {
                    var j = i + 1
                    while (j < p.length && (p[j] == '!' || p[j] == '?')) j++
                    out += p.substring(start, j).trim()
                    start = j
                    i = j
                    continue
                }
            }
            i++
        }
        if (start < p.length) p.substring(start).trim().takeIf { it.isNotEmpty() }?.let { out += it }
        return out
    }

    /** Typographie française : espace insécable devant « : ; ! ? » et à l'intérieur des guillemets. */
    fun frenchTypo(s: String): String = s
        .replace(Regex("\\s+([:;!?])")) { "$NB${it.groupValues[1]}" }
        .replace(Regex("(?<=[\\p{L}0-9)»])([;!?])")) { "$NB${it.groupValues[1]}" }
        .replace("« ", "«$NB").replace(" »", "$NB»")

    /** Commandes et mots-clés techniques à ne jamais mettre en majuscule (« git pull », « df -h »…). */
    private val COMMANDS = setOf("git", "top", "df", "free", "sudo", "apt", "ls", "cd", "grep", "journalctl", "systemctl", "ping",
        "ssh", "npm", "npx", "pip", "docker", "kubectl", "vlan", "show", "interface", "switchport", "ipconfig", "ifconfig", "ip",
        "traceroute", "tracert", "nslookup", "netstat", "chmod", "chown", "cat", "nano", "vim", "curl", "wget", "python", "node", "java")

    private fun cap(s: String): String {
        val first = s.substringBefore(' ').lowercase()
        if (first in COMMANDS && s.startsWith(first)) return s
        return s.replaceFirstChar { it.uppercase() }
    }

    private val SMALL_WORDS = setOf("à", "a", "se", "de", "des", "du", "le", "la", "les", "en", "et", "ou", "un", "une", "pour", "sur",
        "avec", "au", "aux", "dans", "par", "the", "to", "of", "and", "a", "an", "in", "on")

    /** Coupe proprement : jamais de petit mot (« à », « de »…) en fin de ligne. */
    private fun trimWords(words: List<String>): String {
        var w = words
        while (w.size > 1 && w.last().lowercase().trimEnd('\'', '’') in SMALL_WORDS) w = w.dropLast(1)
        return w.joinToString(" ").trimEnd(',', ';', ':', ' ')
    }

    /** Phrase très longue : coupée aux virgules en morceaux d'au plus ~240 caractères (rien n'est retiré). */
    private fun splitLong(s: String): List<String> {
        val out = ArrayList<String>(); var cur = StringBuilder()
        for (part in s.split(Regex("(?<=,) "))) {
            if (cur.isNotEmpty() && cur.length + part.length + 1 > 240) { out += cur.toString().trim(); cur = StringBuilder() }
            cur.append(part).append(' ')
        }
        if (cur.isNotBlank()) out += cur.toString().trim()
        return out
    }

    private fun shorten(s: String, maxChars: Int): String {
        if (s.length <= maxChars) return s
        val cut = s.take(maxChars).substringBeforeLast(' ')
        return cut.trimEnd(',', ';', ':', ' ') + "…"
    }

    /** Élément de liste → « libellé : détail » pour une hiérarchie visuelle claire. */
    private fun item(raw: String): String {
        val s = raw.trim().trimEnd('.', ';').trim()
        if (s.contains(" : ")) return s
        if (s.length <= 52) return cap(s)
        for (sep in listOf(" (", ", ", " pour ", " afin ", " avec ", " sur ", " lorsque ", " quand ")) {
            val i = s.indexOf(sep)
            if (sep == " (" && !s.endsWith(")")) continue
            if (i in 14..52 && s.length - i >= 18) {
                val label = s.substring(0, i).trim()
                val detail = s.substring(i + if (sep == " (") 2 else sep.length).trim().trimEnd(')')
                return cap(label) + " : " + (if (sep.trim() in listOf("pour", "afin", "avec", "sur", "lorsque", "quand")) sep.trim() + " " else "") + detail
            }
        }
        return cap(s) // jamais tronqué : tout le texte de l'auteur est montré
    }

    /** L'idée principale d'un élément de liste, en peu de mots (pour le récapitulatif). */
    fun essence(raw: String): String {
        var s = item(raw).substringBefore(" : ").trim()
        for (c in listOf(" lorsque ", " lorsqu", " quand ", " si ", " pour ", " afin ", " sur ", " avec ", ", ", " (")) {
            val i = s.indexOf(c)
            if (i >= 12) { s = s.substring(0, i); break }
        }
        if (s.length > 44) {
            val words = s.split(' ')
            var n = words.size
            while (n > 2 && words.take(n).joinToString(" ").length > 44) n--
            s = trimWords(words.take(n))
        }
        return s.trimEnd(',', ';', ':', ' ')
    }

    /** Met en valeur 1 ou 2 mots du titre : un nombre, sinon un terme du domaine, sinon le mot le plus long. */
    fun emphasize(title: String, domain: Domain): String {
        if (title.contains('*')) return title
        val words = title.split(' ')
        val num = words.indexOfFirst { w -> w.any(Char::isDigit) }
        if (num >= 0) {
            val end = min(words.size - 1, num + 1)
            return words.mapIndexed { i, w -> when (i) { num -> "*$w"; end -> "$w*"; else -> w } }
                .let { if (num == end) words.mapIndexed { i, w -> if (i == num) "*$w*" else w } else it }.joinToString(" ")
        }
        val kw = words.indexOfFirst { DomainDetector.isKeyword(it, domain) }
        if (kw >= 0) return words.mapIndexed { i, w ->
            if (i != kw) w else { val core = w.trimEnd(',', '.', '!', '?', ':', '\u00A0'); "*$core*" + w.substring(core.length) }
        }.joinToString(" ")
        val stop = setOf("avec", "dans", "pour", "plus", "sans", "votre", "notre", "leurs", "cette", "comment", "toujours", "jamais", "simplement", "vraiment")
        val idx = words.withIndex().filter { (_, w) -> w.trim(',', '.', '!', '?', ':', ' ').length >= 5 && DomainDetector.normalize(w) !in stop }
            .maxByOrNull { (i, w) -> w.length + (if (i > 0) 2 else 0) }?.index ?: return title
        return words.mapIndexed { i, w ->
            if (i != idx) w else {
                val core = w.trimEnd(',', '.', '!', '?', ':', ' ')
                "*$core*" + w.substring(core.length)
            }
        }.joinToString(" ")
    }

    private fun readingMs(vararg texts: String?): Int {
        val words = texts.filterNotNull().sumOf { t -> t.split(' ').count { it.isNotBlank() } }
        return (words / 2.7 * 1000).toInt()
    }

    private val ILLUSTRATION = mapOf(
        Domain.RESEAUX to IllustrationId.NETWORK_TOPOLOGY, Domain.SYSTEMES to IllustrationId.OS_TERMINAL,
        Domain.CYBER to IllustrationId.SHIELD_CHECK, Domain.IA to IllustrationId.NEURAL_NET,
        Domain.FULLSTACK to IllustrationId.CODE_WINDOW, Domain.SANTE to IllustrationId.HEART_BEAT,
        Domain.MARKETING to IllustrationId.CHART_GROWTH, Domain.ANGLAIS to IllustrationId.SPEECH_BUBBLES,
        Domain.GENERAL to IllustrationId.CHECK_CIRCLE,
    )
    private val ICONS = mapOf(
        Domain.RESEAUX to listOf(IconId.ROUTER, IconId.WIFI, IconId.GLOBE), Domain.SYSTEMES to listOf(IconId.TERMINAL, IconId.CPU),
        Domain.CYBER to listOf(IconId.SHIELD, IconId.LOCK, IconId.KEY), Domain.IA to listOf(IconId.BRAIN, IconId.BULB),
        Domain.FULLSTACK to listOf(IconId.CODE, IconId.TERMINAL), Domain.SANTE to listOf(IconId.HEART, IconId.MOON, IconId.DROP),
        Domain.MARKETING to listOf(IconId.MEGAPHONE, IconId.TARGET), Domain.ANGLAIS to listOf(IconId.CHAT, IconId.BOOK),
        Domain.GENERAL to listOf(IconId.BULB, IconId.BOOK),
    )

    fun plan(raw: String, seed: Long = 0, templateId: String? = null): Storyboard {
        val a = analyze(raw)
        val r = Rand.of(seed, 0x6C1A)
        fun pick(options: List<String>) = if (seed == 0L) options.first() else options[(r.nextInt() ushr 1) % options.size]
        fun variant() = if (seed == 0L) 0 else (r.nextInt() ushr 1) % 2
        val d = a.domain
        val english = d == Domain.ANGLAIS && DomainDetector.normalize(a.title).let { " the " in " $it " || " you " in " $it " || " your " in " $it " }
        val norm = DomainDetector.normalize(raw)
        val icons = ICONS.getValue(d)
        val heroIllustration = if (d == Domain.CYBER && listOf("mail", "phishing", "hameconn").any { norm.contains(it) }) IllustrationId.PHISHING_MAIL
            else ILLUSTRATION.getValue(d)

        val scenes = ArrayList<Scene>()
        // 1. Couverture : la première phrase d'introduction sert de sous-titre.
        val firstSec = a.sections.firstOrNull()
        val introSec = firstSec?.takeIf { it.header == null || looksExplanatory(it.header) }
        val heroSub = introSec?.prose?.firstOrNull()?.takeIf { it.length <= 130 && !it.endsWith(":") }
        val kicker = a.number?.let { n -> "${d.kicker} N°${n.padStart(3, '0')}" } ?: d.kicker
        scenes += Scene(id = "s-hero", layout = Layout.HERO, durationMs = 1, kicker = kicker,
            title = frenchTypo(emphasize(cap(a.title), d)), subtitle = heroSub?.let(::frenchTypo), illustration = heroIllustration,
            icon = icons.first(), variant = variant())

        // 2. Tout le texte, dans l'ordre de l'auteur : chaque partie donne ses scènes d'explication
        // (phrases regroupées par ~240 caractères, jamais coupées) puis ses étapes (5 au plus par scène).
        // Une partie « Comment l'éviter » ou « Comment se protéger » a donc ses propres scènes, avec ce titre.
        val main = a.lists.maxByOrNull { it.second.size }
        val stepScenes = ArrayList<Scene>()
        var defCount = 0
        for ((si, sec) in a.sections.withIndex()) {
            val prose = if (sec === introSec && heroSub != null) sec.prose.drop(1) else sec.prose
            val headed = sec.header?.takeIf { !looksExplanatory(it) && it.length in 3..48 }?.let { cap(it.trimEnd(':').trim()) }
            val groups = ArrayList<List<String>>()
            var curG = ArrayList<String>()
            val pieces = prose.flatMap { sn -> if (sn.length <= 240) listOf(sn) else splitLong(sn) }
            for (sentence in pieces) {
                if (curG.isNotEmpty() && curG.sumOf { it.length + 1 } + sentence.length > 240) { groups += curG; curG = ArrayList() }
                curG += sentence
            }
            if (curG.isNotEmpty()) groups += curG
            for ((gi, picked) in groups.withIndex()) {
                val body = picked.joinToString(" ")
                val defTitle = when {
                    headed != null -> emphasize(headed, d) + if (groups.size > 1) " (${gi + 1}/${groups.size})" else ""
                    defCount > 0 -> if (english) "And also" else pick(listOf("Et *aussi*", "À *savoir*", "*Concrètement*"))
                    english -> pick(listOf("The idea", "In short", "Why it matters"))
                    else -> pick(listOf("Le *principe*", "Ce qu’il faut *savoir*", "*Pourquoi* c’est important$NB?", "*Comprendre* en 10 secondes"))
                }
                scenes += Scene(id = if (defCount == 0) "s-def" else "s-def$si-$gi", layout = Layout.DEFINITION, durationMs = 1,
                    kicker = when {
                        english -> "DEFINITION"
                        headed != null -> pick(listOf("SENSIBILISATION", "LE BON RÉFLEXE", "À SAVOIR"))
                        else -> pick(listOf("DÉFINITION", "LE SAVIEZ-VOUS$NB?", "EN BREF"))
                    },
                    icon = icons[1 % icons.size], shape = listOf(ShapeKind.HEXAGON, ShapeKind.RING, ShapeKind.DIAMOND)[(r.nextInt() ushr 1) % 3],
                    title = frenchTypo(defTitle), body = frenchTypo(body), variant = variant())
                defCount++
            }
            val items = sec.items.map { frenchTypo(item(it)) }
            if (items.size >= 2) {
                val nChunks = (items.size + 4) / 5
                val per = (items.size + nChunks - 1) / nChunks
                val chunks = items.chunked(per).let { ch -> if (ch.size > 1 && ch.last().size < 2) ch.dropLast(2) + listOf(ch[ch.size - 2] + ch.last()) else ch }
                for ((k, chunk) in chunks.withIndex()) {
                    val t = when {
                        headed != null && headed.length in 4..42 -> headed + if (chunks.size > 1) " (${k + 1}/${chunks.size})" else ""
                        english -> "Step by step"
                        else -> pick(listOf("Les bons réflexes", "Comment faire$NB?", "Étape par étape", "Passe à l’action"))
                    }
                    val sc = Scene(id = if (stepScenes.isEmpty()) "s-steps$k" else "s-steps$si-$k", layout = Layout.STEPS, durationMs = 1, icon = icons.last(),
                        title = frenchTypo(emphasize(t, d)), items = chunk, variant = variant())
                    stepScenes += sc
                    scenes += sc
                }
            }
        }

        // 4. Comparaison explicite (❌ / ✅).
        val compare = if (a.bad.isNotEmpty() && a.good.isNotEmpty()) Scene(id = "s-cmp", layout = Layout.COMPARE, durationMs = 1,
            kicker = if (english) "WRONG / RIGHT" else "À ÉVITER / À FAIRE", title = if (english) "*Wrong* or *right*?" else "*Vrai* ou *faux*$NB?",
            compare = Compare(if (english) "Avoid" else "À éviter", listOf(Span(a.bad.first(), true)),
                if (english) "Do" else "À faire", listOf(Span(a.good.first()))), variant = variant()) else null
        // Les autres lignes ❌ / ✅ (au-delà de la première paire) ne sont pas perdues : elles forment leur propre liste.
        val extraBad = if (compare != null) a.bad.drop(1) else a.bad
        val extraGood = if (compare != null) a.good.drop(1) else a.good
        for ((label, list) in listOf((if (english) "Avoid" else "À *éviter*") to extraBad, (if (english) "Do" else "À *faire*") to extraGood)) {
            if (list.isEmpty()) continue
            scenes += if (list.size >= 2) Scene(id = "s-x-${label.hashCode()}", layout = Layout.STEPS, durationMs = 1, icon = icons.last(),
                title = frenchTypo(label), items = list.map { frenchTypo(item(it)) }, variant = variant())
            else Scene(id = "s-x-${label.hashCode()}", layout = Layout.DEFINITION, durationMs = 1, kicker = if (english) "TIP" else "LE BON RÉFLEXE",
                icon = icons[1 % icons.size], title = frenchTypo(label), body = frenchTypo(list.first()), variant = variant())
        }

        // 5. Chiffre clé : le premier nombre accompagné d'une unité parlante.
        val stat = findStat(a)?.let { (value, sentence) ->
            Scene(id = "s-stat", layout = Layout.STAT, durationMs = 1, statValue = value,
                title = frenchTypo(emphasize(shorten(sentence.trimEnd('.'), 60), d)), variant = variant())
        }

        // 6. À retenir.
        val takeaways: List<String> = a.conclusion?.let { c ->
            val sents = sentences(c).map { it.trimEnd('.') }
            if (sents.size >= 2) sents.take(3)
            else {
                // Une seule phrase : on la découpe en idées si chaque morceau reste court.
                val parts = c.trimEnd('.').split(Regex(",\\s*(?:et\\s+)?|\\s+et\\s+|;\\s*")).map { it.trim() }.filter { it.length >= 3 }
                if (parts.size >= 2 && parts.all { it.length <= 75 }) parts.take(4) else listOf(c.trimEnd('.'))
            }
        }?.takeIf { it.size >= 2 }
            ?: a.conclusion?.let { listOf(it.trimEnd('.')) }
            // Pas de conclusion écrite par l'auteur : pas de résumé inventé, la vidéo se termine sur l'appel à s'abonner.
            ?: emptyList()
        val outro = Scene(id = "s-end", layout = Layout.CHECKLIST, durationMs = 1,
            illustration = if (d == Domain.CYBER) IllustrationId.SHIELD_CHECK else IllustrationId.CHECK_CIRCLE,
            title = if (takeaways.isEmpty()) (if (english) "*Follow* for more" else "*Abonne-toi* pour la suite")
                else if (english) "*Remember*" else pick(listOf("*À retenir*", "*L’essentiel*", "*En résumé*")),
            items = takeaways.map { cap(frenchTypo(it)) },
            // Pas de doublon : la conclusion n'est en sous-titre que si elle n'est pas déjà la liste.
            subtitle = (a.closing ?: a.conclusion?.takeIf { takeaways.isEmpty() })?.let { frenchTypo(it) },
            body = if (english) pick(listOf("Share it with a friend", "Save this tip")) else pick(listOf("Partage à quelqu’un qui en a besoin", "Enregistre cette astuce", "Partage cette astuce")),
            exit = AnimSpec(AnimType.FADE_OUT, 600, easing = Easing.EASE_IN), variant = variant())

        // Ordre : la graine peut avancer le chiffre clé ou la comparaison, pour varier le récit.
        val middle = ArrayList<Scene>()
        val extras = listOfNotNull(compare, stat).let { if (seed != 0L && r.next() < 0.5f) it.reversed() else it }
        if (seed != 0L && extras.isNotEmpty() && r.next() < 0.35f) middle.add(0, extras.first()) else middle += extras.take(1)
        middle += extras.drop(1).filter { it !in middle }
        scenes += middle
        scenes += outro

        // Pas assez de matière : au moins une liste pour ne pas faire une vidéo vide.
        if (scenes.size < 3 && a.intro.size >= 2) {
            scenes.add(scenes.size, Scene(id = "s-steps-auto", layout = Layout.STEPS, durationMs = 1, icon = icons.last(),
                title = frenchTypo(if (english) "Key points" else "Les points *clés*"),
                items = a.intro.take(4).map { frenchTypo(item(it)) }, variant = variant()))
        }

        return fit(Storyboard(id = "gen-${seed.toString(16)}", title = Marked.strip(a.title), templateId = templateId ?: Templates.recommendFor(d).id,
            scenes = scenes.map { withDuration(it) }, hashtag = a.hashtags.firstOrNull() ?: "#codjia", seed = seed))
    }

    private fun findStat(a: TextAnalysis): Pair<String, String>? {
        val unit = "%|secondes?|minutes?|heures?|jours?|semaines?|mois|ans|caractères|fois|Go|Mo|bits?|ports?|étapes|verres?|pas|h\\b"
        val re = Regex("(\\d{1,4}(?:[.,]\\d+)?)\\s?($unit)(?![\\p{L}])", RegexOption.IGNORE_CASE)
        val sources = a.intro + a.lists.flatMap { it.second } + listOfNotNull(a.conclusion)
        for (s in sources) {
            if (s.length > 90) continue // une phrase trop longue ferait un mauvais titre de chiffre clé
            val m = re.find(s) ?: continue
            val v = m.groupValues[1] + NB + m.groupValues[2]
            return v to s.replace(m.value, m.value).replace(Regex("\\s*\\([^)]*\\)"), "")
        }
        // Numéro de port entre parenthèses (ex. « port RDP (3389) »).
        for (s in sources) {
            if (s.length > 90) continue
            val m = Regex("\\((\\d{2,5})\\)").find(s) ?: continue
            return m.groupValues[1] to s.replace(m.value, "").replace(Regex("\\s{2,}"), " ")
        }
        return null
    }

    private fun withDuration(s: Scene): Scene {
        val read = readingMs(Marked.strip(s.title), s.subtitle, s.body, s.kicker, *s.items.toTypedArray())
        val (base, lo, hi) = when (s.layout) {
            Layout.HERO -> Triple(2600, 4600, 6200)
            Layout.DEFINITION -> Triple(2200, 5200, 14_000)
            // Chaque élément doit avoir le temps d'être lu : jusqu'à ~3,4 s par élément.
            Layout.STEPS -> Triple(2400, 5600, (2_000 + 3_400 * s.items.size).coerceIn(9_000, 19_000))
            Layout.COMPARE -> Triple(3200, 5600, 7200)
            Layout.STAT -> Triple(2600, 4400, 6000)
            Layout.CHECKLIST -> if (s.items.isEmpty()) Triple(3600, 3600, 3600) else Triple(2400, 5200, 9000)
            Layout.SCREEN -> return s
        }
        return s.copy(durationMs = (base + read).coerceIn(lo, hi))
    }

    /** Durée selon le texte : 30 à 59 s. Au-delà, on retire les scènes facultatives puis on resserre. */
    private fun fit(sb: Storyboard): Storyboard {
        val overlap = 600
        fun total(sc: List<Scene>) = sc.sumOf { it.durationMs } - overlap * (sc.size - 1)
        var scenes = sb.scenes
        // Le texte de l'auteur n'est jamais retiré : seul le chiffre clé (déjà présent dans le texte) peut l'être.
        val dropOrder = listOf("s-stat")
        // Durée selon le contenu, entre 30 et 59 s. Au-delà de 58 s, on retire les scènes facultatives
        // puis on resserre ; en deçà de 30 s, on n'étire que modérément (×1,3 au plus) pour garder du rythme.
        for (id in dropOrder) { if (total(scenes) <= SOFT_MAX_MS) break; scenes = scenes.filter { it.id != id } }
        val t = total(scenes)
        if (t > SOFT_MAX_MS) {
            val k = (SOFT_MAX_MS.toDouble() + overlap * (scenes.size - 1)) / scenes.sumOf { it.durationMs }
            scenes = scenes.map { it.copy(durationMs = max(3_200, (it.durationMs * k).toInt())) }
            // Texte très long : on resserre encore (2 s au moins par scène) plutôt que de couper le message.
            if (total(scenes) > SOFT_MAX_MS) {
                val k2 = (SOFT_MAX_MS.toDouble() + overlap * (scenes.size - 1)) / scenes.sumOf { it.durationMs }
                scenes = scenes.map { it.copy(durationMs = max(2_000, (it.durationMs * k2).toInt())) }
            }
        } else if (t < MIN_MS) {
            val k = ((MIN_MS + 500.0 + overlap * (scenes.size - 1)) / scenes.sumOf { it.durationMs }).coerceAtMost(1.3)
            scenes = scenes.map { it.copy(durationMs = (it.durationMs * k).toInt()) }
        }
        return sb.copy(scenes = scenes)
    }
}

/**
 * Directeur artistique : pour un texte et une graine, propose le storyboard, le template et la musique.
 * Chaque nouvelle génération (nouvelle graine) donne une vidéo différente ; la même graine redonne la même.
 */
object CreativeDirector {
    data class Proposal(val storyboard: Storyboard, val template: Template, val musicId: String?, val domain: Domain) {
        val look: Look get() = storyboard.look
    }

    private val MUSIC = mapOf(
        Domain.RESEAUX to listOf("pulse", "synthwave", "cinematic", "horizon"), Domain.SYSTEMES to listOf("pulse", "synthwave", "lofi", "trap"),
        Domain.CYBER to listOf("trap", "synthwave", "pulse", "cinematic", "momentum"), Domain.IA to listOf("future", "synthwave", "elan", "cinematic"),
        Domain.FULLSTACK to listOf("lofi", "synthwave", "trap", "future"), Domain.SANTE to listOf("aube", "canopee", "sahel", "afro"),
        Domain.MARKETING to listOf("future", "afro", "amapiano", "momentum"), Domain.ANGLAIS to listOf("amapiano", "lofi", "afro", "elan"),
        Domain.GENERAL to listOf("afro", "future", "cinematic", "elan"),
    )

    /**
     * @param avoidTemplates templates des dernières générations : on les évite si possible, pour ne pas répéter le même format.
     * @param avoidLooks formats des dernières générations : chaque nouvelle génération change de forme.
     */
    fun propose(
        text: String, seed: Long, avoidTemplates: Collection<String> = emptyList(), forcedTemplate: Template? = null,
        avoidLooks: Collection<Look> = emptyList(), forcedLook: Look? = null,
    ): Proposal {
        val domain = DomainDetector.detect(TextSanitizer.clean(text))
        val r = Rand.of(seed, 0x1D3A)
        val top = DomainDetector.recommend(text).take(3)
        val template = forcedTemplate ?: run {
            val fresh = top.filter { it.id !in avoidTemplates }.ifEmpty { top }
            fresh[(r.nextInt() ushr 1) % fresh.size]
        }
        val musics = MUSIC.getValue(domain)
        val music = musics[(r.nextInt() ushr 1) % musics.size]
        val look = forcedLook ?: run {
            val allowed = Looks.forDomain(domain)
            val fresh = allowed.filter { it !in avoidLooks }.ifEmpty { allowed }
            // Le montage dynamique (tutoriel animé) revient deux fois plus souvent que les autres formats.
            val weighted = fresh + fresh.filter { it == Look.MONTAGE }
            weighted[(Rand.of(seed, 0x2C7F).nextInt() ushr 1) % weighted.size]
        }
        var sb = StoryboardPlanner.plan(text, seed, template.id).copy(look = look)
        Procedures.match(TextSanitizer.clean(text))?.let { topic -> sb = withDemo(sb, topic, text, seed) }
        return Proposal(sb, template, music, domain)
    }

    /**
     * Insère une démonstration à l'écran sans rien retirer du texte de l'auteur : couverture, explications,
     * démo pas à pas, puis toutes les étapes, comparaisons et la fin. Si c'est trop long, on resserre la démo,
     * puis on retire des écrans de démo (la démo illustre, le texte de l'auteur reste entier).
     */
    internal fun withDemo(sb: Storyboard, topic: Procedures.Topic, text: String, seed: Long): Storyboard {
        val demo = Procedures.scenes(Procedures.pick(topic, text, seed))
        val soft = StoryboardPlanner.SOFT_MAX_MS
        val hero = sb.scenes.firstOrNull { it.layout == Layout.HERO }?.let { it.copy(durationMs = it.durationMs.coerceAtMost(4800)) }
        val defs = sb.scenes.filter { it.layout == Layout.DEFINITION }
        val after = sb.scenes.filter { it.layout == Layout.STEPS || it.layout == Layout.COMPARE }
        val stat = sb.scenes.firstOrNull { it.layout == Layout.STAT }
        val end = sb.scenes.lastOrNull { it.layout == Layout.CHECKLIST }
        fun total(sc: List<Scene>) = sc.sumOf { it.durationMs } - sc.dropLast(1).count { it.layout != Layout.SCREEN } * 600
        fun build(d: List<Scene>, withStat: Boolean): List<Scene> {
            val shown = if (d.isEmpty()) d else d.dropLast(1) + d.last().copy(transitionOut = Transition(TransitionType.FADE, 500))
            return listOfNotNull(hero) + defs + shown + after + listOfNotNull(stat.takeIf { withStat }) + listOfNotNull(end)
        }
        var screens = demo
        var scenes = build(screens, true)
        if (total(scenes) > soft) scenes = build(screens, false)
        // Un peu court : léger étirement des écrans de démo (×1,25 au plus, pour garder du rythme).
        if (total(scenes) < StoryboardPlanner.MIN_MS) {
            val screenMs = screens.sumOf { it.durationMs }.coerceAtLeast(1)
            val k = (1.0 + (StoryboardPlanner.MIN_MS + 500 - total(scenes)).toDouble() / screenMs).coerceAtMost(1.25)
            screens = screens.map { it.copy(durationMs = (it.durationMs * k).toInt().coerceAtMost(9500)) }
            scenes = build(screens, true)
        }
        // Trop long : on resserre les écrans de démo, puis on en retire (en gardant au moins deux écrans).
        if (total(scenes) > soft) {
            val over = total(scenes) - (soft - 500)
            val screenMs = screens.sumOf { it.durationMs }.coerceAtLeast(1)
            val k = (1.0 - over.toDouble() / screenMs).coerceAtLeast(0.75)
            screens = screens.map { it.copy(durationMs = (it.durationMs * k).toInt().coerceAtLeast(3_200)) }
            scenes = build(screens, false)
        }
        while (total(scenes) > soft && screens.size > 2) { screens = screens.dropLast(1); scenes = build(screens, false) }
        if (total(scenes) > soft) {
            val k = (soft - 500.0) / total(scenes)
            scenes = scenes.map { it.copy(durationMs = (it.durationMs * k).toInt().coerceAtLeast(2000)) }
        }
        return sb.copy(scenes = scenes)
    }
}
