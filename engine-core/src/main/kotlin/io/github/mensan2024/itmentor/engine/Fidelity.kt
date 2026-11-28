package io.github.mensan2024.itmentor.engine

/**
 * Fidélité au texte de l'auteur : mesure, mot à mot, ce qui reste à l'écran et dans quel ordre.
 *
 * Règle de VIDEO24 : 100 % du texte de l'auteur, dans son ordre. Ici on le **mesure** au lieu de l'affirmer :
 *  - [presence] : part des mots de l'auteur qui apparaissent quelque part dans les scènes ;
 *  - [ordered] : part des mots de l'auteur retrouvés **dans le même ordre** (plus longue sous-suite commune) ;
 *  - [missing] : les mots absents (avec leur rang dans le texte) ; [reordered] : présents mais hors ordre.
 * Hors mesure : les lignes de hashtags (métadonnées, pas du contenu à lire) et les mentions ajoutées par l'application
 * (« Abonne-toi », « Partage… »), qui n'entrent pas dans le texte de l'auteur.
 */
object Fidelity {
    data class Report(val total: Int, val presentCount: Int, val orderedCount: Int, val missing: List<String>, val reordered: List<String>, val connectors: Int = 0) {
        val presence: Double get() = if (total == 0) 1.0 else presentCount.toDouble() / total
        val ordered: Double get() = if (total == 0) 1.0 else orderedCount.toDouble() / total
        val perfect: Boolean get() = orderedCount == total
        fun summary(): String = "mots de l'auteur : $total · présents ${pct(presence)} · dans l'ordre ${pct(ordered)}" +
            (if (connectors > 0) " · $connectors « et/ou » de liaison fondus en puces" else "") +
            (if (missing.isNotEmpty()) " · absents : ${missing.take(12).joinToString(" ")}" else "") +
            (if (reordered.isNotEmpty()) " · hors ordre : ${reordered.take(12).joinToString(" ")}" else "")
        private fun pct(x: Double) = "${Fmt.one(x * 100)} %"
    }

    /** Intitulé numéroté (« Astuce cybersécurité 030 — ») : l'application le refait en bandeau « ASTUCE CYBERSÉCURITÉ N°030 ». */
    private val NUMBERED = Regex("^(?:astuce|tip|conseil)\\b[^\\d\\n]{0,40}?0*(\\d{1,4})\\s*[—–:\\-]+\\s*", RegexOption.IGNORE_CASE)

    /** Même bandeau sans numéro (« Astuce santé — », « English tip — »). */
    private val PREFIXED = Regex("^(?:astuce|tip|english tip|conseil|dev tip)\\b[^—–:\\n]{0,30}\\s[—–:]\\s*", RegexOption.IGNORE_CASE)

    /** Mots du texte de l'auteur, hashtags exclus ; le bandeau « Astuce … N° » n'est compté que par son numéro. */
    fun authorWords(raw: String): List<String> {
        val lines = raw.lines().filterNot { l -> l.trim().startsWith("#") }.toMutableList()
        val i = lines.indexOfFirst { it.isNotBlank() }
        if (i >= 0) {
            val l = lines[i].trim().dropWhile { !it.isLetterOrDigit() }
            NUMBERED.find(l)?.let { m -> lines[i] = m.groupValues[1] + " " + l.removeRange(m.range) } ?: run { lines[i] = PREFIXED.find(l)?.let { m -> l.removeRange(m.range) } ?: l }
        }
        // Le numéro d'une liste ordonnée (« 1. Il te découvre ») est une numérotation, non un mot de l'auteur : la mise en page la refait.
        val marker = Regex("^\\s*\\d{1,2}[.)]\\s+")
        val body = lines.mapIndexed { k, l -> if (k > i) marker.replace(l, "") else l }
        return words(stripInlineTags(body.joinToString("\n")))
    }

    /**
     * Mots **de l'auteur** montrés par une scène, dans l'ordre de lecture. Les titres et bandeaux inventés par l'application
     * (« Et aussi », « EN BREF »…) n'en font pas partie : ils ne doivent pas faire croire qu'un mot de l'auteur est couvert.
     */
    fun sceneWords(s: Scene): List<String> {
        val head = if (s.layout == Layout.HERO) listOfNotNull(s.kicker?.split(Regex("\\D+"))?.filter { it.isNotBlank() }?.map { it.trimStart('0').ifEmpty { "0" } }?.joinToString(" ")?.takeIf { it.isNotBlank() }, Marked.strip(s.title))
            else listOfNotNull(Marked.strip(s.title).takeIf { s.authorTitle })
        return words((head + listOfNotNull(s.subtitle, s.body.takeIf { it.isNotBlank() && s.layout != Layout.CHECKLIST })
            + s.items + (s.compare?.let { listOf(it.badText(), it.goodText()) } ?: emptyList())).joinToString(" "))
    }

    fun measure(raw: String, scenes: List<Scene>): Report {
        val r = authorWords(raw)
        val shown = scenes.filter { it.layout != Layout.SCREEN && !it.isEpisodeOpener }.flatMap { sceneWords(it) }
        val bag = HashMap<String, Int>()
        for (w in shown) bag[w] = (bag[w] ?: 0) + 1
        var present = 0
        val missing = ArrayList<String>()
        for (w in r) { val n = bag[w] ?: 0; if (n > 0) { present++; bag[w] = n - 1 } else missing += w }
        val keep = lcs(r, shown)
        val reordered = ArrayList<String>()
        var j = 0
        for ((i, w) in r.withIndex()) { if (j < keep.size && keep[j] == i) j++ else if (w !in missing) reordered += w }
        // Un « et » / « ou » de liaison disparaît quand une phrase devient une liste à puces (« A, B, et C » → trois puces) :
        // ce n'est pas un mot de contenu perdu ; on le compte à part, sans le cacher.
        val tolerated = missing.count { it == "et" || it == "ou" }
        return Report(r.size, present + tolerated, keep.size + tolerated, missing.filter { it != "et" && it != "ou" }, reordered, tolerated)
    }

    /** Rangs (dans [a]) d'une plus longue sous-suite commune de [a] et [b]. */
    private fun lcs(a: List<String>, b: List<String>): List<Int> {
        val n = a.size; val m = b.size
        val t = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) t[i][j] = if (a[i] == b[j]) t[i + 1][j + 1] + 1 else maxOf(t[i + 1][j], t[i][j + 1])
        val out = ArrayList<Int>()
        var i = 0; var j = 0
        while (i < n && j < m) when {
            a[i] == b[j] -> { out += i; i++; j++ }
            t[i + 1][j] >= t[i][j + 1] -> i++
            else -> j++
        }
        return out
    }

    private fun stripInlineTags(s: String) = s.replace(Regex("#[\\p{L}\\p{N}_-]+"), " ")

    private fun words(s: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        for (c in s.replace('’', '\'').lowercase()) {
            if (c.isLetterOrDigit()) cur.append(c) else if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        // « 024 » et « 24 » sont le même numéro (le bandeau « N°024 » complète les zéros).
        return out.map { w -> if (w.all { it.isDigit() }) w.trimStart('0').ifEmpty { "0" } else w }
    }
}
