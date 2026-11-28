// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.max

/** Place d'un storyboard dans une série : « ÉPISODE 2/3 ». */
data class EpisodeInfo(val index: Int, val count: Int) {
    val label: String get() = "ÉPISODE $index/$count"
}

/** Carte d'ouverture ajoutée par l'application au début des épisodes 2, 3… : elle n'est pas du texte de l'auteur. */
val Scene.isEpisodeOpener: Boolean get() = id.startsWith("ep") && id.endsWith("-open")

/**
 * Texte long → plusieurs épisodes cohérents, au lieu d'une vidéo comprimée.
 *
 * Règle de VIDEO24 : 100 % du texte de l'auteur, dans son ordre, avec le temps de le lire. Quand il ne tient pas dans
 * [BUDGET_MS] (une marge sous les 59 s de la plateforme), on coupe **entre deux scènes** — jamais au milieu d'une scène,
 * jamais en raccourcissant une durée — en cherchant des coupes qui tombent là où le propos change :
 *  - pas de coupe entre deux morceaux d'une même partie (« Les erreurs (1/2) » → « (2/2) »), sauf si rien d'autre n'est possible ;
 *  - épisodes de durées voisines (somme des carrés des écarts à la moyenne minimale) ;
 *  - chaque épisode dure au moins [MIN_EPISODE_MS] (hors carte d'ouverture), et la conclusion n'est jamais seule.
 * Le nombre d'épisodes est le plus petit qui convienne.
 *
 * Les durées utilisées sont celles de **lecture à l'écran** ; avec une voix off, la durée d'une scène est au moins celle de la
 * phrase lue ([Narration.textOf], estimée à [SPEECH_WPS] mots par seconde, volontairement prudent).
 */
object Episodes {
    /** Durée maximale visée par épisode, voix et habillage compris (la plateforme accepte 59 s). */
    const val BUDGET_MS = 50_000
    const val MIN_EPISODE_MS = 15_000
    /** Carte d'ouverture des épisodes suivants. */
    const val OPENER_MS = 2_600
    /** Débit de parole prudent (≈ 150 mots/min). */
    const val SPEECH_WPS = 2.5
    private const val OVERLAP = 600

    /** Durée d'une scène à l'écran, voix comprise si [voice]. */
    fun effectiveMs(s: Scene, voice: Boolean): Int {
        if (!voice) return s.durationMs
        val words = Narration.textOf(s).split(' ').count { w -> w.any { it.isLetterOrDigit() } }
        val speech = (words / SPEECH_WPS * 1000).toInt()
        return max(s.durationMs, (Narration.LEAD_MS + speech + OVERLAP + Narration.GAP_MS).toInt())
    }

    /** Durée d'un groupe de scènes consécutives (chevauchement des transitions déduit). */
    fun spanMs(scenes: List<Scene>, voice: Boolean): Long =
        scenes.sumOf { effectiveMs(it, voice).toLong() } - OVERLAP.toLong() * (scenes.size - 1).coerceAtLeast(0)

    /** Pénalité d'une coupe juste avant la scène [next] : 0 = frontière naturelle, grand = couper un propos en deux. */
    private fun cutPenalty(prev: Scene, next: Scene): Double {
        val part = Regex("\\((\\d+)/(\\d+)\\)\\s*$").find(Marked.strip(next.title))
        return when {
            part != null && part.groupValues[1].toInt() > 1 -> 10.0
            next.layout == Layout.CHECKLIST -> 8.0
            prev.layout == Layout.DEFINITION && next.layout == Layout.DEFINITION && !next.authorTitle -> 3.0
            next.layout == Layout.STAT || next.layout == Layout.COMPARE -> 1.0
            else -> 0.0
        }
    }

    /**
     * Numéro d'épisode (à partir de 1) de chaque scène. Une seule valeur (1) quand tout tient dans [budgetMs].
     * @throws StoryboardException si une scène seule dépasse déjà le budget.
     */
    fun assign(scenes: List<Scene>, voice: Boolean, budgetMs: Int = BUDGET_MS): List<Int> {
        val n = scenes.size
        if (spanMs(scenes, voice) <= budgetMs) return List(n) { 1 }
        val big = scenes.firstOrNull { effectiveMs(it, voice) > budgetMs - OPENER_MS }
        if (big != null) throw StoryboardException("La scène « ${big.id} » dure à elle seule plus que le budget d'un épisode")
        val total = spanMs(scenes, voice)
        var count = ((total + budgetMs - 1) / budgetMs).toInt().coerceAtLeast(2)
        while (count <= n) {
            val cuts = solve(scenes, voice, budgetMs, count)
            if (cuts != null) return cuts
            count++
        }
        throw StoryboardException("Impossible de découper le texte en épisodes de ${budgetMs / 1000} s au plus")
    }

    /** Programmation dynamique : `dp[j][i]` = meilleur coût pour répartir les [i] premières scènes en [j] épisodes. */
    private fun solve(scenes: List<Scene>, voice: Boolean, budgetMs: Int, count: Int): List<Int>? {
        val n = scenes.size
        if (count > n) return null
        val mean = spanMs(scenes, voice).toDouble() / count
        fun cost(a: Int, b: Int, ep: Int): Double? {
            val group = scenes.subList(a, b)
            val ms = spanMs(group, voice) + if (ep > 1) OPENER_MS - OVERLAP else 0
            if (ms > budgetMs) return null
            if (count > 1 && (ms - if (ep > 1) OPENER_MS - OVERLAP else 0) < MIN_EPISODE_MS) return null
            val dev = (ms - mean) / 1000.0
            val cut = if (a > 0) cutPenalty(scenes[a - 1], scenes[a]) else 0.0
            return dev * dev + cut * 40.0
        }
        val inf = Double.MAX_VALUE / 4
        val dp = Array(count + 1) { DoubleArray(n + 1) { inf } }
        val from = Array(count + 1) { IntArray(n + 1) { -1 } }
        dp[0][0] = 0.0
        for (j in 1..count) for (i in j..n) for (a in (j - 1) until i) {
            if (dp[j - 1][a] >= inf) continue
            val c = cost(a, i, j) ?: continue
            if (dp[j - 1][a] + c < dp[j][i]) { dp[j][i] = dp[j - 1][a] + c; from[j][i] = a }
        }
        if (dp[count][n] >= inf) return null
        val out = IntArray(n)
        var i = n
        for (j in count downTo 1) { val a = from[j][i]; for (k in a until i) out[k] = j; i = a }
        return out.toList()
    }

    /** Carte d'ouverture d'un épisode suivant : le titre de la série et le numéro de l'épisode. */
    private fun opener(base: Storyboard, k: Int, count: Int): Scene {
        val hero = base.scenes.first()
        return Scene(id = "ep$k-open", layout = Layout.HERO, kicker = EpisodeInfo(k, count).label, title = hero.title, illustration = hero.illustration,
            icon = hero.icon, variant = hero.variant, durationMs = OPENER_MS, authorTitle = false)
    }

    /**
     * Les storyboards de la série : les scènes de chaque épisode (dans l'ordre du texte) précédées, à partir du 2ᵉ épisode, de leur carte d'ouverture.
     * Les visuels imposés ([Storyboard.pins]) sont transmis ; la carte d'ouverture reçoit son affiche comme n'importe quelle scène (plan « poster »).
     */
    fun assemble(base: Storyboard, assignment: List<Int>): List<Storyboard> {
        require(assignment.size == base.scenes.size) { "Une scène n'a pas d'épisode" }
        val count = assignment.max()
        if (count == 1) return listOf(base.copy(episode = null))
        return (1..count).map { k ->
            val own = base.scenes.filterIndexed { i, _ -> assignment[i] == k }
            val scenes = if (k == 1) own else listOf(opener(base, k, count)) + own
            val pins = base.pins.filterKeys { id -> own.any { it.id == id } }
            val reserved = base.pins.filterKeys { id -> own.none { it.id == id } }.values.filter { it != Pins.TEXT_ONLY }.toSet()
            base.copy(id = base.id + "-e$k", scenes = scenes, episode = EpisodeInfo(k, count), pins = pins, reserved = reserved)
        }
    }
}
