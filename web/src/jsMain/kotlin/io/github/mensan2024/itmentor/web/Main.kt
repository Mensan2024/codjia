// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Layout
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.MusicCategory
import io.github.mensan2024.itmentor.engine.MusicChoice
import io.github.mensan2024.itmentor.engine.MusicTrack
import io.github.mensan2024.itmentor.engine.MusicVolume
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.StyleKit
import io.github.mensan2024.itmentor.engine.Template
import io.github.mensan2024.itmentor.engine.TextSanitizer
import io.github.mensan2024.itmentor.engine.VideoFormat
import io.github.mensan2024.itmentor.prototype.Examples
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * CODJIA dans le navigateur (iPhone, Android, ordinateur) : même parcours que l'application Android.
 * Texte → aperçu animé → style / musique (facultatif) → génération MP4 → enregistrer ou partager.
 * Tout se passe sur l'appareil : aucun texte, aucune vidéo n'est envoyé sur Internet.
 */
private val base: String = run {
    val p = window.location.pathname
    if (p.endsWith("/")) p else p.substringBeforeLast('/') + "/"
}

private enum class Step { TEXT, PREVIEW, STYLE, MUSIC, EXPORT }

private object App {
    val audio = WebAudio(base)
    var catalog: List<MusicTrack> = emptyList()
    var step = Step.TEXT
    var userText = Examples.ALL.first().second
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
    var anim = 0          // identifiant de la boucle d'aperçu en cours (0 = aucune)
    var exporting = false
    var cancelExport = false
    var lastVideoUrl: String? = null
    var installPrompt: dynamic = null
}

fun main() {
    window.addEventListener("beforeinstallprompt", { e -> e.preventDefault(); App.installPrompt = e.asDynamic(); renderHeaderInstall() })
    if (js("'serviceWorker' in navigator") as Boolean) {
        window.addEventListener("load", { window.navigator.asDynamic().serviceWorker.register("sw.js") })
    }
    val root = document.getElementById("app") as HTMLElement
    root.innerHTML = "<div class='loading'>Chargement de CODJIA…</div>"
    launch(onError = { fatal(it) }) {
        WebFonts.loadAll(base)
        App.catalog = App.audio.catalog()
        // Point d'accès pour les tests automatiques (aucune donnée envoyée nulle part).
        window.asDynamic().codjiaReady = true
        show(Step.TEXT)
    }
}

private fun fatal(e: Throwable) {
    console.error(e)
    val root = document.getElementById("app") as HTMLElement
    root.innerHTML = ""
    root.appendChild(el("div", "card error", "Oups : ${e.message ?: "erreur inconnue"}. Recharge la page."))
}

// ───────────────────────────── petits outils DOM ─────────────────────────────

private fun el(tag: String, cls: String = "", text: String? = null): HTMLElement {
    val e = document.createElement(tag) as HTMLElement
    if (cls.isNotEmpty()) e.className = cls
    if (text != null) e.textContent = text
    return e
}

private fun button(text: String, cls: String = "btn", onClick: () -> Unit): HTMLElement =
    el("button", cls, text).also { it.addEventListener("click", { onClick() }) }

private fun HTMLElement.add(vararg children: HTMLElement): HTMLElement { for (c in children) appendChild(c); return this }

private fun toast(msg: String) {
    val t = el("div", "toast", msg)
    document.body?.appendChild(t)
    window.setTimeout({ t.remove() }, 3800)
}

// ───────────────────────────── navigation ─────────────────────────────

private fun show(s: Step) {
    stopPreview()
    App.step = s
    val root = document.getElementById("app") as HTMLElement
    root.innerHTML = ""
    root.appendChild(header(s))
    val body = when (s) {
        Step.TEXT -> textPage()
        Step.PREVIEW -> previewPage()
        Step.STYLE -> stylePage()
        Step.MUSIC -> musicPage()
        Step.EXPORT -> exportPage()
    }
    root.appendChild(body)
    root.appendChild(el("p", "privacy", "Tout se passe sur ton appareil : ton texte et tes vidéos ne sont envoyés nulle part. Logiciel libre (GPL-3.0)."))
    window.scrollTo(0.0, 0.0)
}

private fun header(s: Step): HTMLElement {
    val h = el("header", "top")
    val brand = el("div", "brand", "▶_ CODJIA")
    brand.addEventListener("click", { if (!App.exporting) show(Step.TEXT) })
    h.appendChild(brand)
    val back = when (s) {
        Step.PREVIEW -> "‹ Texte" to Step.TEXT
        Step.STYLE, Step.MUSIC -> "‹ Aperçu" to Step.PREVIEW
        Step.EXPORT -> if (!App.exporting) "‹ Aperçu" to Step.PREVIEW else null
        else -> null
    }
    back?.let { (label, target) -> h.appendChild(button(label, "link") { show(target) }) }
    h.appendChild(el("span", "install-slot").also { it.id = "install-slot" })
    val wrap = el("div")
    wrap.appendChild(h)
    wrap.appendChild(el("h1", "", when (s) {
        Step.TEXT -> "Ton texte"; Step.PREVIEW -> "Ta vidéo"; Step.STYLE -> "Style de la vidéo"; Step.MUSIC -> "Musique"; Step.EXPORT -> "Génération"
    }))
    window.setTimeout({ renderHeaderInstall() }, 0)
    return wrap
}

private fun isIos(): Boolean = js("/iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)") as Boolean
private fun isStandalone(): Boolean = js("window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true") as Boolean

private fun renderHeaderInstall() {
    val slot = document.getElementById("install-slot") as? HTMLElement ?: return
    slot.innerHTML = ""
    if (isStandalone()) return
    if (App.installPrompt != null) {
        slot.appendChild(button("Installer l'appli", "btn small") {
            val p = App.installPrompt
            App.installPrompt = null
            p.prompt()
            renderHeaderInstall()
        })
    } else if (isIos()) {
        slot.appendChild(button("Installer", "btn small ghost") {
            toast("Sur iPhone : touche le bouton Partager (carré avec flèche), puis « Sur l'écran d'accueil ».")
        })
    }
}

// ───────────────────────────── 1. Texte ─────────────────────────────

private fun textPage(): HTMLElement {
    val page = el("main", "page")
    page.appendChild(el("p", "muted", "Colle ou écris ton astuce. CODJIA choisit la mise en scène, la musique et le style, et les change à chaque version."))
    val area = el("textarea", "text") as org.w3c.dom.HTMLTextAreaElement
    area.value = App.userText
    area.maxLength = TextSanitizer.MAX_CHARS
    area.placeholder = "Ex. : Astuce 028 — Active le pare-feu de ton PC…"
    val counter = el("div", "counter", "${area.value.length} / ${TextSanitizer.MAX_CHARS}")
    area.addEventListener("input", { counter.textContent = "${area.value.length} / ${TextSanitizer.MAX_CHARS}" })
    page.add(area, counter)
    val chips = el("div", "chips")
    for ((name, text) in Examples.ALL) chips.appendChild(button(name, "chip") { area.value = text; counter.textContent = "${text.length} / ${TextSanitizer.MAX_CHARS}" })
    page.appendChild(chips)
    page.appendChild(button("Créer ma vidéo  →", "btn primary wide") {
        App.audio.context() // geste de l'utilisateur : le son pourra être joué (iPhone)
        App.userText = area.value
        App.lookLocked = false
        try {
            propose(true)
            show(Step.PREVIEW)
        } catch (e: Throwable) { toast(e.message ?: "Texte inutilisable") }
    })
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

private fun styleName(): String = if (App.look == Look.KIT) "${App.look.label} (${StyleKit.random(App.content.seed).label})" else App.look.label

private fun prepared(): Storyboard = Composer.prepare(App.content, App.template, App.choice.track?.takeIf { it.bpm > 0 })

// ───────────────────────────── 2. Aperçu ─────────────────────────────

private fun previewPage(): HTMLElement {
    val page = el("main", "page")
    val music = App.choice.track?.let { if (App.choice.hasAudio) it.title else "${it.title} (coupée)" } ?: "Sans musique"
    val hasDemo = App.content.scenes.any { it.layout == Layout.SCREEN }
    val info = el("p", "muted small", "Version ${App.generation} · ${styleName()}${if (hasDemo) " + démo à l'écran" else ""} · $music")
    page.appendChild(info)
    val frame = el("div", "stage")
    val canvas = el("canvas", "preview")
    frame.appendChild(canvas)
    val hint = el("div", "hint", "Touche l'aperçu pour mettre en pause")
    page.add(frame, hint)
    val row = el("div", "row")
    row.add(button("Style", "btn") { show(Step.STYLE) }, button("Musique", "btn") { show(Step.MUSIC) },
        button("Autre version", "btn") { try { propose(true); show(Step.PREVIEW) } catch (e: Throwable) { toast(e.message ?: "Erreur") } })
    page.appendChild(row)
    page.appendChild(button("Générer la vidéo", "btn primary wide") { show(Step.EXPORT) })
    if (!Exporter.supported()) page.appendChild(el("p", "warn", "Ce navigateur affiche l'aperçu mais ne sait pas encore fabriquer la vidéo MP4. Utilise Chrome, Edge, Firefox récent ou Safari 16.4+ (iPhone : iOS 16.4 ou plus)."))
    window.setTimeout({ startPreview(canvas, info, hint) }, 0)
    return page
}

private fun stopPreview() {
    App.anim = 0
    App.audio.stop()
}

private fun startPreview(canvas: dynamic, info: HTMLElement, hint: HTMLElement) {
    val sb = try { prepared() } catch (e: Throwable) { hint.textContent = "Aperçu impossible : ${e.message}. Essaie « Autre version »."; return }
    // Taille d'aperçu : nette sur les écrans haute densité, sans être trop lourde.
    val dpr = minOf(2.0, (window.devicePixelRatio as Double))
    val cssW = ((canvas.clientWidth as Int).takeIf { it > 0 } ?: 300).toDouble()
    var w = (cssW * dpr).toInt().coerceIn(270, 720)
    w -= w % 2
    val h = w * 16 / 9 / 2 * 2
    canvas.width = w; canvas.height = h
    val renderer = try { FrameRenderer(sb, VideoFormat(w, h, 30), App.template) } catch (e: Throwable) { hint.textContent = "Aperçu impossible : ${e.message}"; return }
    val total = renderer.timeline.totalMs.toDouble()
    info.textContent = (info.textContent ?: "") + " · ${((total + 500) / 1000).toInt()} s"
    val gfx = CanvasGfx()
    val id = (window.performance.now() * 1000).toInt() + 1
    App.anim = id
    var paused = false
    var clockStart = window.performance.now()
    var pausedAt = 0.0
    var musicBuf: dynamic = null
    val track = App.choice.track?.takeIf { App.choice.hasAudio }
    fun startMusic(fromMs: Double) { if (musicBuf != null) App.audio.play(musicBuf, fromMs, App.choice.volume.gain) }
    canvas.addEventListener("click", {
        paused = !paused
        if (paused) { pausedAt = window.performance.now() - clockStart; App.audio.stop(); hint.textContent = "En pause · touche pour reprendre" }
        else { clockStart = window.performance.now() - pausedAt; startMusic(pausedAt); hint.textContent = "Touche l'aperçu pour mettre en pause" }
    })
    if (track != null) launch {
        val b = App.audio.buffer(track)
        if (App.anim == id) { musicBuf = b; clockStart = window.performance.now(); startMusic(0.0) }
    }
    fun frame(@Suppress("UNUSED_PARAMETER") ts: Double) {
        if (App.anim != id) return
        var t = if (paused) pausedAt else window.performance.now() - clockStart
        if (t >= total) { clockStart = window.performance.now(); t = 0.0; startMusic(0.0) }
        gfx.begin(canvas)
        renderer.render(gfx, t.toFloat())
        window.requestAnimationFrame(::frame)
    }
    window.requestAnimationFrame(::frame)
}

// ───────────────────────────── 3. Style ─────────────────────────────

private fun stylePage(): HTMLElement {
    val page = el("main", "page")
    page.appendChild(el("p", "muted", "Choisis un format. « Style unique » tire une nouvelle combinaison (couleurs, fond, titres, sous-titres, mascotte) à chaque version."))
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
    val chips = el("div", "chips")
    val list = el("div", "list")
    var playing: String? = null
    fun fill() {
        list.innerHTML = ""
        val none = el("div", "track" + if (App.choice.track == null) " selected" else "")
        none.add(el("div", "t-title", "Sans musique"))
        none.addEventListener("click", { App.choice = App.choice.copy(track = null); App.musicLocked = true; App.audio.stop(); fill() })
        list.appendChild(none)
        for (t in App.catalog.filter { App.category == null || it.category == App.category }) {
            val row = el("div", "track" + if (App.choice.track?.id == t.id) " selected" else "")
            val txt = el("div", "t-text")
            txt.add(el("div", "t-title", t.title), el("div", "t-sub", "${t.category.label} · ${t.bpm} BPM"))
            val play = button(if (playing == t.id) "■" else "▶", "play") {}
            play.addEventListener("click", { e ->
                e.stopPropagation()
                if (playing == t.id) { App.audio.stop(); playing = null; fill() }
                else launch { val b = App.audio.buffer(t); App.audio.play(b, 6000.0, App.choice.volume.gain); playing = t.id; fill() }
            })
            row.add(play, txt)
            row.addEventListener("click", { App.choice = App.choice.copy(track = t); App.musicLocked = true; fill() })
            list.appendChild(row)
        }
    }
    val cats = MusicCategory.entries.filter { c -> App.catalog.any { it.category == c } }
    chips.appendChild(button("Toutes", "chip" + if (App.category == null) " on" else "") { App.category = null; show(Step.MUSIC) })
    for (c in cats) chips.appendChild(button(c.label, "chip" + if (App.category == c) " on" else "") { App.category = c; show(Step.MUSIC) })
    page.add(chips, list)
    val vol = el("div", "chips")
    vol.appendChild(el("span", "muted small", "Volume  "))
    for (v in MusicVolume.entries) vol.appendChild(button(v.label, "chip" + if (App.choice.volume == v) " on" else "") { App.choice = App.choice.copy(volume = v); show(Step.MUSIC) })
    page.appendChild(vol)
    page.appendChild(el("p", "muted small", "Musiques originales CODJIA (licence GPL-3.0), sans droits à payer."))
    page.appendChild(button("Valider", "btn primary wide") { App.audio.stop(); show(Step.PREVIEW) })
    fill()
    return page
}

// ───────────────────────────── 5. Génération ─────────────────────────────

private fun exportPage(): HTMLElement {
    val page = el("main", "page")
    val pct = el("div", "pct", "0 %")
    val status = el("div", "muted center", "Préparation…")
    val bar = el("div", "bar"); val fill = el("div", "fill"); bar.appendChild(fill)
    val result = el("div", "result")
    val cancel = button("Annuler", "btn ghost wide") { App.cancelExport = true }
    page.add(pct, status, bar, cancel, result)
    if (!Exporter.supported()) {
        status.textContent = "Ce navigateur ne sait pas fabriquer la vidéo MP4. Mets-le à jour : Chrome, Edge, Firefox 130+, ou Safari 16.4+ sur iPhone."
        pct.textContent = "—"; cancel.remove()
        return page
    }
    App.exporting = true
    App.cancelExport = false
    launch(onError = { e ->
        App.exporting = false
        pct.textContent = "❌"
        status.textContent = "La génération a échoué : ${e.message ?: "erreur inconnue"}"
        cancel.remove()
        result.add(button("Réessayer", "btn primary wide") { show(Step.EXPORT) }, button("Retour à l'aperçu", "btn wide") { show(Step.PREVIEW) })
    }) {
        val sb = prepared()
        val track = App.choice.track?.takeIf { App.choice.hasAudio }
        val pcm = track?.let { status.textContent = "Préparation de la musique…"; App.audio.toPcm(App.audio.buffer(it)) }
        status.textContent = "Rendu des images…"
        val t0 = window.performance.now()
        val r = Exporter.export(sb, App.template, pcm, App.choice.volume.gain, { done, total ->
            val p = done * 100 / maxOf(1, total)
            pct.textContent = "$p %"
            fill.style.width = "$p%"
            if (p in 3..99) {
                val elapsed = window.performance.now() - t0
                val left = (elapsed * (100 - p) / p / 1000).toInt().coerceAtLeast(1)
                status.textContent = "Rendu des images… environ $left s restantes"
            }
        }, { App.cancelExport })
        App.exporting = false
        cancel.remove()
        pct.textContent = "✅"
        status.textContent = "Vidéo prête · ${r.width}×${r.height}" + (r.audioCodec?.let { " · son $it" } ?: if (track != null) " · sans son (ton navigateur ne sait pas encoder l'audio)" else "") +
            " · ${(r.ms / 1000).toInt()} s"
        App.lastVideoUrl?.let { old -> js("URL.revokeObjectURL(old)") }
        val bytes = uint8(r.bytes)
        val blob: dynamic = js("new Blob([bytes], {type: 'video/mp4'})")
        val url = js("URL.createObjectURL(blob)") as String
        App.lastVideoUrl = url
        val video = el("video", "out")
        video.setAttribute("controls", ""); video.setAttribute("playsinline", ""); video.setAttribute("src", url)
        result.appendChild(video)
        val name = "CODJIA_" + App.content.title.take(40).replace(Regex("[^A-Za-z0-9À-ÿ]+"), "_").trim('_') + ".mp4"
        val actions = el("div", "row")
        val dl = el("a", "btn primary", "Enregistrer la vidéo")
        dl.setAttribute("href", url); dl.setAttribute("download", name)
        actions.appendChild(dl)
        val file: dynamic = js("new File([blob], name, {type: 'video/mp4'})")
        val canShare = js("!!(navigator.canShare && navigator.canShare({files: [file]}))") as Boolean
        if (canShare) actions.appendChild(button("Partager", "btn") {
            launch { (window.navigator.asDynamic().share(obj("files" to arrayOf(file), "title" to App.content.title)) as kotlin.js.Promise<dynamic>).await() }
        })
        result.appendChild(actions)
        if (isIos()) result.appendChild(el("p", "muted small", "Sur iPhone : « Partager » puis « Enregistrer la vidéo » la met dans Photos."))
        result.add(button("Une autre version", "btn wide") { try { propose(true); show(Step.PREVIEW) } catch (e: Throwable) { toast(e.message ?: "Erreur") } },
            button("Nouveau texte", "btn ghost wide") { show(Step.TEXT) })
    }
    return page
}
