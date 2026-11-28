// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.min

/**
 * Partie 6.1 — Registre de scènes.
 *
 * Un [SceneKind] est une manière de MONTRER un concept (hameçonnage, pare-feu, tunnel de vente…). Il se décompose en
 * [Beat] : plans qui n'ont pas le même rôle dans le récit (le leurre, l'anatomie du message, le clic, la vérification…).
 * Le registre ne choisit pas « une animation disponible » : il note, pour chaque scène, la capacité de chaque plan à
 * représenter l'intention ([SceneIntent]) — concept, rôle, objets/acteurs/actions cités, objectif pédagogique — puis
 * attribue les plans de la vidéo entière ensemble : jamais deux fois le même plan, les meilleures correspondances d'abord.
 * Une scène sans plan assez adapté reste sans image plutôt que de recevoir une décoration sans rapport.
 */
internal fun cue(a: Act) = "act:" + a.name
internal fun cue(t: Thing) = "thing:" + t.name
internal fun cue(a: Actor) = "actor:" + a.name
private fun cues(vararg x: Any): Set<String> = x.map { when (it) { is Act -> cue(it); is Thing -> cue(it); is Actor -> cue(it); else -> error("cue") } }.toSet()

internal class Beat(
    val id: String,
    val label: String,
    val roles: Set<Role>,
    val goals: Set<Goal>,
    val cues: Set<String>,
    /** Ambiance sobre, admise pour les sujets sensibles (apaisement, gratitude) ; les plans d'attaque ne le sont pas. */
    val calm: Boolean = false,
    /** Plan qui peut revenir dans la même vidéo (l'objet 3D qui tourne) ; les autres ne passent qu'une fois. */
    val repeatable: Boolean = false,
    /** `null` = plan 3D dessiné par le format lui-même (voir [Demo3DLook]). */
    val draw: ((StageCtx) -> Unit)?,
    /** Durée minimale utile du plan : en dessous, l'animation n'a pas le temps de se dérouler. */
    val minMs: Int = 0,
    /** Concepts pour lesquels ce plan est valable (vide = ceux du genre de scène). Un plan de VLAN ne sert pas un texte sur une simple topologie. */
    val onlyFor: Set<Concept> = emptySet(),
    /** Partie utile du cadre virtuel 1000 × 700 : un plan large et bas (une chaîne d'appareils) n'occupe que [y0, y0 + vh], et est agrandi en conséquence. */
    val y0: Float = 0f,
    val vh: Float = 700f,
    /** Indices OBLIGATOIRES : sans eux le plan est exclu (un téléphone et son SMS ne s'affichent pas pour un texte qui ne parle pas de SMS). */
    val needs: Set<String> = emptySet(),
)

internal class SceneKind(
    val id: String,
    val label: String,
    val concepts: Set<Concept>,
    val beats: List<Beat>,
    /** Concepts pour lesquels ce genre de scène sert de repli (note réduite). */
    val fallbackFor: Set<Concept> = emptySet(),
    /** Domaines où ce genre de scène a un sens (vide = tous). */
    val domains: Set<Domain> = emptySet(),
)

internal fun beat(id: String, label: String, roles: Set<Role>, goals: Set<Goal>, cues: Set<String>, calm: Boolean = false, repeatable: Boolean = false, minMs: Int = 0, onlyFor: Set<Concept> = emptySet(), y0: Float = 0f, vh: Float = 700f, needs: Set<String> = emptySet(), draw: ((StageCtx) -> Unit)?) =
    Beat(id, label, roles, goals, cues, calm, repeatable, draw, minMs, onlyFor, y0, vh, needs)

internal fun cueSet(vararg x: Any): Set<String> = cues(*x)

internal data class Pick(val sceneIndex: Int, val kind: SceneKind, val beat: Beat, val fit: Float) {
    val key: String get() = kind.id + "/" + beat.id
}

internal class ScenarioPlan(val intents: List<SceneIntent>, val picks: List<Pick?>, val sceneIds: List<String>) {
    private val byId = sceneIds.zip(picks).toMap()
    fun pickFor(sceneId: String): Pick? = byId[sceneId]

    /** Part des scènes qui ont reçu une représentation. */
    val coverage: Float get() = if (picks.isEmpty()) 0f else picks.count { it != null }.toFloat() / picks.size

    /** Explication lisible, scène par scène. */
    fun report(): String = intents.indices.joinToString("\n") { i ->
        val it = intents[i]; val p = picks[i]
        "Scène ${i + 1} : ${it.concept.label} · ${it.role.label} · ${it.action?.label ?: "—"} · ${it.relation.label} · objectif : ${it.goal.label}" +
            (if (p != null) "  =>  ${p.kind.id}/${p.beat.id} (${fmt2(p.fit)})" else "  =>  texte seul")
    }
}

internal object SceneRegistry {
    val kinds: List<SceneKind> by lazy {
        listOf(SitPhishing.kind, SitMental.kind, SitAid.kind, SitShop.kind, SitNet.kind, PhishingScenes.kind, BruteForceScenes.kind, FirewallScenes.kind, TopologyScenes.kind, ClientServerScenes.kind,
            GratitudeScenes.kind, StressScenes.kind, FunnelScenes.kind, MethodScenes.kind, WaterScenes.kind, Attack3DScenes.attackKind, Attack3DScenes.objectKind)
    }

    const val MIN_FIT = 0.6f

    /** Plan retrouvé par sa clé « genre/plan ». */
    fun find(key: String): Pair<SceneKind, Beat>? {
        val kind = kinds.firstOrNull { key.startsWith(it.id + "/") } ?: return null
        val beat = kind.beats.firstOrNull { it.id == key.removePrefix(kind.id + "/") } ?: return null
        return kind to beat
    }

    /** Note de 0 à 1 : ce plan sait-il montrer cette intention ? 0 = le concept ne correspond pas du tout. */
    fun fit(i: SceneIntent, k: SceneKind, b: Beat): Float {
        if (k.domains.isNotEmpty() && i.domain !in k.domains) return 0f
        if (b.needs.isNotEmpty() && !i.cues.containsAll(b.needs)) return 0f
        if (b.onlyFor.isNotEmpty() && i.concept !in b.onlyFor && i.secondary.none { it in b.onlyFor }) return 0f
        val c = when {
            i.concept in k.concepts -> 1f
            k.concepts.any { it in i.secondary } -> 0.55f
            i.concept != Concept.NONE && i.concept in k.fallbackFor -> 0.5f
            i.concept == Concept.NONE && Concept.NONE in k.concepts -> 0.6f
            else -> return 0f
        }
        val r = if (i.role in b.roles) 1f else 0.1f
        val a = if (b.cues.isEmpty()) 0.5f else min(1f, b.cues.count { it in i.cues }.toFloat() / min(3f, b.cues.size.toFloat()))
        val g = if (i.goal in b.goals) 1f else 0.3f
        // Phase C : à notes égales, une situation incarnée (personnages, objets, lieu) passe avant un schéma abstrait.
        val situation = if (k.id.startsWith("sit-") && b.id != "poster") 0.07f else 0f
        return 0.25f * c + 0.35f * r + 0.30f * a + 0.10f * g + situation
    }
}

internal object ScenarioPlanner {
    private class Cand(val k: SceneKind, val b: Beat, val copy: Int)

    /**
     * Attribue les plans de toute la vidéo ENSEMBLE : affectation de poids maximal (algorithme hongrois). Un plan ne sert
     * qu'une fois (un plan répétable existe en deux exemplaires, le second moins bien noté), et une scène dont aucun plan
     * n'atteint [SceneRegistry.MIN_FIT] reste sans image. Contrairement à un choix scène par scène, cela évite qu'une scène
     * « prenne » le plan dont la scène suivante avait davantage besoin. Les ex æquo sont tranchés par la graine.
     */
    fun plan(sb: Storyboard, profile: ContentProfile = sb.profile ?: ContentAnalyzer.analyze(IntentAnalyzer.videoText(sb))): ScenarioPlan {
        val intents = IntentAnalyzer.analyze(sb, profile)
        val salt = Rand.of(sb.seed, 0x5CE4).nextInt()
        val n = intents.size
        val picks = arrayOfNulls<Pick>(n)
        // Scènes « épinglées » (choix imposé par l'utilisateur ou figé à la génération) : leur plan ne dépend plus des autres scènes.
        val pinned = BooleanArray(n)
        val used = HashSet<String>(sb.reserved)
        for (i in 0 until n) {
            val key = sb.pins[sb.scenes[i].id] ?: continue
            pinned[i] = true
            if (key == Pins.TEXT_ONLY) continue
            val (k, b) = SceneRegistry.find(key) ?: continue
            picks[i] = Pick(i, k, b, SceneRegistry.fit(intents[i], k, b))
            if (!b.repeatable) used += key
        }
        val free = (0 until n).filter { !pinned[it] }
        val cands = ArrayList<Cand>()
        for (k in SceneRegistry.kinds) for (b in k.beats) {
            if (profile.sensitive && !b.calm) continue
            if (!b.repeatable && (k.id + "/" + b.id) in used) continue
            cands += Cand(k, b, 0)
            if (b.repeatable) cands += Cand(k, b, 1)
        }
        val r = free.size
        val m = cands.size
        if (r > 0) {
            // r lignes (scènes libres), m colonnes de plans + r colonnes « texte seul » (poids = seuil).
            val w = Array(r) { DoubleArray(m + r) }
            for ((row, i) in free.withIndex()) {
                val dur = sb.scenes[i].durationMs
                for ((j, c) in cands.withIndex()) {
                    var f = if (dur < c.b.minMs) 0f else SceneRegistry.fit(intents[i], c.k, c.b)
                    if (c.copy == 1) f -= 0.08f
                    // Carte d'ouverture d'épisode : de préférence l'affiche en situation du sujet (personnages, lieu), pas un schéma abstrait.
                    if (f > 0f && sb.scenes[i].isEpisodeOpener && c.b.id == "poster" && c.k.id.startsWith("sit-")) f += 0.12f
                    w[row][j] = if (f >= SceneRegistry.MIN_FIT) f + SK.hash01(i * 131 + (c.k.id + c.b.id).hashCode() + c.copy, salt) * 0.02 else -1.0
                }
                for (d in 0 until r) w[row][m + d] = SceneRegistry.MIN_FIT.toDouble()
            }
            val assign = Hungarian.maximize(w)
            for ((row, i) in free.withIndex()) {
                val j = assign[row]
                if (j in 0 until m && w[row][j] >= SceneRegistry.MIN_FIT) {
                    val c = cands[j]
                    picks[i] = Pick(i, c.k, c.b, SceneRegistry.fit(intents[i], c.k, c.b))
                }
            }
        }
        // Un plan répétable ne passe jamais deux scènes de suite (sauf si l'utilisateur l'a imposé).
        for (i in 1 until n) { val a = picks[i - 1]; val b = picks[i]; if (a != null && b != null && a.key == b.key && b.beat.repeatable && !pinned[i]) picks[i] = null }
        return ScenarioPlan(intents, picks.toList(), sb.scenes.map { it.id })
    }

    /** Les meilleurs plans de chaque scène avec leur note : pour comprendre un choix. */
    fun candidates(sb: Storyboard, top: Int = 5): List<List<String>> {
        val profile = sb.profile ?: ContentAnalyzer.analyze(IntentAnalyzer.videoText(sb))
        val intents = IntentAnalyzer.analyze(sb, profile)
        return intents.map { it0 ->
            SceneRegistry.kinds.flatMap { k -> k.beats.map { b -> Triple(k, b, if (profile.sensitive && !b.calm) 0f else SceneRegistry.fit(it0, k, b)) } }
                .filter { it.third > 0f }.sortedByDescending { it.third }.take(top).map { "${it.first.id}/${it.second.id}=${fmt2(it.third)}" }
        }
    }
}

/** Affectation de poids maximal (Kuhn-Munkres, version à potentiels) pour n lignes ≤ m colonnes. Retourne la colonne de chaque ligne. */
internal object Hungarian {
    fun maximize(w: Array<DoubleArray>): IntArray {
        val n = w.size
        if (n == 0) return IntArray(0)
        val m = w[0].size
        require(n <= m)
        val inf = Double.MAX_VALUE / 4
        val u = DoubleArray(n + 1); val v = DoubleArray(m + 1)
        val p = IntArray(m + 1); val way = IntArray(m + 1)
        for (i in 1..n) {
            p[0] = i
            var j0 = 0
            val minv = DoubleArray(m + 1) { inf }
            val used = BooleanArray(m + 1)
            do {
                used[j0] = true
                val i0 = p[j0]
                var delta = inf; var j1 = 0
                for (j in 1..m) if (!used[j]) {
                    val cur = -w[i0 - 1][j - 1] - u[i0] - v[j]
                    if (cur < minv[j]) { minv[j] = cur; way[j] = j0 }
                    if (minv[j] < delta) { delta = minv[j]; j1 = j }
                }
                for (j in 0..m) if (used[j]) { u[p[j]] += delta; v[j] -= delta } else minv[j] -= delta
                j0 = j1
            } while (p[j0] != 0)
            do { val j1 = way[j0]; p[j0] = p[j1]; j0 = j1 } while (j0 != 0)
        }
        val res = IntArray(n)
        for (j in 1..m) if (p[j] != 0) res[p[j] - 1] = j - 1
        return res
    }
}

/** Clés spéciales des visuels imposés scène par scène. */
object Pins {
    /** Scène volontairement sans image : le texte seul. */
    const val TEXT_ONLY = "none"
}

/** Accès public au diagnostic du scénario visuel (banc d'essai, tests, interface) : l'intention de chaque scène et le plan retenu. */
object Scenario {
    /** Une ligne par scène : concept, rôle, action, relation, objectif, plan retenu et sa note. */
    fun report(sb: Storyboard): String = ScenarioPlanner.plan(sb).report()

    /** Identifiant « genre/plan » retenu pour chaque scène, ou `null` quand la scène reste sans image. */
    fun picks(sb: Storyboard): List<String?> = ScenarioPlanner.plan(sb).picks.map { it?.key }

    /** Les meilleures notes par scène (diagnostic). */
    fun candidates(sb: Storyboard): List<List<String>> = ScenarioPlanner.candidates(sb)

    /** Concept de chaque scène. */
    fun concepts(sb: Storyboard): List<Concept> = ScenarioPlanner.plan(sb).intents.map { it.concept }

    /** Part des scènes qui ont reçu une représentation (0..1). */
    fun coverage(sb: Storyboard): Float = ScenarioPlanner.plan(sb).coverage
}
