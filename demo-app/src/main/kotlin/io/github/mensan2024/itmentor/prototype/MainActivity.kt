// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.prototype

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import io.github.mensan2024.itmentor.engine.DemoStoryboard
import io.github.mensan2024.itmentor.engine.android.DeviceProfile
import io.github.mensan2024.itmentor.engine.android.GalleryExport
import io.github.mensan2024.itmentor.engine.android.Media3VideoExporter
import io.github.mensan2024.itmentor.engine.android.Mp4Inspector
import io.github.mensan2024.itmentor.engine.android.Narrator
import io.github.mensan2024.itmentor.engine.android.OfflineVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Banc d'essai minimal (pas l'interface d'IT_MENTOR) : un bouton, une barre de progression,
 * et le rapport de validation mesuré sur le téléphone.
 */
@UnstableApi
class MainActivity : ComponentActivity() {

    private lateinit var log: TextView
    private lateinit var bar: ProgressBar
    private lateinit var run: Button
    private lateinit var open: Button
    private lateinit var installVoice: Button
    private var lastUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        root.addView(TextView(this).apply { text = "IT_MENTOR · prototype du moteur vidéo"; textSize = 20f; setTypeface(typeface, Typeface.BOLD) })
        run = Button(this).apply { text = "Générer la vidéo de démonstration" }
        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        open = Button(this).apply { text = "Ouvrir la vidéo"; isEnabled = false }
        installVoice = Button(this).apply { text = "Installer une voix française"; visibility = android.view.View.GONE }
        log = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 12f; setTextIsSelectable(true) }
        root.addView(run); root.addView(bar); root.addView(open); root.addView(installVoice)
        root.addView(ScrollView(this).apply { addView(log) })
        setContentView(root)

        run.setOnClickListener { generate() }
        installVoice.setOnClickListener { runCatching { startActivity(OfflineVoice.installIntent()) } }
        open.setOnClickListener {
            lastUri?.let { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(it, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
        }
    }

    private fun thermal(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) (getSystemService(POWER_SERVICE) as PowerManager).currentThermalStatus.toString() else "n/d"

    private fun generate() {
        run.isEnabled = false
        open.isEnabled = false
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        log.text = ""
        lifecycleScope.launch {
            try {
                val format = withContext(Dispatchers.Default) { DeviceProfile.pickFormat() }
                say("Appareil      : ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                say("Format choisi : ${format.width}×${format.height} à ${format.fps} i/s")
                say("Thermique     : ${thermal()} au départ")
                val out = File(getExternalFilesDir(null) ?: filesDir, "IT_MENTOR_demo_${format.height}p.mp4")

                // Voix off hors ligne : si aucune voix française n'est installée, la vidéo reste muette.
                var storyboard = DemoStoryboard.phishing()
                var audio: File? = null
                val voice = OfflineVoice.open(this@MainActivity)
                if (voice == null) {
                    say("Voix off      : aucune voix française hors ligne → vidéo muette")
                    installVoice.visibility = android.view.View.VISIBLE
                } else {
                    try {
                        val t0 = android.os.SystemClock.elapsedRealtime()
                        val narrated = Narrator.prepare(voice, storyboard, File(cacheDir, "voix"))
                        storyboard = narrated.storyboard
                        audio = narrated.audio
                        say("Voix off      : ${narrated.voiceName} (${voice.engine})")
                        say("Débit parole  : ×%.2f · %.1f s de voix · synthèse %.1f s".format(
                            narrated.speechRate, narrated.voiceMs / 1000.0, (android.os.SystemClock.elapsedRealtime() - t0) / 1000.0))
                    } finally {
                        voice.shutdown()
                    }
                }
                val stats = Media3VideoExporter(this@MainActivity).export(storyboard, format, out, audio) { bar.progress = it }
                bar.progress = 100
                val probe = withContext(Dispatchers.IO) { Mp4Inspector.inspect(out) }
                lastUri = withContext(Dispatchers.IO) { GalleryExport.publish(this@MainActivity, out) }
                say("")
                say("── Fichier produit (relu avec MediaExtractor)")
                say("Codec         : ${probe.mime} ${if (probe.isH264) "✓" else "✗"}")
                say("Affichage     : ${probe.displayWidth}×${probe.displayHeight}" +
                    if (probe.rotationDegrees != 0) " (encodée ${probe.width}×${probe.height}, rotation ${probe.rotationDegrees}°)" else "")
                say("Durée         : ${probe.durationMs} ms · ${probe.sampleCount} images · %.2f i/s".format(probe.measuredFps))
                say("Audio         : " + (probe.audioMime?.let { "$it · ${probe.audioSampleRate} Hz · ${probe.audioDurationMs} ms" } ?: "aucune piste"))
                say("Taille        : %.1f Mo".format(stats.fileBytes / 1048576.0))
                say("Encodeur      : ${stats.encoderName}")
                say("")
                say("── Performances")
                say("Export total  : %.1f s (%.2f× le temps réel)".format(stats.wallMs / 1000.0, stats.wallMs / stats.durationMs.toDouble()))
                say("Rendu/image   : moy. %.1f ms · p95 %.1f ms · max %.1f ms".format(stats.avgFrameMs, stats.p95FrameMs, stats.maxFrameMs))
                say("Mémoire       : Java %.0f Mo · native %.0f Mo (pics)".format(stats.javaHeapPeakMb, stats.nativeHeapPeakMb))
                say("Thermique     : ${thermal()} à la fin")
                say("")
                say("Vidéo : Galerie › Movies › IT_MENTOR")
                // Sur Android 8 et 9, la vidéo est dans le dossier Movies de l’appli (pas d’URI partageable sans FileProvider).
                open.isEnabled = lastUri?.scheme == "content"
            } catch (e: Exception) {
                say("ÉCHEC : ${e::class.simpleName} — ${e.message}")
                e.cause?.let { say("Cause : ${it::class.simpleName} — ${it.message}") }
            } finally {
                run.isEnabled = true
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun say(line: String) { log.append(line + "\n") }
}
