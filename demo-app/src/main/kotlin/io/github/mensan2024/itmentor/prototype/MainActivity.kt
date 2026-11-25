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
import android.view.animation.DecelerateInterpolator
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
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.DomainDetector
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.ImportPolicy
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.StyleKit
import io.github.mensan2024.itmentor.engine.VoiceMode
import io.github.mensan2024.itmentor.engine.android.Narrator
import io.github.mensan2024.itmentor.engine.android.OfflineVoices
import io.github.mensan2024.itmentor.engine.android.VoicedResult
import io.github.mensan2024.itmentor.engine.Layout
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
import kotlinx.coroutines.NonCancellable
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
 * CODJIA — parcours de création :
 * texte → aperçu immédiat (format, modèle et musique choisis automatiquement, différents à chaque génération)
 * → ajustements facultatifs (style, musique) → génération du MP4.
 */
@UnstableApi
class MainActivity : ComponentActivity() {

    private enum class Step { CONTENT, PREVIEW, STYLE, MUSIC, EXPORT }

    // ── État de la création
    private var userText: String = Examples.ALL.first().second
    private var seed: Long = newSeed()
    private var content: Storyboard = CreativeDirector.propose(userText, seed).storyboard
    private var template: Template = Templates.CYBER_NEON
    private var look: Look = content.look
    /** Choix faits par l'utilisateur : « Une autre version » les respecte. */
    private var templateLocked = false
    private var lookLocked = false
    private val recentTemplates = ArrayDeque<String>()
    private val recentLooks = ArrayDeque<Look>()
    private var generation = 1
    private var choice = MusicChoice(null)
    private var category: MusicCategory? = null
    private lateinit var catalog: List<MusicTrack>
    private val imported = ArrayList<MusicTrack>()
    private var step = Step.CONTENT
    private var exporting = false

    // ── Voix off (synthèse vocale hors ligne du téléphone), mélangée à la musique
    private var voiceMode = VoiceMode.AUTO
    /** Langue de voix à rétablir quand l'utilisateur réactive la voix depuis l'aperçu. */
    private var lastVoice = VoiceMode.AUTO
    private var voices: OfflineVoices? = null
    private var voicesChecked = false
    private var voiced: Pair<String, VoicedResult>? = null

    private fun audioKey() = "${content.id}|$seed|${content.look}|${template.id}|${choice.track?.id}|${choice.volume}|$voiceMode|$generation|${content.scenes.size}"

    /**
     * Storyboard final et piste audio : avec voix off, les scènes s'allongent pour la voix et la musique
     * baisse sous la voix ; sans voix, la musique seule (préparée à l'export).
     */
    private suspend fun audioPlan(onStatus: (String) -> Unit = {}): Pair<Storyboard, File?> {
        val base = Composer.prepare(content, template, choice.track?.takeIf { it.bpm > 0 })
        if (voiceMode == VoiceMode.NONE) return base to null
        val key = audioKey()
        voiced?.takeIf { it.first == key && it.second.audio.isFile }?.let { return it.second.storyboard to it.second.audio }
        if (!voicesChecked) {
            onStatus("Ouverture de la voix…")
            voices = runCatching { OfflineVoices.open(this) }.getOrNull()
            voicesChecked = true
        }
        val v = voices ?: run {
            Toast.makeText(this, "Aucune voix française ou anglaise hors ligne n'est installée : vidéo sans voix.", Toast.LENGTH_LONG).show()
            runCatching { startActivity(OfflineVoices.installIntent()) }
            return base to null
        }
        onStatus("Préparation de la voix off…")
        val res = Narrator.prepare(v, base, voiceMode, File(cacheDir, "voix")) { total ->
            withContext(Dispatchers.IO) { AudioDecoder.bedPcm(this@MainActivity, choice, total) }
        }
        voiced = key to res
        return res.storyboard to res.audio
    }

    // ── Rendu sur un fil dédié (le moteur n'est pas partagé entre fils)
    private val renderThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val thumbs = HashMap<String, Bitmap>()
    private var previewJob: Job? = null
    private var previewPaused = false
    private var player: MediaPlayer? = null
    private var playingId: String? = null
    private var waveTicker: Job? = null
    private val gfx by lazy { AndroidCanvasGfx(AndroidFonts(assets)) }

    private lateinit var root: FrameLayout

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        // Analyse hors du fil d'interface : un fichier lourd ou piégé ne doit pas figer l'appli.
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { MusicCatalog.fromUserFile(this@MainActivity, uri) } }
                .onSuccess { t -> imported.removeAll { it.id == t.id }; imported.add(0, t); choice = choice.copy(track = t); category = null; show(Step.MUSIC, animate = false) }
                .onFailure { e ->
                    val msg = (e as? IllegalArgumentException)?.message ?: "Fichier audio illisible"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bord à bord (obligatoire à partir d'Android 15) : le contenu est décalé sous la barre d'état,
        // au-dessus de la barre de navigation et du clavier, sur tous les téléphones et toutes les encoches.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        catalog = MusicCatalog.load(this)
        root = FrameLayout(this).apply { setBackgroundColor(Brand.BG) }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (step) {
                    Step.CONTENT -> finish()
                    Step.PREVIEW -> show(Step.CONTENT, forward = false)
                    Step.STYLE, Step.MUSIC -> show(Step.PREVIEW, forward = false)
                    Step.EXPORT -> if (exporting) Toast.makeText(this@MainActivity, "Génération en cours…", Toast.LENGTH_SHORT).show()
                        else show(Step.PREVIEW, forward = false)
                }
            }
        })
        show(Step.CONTENT, animate = false)
    }

    override fun onStop() {
        super.onStop()
        stopPlayer()
        previewJob?.cancel()
    }

    override fun onRestart() {
        super.onRestart()
        // Retour dans l'appli (après la Galerie, par exemple) : l'aperçu reprend.
        if (step == Step.PREVIEW) show(Step.PREVIEW, animate = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        voices?.shutdown()
        player?.release()
        renderThread.close()
    }

    // ───────────────────────────── Navigation ─────────────────────────────

    private fun show(s: Step, forward: Boolean = true, animate: Boolean = true) {
        stopPlayer()
        previewJob?.cancel()
        step = s
        root.removeAllViews()
        val page = vbox(dp(20f))
        page.addView(header(s))
        val body = when (s) {
            Step.CONTENT -> contentPage()
            Step.PREVIEW -> previewPage()
            Step.STYLE -> stylePage()
            Step.MUSIC -> musicPage()
            Step.EXPORT -> exportPage()
        }
        page.addView(body, lp(MATCH, 0, 1f, top = dp(12f)))
        root.addView(page)
        if (animate) {
            // Transition d'écran : glissement court et fondu, dans le sens de la navigation.
            page.alpha = 0f
            page.translationX = dp(28f).toFloat() * (if (forward) 1 else -1)
            page.animate().alpha(1f).translationX(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun header(s: Step): View {
        val h = hbox()
        h.addView(TextView(this).apply {
            text = "▶_ CODJIA"; textSize = 20f; setTextColor(Brand.TEXT); setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD); letterSpacing = 0.06f
        }, lp(0, WRAP, 1f))
        val back = when (s) {
            Step.PREVIEW -> "‹ Texte"
            Step.STYLE, Step.MUSIC -> "‹ Aperçu"
            else -> null
        }
        back?.let { h.addView(label(it, 15f, Brand.MUTED).apply { setPadding(dp(8f), dp(8f), dp(8f), dp(8f)); pressable { onBackPressedDispatcher.onBackPressed() } }) }
        val title = when (s) {
            Step.CONTENT -> "Ton texte"
            Step.PREVIEW -> "Ta vidéo"
            Step.STYLE -> "Style de la vidéo"
            Step.MUSIC -> "Musique"
            Step.EXPORT -> "Génération"
        }
        return vbox().apply {
            addView(h)
            addView(label(title, 24f, bold = true), lp(MATCH, WRAP, top = dp(10f)))
        }
    }

    private fun newSeed(): Long {
        var s = 0L
        while (s == 0L) s = SecureRandom().nextLong() // 0 est réservé aux rendus de référence
        return s
    }

    /** Nouvelle proposition (format, storyboard, modèle, musique) pour le texte courant. */
    private fun propose(regenerate: Boolean) {
        if (regenerate) { seed = newSeed(); generation++ }
        val p = CreativeDirector.propose(userText, seed,
            avoidTemplates = recentTemplates, forcedTemplate = if (templateLocked) template else null,
            avoidLooks = recentLooks, forcedLook = if (lookLocked) look else null)
        content = p.storyboard
        template = p.template
        look = p.look
        recentTemplates.addLast(p.template.id); while (recentTemplates.size > 2) recentTemplates.removeFirst()
        recentLooks.addLast(p.look); while (recentLooks.size > 2) recentLooks.removeFirst()
        if (choice.track?.source != MusicSource.USER_IMPORT) {
            p.musicId?.let { id -> catalog.firstOrNull { it.id == id } }?.let { choice = choice.copy(track = it) }
        }
    }

    private val hasDemo get() = content.scenes.any { it.layout == Layout.SCREEN }

    // ───────────────────────────── 1. Texte ─────────────────────────────

    private fun contentPage(): View {
        val page = vbox()
        page.addView(label("Colle ou écris ton astuce. CODJIA choisit la mise en scène (démo à l'écran, tableau blanc, " +
            "personnage animé, magazine…) et la change à chaque génération.", 14f, Brand.MUTED))
        val counter = label("", 12f, Brand.MUTED)
        val input = EditText(this).apply {
            setText(userText)
            setTextColor(Brand.TEXT); setHintTextColor(Brand.MUTED); textSize = 15f
            hint = "Ex. : Astuce 028 — Active le pare-feu de ton PC…"
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
        for ((name, text) in Examples.ALL) examples.addView(chip(name, false) { input.setText(text); input.setSelection(0) }, lp(WRAP, WRAP, end = dp(8f)))
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(examples) }, lp(MATCH, WRAP, top = dp(8f)))
        val go = button("Créer ma vidéo  →") {}
        go.pressable {
            val text = input.text.toString()
            go.isEnabled = false; go.text = "Analyse du texte…"
            lifecycleScope.launch {
                val r = runCatching {
                    withContext(Dispatchers.Default) {
                        userText = text; templateLocked = false; lookLocked = false
                        propose(regenerate = true)
                    }
                }
                go.isEnabled = true; go.text = "Créer ma vidéo  →"
                r.onSuccess { show(Step.PREVIEW) }
                    .onFailure { Toast.makeText(this@MainActivity, it.message ?: "Texte inutilisable", Toast.LENGTH_LONG).show() }
            }
        }
        page.addView(go, lp(MATCH, WRAP, top = dp(12f)))
        return page
    }

    // ───────────────────────────── 2. Aperçu (écran central) ─────────────────────────────

    private fun previewPage(): View {
        val page = vbox()
        val music = choice.track?.let { if (choice.hasAudio) it.title else "${it.title} (coupée)" } ?: "Sans musique"
        val styleName = if (look == Look.KIT) "${look.label} (${StyleKit.random(content.seed).label})" else look.label
        val info = label("Version $generation · $styleName${if (hasDemo) " + démo à l'écran" else ""} · $music · voix ${voiceMode.label}", 13f, Brand.MUTED)
        page.addView(info)
        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            clipToOutline = true
            background = rounded(Color.BLACK, dp(16f).toFloat())
        }
        val hint = label("Touche pour mettre en pause", 12f, Brand.MUTED).apply { gravity = Gravity.CENTER }
        // L'aperçu prend toute la place disponible en gardant le format 9:16, quelle que soit la taille de l'écran.
        val frame = FrameLayout(this).apply { addView(img, FrameLayout.LayoutParams(1, 1, Gravity.CENTER)) }
        frame.addOnLayoutChangeListener { v, l, t, r, b, _, _, _, _ ->
            val w = r - l; val h = b - t
            if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
            val pw = minOf(w, h * 9 / 16); val ph = pw * 16 / 9
            val lp = img.layoutParams as FrameLayout.LayoutParams
            if (lp.width != pw || lp.height != ph) { lp.width = pw; lp.height = ph; v.post { img.layoutParams = lp } }
        }
        img.pressable { previewPaused = !previewPaused; hint.text = if (previewPaused) "En pause · touche pour reprendre" else "Touche pour mettre en pause"; if (previewPaused) player?.pause() else player?.start() }
        page.addView(frame, lp(MATCH, 0, 1f, top = dp(8f)))
        page.addView(hint, lp(MATCH, WRAP, top = dp(4f)))
        val actions = hbox()
        actions.addView(button("Style", primary = false) { show(Step.STYLE) }, lp(0, WRAP, 1f, end = dp(8f)))
        actions.addView(button("Musique", primary = false) { show(Step.MUSIC) }, lp(0, WRAP, 1f, end = dp(8f)))
        actions.addView(button("Autre version", primary = false) {
            // Calcul hors du fil d'interface ; une erreur ne doit jamais fermer l'appli.
            lifecycleScope.launch {
                runCatching { withContext(Dispatchers.Default) { propose(regenerate = true) } }
                    .onSuccess { show(Step.PREVIEW, animate = false) }
                    .onFailure { Toast.makeText(this@MainActivity, it.message ?: "Impossible de créer une autre version", Toast.LENGTH_LONG).show() }
            }
        }, lp(0, WRAP, 1f))
        page.addView(actions, lp(MATCH, WRAP, top = dp(10f)))
        // Choix explicite de la voix off, directement sur l'aperçu (la langue se règle dans « Musique »).
        val bottom = hbox()
        val voiceOn = voiceMode != VoiceMode.NONE
        bottom.addView(button(if (voiceOn) "Voix : oui" else "Voix : non", primary = false) {
            if (voiceMode == VoiceMode.NONE) voiceMode = lastVoice else { lastVoice = voiceMode; voiceMode = VoiceMode.NONE }
            show(Step.PREVIEW, animate = false)
        }.apply { contentDescription = if (voiceOn) "Voix off activée, toucher pour la couper" else "Voix off coupée, toucher pour l'activer" },
            lp(0, WRAP, 1f, end = dp(8f)))
        bottom.addView(button("Générer la vidéo") { show(Step.EXPORT) }, lp(0, WRAP, 2f))
        page.addView(bottom, lp(MATCH, WRAP, top = dp(10f)))
        previewPaused = false
        if (voiceMode != VoiceMode.NONE) hint.text = "Préparation de la voix off…"
        previewJob = lifecycleScope.launch {
            val plan = runCatching { audioPlan { hint.text = it } }.recoverCatching { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(this@MainActivity, "Voix off impossible : ${e.message}", Toast.LENGTH_LONG).show()
                withContext(Dispatchers.Default) { Composer.prepare(content, template, choice.track?.takeIf { it.bpm > 0 }) } to null
            }
            val (sb, audio) = plan.getOrElse { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                hint.text = "Aperçu impossible : ${e.message ?: "erreur inconnue"}. Essaie « Autre version » ou raccourcis le texte."
                return@launch
            }
            val secs = runCatching { withContext(renderThread) { (FrameRenderer(sb, VideoFormat(360, 640, 30), template).timeline.totalMs + 500) / 1000 } }.getOrNull()
            if (secs != null) info.text = "${info.text} · $secs s"
            hint.text = "Touche pour mettre en pause"
            startPreview(sb, img, audio)
        }
        return page
    }

    /** Aperçu animé en direct (360 × 640), musique synchronisée sur le temps de lecture. */
    private fun startPreview(sb: Storyboard, view: ImageView, mixed: File? = null) {
        val mp = if (mixed != null) runCatching { MediaPlayer().apply { setDataSource(mixed.absolutePath); prepare() } }.getOrNull()
            else choice.track?.takeIf { choice.hasAudio }?.let { runCatching { openPlayer(it) }.getOrNull() }
        if (mixed == null) mp?.setVolume(choice.volume.gain, choice.volume.gain)
        // La musique (48 s) peut être plus courte que la vidéo (jusqu'à 59 s) : elle boucle, comme à l'export.
        if (mixed == null) mp?.isLooping = true
        player = mp
        previewJob = lifecycleScope.launch {
            val renderer = withContext(renderThread) { FrameRenderer(sb, VideoFormat(360, 640, 30), template) }
            val total = renderer.timeline.totalMs
            val bufs = Array(2) { Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888) }
            var flip = 0
            var clock = 0L
            var last = SystemClock.elapsedRealtime()
            mp?.start()
            view.alpha = 0f
            var first = true
            val mpLen = runCatching { mp?.duration?.toLong() ?: 0L }.getOrDefault(0L)
            var lastSync = 0L
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                if (!previewPaused) clock += now - last
                last = now
                // Le temps de la vidéo suit l'horloge ; le son est recalé s'il dérive (> 150 ms).
                if (clock >= total) { clock = 0; runCatching { mp?.seekTo(0) } }
                val t = clock
                if (mp != null && !previewPaused && mpLen > 0 && now - lastSync > 600) runCatching {
                    lastSync = now
                    val expected = if (mixed != null) t.coerceAtMost(mpLen - 1) else t % mpLen
                    if (kotlin.math.abs(mp.currentPosition - expected) > 400) mp.seekTo(expected.toInt())
                }
                val target = bufs[flip]
                withContext(renderThread) {
                    target.eraseColor(Color.BLACK)
                    gfx.begin(Canvas(target))
                    renderer.render(gfx, t.toFloat())
                }
                view.setImageBitmap(target)
                if (first) { view.animate().alpha(1f).setDuration(200).start(); first = false }
                flip = 1 - flip
                delay(16)
            }
        }
    }

    // ───────────────────────────── 3. Style (format + modèle) ─────────────────────────────

    private fun stylePage(): View {
        val page = vbox()
        val scroll = vbox()
        scroll.addView(label("Format", 16f, bold = true))
        scroll.addView(label("La mise en scène. « Une autre version » en change automatiquement si tu n'en choisis pas.", 12f, Brand.MUTED))
        val looksRow = hbox()
        val cardW = dp(132f)
        val lookCells = HashMap<Look, View>()
        val templateBox = vbox()
        for (l in Look.entries) {
            val cell = vbox(dp(6f))
            val img = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(Brand.SURFACE_2) }
            cell.addView(img, lp(MATCH, cardW * 16 / 9))
            cell.addView(label(l.label, 13f, bold = true), lp(WRAP, WRAP, top = dp(6f)))
            cell.addView(label(l.tagline, 11f, Brand.MUTED).apply { maxLines = 2 })
            cell.background = rounded(Brand.SURFACE, dp(14f).toFloat(), if (l == look) Brand.SAFFRON else Brand.SURFACE, dp(3f))
            cell.pressable {
                look = l; lookLocked = true
                content = content.copy(look = l)
                for ((k, v) in lookCells) v.background = rounded(Brand.SURFACE, dp(14f).toFloat(), if (k == look) Brand.SAFFRON else Brand.SURFACE, dp(3f))
                fillTemplates(templateBox)
            }
            lookCells[l] = cell
            looksRow.addView(cell, lp(cardW, WRAP, end = dp(10f)))
            thumbnail(content.copy(look = l), template, img)
        }
        scroll.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(looksRow) }, lp(MATCH, WRAP, top = dp(10f)))
        scroll.addView(templateBox, lp(MATCH, WRAP, top = dp(18f)))
        fillTemplates(templateBox)
        page.addView(ScrollView(this).apply { addView(scroll) }, lp(MATCH, 0, 1f))
        page.addView(button("Voir l'aperçu") { show(Step.PREVIEW, forward = false) }, lp(MATCH, WRAP, top = dp(12f)))
        return page
    }

    /** Couleurs (modèles) : utiles pour les formats qui reprennent la palette du modèle. */
    private fun fillTemplates(box: LinearLayout) {
        box.removeAllViews()
        if (look == Look.TABLEAU || look == Look.EDITORIAL) {
            box.addView(label("Couleurs", 16f, bold = true))
            box.addView(label("Ce format a sa propre palette (feutres ou magazine) : elle varie à chaque version.", 12f, Brand.MUTED))
            return
        }
        box.addView(label("Couleurs et typographie", 16f, bold = true))
        val domain = DomainDetector.detect(TextSanitizer.clean(userText))
        box.addView(label("Domaine détecté : ${domain.label}. Les 3 premiers sont recommandés.", 12f, Brand.MUTED))
        val row = hbox()
        val cardW = dp(112f)
        val cells = HashMap<String, View>()
        for ((i, tp) in DomainDetector.recommend(userText).withIndex()) {
            val cell = vbox(dp(5f))
            val img = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(tp.style.bgTop) }
            cell.addView(img, lp(MATCH, cardW * 16 / 9))
            cell.addView(label(tp.name, 12f, bold = true), lp(WRAP, WRAP, top = dp(5f)))
            if (i < 3) cell.addView(label("★ Recommandé", 11f, Brand.SAFFRON))
            cell.background = rounded(Brand.SURFACE, dp(12f).toFloat(), if (tp.id == template.id) Brand.SAFFRON else Brand.SURFACE, dp(3f))
            cell.pressable {
                template = tp; templateLocked = true
                for ((k, v) in cells) v.background = rounded(Brand.SURFACE, dp(12f).toFloat(), if (k == template.id) Brand.SAFFRON else Brand.SURFACE, dp(3f))
            }
            cells[tp.id] = cell
            row.addView(cell, lp(cardW, WRAP, end = dp(10f)))
            thumbnail(content, tp, img)
        }
        box.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) }, lp(MATCH, WRAP, top = dp(10f)))
    }

    /** Vignette = vraie image du moteur (couverture du contenu, dans ce format et ce modèle). */
    private fun thumbnail(sbIn: Storyboard, tp: Template, view: ImageView) {
        val key = "${sbIn.id}/${sbIn.look}/${tp.id}/$seed"
        thumbs[key]?.let { view.setImageBitmap(it); return }
        lifecycleScope.launch {
            val bmp = withContext(renderThread) {
                val sb = Composer.prepare(sbIn, tp, null)
                val r = FrameRenderer(sb, VideoFormat(270, 480, 30), tp)
                val b = Bitmap.createBitmap(270, 480, Bitmap.Config.ARGB_8888)
                gfx.begin(Canvas(b))
                r.render(gfx, r.timeline.slots[0].durationMs * 0.8f)
                b
            }
            thumbs[key] = bmp
            view.alpha = 0f
            view.setImageBitmap(bmp)
            view.animate().alpha(1f).setDuration(180).start()
        }
    }

    // ───────────────────────────── 4. Musique ─────────────────────────────

    private val trackRows = HashMap<String, View>()
    private var noMusic: View? = null

    private fun refreshSelection() {
        noMusic?.background = rounded(Brand.SURFACE, dp(14f).toFloat(), if (choice.track == null) Brand.SAFFRON else Brand.SURFACE, dp(2f))
        for ((id, v) in trackRows) v.background = rounded(Brand.SURFACE, dp(14f).toFloat(), if (choice.track?.id == id) Brand.SAFFRON else Brand.SURFACE, dp(2f))
    }

    private fun musicPage(): View {
        val page = vbox()
        val list = vbox()
        val chips = hbox()
        val chipViews = ArrayList<Pair<MusicCategory?, TextView>>()
        fun fill() {
            stopPlayer()
            list.removeAllViews(); trackRows.clear()
            noMusic = noMusicRow()
            list.addView(noMusic, lp(MATCH, WRAP, top = dp(12f)))
            val tracks = (imported + catalog).filter { category == null || (it.category == category && it.source == MusicSource.BUILT_IN) }
            for (t in tracks) list.addView(trackRow(t).also { trackRows[t.id] = it }, lp(MATCH, WRAP, top = dp(10f)))
            list.addView(button("＋ Importer un fichier audio", primary = false) { pickAudio.launch(arrayOf("audio/*")) }, lp(MATCH, WRAP, top = dp(14f)))
            list.addView(label("Les musiques intégrées sont des compositions originales CODJIA (licence GPL-3.0). " +
                "Un fichier importé est utilisé sous ta responsabilité.", 12f, Brand.MUTED), lp(MATCH, WRAP, top = dp(10f)))
            refreshSelection()
            for ((c, v) in chipViews) v.setChipSelected(c == category)
        }
        val all = chip("Toutes", category == null) { category = null; fill() }
        chipViews += null to all
        chips.addView(all, lp(WRAP, WRAP, end = dp(8f)))
        // Seules les catégories qui contiennent au moins une musique sont proposées.
        for (c in MusicCategory.entries.filter { cat -> catalog.any { it.category == cat } || imported.any { it.category == cat } }) {
            val v = chip(c.label, category == c) { category = c; fill() }
            chipViews += c to v
            chips.addView(v, lp(WRAP, WRAP, end = dp(8f)))
        }
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
        page.addView(ScrollView(this).apply { addView(list) }, lp(MATCH, 0, 1f))
        fill()
        // Volume dans la vidéo.
        val vol = hbox()
        vol.addView(label("Volume", 14f, Brand.MUTED), lp(WRAP, WRAP, end = dp(12f)))
        val volChips = ArrayList<Pair<MusicVolume, TextView>>()
        for (v in MusicVolume.entries) {
            val c = chip(v.label, choice.volume == v) { choice = choice.copy(volume = v); for ((k, cv) in volChips) cv.setChipSelected(k == choice.volume) }
            volChips += v to c
            vol.addView(c, lp(WRAP, WRAP, end = dp(8f)))
        }
        page.addView(vol, lp(MATCH, WRAP, top = dp(10f)))
        val vrow = hbox()
        vrow.addView(label("Voix off", 14f, Brand.MUTED), lp(WRAP, WRAP, end = dp(12f)))
        val vChips = ArrayList<Pair<VoiceMode, TextView>>()
        for (m in VoiceMode.entries) {
            val c = chip(m.label, voiceMode == m) { voiceMode = m; if (m != VoiceMode.NONE) lastVoice = m; for ((k, cv) in vChips) cv.setChipSelected(k == voiceMode) }
            vChips += m to c
            vrow.addView(c, lp(WRAP, WRAP, end = dp(8f)))
        }
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(vrow) }, lp(MATCH, WRAP, top = dp(8f)))
        page.addView(button("Voir l'aperçu") { show(Step.PREVIEW, forward = false) }, lp(MATCH, WRAP, top = dp(12f)))
        return page
    }

    private fun noMusicRow(): View = hbox().apply {
        setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
        addView(label("🔇", 20f), lp(WRAP, WRAP, end = dp(12f)))
        addView(label("Sans musique", 16f, bold = true), lp(0, WRAP, 1f))
        pressable { choice = choice.copy(track = null); refreshSelection() }
    }

    private fun trackRow(t: MusicTrack): View {
        val row = hbox().apply { setPadding(dp(12f), dp(12f), dp(14f), dp(12f)) }
        row.pressable { choice = choice.copy(track = t); refreshSelection() }
        val wave = WaveformView(this).apply { peaks = t.peaks }
        val play = TextView(this).apply {
            text = if (playingId == t.id) "❚❚" else "▶"; textSize = 18f; gravity = Gravity.CENTER; setTextColor(Brand.INK)
            background = rounded(Brand.TEAL, dp(22f).toFloat())
        }
        play.pressable { togglePreview(t, play, wave) }
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

    private var playingButton: TextView? = null

    private fun togglePreview(t: MusicTrack, button: TextView, wave: WaveformView) {
        if (playingId == t.id) { stopPlayer(); return }
        stopPlayer()
        val mp = runCatching { openPlayer(t) }.getOrElse { Toast.makeText(this, "Lecture impossible", Toast.LENGTH_SHORT).show(); return }
        player = mp; playingId = t.id; playingButton = button
        mp.setOnCompletionListener { stopPlayer() }
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
        playingButton?.text = "▶"
        playingButton = null
    }

    // ───────────────────────────── 5. Génération ─────────────────────────────

    private lateinit var log: TextView

    private fun exportPage(): View {
        val page = vbox()
        val percent = label("0 %", 44f, bold = true).apply { gravity = Gravity.CENTER }
        val status = label("Préparation…", 15f, Brand.MUTED).apply { gravity = Gravity.CENTER }
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        page.addView(percent, lp(MATCH, WRAP, top = dp(24f)))
        page.addView(status, lp(MATCH, WRAP, top = dp(4f)))
        page.addView(bar, lp(MATCH, WRAP, top = dp(16f)))
        val result = vbox()
        page.addView(result, lp(MATCH, WRAP, top = dp(18f)))
        log = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 11f; setTextColor(Brand.MUTED); setTextIsSelectable(true); visibility = View.GONE }
        val details = label("Détails techniques ▾", 13f, Brand.MUTED).apply { setPadding(0, dp(8f), 0, dp(8f)) }
        details.pressable { log.visibility = if (log.visibility == View.GONE) View.VISIBLE else View.GONE }
        page.addView(details, lp(WRAP, WRAP, top = dp(10f)))
        page.addView(ScrollView(this).apply { addView(log) }, lp(MATCH, 0, 1f))
        generate(bar, percent, status, result)
        return page
    }

    private fun say(line: String) { log.append(line + "\n") }

    private fun thermal(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) (getSystemService(POWER_SERVICE) as PowerManager).currentThermalStatus.toString() else "n/d"

    private fun generate(bar: ProgressBar, percent: TextView, status: TextView, result: LinearLayout) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        exporting = true
        lifecycleScope.launch {
            var tmpBed: File? = null
            var tmpOut: File? = null
            try {
                val format = withContext(Dispatchers.Default) { DeviceProfile.pickFormat() }
                val music = choice.track
                // Si la voix off échoue, la vidéo est quand même générée, avec la musique seule.
                val (sb, voicedAudio) = runCatching { audioPlan { status.text = it } }.getOrElse { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    say("Voix off      : impossible (${e.message}) · vidéo générée sans voix")
                    withContext(Dispatchers.Default) { Composer.prepare(content, template, choice.track?.takeIf { it.bpm > 0 }) } to null
                }
                val total = withContext(Dispatchers.Default) { FrameRenderer(sb, format, template).timeline.totalMs }
                say("Appareil      : ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                say("Format vidéo  : ${look.label}${if (hasDemo) " + démo à l'écran" else ""} · modèle ${template.name}")
                say("Musique       : ${music?.let { "${it.title} · volume ${choice.volume.label}" } ?: "sans musique"}")
                say("Voix off      : ${voiceMode.label}" + (voiced?.second?.let { " · ${it.languages.joinToString("+")} · vitesse ${it.speechRate}×" } ?: ""))
                say("Sortie        : ${format.width}×${format.height} à ${format.fps} i/s · ${total} ms")
                say("Thermique     : ${thermal()} au départ")

                status.text = if (music != null) "Préparation de la musique…" else "Préparation…"
                val t0 = SystemClock.elapsedRealtime()
                val workDir = File(cacheDir, "audio")
                val bed = voicedAudio ?: withContext(Dispatchers.IO) { AudioDecoder.prepareBed(this@MainActivity, choice, total, workDir) }
                if (voicedAudio == null) tmpBed = bed
                if (bed != null) say("Musique prête : ${(SystemClock.elapsedRealtime() - t0)} ms")

                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                val out = File(File(filesDir, "export").apply { mkdirs() }, "CODJIA_${ImportPolicy.safeFileStem(sb.id.removePrefix("demo-"))}_$stamp.mp4")
                tmpOut = out
                status.text = "Rendu des images…"
                val started = SystemClock.elapsedRealtime()
                val stats = Media3VideoExporter(this@MainActivity).export(sb, format, out, bed) { p ->
                    bar.progress = p
                    percent.text = "$p %"
                    if (p in 3..99) {
                        val el = SystemClock.elapsedRealtime() - started
                        val left = (el * (100 - p) / p / 1000).coerceAtLeast(1)
                        status.text = "Rendu des images… environ $left s restantes"
                    }
                }
                bar.progress = 100; percent.text = "100 %"
                status.text = "Vérification du fichier…"
                val probe = withContext(Dispatchers.IO) { Mp4Inspector.inspect(out) }
                // Règle : jamais de vidéo muette si une musique a été choisie.
                if ((choice.hasAudio || voicedAudio != null) && probe.audioMime == null) error("La piste audio est absente du fichier : export refusé")

                status.text = "Enregistrement dans la Galerie…"
                val uri = withContext(Dispatchers.IO) { GalleryExport.publish(this@MainActivity, out, ImportPolicy.displayName("${sb.title} — CODJIA.mp4")) }
                say("")
                say("Vidéo         : ${probe.mime} ${if (probe.isH264) "✓" else "✗"} · ${probe.displayWidth}×${probe.displayHeight}")
                say("Durée         : ${probe.durationMs} ms · ${probe.sampleCount} images · %.2f i/s".format(probe.measuredFps))
                say("Audio         : " + (probe.audioMime?.let { "$it ✓ · ${probe.audioSampleRate} Hz · ${probe.audioDurationMs} ms" } ?: "aucune piste"))
                if (probe.audioMime != null) say("Synchro       : écart audio/vidéo ${abs(probe.audioDurationMs - probe.durationMs)} ms")
                say("Taille        : %.1f Mo".format(stats.fileBytes / 1048576.0))
                say("Encodeur      : ${stats.encoderName}")
                say("Export        : %.1f s · rendu moy. %.1f ms/image".format(stats.wallMs / 1000.0, stats.avgFrameMs))
                say("Thermique     : ${thermal()} à la fin")

                percent.text = "✅"
                status.text = "Vidéo prête · Galerie › Movies › CODJIA"
                val actions = hbox()
                val canShare = uri.scheme == "content"
                val open = button("Ouvrir", primary = false) {
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
                val share = button("Partager") {
                    val send = Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(Intent.createChooser(send, "Partager la vidéo CODJIA"))
                }
                if (!canShare) { open.isEnabled = false; open.alpha = 0.4f; share.isEnabled = false; share.alpha = 0.4f }
                actions.addView(open, lp(0, WRAP, 1f, end = dp(10f)))
                actions.addView(share, lp(0, WRAP, 1f))
                result.addView(actions, lp(MATCH, WRAP))
                result.addView(button("🔀 Une autre version", primary = false) {
                    lifecycleScope.launch {
                        runCatching { withContext(Dispatchers.Default) { propose(regenerate = true) } }
                            .onSuccess { show(Step.PREVIEW, forward = false) }
                            .onFailure { Toast.makeText(this@MainActivity, it.message ?: "Impossible de créer une autre version", Toast.LENGTH_LONG).show() }
                    }
                }, lp(MATCH, WRAP, top = dp(10f)))
                result.addView(button("Nouveau texte", primary = false) { show(Step.CONTENT, forward = false) }, lp(MATCH, WRAP, top = dp(10f)))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                percent.text = "❌"
                status.text = "La génération a échoué : ${e.message ?: e::class.simpleName}"
                say("ÉCHEC : ${e::class.simpleName} — ${e.message}")
                e.cause?.let { say("Cause : ${it::class.simpleName} — ${it.message}") }
                result.addView(button("Réessayer") { show(Step.EXPORT, animate = false) }, lp(MATCH, WRAP))
                result.addView(button("Retour à l'aperçu", primary = false) { show(Step.PREVIEW, forward = false) }, lp(MATCH, WRAP, top = dp(10f)))
            } finally {
                exporting = false
                // Nettoyage : la piste WAV intermédiaire et la copie privée (la vidéo est dans la Galerie).
                withContext(NonCancellable + Dispatchers.IO) {
                    tmpBed?.delete()
                    tmpOut?.let { f -> f.delete(); File(f.parentFile, f.nameWithoutExtension + ".part.mp4").delete() }
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
