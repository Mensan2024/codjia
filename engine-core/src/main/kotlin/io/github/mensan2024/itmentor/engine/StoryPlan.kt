// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/** Modification faite par l'utilisateur sur une scène (`null` = inchangé). */
data class SceneEdit(
    val durationMs: Int? = null, val transition: Transition? = null, val variant: Int? = null,
    val title: String? = null, val subtitle: String? = null, val body: String? = null, val kicker: String? = null, val items: List<String>? = null,
) {
    fun applyTo(s: Scene): Scene = s.copy(
        durationMs = durationMs ?: s.durationMs, transitionOut = transition ?: s.transitionOut, variant = variant ?: s.variant,
        title = title ?: s.title, subtitle = subtitle ?: s.subtitle, body = body ?: s.body, kicker = kicker ?: s.kicker, items = items ?: s.items,
        authorTitle = if (title != null) false else s.authorTitle,
    )
    val isEmpty: Boolean get() = this == SceneEdit()
}

/** Un visuel possible pour une scène, avec sa note d'adéquation (0 à 1) et le seuil de pertinence du registre. */
data class Alternative(val key: String, val kind: String, val label: String, val fit: Float, val usedBy: String?) {
    val belowThreshold: Boolean get() = fit < SceneRegistry.MIN_FIT
}

/**
 * Fiche d'une scène du storyboard : les 13 éléments demandés (texte exact, narration, image, personnages, objets, action,
 * décor, animation, cadrage, caméra, durée, transition, son). Chaque champ vient de ce que le moteur sait réellement ;
 * ce qu'il ne modélise pas encore est `null` (affiché « — ») et non inventé.
 */
data class SceneCard(
    val id: String, val episode: Int, val index: Int, val layout: Layout,
    val text: String, val narration: String,
    val image: String, val visualKey: String?, val visualFit: Float?, val visualLabel: String?,
    val characters: List<String>, val objects: List<String>, val action: String?, val setting: String,
    val animation: String, val framing: String?, val camera: String?,
    val durationMs: Int, val transition: String, val audio: String,
    val intent: String, val userEdits: List<String>, val hash: String,
)

/**
 * Plan de production d'une vidéo VIDEO24 : tout ce qu'il faut pour la refaire à l'identique, et pour la modifier scène par scène.
 *
 *  - la **source** : le texte de l'auteur, la graine, le format (look), le modèle, la musique, la voix ;
 *  - ce qui a été **figé à la génération** : le visuel de chaque scène (« épingle ») et l'épisode de chaque scène ;
 *  - les **modifications de l'utilisateur**.
 *
 * Remplacer le visuel d'une scène ne change que l'épingle de cette scène : les autres épingles, les épisodes, les durées et le
 * texte restent exactement les mêmes, donc leurs images aussi. Un [StoryPlan] est immuable : chaque modification en renvoie un nouveau.
 * Aller-retour JSON : [toJson] / [fromJson] (la génération est déterministe, seules les décisions sont stockées).
 */
class StoryPlan(
    val text: String, val seed: Long, val look: Look, val templateId: String, val musicId: String?, val voice: Boolean, val budgetMs: Int,
    val episodes: Map<String, Int>, val pins: Map<String, String>,
    val edits: Map<String, SceneEdit> = emptyMap(), val userPins: Set<String> = emptySet(),
) {
    private fun copy(
        templateId: String = this.templateId, musicId: String? = this.musicId, voice: Boolean = this.voice,
        episodes: Map<String, Int> = this.episodes, pins: Map<String, String> = this.pins,
        edits: Map<String, SceneEdit> = this.edits, userPins: Set<String> = this.userPins,
    ) = StoryPlan(text, seed, look, templateId, musicId, voice, budgetMs, episodes, pins, edits, userPins)

    private val generated: CreativeDirector.Proposal by lazy { CreativeDirector.propose(text, seed, forcedLook = look, compress = false) }

    val template: Template get() = Templates.byId(templateId)

    /** Toutes les scènes de la série, modifications appliquées, avant découpage en épisodes. */
    val storyboard: Storyboard by lazy {
        val g = generated.storyboard
        g.copy(templateId = templateId, scenes = g.scenes.map { sc -> edits[sc.id]?.applyTo(sc) ?: sc }, pins = pins)
    }

    fun episodeOf(sceneId: String): Int {
        episodes[sceneId]?.let { return it }
        val ids = storyboard.scenes.map { it.id }
        val i = ids.indexOf(sceneId)
        return (i - 1 downTo 0).firstNotNullOfOrNull { episodes[ids[it]] } ?: 1
    }

    /** Les vidéos de la série (une seule si tout tient dans le budget), non habillées. */
    val series: List<Storyboard> by lazy { Episodes.assemble(storyboard, storyboard.scenes.map { episodeOf(it.id) }) }

    val episodeCount: Int get() = series.size

    /** Vidéos prêtes à rendre : habillage par le modèle, calage sur la musique s'il y en a une. */
    fun prepared(music: MusicTrack?): List<Storyboard> = series.map { Composer.prepare(it, template, music) }

    // ───────── modifications (chacune renvoie un nouveau plan) ─────────

    private fun need(id: String) = require(storyboard.scenes.any { it.id == id }) { "Scène inconnue : $id" }

    /** Remplace le visuel d'une scène (clé « genre/plan » ou [Pins.TEXT_ONLY]). Rien d'autre ne bouge. */
    fun replaceVisual(id: String, key: String): StoryPlan {
        need(id)
        require(key == Pins.TEXT_ONLY || SceneRegistry.find(key) != null) { "Visuel inconnu : $key" }
        return copy(pins = pins + (id to key), userPins = userPins + id)
    }

    /** Autre visuel pour la scène : le suivant dans le classement (sans reprendre un visuel déjà employé ailleurs ni passer sous le seuil). */
    fun reroll(id: String): StoryPlan {
        val alts = alternatives(id, 12).filter { !it.belowThreshold && it.usedBy == null && it.key != pins[id] }
        return replaceVisual(id, alts.firstOrNull()?.key ?: Pins.TEXT_ONLY)
    }

    private fun edit(id: String, f: (SceneEdit) -> SceneEdit): StoryPlan {
        need(id)
        val e = f(edits[id] ?: SceneEdit())
        return copy(edits = if (e.isEmpty) edits - id else edits + (id to e))
    }

    fun withDuration(id: String, ms: Int): StoryPlan { require(ms in 1_500..30_000) { "Durée hors limites (1,5 à 30 s)" }; return edit(id) { it.copy(durationMs = ms) } }
    fun withTransition(id: String, t: Transition) = edit(id) { it.copy(transition = t) }
    fun withVariant(id: String, v: Int) = edit(id) { it.copy(variant = v) }
    fun withText(id: String, title: String? = null, subtitle: String? = null, body: String? = null, kicker: String? = null, items: List<String>? = null) =
        edit(id) { it.copy(title = title ?: it.title, subtitle = subtitle ?: it.subtitle, body = body ?: it.body, kicker = kicker ?: it.kicker, items = items ?: it.items) }
    fun withTemplate(id: String) = copy(templateId = id)
    fun withMusic(id: String?) = copy(musicId = id)
    fun withVoice(on: Boolean) = copy(voice = on)

    /** Redécoupe en épisodes (après avoir changé des durées) ; les visuels ne changent pas. */
    fun resplit(): StoryPlan {
        val a = Episodes.assign(storyboard.scenes, voice, budgetMs)
        return copy(episodes = storyboard.scenes.mapIndexed { i, s -> s.id to a[i] }.toMap())
    }

    // ───────── lecture ─────────

    private fun planOf(ep: Storyboard): ScenarioPlan = ScenarioPlanner.plan(ep)

    /** Visuels envisageables pour une scène, du plus adapté au moins adapté (les notes sous le seuil sont signalées). */
    fun alternatives(id: String, top: Int = 6): List<Alternative> {
        need(id)
        val ep = series.first { s -> s.scenes.any { it.id == id } }
        val i = ep.scenes.indexOfFirst { it.id == id }
        val profile = ep.profile ?: ContentAnalyzer.analyze(IntentAnalyzer.videoText(ep))
        val intent = ScenarioPlanner.plan(ep, profile).intents[i]
        val dur = ep.scenes[i].durationMs
        val sameVideo = ep.scenes.map { it.id }.toSet()
        val out = ArrayList<Alternative>()
        for (k in SceneRegistry.kinds) for (b in k.beats) {
            if (profile.sensitive && !b.calm) continue
            val f = if (dur < b.minMs) 0f else SceneRegistry.fit(intent, k, b)
            if (f <= 0f) continue
            val key = k.id + "/" + b.id
            // « Déjà utilisé » ne vaut que dans la même vidéo : deux épisodes différents peuvent montrer le même plan.
            val user = if (b.repeatable) null else pins.entries.firstOrNull { it.value == key && it.key != id && it.key in sameVideo }?.key
            out += Alternative(key, k.label, b.label, f, user)
        }
        return out.sortedByDescending { it.fit }.take(top)
    }

    /** Empreinte de ce qui détermine les images d'une scène : si elle ne change pas, les images ne changent pas. */
    fun sceneHash(id: String): String {
        val ep = series.first { e -> e.scenes.any { it.id == id } }
        val sc = ep.scenes.first { it.id == id }
        return Hash64.of("${sc}|pin=${ep.pins[id]}|$look|$templateId|$seed")
    }

    /** Une fiche par scène de chaque épisode (cartes d'ouverture comprises). */
    fun cards(): List<SceneCard> {
        val out = ArrayList<SceneCard>()
        for (ep in series) {
            val profile = ep.profile ?: ContentAnalyzer.analyze(IntentAnalyzer.videoText(ep))
            val plan = ScenarioPlanner.plan(ep, profile)
            val epNo = ep.episode?.index ?: 1
            for ((i, sc) in ep.scenes.withIndex()) {
                val it = plan.intents[i]; val pick = plan.picks[i]
                val e = edits[sc.id]
                val user = buildList {
                    if (sc.id in userPins) add("visuel")
                    if (e?.durationMs != null) add("durée"); if (e?.transition != null) add("transition"); if (e?.variant != null) add("variante")
                    if (e?.title != null || e?.subtitle != null || e?.body != null || e?.kicker != null || e?.items != null) add("texte")
                }
                val things = it.things.map { t -> t.label }
                val actors = (it.actors + it.impliedActors).map { a -> a.label }
                val image = when {
                    sc.isEpisodeOpener && pick == null -> "Carte d'ouverture de l'épisode (titre de la série), ajoutée par l'application : texte seul."
                    pick != null -> "« ${pick.beat.label} » (${pick.kind.label}) : ${it.relation.label}" +
                        (if (things.isNotEmpty()) " ; objets montrés : ${things.joinToString(", ")}" else "") +
                        (if (actors.isNotEmpty()) " ; acteurs : ${actors.joinToString(", ")}" else "") +
                        " — pour ${it.goal.label}. Adéquation ${fmt2(pick.fit)}" + if (pick.fit < SceneRegistry.MIN_FIT) " (sous le seuil de ${fmt2(SceneRegistry.MIN_FIT)} : choix de l'utilisateur)." else "."
                    sc.id in userPins && pins[sc.id] == Pins.TEXT_ONLY -> "Texte seul (choix de l'utilisateur)."
                    else -> "Aucun visuel du registre n'atteint ${fmt2(SceneRegistry.MIN_FIT)} pour cette scène (concept : ${it.concept.label}, rôle : ${it.role.label}) : texte seul."
                }
                val framing = when {
                    pick == null -> "texte plein cadre"
                    pick.beat.draw == null -> "objet 3D au centre"
                    pick.beat.vh < 700f -> "bandeau large (${pick.beat.vh.toInt()} sur 700)"
                    else -> "plan plein cadre"
                }
                out += SceneCard(
                    id = sc.id, episode = epNo, index = i + 1, layout = sc.layout,
                    text = exactText(sc), narration = if (voice) Narration.textOf(sc) else "",
                    image = image, visualKey = pick?.key, visualFit = pick?.fit, visualLabel = pick?.beat?.label,
                    characters = actors, objects = things, action = it.action?.label,
                    setting = "format ${look.label}, modèle ${template.name}",
                    animation = "entrée ${sc.enter.type.name.lowercase()}, sortie ${sc.exit.type.name.lowercase()}",
                    framing = framing, camera = if (pick != null && pick.beat.draw == null) "rotation lente de l'objet" else if (pick != null) "fixe" else null,
                    durationMs = sc.durationMs,
                    transition = if (i == ep.scenes.lastIndex) "fin de l'épisode" else "${sc.transitionOut.type.name.lowercase()} ${sc.transitionOut.durationMs} ms",
                    audio = (musicId?.let { m -> "musique « $m »" } ?: "sans musique") + if (voice) ", voix off" else ", sans voix",
                    intent = "${it.concept.label} · ${it.role.label} · ${it.relation.label}",
                    userEdits = user, hash = sceneHash(sc.id),
                )
            }
        }
        return out
    }

    private fun exactText(s: Scene): String = buildList {
        if (s.layout == Layout.HERO) s.kicker?.let { add(it) }
        if (s.layout == Layout.HERO || s.authorTitle || s.isEpisodeOpener) add(Marked.strip(s.title))
        s.subtitle?.let { add(it) }
        if (s.body.isNotBlank() && s.layout != Layout.CHECKLIST) add(s.body)
        s.items.forEach { add("• $it") }
        s.compare?.let { add("✗ ${it.badText()}   ✓ ${it.goodText()}") }
        s.statValue?.let { add(it) }
    }.joinToString("\n")

    /** Fidélité de la série au texte de l'auteur (mots présents, mots dans l'ordre). */
    fun fidelity(): Fidelity.Report = Fidelity.measure(text, storyboard.scenes)

    /** Contrôle qualité de la série : fidélité, durée des épisodes, visuels imposés douteux, et le contrôle de pertinence habituel. */
    fun qc(): List<String> {
        val out = ArrayList<String>()
        val fid = fidelity()
        if (edits.values.none { it.title != null || it.subtitle != null || it.body != null || it.kicker != null || it.items != null } && !fid.perfect)
            out += "BLOQUANT fidélité : ${fid.summary()}"
        for (ep in series) {
            val ms = Episodes.spanMs(ep.scenes, voice)
            val n = ep.episode?.index ?: 1
            if (ms > 58_000) out += "BLOQUANT épisode $n : ${ms / 1000} s dépasse les 59 s de la plateforme"
            else if (ms > budgetMs + Episodes.OPENER_MS) out += "Attention épisode $n : ${ms / 1000} s dépasse le budget visé de ${budgetMs / 1000} s (voix et habillage peuvent s'ajouter) : redécouper"
            for (f in QualityControl.audit(ep)) out += (if (f.blocking) "BLOQUANT " else "Attention ") + "scène ${ep.scenes.getOrNull(f.scene)?.id ?: f.scene} : ${f.message}"
        }
        for (id in userPins) {
            val key = pins[id] ?: continue
            if (key == Pins.TEXT_ONLY) continue
            val alt = alternatives(id, 200).firstOrNull { it.key == key }
            if (alt == null) out += "Attention scène $id : le visuel $key n'a aucun lien avec ce passage (note 0)"
            else if (alt.belowThreshold) out += "Attention scène $id : visuel imposé sous le seuil de pertinence (${fmt2(alt.fit)} < ${fmt2(SceneRegistry.MIN_FIT)})"
            alt?.usedBy?.let { out += "Attention scène $id : le visuel $key est déjà utilisé par la scène $it" }
        }
        return out
    }

    // ───────── JSON ─────────

    fun toJson(withCards: Boolean = true): String {
        val m = LinkedHashMap<String, Any?>()
        m["format"] = "VIDEO24-STORYPLAN"; m["version"] = VERSION
        m["source"] = linkedMapOf("text" to text, "seed" to seed, "look" to look.name, "template" to templateId, "music" to musicId, "voice" to voice, "budgetMs" to budgetMs)
        m["scenes"] = storyboard.scenes.map { sc ->
            val e = edits[sc.id]
            val o = LinkedHashMap<String, Any?>()
            o["id"] = sc.id; o["episode"] = episodeOf(sc.id); o["visual"] = pins[sc.id] ?: Pins.TEXT_ONLY
            if (sc.id in userPins) o["visualByUser"] = true
            if (e != null) o["edit"] = linkedMapOf(
                "durationMs" to e.durationMs, "transition" to e.transition?.let { t -> linkedMapOf("type" to t.type.name, "ms" to t.durationMs) }, "variant" to e.variant,
                "title" to e.title, "subtitle" to e.subtitle, "body" to e.body, "kicker" to e.kicker, "items" to e.items).filterValues { it != null }
            o
        }
        if (withCards) m["cards"] = cards().map { c ->
            linkedMapOf("scene" to c.id, "episode" to c.episode, "layout" to c.layout.name,
                "texteExact" to c.text, "narration" to c.narration, "image" to c.image, "personnages" to c.characters, "objets" to c.objects,
                "action" to c.action, "decor" to c.setting, "animation" to c.animation, "cadrage" to c.framing, "camera" to c.camera,
                "dureeMs" to c.durationMs, "transition" to c.transition, "son" to c.audio, "hash" to c.hash)
        }
        return MiniJson.write(m)
    }

    companion object {
        const val VERSION = 1

        @Suppress("UNCHECKED_CAST")
        fun fromJson(json: String): StoryPlan {
            val root = MiniJson.parse(json) as? Map<String, Any?> ?: throw StoryboardException("Plan illisible : objet attendu")
            if (root["format"] != "VIDEO24-STORYPLAN") throw StoryboardException("Ce n'est pas un plan VIDEO24")
            val v = (root["version"] as? Long)?.toInt() ?: 0
            if (v > VERSION) throw StoryboardException("Plan de version $v : cette application comprend jusqu'à la version $VERSION")
            val src = root["source"] as? Map<String, Any?> ?: throw StoryboardException("Plan sans source")
            val look = Look.entries.firstOrNull { it.name == src["look"] } ?: throw StoryboardException("Format inconnu : ${src["look"]}")
            val episodes = LinkedHashMap<String, Int>(); val pins = LinkedHashMap<String, String>(); val edits = LinkedHashMap<String, SceneEdit>(); val userPins = LinkedHashSet<String>()
            for (o in root["scenes"] as? List<Any?> ?: emptyList()) {
                val sc = o as? Map<String, Any?> ?: continue
                val id = sc["id"] as? String ?: continue
                episodes[id] = (sc["episode"] as? Long)?.toInt() ?: 1
                pins[id] = sc["visual"] as? String ?: Pins.TEXT_ONLY
                if (sc["visualByUser"] == true) userPins += id
                (sc["edit"] as? Map<String, Any?>)?.let { e ->
                    val tr = (e["transition"] as? Map<String, Any?>)?.let { t -> Transition(TransitionType.valueOf(t["type"] as String), ((t["ms"] as? Long) ?: 500L).toInt()) }
                    edits[id] = SceneEdit((e["durationMs"] as? Long)?.toInt(), tr, (e["variant"] as? Long)?.toInt(), e["title"] as? String, e["subtitle"] as? String,
                        e["body"] as? String, e["kicker"] as? String, (e["items"] as? List<Any?>)?.map { it.toString() })
                }
            }
            return StoryPlan(src["text"] as? String ?: throw StoryboardException("Plan sans texte"), (src["seed"] as? Long) ?: 0L, look,
                src["template"] as? String ?: throw StoryboardException("Plan sans modèle"), src["music"] as? String, src["voice"] == true,
                ((src["budgetMs"] as? Long) ?: Episodes.BUDGET_MS.toLong()).toInt(), episodes, pins, edits, userPins)
        }
    }
}

/** Point d'entrée « studio » : du texte à un plan de production modifiable, sans compression des scènes. */
object Studio {
    /**
     * @param voice avec voix off, les durées tiennent compte de la phrase lue
     * @param look format de réalisation (par défaut Scénario, le pipeline principal)
     */
    fun plan(text: String, seed: Long = 7, voice: Boolean = true, look: Look = Look.SCENARIO, budgetMs: Int = Episodes.BUDGET_MS): StoryPlan {
        val p = CreativeDirector.propose(text, seed, forcedLook = look, compress = false)
        val sb = p.storyboard
        val picks = ScenarioPlanner.plan(sb).picks
        val pins = sb.scenes.mapIndexed { i, s -> s.id to (picks[i]?.key ?: Pins.TEXT_ONLY) }.toMap()
        val a = Episodes.assign(sb.scenes, voice, budgetMs)
        return StoryPlan(text, seed, look, p.template.id, p.musicId, voice, budgetMs, sb.scenes.mapIndexed { i, s -> s.id to a[i] }.toMap(), pins)
    }
}
