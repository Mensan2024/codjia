// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import io.github.mensan2024.itmentor.engine.AudioMix
import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.FontFamily
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Layout
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.MusicCategory
import io.github.mensan2024.itmentor.engine.MusicChoice
import io.github.mensan2024.itmentor.engine.MusicTrack
import io.github.mensan2024.itmentor.engine.MusicVolume
import io.github.mensan2024.itmentor.engine.Narration
import io.github.mensan2024.itmentor.engine.Pcm
import io.github.mensan2024.itmentor.engine.Pins
import io.github.mensan2024.itmentor.engine.Scene
import io.github.mensan2024.itmentor.engine.StoryPlan
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.Studio
import io.github.mensan2024.itmentor.engine.Transition
import io.github.mensan2024.itmentor.engine.TransitionType
import io.github.mensan2024.itmentor.engine.isEpisodeOpener
import io.github.mensan2024.itmentor.engine.StyleKit
import io.github.mensan2024.itmentor.engine.Template
import io.github.mensan2024.itmentor.engine.TextSanitizer
import io.github.mensan2024.itmentor.engine.Timeline
import io.github.mensan2024.itmentor.engine.VideoFormat
import io.github.mensan2024.itmentor.engine.VoiceMode
import io.github.mensan2024.itmentor.prototype.Examples
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * VIDEO24 dans le navigateur (iPhone, Android, ordinateur) : même moteur que l'application Android.
 * Texte → aperçu animé (voix off oui/non, style, musique) → génération MP4 → enregistrer ou partager.
 * Tout se passe sur l'appareil : aucun texte, aucune vidéo, aucune musique n'est envoyé sur Internet.
 */
private val base: String = run {
    val p = window.location.pathname
    if (p.endsWith("/")) p else p.substringBeforeLast('/') + "/"
}

/** Sauvegarde locale (sur l'appareil seulement) : texte saisi et réglages, retrouvés à la prochaine ouverture. */
private object Store {
    private const val KEY = "codjia.v1."
    fun get(k: String): String? = try { window.localStorage.getItem(KEY + k) } catch (e: Throwable) { null }
    fun set(k: String, v: String) { try { window.localStorage.setItem(KEY + k, v) } catch (e: Throwable) {} }
}

private fun persist() {
    Store.set("text", App.userText)
    Store.set("voice", if (App.voiceOn) "1" else "0")
    Store.set("vol", App.choice.volume.name)
}

private enum class Step { TEXT, PREVIEW, STYLE, MUSIC, EXPORT, STORY }

private object App {
    val audio = WebAudio(base)
    val voice = WebVoice(base)
    var catalog: List<MusicTrack> = emptyList()
    var mine: List<MusicTrack> = emptyList()
    var step = Step.TEXT
    var userText = Store.get("text") ?: ""
    var seed = 1L
    var generation = 0
    lateinit var content: Storyboard
    lateinit var template: Template
    var look = Look.KIT
    var lookLocked = false
    val recentLooks = ArrayList<Look>()
    val recentTemplates = ArrayList<String>()
    var choice = MusicChoice(null)
    var musicLocked = false
    var category: MusicCategory? = null
    var voiceOn = Store.get("voice") == "1"
    var voiceMode = VoiceMode.AUTO
    var voiced: Pair<String, Narration.Voiced>? = null
    var anim = 0          // identifiant de la boucle d'aperçu en cours (0 = aucune)
    var exporting = false
    var cancelExport = false
    var lastVideoUrl: String? = null
    var installPrompt: dynamic = null
    var updateReady = false
    /** Mode Scénario éditable : le plan complet (tous les épisodes) et l'épisode affiché (0 = premier). */
    var story: StoryPlan? = null
    var storyEp = 0
}

fun main() {
    window.addEventListener("beforeinstallprompt", { e -> e.preventDefault(); App.installPrompt = e.asDynamic(); renderHeaderInstall() })
    setupUpdates()
    val root = document.getElementById("app") as HTMLElement
    launch(onError = { fatal(it) }) {
        WebFonts.start(base)   // en parallèle : l'interface s'affiche tout de suite
        // L'interface utilise les mêmes polices que les vidéos.
        val css = document.documentElement.asDynamic().style
        css.setProperty("--font-ui", "\"${WebFonts.family(io.github.mensan2024.itmentor.engine.FontSpec(FontFamily.TEXT, 600, 16f))}\", system-ui, sans-serif")
        css.setProperty("--font-strong", "\"${WebFonts.family(io.github.mensan2024.itmentor.engine.FontSpec(FontFamily.TEXT, 800, 16f))}\", system-ui, sans-serif")
        css.setProperty("--font-display", "\"${WebFonts.family(io.github.mensan2024.itmentor.engine.FontSpec(FontFamily.DISPLAY, 900, 16f))}\", system-ui, sans-serif")
        App.catalog = App.audio.catalog()
        App.mine = MyMusic.list()
        Store.get("vol")?.let { v -> MusicVolume.entries.firstOrNull { it.name == v }?.let { App.choice = App.choice.copy(volume = it) } }
        window.asDynamic().codjiaReady = true   // pour les tests automatiques
        root.classList.remove("boot")
        show(Step.TEXT)
    }
}

private fun fatal(e: Throwable) {
    console.error(e)
    val root = document.getElementById("app") as HTMLElement
    root.innerHTML = ""
    root.appendChild(el("div", "card error", "Oups : ${e.message ?: "erreur inconnue"}. Recharge la page."))
}

// ───────────────────────────── Mises à jour automatiques ─────────────────────────────

/**
 * Chaque publication sur le site arrive chez tout le monde : le navigateur vérifie la version au lancement,
 * quand on revient sur l'appli et toutes les 20 minutes ; la nouvelle version s'installe en arrière-plan,
 * puis la page se recharge d'elle-même (jamais pendant la fabrication d'une vidéo).
 */
private fun setupUpdates() {
    if (!(js("'serviceWorker' in navigator") as Boolean)) return
    val sw = window.navigator.asDynamic().serviceWorker
    val hadController = sw.controller != null
    window.addEventListener("load", {
        launch {
            val reg: dynamic = (sw.register("sw.js", obj("updateViaCache" to "none")) as kotlin.js.Promise<dynamic>).await()
            val check = { try { reg.update() } catch (e: Throwable) {} ; Unit }
            window.setInterval({ check() }, 20 * 60 * 1000)
            document.addEventListener("visibilitychange", { if (document.asDynamic().visibilityState == "visible") check() })
        }
    })
    sw.addEventListener("controllerchange", {
        if (!hadController) return@addEventListener   // première installation : rien à recharger
        App.updateReady = true
        applyUpdateWhenIdle()
    })
}

private fun applyUpdateWhenIdle() {
    if (!App.updateReady) return
    if (App.exporting) return   // relancé à la fin de la génération
    toast("Nouvelle version de VIDEO24 : mise à jour…")
    window.setTimeout({ window.location.reload() }, 1200)
}

// ───────────────────────────── petits outils DOM ─────────────────────────────

private fun el(tag: String, cls: String = "", text: String? = null): HTMLElement {
    val e = document.createElement(tag) as HTMLElement
    if (cls.isNotEmpty()) e.className = cls
    if (text != null) e.textContent = text
    return e
}

/** Icônes SVG (constantes du code, jamais de texte venu de l'utilisateur). */
private object Ic {
    private fun svg(path: String) = "<svg viewBox='0 0 24 24' width='20' height='20' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>$path</svg>"
    val play = svg("<polygon points='6 4 20 12 6 20 6 4' fill='currentColor'/>")
    val pause = svg("<rect x='6' y='4' width='4' height='16' fill='currentColor'/><rect x='14' y='4' width='4' height='16' fill='currentColor'/>")
    val dice = svg("<rect x='3' y='3' width='18' height='18' rx='4'/><circle cx='8.5' cy='8.5' r='1.2' fill='currentColor'/><circle cx='15.5' cy='15.5' r='1.2' fill='currentColor'/><circle cx='15.5' cy='8.5' r='1.2' fill='currentColor'/><circle cx='8.5' cy='15.5' r='1.2' fill='currentColor'/>")
    val palette = svg("<path d='M12 3a9 9 0 1 0 0 18c1 0 1.6-.8 1.6-1.6 0-.5-.2-.8-.4-1.1-.3-.3-.4-.7-.4-1.1 0-.9.7-1.6 1.6-1.6H16a5 5 0 0 0 5-5c0-4.1-4-7.6-9-7.6z'/><circle cx='7.5' cy='11' r='1' fill='currentColor'/><circle cx='10' cy='7' r='1' fill='currentColor'/><circle cx='14.5' cy='7' r='1' fill='currentColor'/>")
    val music = svg("<path d='M9 18V5l12-2v13'/><circle cx='6' cy='18' r='3'/><circle cx='18' cy='16' r='3'/>")
    val mic = svg("<rect x='9' y='2' width='6' height='12' rx='3'/><path d='M5 10v1a7 7 0 0 0 14 0v-1M12 18v4M8 22h8'/>")
    val upload = svg("<path d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/><polyline points='17 8 12 3 7 8'/><line x1='12' y1='3' x2='12' y2='15'/>")
    val download = svg("<path d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/><polyline points='7 10 12 15 17 10'/><line x1='12' y1='15' x2='12' y2='3'/>")
    val share = svg("<circle cx='18' cy='5' r='3'/><circle cx='6' cy='12' r='3'/><circle cx='18' cy='19' r='3'/><line x1='8.6' y1='13.5' x2='15.4' y2='17.5'/><line x1='15.4' y1='6.5' x2='8.6' y2='10.5'/>")
    val sparkle = svg("<path d='M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z' fill='currentColor'/><path d='M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z' fill='currentColor'/>")
    val trash = svg("<polyline points='3 6 5 6 21 6'/><path d='M19 6l-1 14H6L5 6M10 11v6M14 11v6M9 6V4h6v2'/>")
    val help = svg("<circle cx='12' cy='12' r='10'/><path d='M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3M12 17h.01'/>")
    val back = svg("<polyline points='15 18 9 12 15 6'/>")
}

private fun button(text: String, cls: String = "btn", icon: String? = null, onClick: () -> Unit): HTMLElement {
    val b = el("button", cls)
    if (icon != null) b.appendChild(el("span", "ic").also { it.innerHTML = icon })
    if (text.isNotEmpty()) b.appendChild(el("span", "", text))
    b.addEventListener("click", { onClick() })
    return b
}

private fun HTMLElement.add(vararg children: HTMLElement): HTMLElement { for (c in children) appendChild(c); return this }

private fun toast(msg: String) {
    document.querySelectorAll(".toast").asDynamic().forEach { n: dynamic -> n.remove() }
    val t = el("div", "toast", msg)
    document.body?.appendChild(t)
    window.setTimeout({ t.classList.add("out") }, 3400)
    window.setTimeout({ t.remove() }, 3800)
}

// ───────────────────────────── navigation ─────────────────────────────

private fun show(s: Step) {
    stopPreview()
    App.step = s
    val root = document.getElementById("app") as HTMLElement
    root.innerHTML = ""
    root.appendChild(topBar(s))
    val body = when (s) {
        Step.TEXT -> textPage()
        Step.PREVIEW -> previewPage()
        Step.STYLE -> stylePage()
        Step.MUSIC -> musicPage()
        Step.EXPORT -> exportPage()
        Step.STORY -> storyPage()
    }
    root.appendChild(body)
    root.appendChild(el("p", "privacy", "Tout se passe sur ton appareil : ton texte, ta voix, ta musique et tes vidéos ne sont envoyés nulle part. Logiciel libre (GPL-3.0)."))
    window.scrollTo(0.0, 0.0)
}

private fun topBar(s: Step): HTMLElement {
    val wrap = el("div", "top-wrap")
    val h = el("header", "top")
    val brand = el("div", "brand")
    brand.add(el("span", "logo", "▶_"), el("span", "brand-name", "VIDEO24"))
    brand.addEventListener("click", { if (!App.exporting) show(Step.TEXT) })
    h.appendChild(brand)
    h.appendChild(el("span", "install-slot").also { it.id = "install-slot" })
    h.appendChild(button("", "icon-btn", Ic.help) { showHelp() }.also { it.title = "Aide : comment ça marche"; it.setAttribute("aria-label", "Aide") })
    wrap.appendChild(h)
    // Étapes : Texte → Aperçu → Vidéo
    val idx = when (s) { Step.TEXT -> 0; Step.PREVIEW, Step.STYLE, Step.MUSIC, Step.STORY -> 1; Step.EXPORT -> 2 }
    val stepper = el("nav", "stepper")
    listOf("Texte", "Aperçu", "Vidéo").forEachIndexed { i, label ->
        val it = el("div", "st" + (if (i == idx) " on" else if (i < idx) " done" else ""))
        it.add(el("span", "dot", "${i + 1}"), el("span", "lab", label))
        if (i < idx && !App.exporting) {
            it.setAttribute("tabindex", "0"); it.setAttribute("role", "button")
            it.addEventListener("click", { show(if (i == 0) Step.TEXT else Step.PREVIEW) })
        }
        if (i == idx) it.setAttribute("aria-current", "step")
        stepper.appendChild(it)
        if (i < 2) stepper.appendChild(el("span", "bar" + if (i < idx) " done" else ""))
    }
    wrap.appendChild(stepper)
    window.setTimeout({ renderHeaderInstall() }, 0)
    return wrap
}

/** Aide facultative : cinq étapes courtes, jamais imposée. */
private fun showHelp() {
    val bg = el("div", "sheet-bg")
    val sheet = el("div", "sheet")
    sheet.appendChild(el("h2", "", "Comment ça marche"))
    val ol = el("ol")
    listOf(
        "Texte" to "Colle ton texte (ou touche « Essayer un exemple »). Les titres, les listes et l'ordre de tes phrases sont conservés.",
        "Analyse" to "VIDEO24 repère le sujet, les étapes et la conclusion, puis construit le scénario de la vidéo, sur ton appareil.",
        "Style" to "Dans l'aperçu, « Format » change l'allure de la vidéo et « Autre version » en propose une nouvelle.",
        "Musique et voix" to "« Musique » choisit ou importe un morceau ; « Voix » ajoute une voix off. Le bouton est allumé quand l'option est active.",
        "Vidéo" to "« Générer la vidéo » fabrique le MP4 ; tu peux ensuite l'enregistrer ou le partager.",
    ).forEach { (t, d) -> ol.appendChild(el("li").also { li -> li.appendChild(el("b", "", "$t : ")); li.appendChild(el("span", "", d)) }) }
    sheet.appendChild(ol)
    sheet.appendChild(button("Compris", "btn primary wide") { bg.remove() })
    bg.appendChild(sheet)
    bg.addEventListener("click", { e -> if (e.target === bg) bg.remove() })
    document.body?.appendChild(bg)
}

private fun isIos(): Boolean = js("/iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)") as Boolean
private fun isStandalone(): Boolean = js("window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true") as Boolean

private fun renderHeaderInstall() {
    val slot = document.getElementById("install-slot") as? HTMLElement ?: return
    slot.innerHTML = ""
    if (isStandalone()) return
    if (App.installPrompt != null) {
        slot.appendChild(button("Installer", "btn small glow", Ic.download) {
            val p = App.installPrompt
            App.installPrompt = null
            p.prompt()
            renderHeaderInstall()
        })
    } else if (isIos()) {
        slot.appendChild(button("Installer", "btn small", Ic.download) {
            toast("Sur iPhone : touche Partager (carré avec flèche), puis « Sur l'écran d'accueil ».")
        })
    }
}

private fun subPage(title: String, back: Step): HTMLElement {
    val head = el("div", "subhead")
    head.add(button("", "icon-btn", Ic.back) { show(back) }, el("h2", "", title))
    return head
}

// ───────────────────────────── 1. Texte ─────────────────────────────

private fun textPage(): HTMLElement {
    val page = el("main", "page")
    // Présentation courte pour les nouveaux utilisateurs ; réduite dès qu'on écrit, fermable pour de bon.
    val intro = el("section", "intro")
    intro.appendChild(el("p").also {
        it.innerHTML = "<b>Transformez vos idées et vos textes en vidéos captivantes.</b> Choisissez un style, personnalisez votre création et générez une vidéo professionnelle en quelques étapes."
    })
    intro.appendChild(button("×", "x") { Store.set("intro", "0"); intro.classList.add("min") }.also { it.title = "Masquer"; it.setAttribute("aria-label", "Masquer la présentation") })
    if (Store.get("intro") == "0" || App.userText.isNotBlank()) intro.classList.add("min")
    page.appendChild(intro)
    val card = el("div", "card editor")
    val area = el("textarea", "text") as org.w3c.dom.HTMLTextAreaElement
    area.value = App.userText
    area.maxLength = TextSanitizer.MAX_CHARS
    area.placeholder = "Colle ou écris ton texte ici… Ex. : Astuce 028 — Active le pare-feu de ton PC…"
    area.setAttribute("aria-label", "Ton texte")
    val meta = el("div", "editor-meta")
    val counter = el("span", "", "")
    val estimate = el("span", "pill", "")
    meta.add(counter, estimate)
    var timer = 0
    fun refresh() {
        val words = area.value.split(Regex("\\s+")).count { w -> w.any { it.isLetterOrDigit() } }
        counter.textContent = "$words mots · ${area.value.length} / ${TextSanitizer.MAX_CHARS}"
        window.clearTimeout(timer)
        if (area.value.isBlank()) { estimate.textContent = "Colle un texte"; return }
        timer = window.setTimeout({
            val ms = try {
                val p = CreativeDirector.propose(area.value, 7L)
                Timeline.build(Composer.prepare(p.storyboard, p.template, null)).totalMs
            } catch (e: Throwable) { -1L }
            estimate.textContent = if (ms > 0) "Vidéo ≈ ${(ms + 500) / 1000} s" else "Texte trop court"
        }, 350)
    }
    area.addEventListener("input", { App.userText = area.value; persist(); refresh() })
    area.addEventListener("focus", { intro.classList.add("min") })
    area.addEventListener("blur", { if (area.value.isBlank() && Store.get("intro") != "0") intro.classList.remove("min") })
    card.add(area, meta)
    page.appendChild(card)
    // « Essayer un exemple » charge un vrai exemple à chaque touche, et passe au suivant ensuite.
    var next = (App.userText.length + App.generation) % Examples.ALL.size
    page.appendChild(el("div", "chips").add(button("Essayer un exemple", "chip") {
        val (name, txt) = Examples.ALL[next % Examples.ALL.size]
        next++
        area.value = txt; App.userText = txt; persist(); refresh()
        area.scrollTop = 0.0
        toast("Exemple chargé : $name")
    }))
    refresh()
    val go = button("Créer ma vidéo", "btn primary wide big", Ic.sparkle) {
        if (area.value.isBlank()) { toast("Colle d'abord ton texte, ou touche « Essayer un exemple »."); area.focus(); return@button }
        App.audio.context() // geste de l'utilisateur : le son pourra être joué (iPhone)
        App.userText = area.value; persist()
        App.lookLocked = false
        try {
            propose(true)
            show(Step.PREVIEW)
        } catch (e: Throwable) { toast(e.message ?: "Texte inutilisable") }
    }
    val goStory = button("Scénario éditable", "btn wide", Ic.palette) {
        if (area.value.isBlank()) { toast("Colle d'abord ton texte, ou touche « Essayer un exemple »."); area.focus(); return@button }
        App.audio.context()
        App.userText = area.value; persist()
        try {
            App.story = Studio.plan(App.userText, newSeed(), voice = App.voiceOn)
            App.storyEp = 0
            show(Step.STORY)
        } catch (e: Throwable) { toast(e.message ?: "Texte inutilisable") }
    }
    page.appendChild(el("div", "dock").add(goStory, go))
    return page
}

private fun newSeed(): Long {
    val a: dynamic = js("new Uint32Array(2)")
    js("crypto.getRandomValues(a)")
    val hi = (a[0] as Double).toLong(); val lo = (a[1] as Double).toLong()
    val s = (hi shl 32) xor lo
    return if (s == 0L) 1L else s
}

private fun propose(regenerate: Boolean) {
    if (regenerate) { App.seed = newSeed(); App.generation++ }
    val p = CreativeDirector.propose(App.userText, App.seed, avoidTemplates = App.recentTemplates,
        avoidLooks = App.recentLooks, forcedLook = if (App.lookLocked) App.look else null)
    App.content = p.storyboard
    App.template = p.template
    App.look = p.look
    App.recentTemplates.add(p.template.id); while (App.recentTemplates.size > 2) App.recentTemplates.removeAt(0)
    App.recentLooks.add(p.look); while (App.recentLooks.size > 2) App.recentLooks.removeAt(0)
    if (!App.musicLocked) App.choice = App.choice.copy(track = p.musicId?.let { id -> App.catalog.firstOrNull { it.id == id } })
}

private fun styleName(): String = when (App.look) {
    Look.KIT -> "${App.look.label} · ${StyleKit.random(App.content.seed).label}"
    Look.INFOGRAPHIE -> "${App.look.label} · ${StyleKit.infographic(App.content.seed).palette.name}"
    else -> App.look.label
}

private fun prepared(): Storyboard = Composer.prepare(App.content, App.template, App.choice.track?.takeIf { it.bpm > 0 })

private fun voiceKey() = "${App.content.scenes.joinToString(",") { "${it.id}:${it.durationMs}:${it.title}:${it.body}" }}|${App.story != null}|${App.content.id}|${App.seed}|${App.content.look}|${App.template.id}|${App.choice.track?.id}|${App.choice.volume}|${App.voiceMode}|${App.generation}|${App.content.scenes.size}"

/**
 * Storyboard final et piste audio. Sans voix : la musique seule (mixée à l'export).
 * Avec voix : chaque phrase est lue, les scènes s'allongent si besoin et la musique baisse sous la voix.
 */
private suspend fun plan(status: (String, Int?) -> Unit): Pair<Storyboard, Pcm?> {
    val base = prepared()
    if (!App.voiceOn) return base to null
    App.voiced?.takeIf { it.first == voiceKey() }?.let { return it.second.storyboard to it.second.audio }
    val track = App.choice.track?.takeIf { App.choice.hasAudio }
    val gain = App.choice.volume.gain
    status("Préparation de la voix off…", null)
    val res = Narration.prepare(base, App.voiceMode, App.voice.languages,
        synth = { text, lang, rate ->
            App.voice.synth(text, lang, rate) { got, total ->
                if (got < total) status("Téléchargement de la voix (une seule fois) : ${(got / 1e6).toInt()} / ${(total / 1e6).toInt()} Mo", (got * 100 / total).toInt())
            }
        },
        music = { total -> track?.let { AudioMix.musicBed(App.audio.toPcm(App.audio.buffer(it)), total, gain) } },
        progress = { d, t -> status("Voix off : phrase $d sur $t", d * 100 / maxOf(1, t)) },
        allowSpeedUp = App.story == null)
    App.voiced = voiceKey() to res
    return res.storyboard to res.audio
}

// ───────────────────────────── 2. Aperçu ─────────────────────────────

private class PreviewUi(val ctrl: HTMLElement, val playBtn: HTMLElement, val seek: HTMLElement, val fill: HTMLElement, val time: HTMLElement)

private fun mmss(ms: Double): String { val s = (ms / 1000).toInt().coerceAtLeast(0); return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }

/** Bouton de la barre d'outils : icône + libellé court ; allumé quand l'option est active. */
private fun toolButton(label: String, icon: String, on: Boolean, hint: String, onClick: () -> Unit): HTMLElement {
    val b = button(label, "tb" + if (on) " on" else "", icon, onClick)
    b.title = hint
    b.setAttribute("aria-label", hint)
    b.setAttribute("aria-pressed", if (on) "true" else "false")
    return b
}

private fun previewPage(): HTMLElement {
    val page = el("main", "page preview-page")
    val dur = el("span", "tag", "…")
    val storyTag = App.story?.let { sp -> el("span", "tag", "Épisode ${App.storyEp + 1}/${sp.episodeCount}") }
    page.appendChild(el("div", "tags").add(*listOfNotNull(el("span", "tag accent", styleName()), storyTag, dur).toTypedArray()))
    if (Store.get("tip") != "0") {
        val tip = el("div", "tip")
        tip.add(el("span", "", "Touche l'aperçu pour le mettre en pause."),
            button("Compris", "") { Store.set("tip", "0"); tip.remove() })
        page.appendChild(tip)
    }

    val stage = el("div", "stage")
    val canvas = el("canvas", "preview")
    canvas.setAttribute("aria-label", "Aperçu de la vidéo")
    val overlay = el("div", "overlay busy")
    val ovIcon = el("div", "ov-icon")
    val ovText = el("div", "ov-text", "Préparation de l'aperçu…")
    val ovBar = el("div", "bar"); val ovFill = el("div", "fill"); ovBar.appendChild(ovFill)
    ovBar.asDynamic().style.visibility = "hidden"
    overlay.add(ovIcon, ovText, ovBar)
    stage.add(canvas, overlay)
    page.appendChild(stage)

    // Lecture / pause / reprise et barre de progression (visibles dès que l'aperçu est prêt).
    val playBtn = button("", "icon-btn", Ic.pause) {}
    playBtn.setAttribute("aria-label", "Lecture / pause")
    val fill = el("div", "fill")
    val seekBar = el("div", "bar"); seekBar.appendChild(fill)
    val seek = el("div", "seek"); seek.appendChild(seekBar)
    val time = el("span", "time", "0:00 / 0:00")
    val ctrl = el("div", "ctrl").add(playBtn, seek, time)
    ctrl.asDynamic().style.visibility = "hidden"
    page.appendChild(ctrl)

    if (!Exporter.supported()) page.appendChild(el("p", "warn", "Ce navigateur affiche l'aperçu mais ne sait pas encore fabriquer la vidéo MP4. Utilise Chrome, Edge, Firefox récent ou Safari 16.4+ (iPhone : iOS 16.4 ou plus)."))

    // Barre d'outils fixe en bas : une seule ligne ; défilement horizontal si l'écran est très étroit.
    val tools = el("div", "toolbar")
    tools.setAttribute("role", "toolbar")
    tools.add(
        toolButton("Voix", Ic.mic, App.voiceOn, if (App.voiceOn) "Voix off activée : touche pour la couper" else "Voix off coupée : touche pour l'activer") {
            App.voiceOn = !App.voiceOn; persist()
            toast(if (App.voiceOn) "Voix off activée" else "Voix off désactivée")
            show(Step.PREVIEW)
        },
        toolButton("Format", Ic.palette, App.lookLocked, "Choisir le format de la vidéo") {
            if (App.story != null) toast("Le mode Scénario est fixe : les autres formats se choisissent depuis « Créer ma vidéo ».") else show(Step.STYLE)
        },
        toolButton("Musique", Ic.music, App.choice.track != null, if (App.choice.track != null) "Musique choisie : ${App.choice.track?.title}" else "Choisir ou importer une musique") { show(Step.MUSIC) },
        if (App.story != null) toolButton("Storyboard", Ic.dice, false, "Voir et modifier les scènes avant le rendu") { show(Step.STORY) }
        else toolButton("Autre version", Ic.dice, false, "Générer une autre version à partir du même texte") {
            try { propose(true); show(Step.PREVIEW) } catch (e: Throwable) { toast(e.message ?: "Erreur") }
        },
    )
    page.appendChild(el("div", "dock").add(tools, button("Générer la vidéo", "btn primary wide big", Ic.download) { show(Step.EXPORT) }))
    window.setTimeout({ startPreview(canvas, dur, overlay, ovIcon, ovText, ovFill, PreviewUi(ctrl, playBtn, seek, fill, time)) }, 0)
    return page
}

private fun stopPreview() {
    App.anim = 0
    App.audio.stop()
}

private fun startPreview(canvas: dynamic, dur: HTMLElement, overlay: HTMLElement, ovIcon: HTMLElement, ovText: HTMLElement, ovFill: HTMLElement, ui: PreviewUi) {
    val id = (window.performance.now() * 1000).toInt() + 1
    App.anim = id
    // Taille d'aperçu : nette sur les écrans haute densité, sans être trop lourde.
    val dpr = minOf(2.0, (window.devicePixelRatio as Double))
    val cssW = ((canvas.clientWidth as Int).takeIf { it > 0 } ?: 300).toDouble()
    var w = (cssW * dpr).toInt().coerceIn(270, 720)
    w -= w % 2
    val h = w * 16 / 9 / 2 * 2
    canvas.width = w; canvas.height = h
    fun busy(text: String, pct: Int?) {
        if (App.anim != id) return
        overlay.className = "overlay busy"
        ovIcon.innerHTML = ""
        ovText.textContent = text
        ovFill.style.width = "${pct ?: 0}%"
        ovFill.parentElement?.asDynamic()?.style?.visibility = if (pct == null) "hidden" else "visible"
    }
    launch(onError = { e ->
        if (App.anim == id) {
            overlay.className = "overlay busy"
            ovIcon.innerHTML = ""
            ovText.textContent = "Voix off impossible : ${e.message}. Vidéo sans voix."
            App.voiceOn = false; persist()
            window.setTimeout({ if (App.anim == id) show(Step.PREVIEW) }, 2600)
        }
    }) {
        busy("Chargement des polices…", null)
        WebFonts.ready()
        val (sb, mixed) = plan(::busy)
        if (App.anim != id) return@launch
        val renderer = FrameRenderer(sb, VideoFormat(w, h, 30), App.template)
        val total = renderer.timeline.totalMs.toDouble()
        dur.textContent = "⏱ ${((total + 500) / 1000).toInt()} s" + if (App.voiceOn) " · voix" else ""
        overlay.className = "overlay"
        ui.ctrl.asDynamic().style.visibility = "visible"
        val gfx = CanvasGfx()
        var paused = false
        var clockStart = window.performance.now()
        var pausedAt = 0.0
        var buf: dynamic = null
        val gain = if (mixed != null) 1f else App.choice.volume.gain
        fun startAudio(fromMs: Double) { if (buf != null) App.audio.play(buf, fromMs, gain) }
        fun setPaused(p: Boolean) {
            if (p == paused) return
            paused = p
            if (paused) {
                pausedAt = window.performance.now() - clockStart; App.audio.stop()
                overlay.className = "overlay paused"; ovIcon.innerHTML = Ic.play; ovText.textContent = ""
                ui.playBtn.querySelector(".ic")?.innerHTML = Ic.play
            } else {
                clockStart = window.performance.now() - pausedAt; startAudio(pausedAt)
                overlay.className = "overlay"; ovIcon.innerHTML = Ic.pause
                ui.playBtn.querySelector(".ic")?.innerHTML = Ic.pause
            }
        }
        canvas.addEventListener("click", { setPaused(!paused) })
        ui.playBtn.addEventListener("click", { setPaused(!paused) })
        ui.seek.addEventListener("click", { e ->
            val r = ui.seek.getBoundingClientRect()
            val frac = ((e.asDynamic().clientX as Double - r.left) / r.width).coerceIn(0.0, 0.999)
            val to = frac * total
            if (paused) pausedAt = to else { clockStart = window.performance.now() - to; App.audio.stop(); startAudio(to) }
        })
        if (mixed != null) { buf = App.audio.fromPcm(mixed); clockStart = window.performance.now(); startAudio(0.0) }
        else App.choice.track?.takeIf { App.choice.hasAudio }?.let { track ->
            launch { val b = App.audio.buffer(track); if (App.anim == id) { buf = b; clockStart = window.performance.now(); startAudio(0.0) } }
        }
        var lastSec = -1
        val totalLabel = mmss(total)
        fun frame(@Suppress("UNUSED_PARAMETER") ts: Double) {
            if (App.anim != id) return
            var t = if (paused) pausedAt else window.performance.now() - clockStart
            if (t >= total) { clockStart = window.performance.now(); t = 0.0; startAudio(0.0) }
            gfx.begin(canvas)
            renderer.render(gfx, t.toFloat())
            ui.fill.style.width = "${(t / total * 100).coerceIn(0.0, 100.0)}%"
            val sec = (t / 1000).toInt()
            if (sec != lastSec) { lastSec = sec; ui.time.textContent = "${mmss(t)} / $totalLabel" }
            window.requestAnimationFrame(::frame)
        }
        window.requestAnimationFrame(::frame)
    }
}

// ───────────────────────────── 3. Style ─────────────────────────────

private fun stylePage(): HTMLElement {
    val page = el("main", "page")
    page.appendChild(subPage("Style de la vidéo", Step.PREVIEW))
    val n = StyleKit.combinations
    page.appendChild(el("p", "muted", "Choisis un format. Chacun change de couleurs, de fond, de polices et d'animations à chaque version : plus de ${fmtBig(n)} combinaisons, toujours lisibles."))
    val grid = el("div", "grid")
    for (l in Look.entries) {
        val cell = el("div", "look" + if (l == App.look) " selected" else "")
        val c = el("canvas", "thumb")
        cell.add(c, el("div", "look-name", l.label), el("div", "look-tag", l.tagline))
        cell.addEventListener("click", {
            App.look = l; App.lookLocked = true
            App.content = App.content.copy(look = l)
            show(Step.PREVIEW)
        })
        grid.appendChild(cell)
        window.setTimeout({ thumbnail(c, App.content.copy(look = l)) }, 30)
    }
    page.appendChild(grid)
    return page
}

private fun fmtBig(n: Long): String {
    val s = n.toString()
    return s.reversed().chunked(3).joinToString(" ").reversed()
}

private fun thumbnail(canvas: dynamic, content: Storyboard) {
    try {
        val sb = Composer.prepare(content, App.template, null)
        val r = FrameRenderer(sb, VideoFormat(216, 384, 30), App.template)
        val slot = r.timeline.slots.getOrNull(1) ?: r.timeline.slots.first()
        canvas.width = 216; canvas.height = 384
        val g = CanvasGfx()
        g.begin(canvas)
        r.render(g, (slot.startMs + (slot.endMs - slot.startMs) * 0.6).toFloat())
    } catch (e: Throwable) { console.warn(e) }
}

// ───────────────────────────── 4. Musique ─────────────────────────────

private fun musicPage(): HTMLElement {
    val page = el("main", "page")
    page.appendChild(subPage("Musique", Step.PREVIEW))
    var playing: String? = null
    val list = el("div", "list")

    // Importer sa propre musique (le fichier reste sur l'appareil).
    val input = el("input") as org.w3c.dom.HTMLInputElement
    input.type = "file"; input.accept = "audio/*,.mp3,.m4a,.aac,.wav,.ogg,.opus,.flac"; input.className = "hidden"
    val importBtn = button("Importer ma musique", "btn primary wide", Ic.upload) { input.click() }
    page.add(input, importBtn, el("p", "muted small center", "MP3, M4A, WAV, OGG… depuis ton téléphone ou ton ordinateur. Elle reste sur ton appareil."))
    input.addEventListener("change", {
        val f: dynamic = input.files?.item(0) ?: return@addEventListener
        importBtn.classList.add("loading"); importBtn.lastElementChild?.textContent = "Import en cours…"
        launch(onError = { e -> toast(e.message ?: "Import impossible"); show(Step.MUSIC) }) {
            val t = MyMusic.import(f, App.audio)
            App.mine = listOf(t) + App.mine.filter { it.file != t.file }
            App.choice = App.choice.copy(track = t, volume = if (App.choice.volume == MusicVolume.COUPE) MusicVolume.NORMAL else App.choice.volume)
            App.musicLocked = true
            toast("« ${t.title} » ajoutée")
            show(Step.MUSIC)
        }
    })

    fun row(t: MusicTrack?, title: String, sub: String, removable: Boolean, refill: () -> Unit): HTMLElement {
        val selected = (t == null && App.choice.track == null) || (t != null && App.choice.track?.id == t.id)
        val row = el("div", "track" + if (selected) " selected" else "")
        if (t != null) {
            val play = button("", "play", if (playing == t.id) Ic.pause else Ic.play) {}
            play.addEventListener("click", { e ->
                e.stopPropagation()
                if (playing == t.id) { App.audio.stop(); playing = null; refill() }
                else launch(onError = { toast(it.message ?: "Lecture impossible") }) {
                    val b = App.audio.buffer(t)
                    App.audio.play(b, if (t.category == MusicCategory.PERSO) 0.0 else 6000.0, App.choice.volume.gain.coerceAtLeast(0.6f))
                    playing = t.id; refill()
                }
            })
            row.appendChild(play)
        } else row.appendChild(el("div", "play ghost", "∅"))
        val txt = el("div", "t-text")
        txt.add(el("div", "t-title", title), el("div", "t-sub", sub))
        row.appendChild(txt)
        if (selected) row.appendChild(el("span", "check", "✓"))
        if (removable && t != null) row.appendChild(button("", "icon-btn danger", Ic.trash) {}.also { b ->
            b.addEventListener("click", { e ->
                e.stopPropagation()
                launch {
                    MyMusic.remove(t.file); App.audio.forget(t.id)
                    App.mine = App.mine.filter { it.id != t.id }
                    if (App.choice.track?.id == t.id) App.choice = App.choice.copy(track = null)
                    show(Step.MUSIC)
                }
            })
        })
        row.addEventListener("click", { App.choice = App.choice.copy(track = t); App.musicLocked = true; App.voiced = null; refill() })
        return row
    }

    fun fill() {
        list.innerHTML = ""
        if (App.mine.isNotEmpty()) {
            list.appendChild(el("div", "label", "Mes musiques"))
            for (t in App.mine) list.appendChild(row(t, t.title, if (t.durationMs > 0) "${t.durationMs / 60000}:${((t.durationMs / 1000) % 60).toString().padStart(2, '0')} · importée" else "importée", true, ::fill))
        }
        list.appendChild(el("div", "label", "Musiques VIDEO24"))
        list.appendChild(row(null, "Sans musique", "Seulement la voix, ou le silence", false, ::fill))
        // Choix réduit : une musique par ambiance (6 au plus), plus celle déjà choisie.
        val few = (App.catalog.groupBy { it.category }.values.map { it.first() }.take(6) + listOfNotNull(App.choice.track?.takeIf { c -> App.catalog.any { it.id == c.id } })).distinctBy { it.id }
        for (t in few) list.appendChild(row(t, t.title, "${t.category.label} · ${t.bpm} BPM", false, ::fill))
    }
    val vol = el("div", "chips")
    vol.appendChild(el("span", "muted small vlabel", "Volume"))
    for (v in MusicVolume.entries) vol.appendChild(button(v.label, "chip" + if (App.choice.volume == v) " on" else "") { App.choice = App.choice.copy(volume = v); App.voiced = null; persist(); show(Step.MUSIC) })
    page.add(vol, list)
    page.appendChild(el("p", "muted small", "Musiques VIDEO24 : compositions originales (licence GPL-3.0). Pour une musique importée, assure-toi d'avoir le droit de l'utiliser."))
    page.appendChild(el("div", "dock").add(button("Valider", "btn primary wide big") { App.audio.stop(); show(Step.PREVIEW) }))
    fill()
    return page
}

// ───────────────────────────── Storyboard éditable (mode Scénario) ─────────────────────────────

/** Charge l'épisode [k] (0 = premier) dans l'aperçu / l'export existants, puis ouvre [target]. */
private fun openEpisode(k: Int, target: Step) {
    val sp = App.story ?: return
    App.storyEp = k.coerceIn(0, sp.episodeCount - 1)
    val ep = sp.series[App.storyEp]
    App.content = ep
    App.template = sp.template
    App.look = ep.look
    App.lookLocked = true
    App.voiced = null
    if (!App.musicLocked) App.choice = App.choice.copy(track = sp.musicId?.let { id -> App.catalog.firstOrNull { it.id == id } })
    show(target)
}

/** Applique une modification du plan puis réaffiche la page sans remonter en haut. */
private fun updateStory(f: (StoryPlan) -> StoryPlan) {
    val sp = App.story ?: return
    try {
        val y = window.scrollY
        App.story = f(sp)
        App.storyEp = App.storyEp.coerceIn(0, App.story!!.episodeCount - 1)
        show(Step.STORY)
        window.scrollTo(0.0, y)
    } catch (e: Throwable) { toast(e.message ?: "Modification impossible") }
}

private fun storyPage(): HTMLElement {
    val sp = App.story!!
    val page = el("main", "page")
    page.appendChild(subPage("Storyboard", Step.TEXT))
    val fid = sp.fidelity()
    page.appendChild(el("p", "sb-sum", "${sp.episodeCount} épisode(s) · ${fid.summary()}"))
    for (m in sp.qc().take(6)) page.appendChild(el("p", "sb-warn", m))
    // Choix de l'épisode
    val eps = el("div", "sb-eps")
    for (k in 0 until sp.episodeCount) eps.appendChild(button("Épisode ${k + 1}", "chip" + if (k == App.storyEp) " on" else "") { App.storyEp = k; val y = window.scrollY; show(Step.STORY); window.scrollTo(0.0, y) })
    page.appendChild(eps)

    val ep = sp.series[App.storyEp.coerceIn(0, sp.episodeCount - 1)]
    val cards = sp.cards().filter { it.episode == (ep.episode?.index ?: 1) }
    val sb = Composer.prepare(ep, sp.template, null)
    val renderer = FrameRenderer(sb, VideoFormat(216, 384, 30), sp.template)
    for (c in cards) {
        val scene: Scene? = ep.scenes.firstOrNull { it.id == c.id }
        val opener = scene?.isEpisodeOpener ?: false
        val card = el("div", "card sb-card")
        val canvas = el("canvas")
        canvas.setAttribute("aria-label", "Aperçu de la scène ${c.index}")
        val body = el("div", "sb-body")
        val head = el("div", "sb-head")
        head.add(el("b", "", "Scène ${c.index}"), el("span", "", "${c.layout} · ${(c.durationMs + 50) / 100 / 10.0} s"))
        if (c.userEdits.isNotEmpty()) head.appendChild(el("span", "sb-edited", "modifiée : ${c.userEdits.joinToString(", ")}"))
        body.appendChild(head)
        body.appendChild(el("p", "sb-text", c.text.ifBlank { "—" }))
        body.appendChild(el("p", "sb-intent", "Image : " + c.image + (c.visualLabel?.let { l -> " — « $l » (adéquation ${((c.visualFit ?: 0f) * 100).toInt()} %)" } ?: "")))
        val d = el("details", "sb-details")
        d.appendChild(el("summary", "", "Fiche complète (13 éléments)"))
        val dl = el("dl")
        fun row(k: String, v: String?) { dl.appendChild(el("dt", "", k)); dl.appendChild(el("dd", "", v?.ifBlank { null } ?: "—")) }
        row("Texte exact", c.text); row("Narration", c.narration); row("Description de l'image", c.image)
        row("Personnages", c.characters.joinToString(", ")); row("Objets", c.objects.joinToString(", ")); row("Action", c.action)
        row("Décor", c.setting); row("Animation", c.animation); row("Cadrage", c.framing); row("Caméra", c.camera)
        row("Durée", "${c.durationMs / 1000.0} s"); row("Transition", c.transition); row("Son", c.audio); row("Intention", c.intent)
        d.appendChild(dl)
        body.appendChild(d)
        if (opener || scene == null) {
            body.appendChild(el("p", "sb-intent", "Carte d'ouverture ajoutée automatiquement : non modifiable."))
        } else {
            val id = c.id
            val ctl = el("div", "sb-ctl")
            // Visuel : liste des visuels envisageables, du plus adapté au moins adapté.
            val sel = document.createElement("select") as org.w3c.dom.HTMLSelectElement
            sel.setAttribute("aria-label", "Visuel de la scène ${c.index}")
            fun opt(value: String, label: String) { val o = document.createElement("option") as org.w3c.dom.HTMLOptionElement; o.value = value; o.textContent = label; sel.appendChild(o) }
            opt(Pins.TEXT_ONLY, "Texte seul")
            for (a in sp.alternatives(id, 12)) opt(a.key, "${a.label} · ${(a.fit * 100).toInt()} %" + (if (a.belowThreshold) " (faible)" else "") + (if (a.usedBy != null) " (déjà utilisé)" else ""))
            sel.value = sp.pins[id] ?: Pins.TEXT_ONLY
            sel.addEventListener("change", { updateStory { it.replaceVisual(id, sel.value) } })
            ctl.appendChild(sel)
            ctl.appendChild(button("Autre visuel", "btn small", Ic.dice) { updateStory { it.reroll(id) } })
            body.appendChild(ctl)
            val ctl2 = el("div", "sb-ctl")
            ctl2.appendChild(button("− 1 s", "btn small") { updateStory { it.withDuration(id, scene.durationMs - 1000) } })
            ctl2.appendChild(button("+ 1 s", "btn small") { updateStory { it.withDuration(id, scene.durationMs + 1000) } })
            val tsel = document.createElement("select") as org.w3c.dom.HTMLSelectElement
            tsel.setAttribute("aria-label", "Transition de la scène ${c.index}")
            for (t in TransitionType.entries) { val o = document.createElement("option") as org.w3c.dom.HTMLOptionElement; o.value = t.name; o.textContent = t.name.lowercase(); tsel.appendChild(o) }
            tsel.value = scene.transitionOut.type.name
            tsel.addEventListener("change", { updateStory { it.withTransition(id, Transition(TransitionType.valueOf(tsel.value), if (tsel.value == "CUT") 0 else 500)) } })
            ctl2.appendChild(tsel)
            body.appendChild(ctl2)
            if (scene.body.isNotBlank()) {
                val ta = document.createElement("textarea") as org.w3c.dom.HTMLTextAreaElement
                ta.value = scene.body
                ta.setAttribute("aria-label", "Texte de la scène ${c.index}")
                val ctl3 = el("div", "sb-ctl")
                ctl3.appendChild(ta)
                ctl3.appendChild(button("Appliquer le texte", "btn small") { updateStory { it.withText(id, body = ta.value) } })
                body.appendChild(ctl3)
            }
        }
        card.add(canvas, body)
        page.appendChild(card)
        val slot = renderer.timeline.slots.firstOrNull { it.scene.id == c.id }
        if (slot != null) window.setTimeout({
            try {
                canvas.asDynamic().width = 216; canvas.asDynamic().height = 384
                val g = CanvasGfx(); g.begin(canvas)
                renderer.render(g, (slot.startMs + (slot.endMs - slot.startMs) * 0.7).toFloat())
            } catch (e: Throwable) { console.error(e) }
        }, 30)
    }
    val tools = el("div", "row")
    tools.appendChild(button("Redécouper les épisodes", "btn small") { updateStory { it.resplit() } })
    page.appendChild(tools)
    page.appendChild(el("div", "dock").add(
        button("Aperçu de l'épisode ${App.storyEp + 1}", "btn primary wide big", Ic.play) { openEpisode(App.storyEp, Step.PREVIEW) }))
    return page
}

// ───────────────────────────── 5. Génération ─────────────────────────────

private fun exportPage(): HTMLElement {
    val page = el("main", "page")
    val ring = el("div", "ring")
    val pct = el("div", "pct", "0 %")
    ring.appendChild(pct)
    val status = el("div", "muted center", "Préparation…")
    val bar = el("div", "bar"); val fill = el("div", "fill"); bar.appendChild(fill)
    val result = el("div", "result")
    val cancel = button("Annuler", "btn ghost wide") { App.cancelExport = true }
    page.add(ring, status, bar, cancel, result)
    if (!Exporter.supported()) {
        status.textContent = "Ce navigateur ne sait pas fabriquer la vidéo MP4. Mets-le à jour : Chrome, Edge, Firefox 130+, ou Safari 16.4+ sur iPhone."
        pct.textContent = "—"; cancel.remove()
        return page
    }
    App.exporting = true
    App.cancelExport = false
    fun progress(p: Int) { pct.textContent = "$p %"; fill.style.width = "$p%"; ring.asDynamic().style.setProperty("--p", "$p") }
    launch(onError = { e ->
        App.exporting = false
        pct.textContent = "!"
        ring.classList.add("err")
        status.textContent = "La génération a échoué : ${e.message ?: "erreur inconnue"}"
        cancel.remove()
        result.add(button("Réessayer", "btn primary wide") { show(Step.EXPORT) }, button("Retour à l'aperçu", "btn wide") { show(Step.PREVIEW) })
        applyUpdateWhenIdle()
    }) {
        // Si la voix off échoue, la vidéo est quand même générée, avec la musique seule.
        val (sb, mixed) = try { plan { s, _ -> status.textContent = s } } catch (e: Throwable) {
            toast("Voix off impossible (${e.message}) : vidéo sans voix."); App.voiceOn = false; prepared() to null
        }
        WebFonts.ready()
        val track = App.choice.track?.takeIf { App.choice.hasAudio }
        val pcm = if (mixed == null) track?.let { status.textContent = "Préparation de la musique…"; App.audio.toPcm(App.audio.buffer(it)) } else null
        status.textContent = "Rendu des images…"
        val t0 = window.performance.now()
        val r = Exporter.export(sb, App.template, pcm, App.choice.volume.gain, { done, total ->
            val p = done * 100 / maxOf(1, total)
            progress(p)
            if (p in 3..99) {
                val elapsed = window.performance.now() - t0
                val left = (elapsed * (100 - p) / p / 1000).toInt().coerceAtLeast(1)
                status.textContent = "Rendu des images… environ $left s restantes"
            }
        }, { App.cancelExport }, premixed = mixed)
        App.exporting = false
        cancel.remove()
        progress(100)
        pct.textContent = "✓"
        ring.classList.add("ok")
        status.textContent = "Vidéo prête · ${(Timeline.build(sb).totalMs + 500) / 1000} s" + (if (mixed != null) " · voix off" else "") +
            (r.audioCodec?.let { "" } ?: if (track != null || mixed != null) " · sans son (ton navigateur ne sait pas encoder l'audio)" else "")
        App.lastVideoUrl?.let { old -> js("URL.revokeObjectURL(old)") }
        val bytes = uint8(r.bytes)
        val blob: dynamic = js("new Blob([bytes], {type: 'video/mp4'})")
        val url = js("URL.createObjectURL(blob)") as String
        App.lastVideoUrl = url
        val video = el("video", "out")
        video.setAttribute("controls", ""); video.setAttribute("playsinline", ""); video.setAttribute("src", url)
        result.appendChild(video)
        val epSuffix = App.story?.let { sp -> "_ep${App.storyEp + 1}sur${sp.episodeCount}" } ?: ""
        val name = App.content.title.take(40).replace(Regex("[^A-Za-z0-9À-ÿ]+"), "_").trim('_').ifEmpty { "video" } + epSuffix + ".mp4"
        val actions = el("div", "row")
        val dl = el("a", "btn primary")
        dl.appendChild(el("span", "ic").also { it.innerHTML = Ic.download }); dl.appendChild(el("span", "", "Enregistrer"))
        dl.setAttribute("href", url); dl.setAttribute("download", name)
        actions.appendChild(dl)
        val file: dynamic = js("new File([blob], name, {type: 'video/mp4'})")
        val canShare = js("!!(navigator.canShare && navigator.canShare({files: [file]}))") as Boolean
        if (canShare) actions.appendChild(button("Partager", "btn", Ic.share) {
            launch { (window.navigator.asDynamic().share(obj("files" to arrayOf(file), "title" to App.content.title)) as kotlin.js.Promise<dynamic>).await() }
        })
        result.appendChild(actions)
        if (isIos()) result.appendChild(el("p", "muted small", "Sur iPhone : « Partager » puis « Enregistrer la vidéo » la met dans Photos."))
        val sp = App.story
        if (sp != null) {
            if (App.storyEp + 1 < sp.episodeCount) result.add(button("Épisode suivant (${App.storyEp + 2}/${sp.episodeCount})", "btn primary wide") { openEpisode(App.storyEp + 1, Step.EXPORT) })
            result.add(button("Retour au storyboard", "btn wide", Ic.palette) { show(Step.STORY) }, button("Nouveau texte", "btn ghost wide") { App.story = null; show(Step.TEXT) })
        } else {
            result.add(button("Une autre version", "btn wide", Ic.dice) { try { propose(true); show(Step.PREVIEW) } catch (e: Throwable) { toast(e.message ?: "Erreur") } },
                button("Nouveau texte", "btn ghost wide") { show(Step.TEXT) })
        }
        applyUpdateWhenIdle()
    }
    return page
}
