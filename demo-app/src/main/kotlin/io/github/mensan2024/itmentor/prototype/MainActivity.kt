// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.prototype

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.text.InputFilter
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.DomainDetector
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.MusicCategory
import io.github.mensan2024.itmentor.engine.MusicChoice
import io.github.mensan2024.itmentor.engine.MusicSource
import io.github.mensan2024.itmentor.engine.MusicTrack
import io.github.mensan2024.itmentor.engine.MusicVolume
import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.TextSanitizer
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.Template
import io.github.mensan2024.itmentor.engine.Templates
import io.github.mensan2024.itmentor.engine.VideoFormat
import io.github.mensan2024.itmentor.engine.android.AndroidCanvasGfx
import io.github.mensan2024.itmentor.engine.android.AndroidFonts
import io.github.mensan2024.itmentor.engine.android.AudioDecoder
import io.github.mensan2024.itmentor.engine.android.DeviceProfile
import io.github.mensan2024.itmentor.engine.android.GalleryExport
import io.github.mensan2024.itmentor.engine.android.Media3VideoExporter
import io.github.mensan2024.itmentor.engine.android.MusicCatalog
import io.github.mensan2024.itmentor.engine.android.Mp4Inspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.security.SecureRandom
import kotlin.math.abs

/**
 * CODJIA — prototype du parcours de création, inspiré des Reels :
 * contenu → modèle (recommandé selon le domaine) → musique (écoute, forme d'onde, volume) → aperçu → génération.
 * On peut revenir en arrière à chaque étape avant l'export. Ce n'est pas encore l'application complète (phase D).
 */
@UnstableApi
class MainActivity : ComponentActivity() {

    private enum class Step(val title: String, val number: Int) {
        CONTENT("Ton contenu", 1), TEMPLATE("Choisis un modèle", 2), MUSIC("Choisis ta musique", 3),
        PREVIEW("Aperçu", 4), EXPORT("Génération", 5),
    }

    // État de la création.
    // Texte de l'utilisateur et génération en cours. Une nouvelle graine = une nouvelle mise en scène.
    private var userText: String = Examples.ALL.first().second
    private var seed: Long = newSeed()
    private var content: Storyboard = CreativeDirector.propose(userText, seed).storyboard
    private var template: Template = Templates.CYBER_NEON
    /** L'utilisateur a choisi lui-même son modèle : « Une autre version » le garde. */
    private var templateLocked = false
    /** Derniers modèles utilisés : on les évite pour ne pas répéter le même format. */
    private val recentTemplates = ArrayDeque<String>()
    private var generation = 1
    private var choice = MusicChoice(null)
    private var category: MusicCategory? = null
    private lateinit var catalog: List<MusicTrack>
    private val imported = ArrayList<MusicTrack>()
    private var step = Step.CONTENT

    // Rendu des vignettes et de l'aperçu sur un fil dédié (le moteur n'est pas partagé entre fils).
    private val renderThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val thumbs = HashMap<String, Bitmap>()
    private var previewJob: Job? = null
    private var player: MediaPlayer? = null
    private var playingId: String? = null
    private var waveTicker: Job? = null

    private lateinit var root: FrameLayout

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        runCatching { MusicCatalog.fromUserFile(this, uri) }
            .onSuccess { t -> imported.add(0, t); choice = choice.copy(track = t); category = null; show(Step.MUSIC) }
            .onFailure { Toast.makeText(this, it.message ?: "Fichier illisible", Toast.LENGTH_LONG).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Brand.BG
        catalog = MusicCatalog.load(this)
        root = FrameLayout(this).apply { setBackgroundColor(Brand.BG) }
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (step) {
                    Step.CONTENT -> finish()
                    Step.TEMPLATE -> show(Step.CONTENT)
                    Step.MUSIC -> show(Step.TEMPLATE)
                    Step.PREVIEW -> show(Step.MUSIC)
                    Step.EXPORT -> show(Step.PREVIEW)
                }
            }
        })
        show(Step.CONTENT)
    }

    override fun onStop() {
        super.onStop()
        stopPlayer()
        previewJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        renderThread.close()
    }

    // ───────────────────────────── Navigation ─────────────────────────────

    private fun show(s: Step) {
        stopPlayer()
        previewJob?.cancel()
        step = s
        root.removeAllViews()
        val page = vbox(dp(20f))
        page.addView(header(s))
        val body = when (s) {
            Step.CONTENT -> contentPage()
            Step.TEMPLATE -> templatePage()
            Step.MUSIC -> musicPage()
            Step.PREVIEW -> previewPage()
            Step.EXPORT -> exportPage()
        }
        page.addView(body, lp(MATCH, 0, 1f, top = dp(12f)))
        root.addView(page)
    }

    private fun header(s: Step): View {
        val h = vbox()
        val top = hbox()
        val logo = TextView(this).apply {
            text = "▶_ CODJIA"; textSize = 20f; setTextColor(Brand.TEXT); setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD); letterSpacing = 0.06f
        }
        top.addView(logo, lp(0, WRAP, 1f))
        if (s.number in 2..4) top.addView(label("‹ Retour", 15f, Brand.MUTED).apply { setOnClickListener { onBackPressedDispatcher.onBackPressed() } })
        h.addView(top)
        val bar = hbox()
        for (i in 1..4) bar.addView(View(this).apply {
            background = rounded(if (i <= minOf(s.number, 4)) Brand.SAFFRON else Brand.SURFACE_2, dp(3f).toFloat())
        }, lp(0, dp(5f), 1f, top = dp(14f), end = if (i < 4) dp(6f) else 0))
        h.addView(bar)
        h.addView(label(if (s == Step.EXPORT) s.title else "Étape ${s.number} sur 4 · ${s.title}", 22f, bold = true), lp(MATCH, WRAP, top = dp(14f)))
        return h
    }

    private fun newSeed(): Long {
        var s = 0L
        while (s == 0L) s = SecureRandom().nextLong() // 0 est réservé aux rendus de référence
        return s
    }

    /** Nouvelle proposition (storyboard, modèle, musique) pour le texte courant. */
    private fun propose(regenerate: Boolean) {
        if (regenerate) { seed = newSeed(); generation++ }
        val p = CreativeDirector.propose(userText, seed, avoidTemplates = recentTemplates, forcedTemplate = if (templateLocked) template else null)
        content = p.storyboard
        template = p.template
        recentTemplates.addLast(p.template.id); while (recentTemplates.size > 2) recentTemplates.removeFirst()
        if (!templateLocked || choice.track == null) {
            p.musicId?.let { id -> catalog.firstOrNull { it.id == id } }?.let { if (choice.track?.source != MusicSource.USER_IMPORT) choice = choice.copy(track = it) }
        }
    }

    // ───────────────────────────── 1. Texte ─────────────────────────────

    private fun contentPage(): View {
        val page = vbox()
        page.addView(label("Colle ou écris ton texte : astuce, post, notes de cours. CODJIA en fait une vidéo, différente à chaque génération.", 14f, Brand.MUTED))
        val counter = label("", 12f, Brand.MUTED)
        val input = EditText(this).apply {
            setText(userText)
            setTextColor(Brand.TEXT); setHintTextColor(Brand.MUTED); textSize = 15f
            hint = "Ex. : Astuce 028 — Active la double authentification…"
            gravity = Gravity.TOP or Gravity.START
            background = rounded(Brand.SURFACE, dp(14f).toFloat(), Brand.SURFACE_2, dp(1.5f))
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            filters = arrayOf(InputFilter.LengthFilter(TextSanitizer.MAX_CHARS))
            addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(e: android.text.Editable?) { counter.text = "${e?.length ?: 0} / ${TextSanitizer.MAX_CHARS}" }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
        }
        counter.text = "${userText.length} / ${TextSanitizer.MAX_CHARS}"
        page.addView(input, lp(MATCH, 0, 1f, top = dp(10f)))
        page.addView(counter, lp(WRAP, WRAP, top = dp(4f)))
        val examples = hbox()
        for ((name, text) in Examples.ALL) examples.addView(chip(name, false) { input.setText(text) }, lp(WRAP, WRAP, end = dp(8f)))
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(examples) }, lp(MATCH, WRAP, top = dp(8f)))
        page.addView(button("Analyser et créer ma vidéo") {
            val text = input.text.toString()
            val result = runCatching {
                userText = text
                templateLocked = false
                propose(regenerate = true)
            }
            result.onSuccess { show(Step.TEMPLATE) }
                .onFailure { Toast.makeText(this, it.message ?: "Texte inutilisable", Toast.LENGTH_LONG).show() }
        }, lp(MATCH, WRAP, top = dp(12f)))
        return page
    }

    // ───────────────────────────── 2. Modèle ─────────────────────────────

    private fun templatePage(): View {
        val page = vbox()
        val rec = DomainDetector.recommend(userText)
        val domain = DomainDetector.detect(TextSanitizer.clean(userText))
        page.addView(label("Domaine détecté : ${domain.label}. Les 3 premiers sont recommandés ; tous restent disponibles.", 14f, Brand.MUTED))
        val grid = vbox()
        var row: LinearLayout? = null
        val w = (resources.displayMetrics.widthPixels - dp(40f) - dp(12f)) / 2
        for ((i, tp) in rec.withIndex()) {
            if (i % 2 == 0) { row = hbox(); grid.addView(row, lp(MATCH, WRAP, top = dp(12f))) }
            val selected = tp.id == template.id
            val cell = vbox(dp(6f)).apply {
                background = rounded(Brand.SURFACE, dp(14f).toFloat(), if (selected) Brand.SAFFRON else Brand.SURFACE, dp(3f))
                setOnClickListener { template = tp; templateLocked = true; show(Step.TEMPLATE) }
            }
            val img = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(tp.style.bgTop) }
            cell.addView(img, lp(MATCH, (w - dp(12f)) * 16 / 9))
            cell.addView(label(tp.name, 14f, bold = true), lp(WRAP, WRAP, top = dp(6f)))
            cell.addView(label(if (i < 3) "★ Recommandé" else tp.tagline, 12f, if (i < 3) Brand.SAFFRON else Brand.MUTED))
            row!!.addView(cell, lp(0, WRAP, 1f, end = if (i % 2 == 0) dp(12f) else 0))
            thumbnail(tp, img)
        }
        page.addView(ScrollView(this).apply { addView(grid) }, lp(MATCH, 0, 1f))
        page.addView(button("Continuer avec ${template.name}") { show(Step.MUSIC) }, lp(MATCH, WRAP, top = dp(12f)))
        return page
    }

    /** Vignette = vraie image du moteur (couverture du contenu choisi, dans ce modèle). */
    private fun thumbnail(tp: Template, view: ImageView) {
        val key = "${content.id}/${tp.id}/$seed"
        thumbs[key]?.let { view.setImageBitmap(it); return }
        lifecycleScope.launch {
            val bmp = withContext(renderThread) {
                val sb = Composer.prepare(content, tp, null)
                val r = FrameRenderer(sb, VideoFormat(270, 480, 30), tp)
                val b = Bitmap.createBitmap(270, 480, Bitmap.Config.ARGB_8888)
                gfx.begin(Canvas(b))
                r.render(gfx, r.timeline.slots[0].durationMs * 0.8f)
                b
            }
            thumbs[key] = bmp
            view.setImageBitmap(bmp)
        }
    }

    private val gfx by lazy { AndroidCanvasGfx(AndroidFonts(assets)) }

    // ───────────────────────────── 3. Musique ─────────────────────────────

    private fun musicPage(): View {
        val page = vbox()
        // Catégories, comme dans les Reels.
        val chips = hbox()
        chips.addView(chip("Toutes", category == null) { category = null; show(Step.MUSIC) }, lp(WRAP, WRAP, end = dp(8f)))
        for (c in MusicCategory.entries) chips.addView(chip(c.label, category == c) { category = c; show(Step.MUSIC) }, lp(WRAP, WRAP, end = dp(8f)))
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) })

        val list = vbox()
        list.addView(noMusicRow(), lp(MATCH, WRAP, top = dp(12f)))
        val tracks = (imported + catalog).filter { category == null || (it.category == category && it.source == MusicSource.BUILT_IN) }
        for (t in tracks) list.addView(trackRow(t), lp(MATCH, WRAP, top = dp(10f)))
        list.addView(button("＋ Importer un fichier audio", primary = false) { pickAudio.launch(arrayOf("audio/*")) }, lp(MATCH, WRAP, top = dp(14f)))
        list.addView(label("Les musiques intégrées sont des compositions originales CODJIA (licence GPL-3.0). " +
            "Un fichier importé est utilisé sous ta responsabilité.", 12f, Brand.MUTED), lp(MATCH, WRAP, top = dp(10f)))
        page.addView(ScrollView(this).apply { addView(list) }, lp(MATCH, 0, 1f))

        // Volume dans la vidéo.
        val vol = hbox()
        vol.addView(label("Volume", 14f, Brand.MUTED), lp(WRAP, WRAP, end = dp(12f)))
        for (v in MusicVolume.entries) vol.addView(chip(v.label, choice.volume == v) { choice = choice.copy(volume = v); show(Step.MUSIC) }, lp(WRAP, WRAP, end = dp(8f)))
        page.addView(vol, lp(MATCH, WRAP, top = dp(10f)))
        page.addView(button("Voir l'aperçu") { show(Step.PREVIEW) }, lp(MATCH, WRAP, top = dp(12f)))
        return page
    }

    private fun noMusicRow(): View {
        val selected = choice.track == null
        return hbox().apply {
            background = rounded(Brand.SURFACE, dp(14f).toFloat(), if (selected) Brand.SAFFRON else Brand.SURFACE, dp(2f))
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            addView(label("🔇", 20f), lp(WRAP, WRAP, end = dp(12f)))
            addView(label("Sans musique", 16f, bold = true), lp(0, WRAP, 1f))
            setOnClickListener { choice = choice.copy(track = null); show(Step.MUSIC) }
        }
    }

    private fun trackRow(t: MusicTrack): View {
        val selected = choice.track?.id == t.id
        val row = hbox().apply {
            background = rounded(Brand.SURFACE, dp(14f).toFloat(), if (selected) Brand.SAFFRON else Brand.SURFACE, dp(2f))
            setPadding(dp(12f), dp(12f), dp(14f), dp(12f))
            setOnClickListener { choice = choice.copy(track = t); show(Step.MUSIC) }
        }
        val wave = WaveformView(this).apply { peaks = t.peaks }
        val play = TextView(this).apply {
            text = if (playingId == t.id) "❚❚" else "▶"; textSize = 18f; gravity = Gravity.CENTER; setTextColor(Brand.INK)
            background = rounded(Brand.TEAL, dp(22f).toFloat())
        }
        play.setOnClickListener { togglePreview(t, play, wave) }
        row.addView(play, lp(dp(44f), dp(44f), end = dp(12f)))
        val info = vbox()
        val title = hbox()
        title.addView(label(t.title, 16f, bold = true), lp(0, WRAP, 1f))
        title.addView(label(formatDuration(t.durationMs), 13f, Brand.MUTED))
        info.addView(title)
        info.addView(label(if (t.source == MusicSource.USER_IMPORT) "Importée" else t.category.label + " · ${t.bpm} BPM", 12f, Brand.MUTED))
        info.addView(wave, lp(MATCH, dp(28f), top = dp(6f)))
        row.addView(info, lp(0, WRAP, 1f))
        return row
    }

    private fun openPlayer(t: MusicTrack): MediaPlayer {
        val mp = MediaPlayer()
        if (t.source == MusicSource.USER_IMPORT) mp.setDataSource(this, Uri.parse(t.file))
        else assets.openFd(t.file).use { fd -> mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
        mp.prepare()
        return mp
    }

    private fun togglePreview(t: MusicTrack, button: TextView, wave: WaveformView) {
        if (playingId == t.id) { stopPlayer(); button.text = "▶"; return }
        stopPlayer()
        val mp = runCatching { openPlayer(t) }.getOrElse { Toast.makeText(this, "Lecture impossible", Toast.LENGTH_SHORT).show(); return }
        player = mp; playingId = t.id
        mp.setOnCompletionListener { stopPlayer(); button.text = "▶" }
        mp.start()
        button.text = "❚❚"
        waveTicker = lifecycleScope.launch {
            while (isActive && player === mp) {
                runCatching { wave.played = mp.currentPosition / mp.duration.toFloat() }
                delay(100)
            }
        }
    }

    private fun stopPlayer() {
        waveTicker?.cancel()
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        playingId = null
    }

    // ───────────────────────────── 4. Aperçu ─────────────────────────────

    private fun previewPage(): View {
        val page = vbox()
        val sb = Composer.prepare(content, template, choice.track?.takeIf { it.bpm > 0 })
        val music = choice.track?.let { if (choice.hasAudio) it.title else "${it.title} (coupée)" } ?: "Sans musique"
        page.addView(label("Version $generation · ${template.name} · $music", 14f, Brand.MUTED))
        val screenW = resources.displayMetrics.widthPixels
        val pw = (screenW * 0.62f).toInt()
        val img = ImageView(this).apply { setBackgroundColor(template.style.bgTop) }
        val frame = FrameLayout(this).apply { addView(img, FrameLayout.LayoutParams(pw, pw * 16 / 9, Gravity.CENTER)) }
        page.addView(frame, lp(MATCH, 0, 1f, top = dp(8f)))
        val actions = hbox()
        actions.addView(button("Modèle", primary = false) { show(Step.TEMPLATE) }, lp(0, WRAP, 1f, end = dp(10f)))
        actions.addView(button("Musique", primary = false) { show(Step.MUSIC) }, lp(0, WRAP, 1f))
        page.addView(actions, lp(MATCH, WRAP, top = dp(12f)))
        page.addView(button("🔀 Une autre version", primary = false) { propose(regenerate = true); show(Step.PREVIEW) }, lp(MATCH, WRAP, top = dp(10f)))
        page.addView(button("Générer la vidéo") { show(Step.EXPORT) }, lp(MATCH, WRAP, top = dp(10f)))
        startPreview(sb, img)
        return page
    }

    /** Aperçu animé en direct (360 × 640), musique synchronisée sur le temps de lecture. */
    private fun startPreview(sb: Storyboard, view: ImageView) {
        val mp = choice.track?.takeIf { choice.hasAudio }?.let { runCatching { openPlayer(it) }.getOrNull() }
        mp?.setVolume(choice.volume.gain, choice.volume.gain)
        player = mp
        previewJob = lifecycleScope.launch {
            val renderer = withContext(renderThread) { FrameRenderer(sb, VideoFormat(360, 640, 30), template) }
            val total = renderer.timeline.totalMs
            val bufs = Array(2) { Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888) }
            var flip = 0
            var t0 = SystemClock.elapsedRealtime()
            mp?.start()
            while (isActive) {
                var t = mp?.currentPosition?.toLong() ?: (SystemClock.elapsedRealtime() - t0)
                if (t >= total) { t0 = SystemClock.elapsedRealtime(); mp?.seekTo(0); t = 0 }
                val target = bufs[flip]
                withContext(renderThread) {
                    target.eraseColor(Color.BLACK)
                    gfx.begin(Canvas(target))
                    renderer.render(gfx, t.toFloat())
                }
                view.setImageBitmap(target)
                flip = 1 - flip
                delay(16)
            }
        }
    }

    // ───────────────────────────── 5. Génération ─────────────────────────────

    private lateinit var log: TextView

    private fun exportPage(): View {
        val page = vbox()
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        page.addView(bar, lp(MATCH, WRAP))
        log = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 12f; setTextColor(Brand.TEXT); setTextIsSelectable(true) }
        page.addView(ScrollView(this).apply { addView(log) }, lp(MATCH, 0, 1f, top = dp(10f)))
        val actions = hbox()
        val open = button("Ouvrir", primary = false) {}.apply { isEnabled = false; alpha = 0.4f }
        val share = button("Partager") {}.apply { isEnabled = false; alpha = 0.4f }
        actions.addView(open, lp(0, WRAP, 1f, end = dp(10f)))
        actions.addView(share, lp(0, WRAP, 1f))
        page.addView(actions, lp(MATCH, WRAP, top = dp(10f)))
        page.addView(button("Nouvelle vidéo", primary = false) { show(Step.CONTENT) }, lp(MATCH, WRAP, top = dp(10f)))
        generate(bar, open, share)
        return page
    }

    private fun say(line: String) { log.append(line + "\n") }

    private fun thermal(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) (getSystemService(POWER_SERVICE) as PowerManager).currentThermalStatus.toString() else "n/d"

    private fun generate(bar: ProgressBar, open: TextView, share: TextView) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lifecycleScope.launch {
            try {
                val format = withContext(Dispatchers.Default) { DeviceProfile.pickFormat() }
                val music = choice.track
                val sb = Composer.prepare(content, template, music?.takeIf { it.bpm > 0 })
                val total = FrameRenderer(sb, format, template).timeline.totalMs
                say("Appareil      : ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                say("Modèle        : ${template.name}")
                say("Musique       : ${music?.let { "${it.title} · volume ${choice.volume.label}" } ?: "sans musique"}")
                if (music != null && music.bpm > 0) say("Montage       : scènes calées sur le tempo (${music.bpm} BPM)")
                say("Format        : ${format.width}×${format.height} à ${format.fps} i/s · ${total} ms")
                say("Thermique     : ${thermal()} au départ")

                val t0 = SystemClock.elapsedRealtime()
                val bed = withContext(Dispatchers.IO) { AudioDecoder.prepareBed(this@MainActivity, choice, total, File(cacheDir, "audio")) }
                if (bed != null) say("Musique prête : décodage + fondus en ${(SystemClock.elapsedRealtime() - t0)} ms")

                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                val out = File(getExternalFilesDir(null) ?: filesDir, "CODJIA_${sb.id.removePrefix("demo-")}_$stamp.mp4")
                val stats = Media3VideoExporter(this@MainActivity).export(sb, format, out, bed) { bar.progress = it }
                bar.progress = 100
                val probe = withContext(Dispatchers.IO) { Mp4Inspector.inspect(out) }

                // Règle : jamais de vidéo muette si une musique a été choisie.
                if (choice.hasAudio && probe.audioMime == null) error("La piste audio est absente du fichier : export refusé")

                val uri = withContext(Dispatchers.IO) { GalleryExport.publish(this@MainActivity, out, "${sb.title} — CODJIA") }
                say("")
                say("── Fichier produit (relu avec MediaExtractor)")
                say("Vidéo         : ${probe.mime} ${if (probe.isH264) "✓" else "✗"} · ${probe.displayWidth}×${probe.displayHeight}" +
                    if (probe.rotationDegrees != 0) " (encodée ${probe.width}×${probe.height}, rotation ${probe.rotationDegrees}°)" else "")
                say("Durée         : ${probe.durationMs} ms · ${probe.sampleCount} images · %.2f i/s".format(probe.measuredFps))
                say("Audio         : " + (probe.audioMime?.let { "$it ✓ · ${probe.audioSampleRate} Hz · ${probe.audioDurationMs} ms" } ?: "aucune piste"))
                if (probe.audioMime != null) say("Synchro       : écart audio/vidéo ${abs(probe.audioDurationMs - probe.durationMs)} ms")
                say("Taille        : %.1f Mo".format(stats.fileBytes / 1048576.0))
                say("Encodeur      : ${stats.encoderName}")
                say("")
                say("── Performances")
                say("Export total  : %.1f s (%.2f× le temps réel)".format(stats.wallMs / 1000.0, stats.wallMs / stats.durationMs.toDouble()))
                say("Rendu/image   : moy. %.1f ms · p95 %.1f ms · max %.1f ms".format(stats.avgFrameMs, stats.p95FrameMs, stats.maxFrameMs))
                say("Mémoire       : Java %.0f Mo · native %.0f Mo (pics)".format(stats.javaHeapPeakMb, stats.nativeHeapPeakMb))
                say("Thermique     : ${thermal()} à la fin")
                say("")
                say("Vidéo : Galerie › Movies › CODJIA")
                if (uri.scheme == "content") {
                    open.isEnabled = true; open.alpha = 1f
                    share.isEnabled = true; share.alpha = 1f
                    open.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }
                    share.setOnClickListener {
                        val send = Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(Intent.createChooser(send, "Partager la vidéo CODJIA"))
                    }
                }
            } catch (e: Exception) {
                say("ÉCHEC : ${e::class.simpleName} — ${e.message}")
                e.cause?.let { say("Cause : ${it::class.simpleName} — ${it.message}") }
            } finally {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
