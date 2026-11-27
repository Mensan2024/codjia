// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.prototype

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Couleurs et petits composants de l'interface VIDEO24 (charte : encre, safran, sarcelle). */
object Brand {
    val INK = Color.parseColor("#0E1A2B")
    val BG = Color.parseColor("#0B1220")
    val SURFACE = Color.parseColor("#16243A")
    val SURFACE_2 = Color.parseColor("#1E2F4A")
    val TEXT = Color.parseColor("#F5F7FA")
    val MUTED = Color.parseColor("#9AA8BD")
    val SAFFRON = Color.parseColor("#F5B301")
    val TEAL = Color.parseColor("#2DD4BF")
    val DANGER = Color.parseColor("#FB7185")
}

fun Context.dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

fun rounded(color: Int, radius: Float, stroke: Int? = null, strokeW: Int = 0) = GradientDrawable().apply {
    setColor(color); cornerRadius = radius
    if (stroke != null) setStroke(strokeW, stroke)
}

fun Context.label(text: String, size: Float = 15f, color: Int = Brand.TEXT, bold: Boolean = false) = TextView(this).apply {
    this.text = text; textSize = size; setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
}

/** Retour tactile léger + effet d'enfoncement : l'appli répond au doigt comme une appli native. */
@SuppressLint("ClickableViewAccessibility")
fun View.pressable(onClick: () -> Unit): View {
    isClickable = true
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
        }
        false
    }
    setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onClick() }
    return this
}

/** Bouton principal (safran) ou secondaire (contour). */
fun Context.button(text: String, primary: Boolean = true, onClick: () -> Unit) = TextView(this).apply {
    this.text = text; textSize = 16f; gravity = Gravity.CENTER
    setTypeface(typeface, Typeface.BOLD)
    setTextColor(if (primary) Brand.INK else Brand.TEXT)
    background = if (primary) rounded(Brand.SAFFRON, dp(14f).toFloat())
    else rounded(Color.TRANSPARENT, dp(14f).toFloat(), Brand.MUTED, dp(1.5f))
    setPadding(dp(12f), dp(14f), dp(12f), dp(14f))
    // Une seule ligne, quelle que soit la largeur de l'écran : le texte se réduit au lieu de passer à la ligne.
    maxLines = 1
    if (android.os.Build.VERSION.SDK_INT >= 26) setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP)
    pressable(onClick)
}

/** Pastille de filtre (catégories, volume). */
fun Context.chip(text: String, selected: Boolean, onClick: () -> Unit) = TextView(this).apply {
    this.text = text; textSize = 14f
    setChipSelected(selected)
    setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
    pressable(onClick)
}

fun TextView.setChipSelected(selected: Boolean) {
    setTextColor(if (selected) Brand.INK else Brand.TEXT)
    background = if (selected) rounded(Brand.TEAL, context.dp(20f).toFloat()) else rounded(Brand.SURFACE_2, context.dp(20f).toFloat())
}

fun Context.vbox(pad: Int = 0) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
fun Context.hbox() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

fun lp(w: Int, h: Int, weight: Float = 0f, top: Int = 0, end: Int = 0) =
    LinearLayout.LayoutParams(w, h, weight).apply { topMargin = top; marginEnd = end }

const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

/** Représentation visuelle d'une piste : barres d'amplitude, la partie écoutée en couleur. */
class WaveformView(context: Context) : View(context) {
    var peaks: List<Float> = emptyList(); set(v) { field = v; invalidate() }
    var played: Float = 0f; set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var activeColor = Brand.TEAL
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val n = if (peaks.isEmpty()) 48 else peaks.size
        val w = width.toFloat()
        val h = height.toFloat()
        val bw = w / n
        for (i in 0 until n) {
            val v = if (peaks.isEmpty()) 0.15f else 0.12f + 0.88f * peaks[i]
            paint.color = if ((i + 0.5f) / n <= played) activeColor else Brand.MUTED and 0x80FFFFFF.toInt()
            val bh = h * v
            canvas.drawRoundRect(i * bw + bw * 0.2f, (h - bh) / 2, (i + 1) * bw - bw * 0.2f, (h + bh) / 2, bw * 0.3f, bw * 0.3f, paint)
        }
    }
}

fun formatDuration(ms: Long): String {
    val s = (ms + 500) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
