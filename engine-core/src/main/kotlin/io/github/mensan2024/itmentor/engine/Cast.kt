// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase C — Distribution de personnages (« Cast »).
 *
 * Personnages 2D vectoriels de face, plats, avec un vrai visage : sourcils, yeux qui clignent, bouche, joues, larmes, sueur ; un corps
 * à bras et jambes articulés (angles absolus), des coiffures (courte, bouclée, longue, tresses, foulard, chignon, crâne rasé) et des
 * tenues (t-shirt, chemise, sweat à capuche, robe à motifs, costume). Une [Face] décrit l'expression, une [Body] la pose ; les deux
 * s'interpolent, ce qui permet de faire passer un personnage de l'inquiétude au soulagement dans une même scène.
 *
 * Limites assumées : style plat, sans ombrage ni doigts ; personnage de face (regard et tête peuvent s'incliner), pas de rotation 3D.
 * Les apparences sont fictives et variées ; aucune ne représente une personne réelle.
 */
internal enum class Hair { SHORT, CURLY, LONG, BRAIDS, WRAP, BUN, BALD }
internal enum class Outfit { TEE, SHIRT, HOODIE, DRESS, SUIT, BLOUSE }

internal class Persona(
    val skin: Int, val hair: Int, val hairStyle: Hair, val top: Int, val bottom: Int, val outfit: Outfit,
    val glasses: Boolean = false, val shoe: Int = rgb(0x1F2430), val accent: Int = top, val beard: Boolean = false,
)

/** Expression du visage : tous les paramètres sont continus (voir [Face.mix]). */
internal data class Face(
    /** > 0 : sourcils froncés (déterminé), < 0 : sourcils inquiets (bouts intérieurs relevés). */
    val tilt: Float = 0f,
    val liftL: Float = 0f, val liftR: Float = 0f,
    val eyeOpen: Float = 1f,
    /** −1 bouche triste, +1 grand sourire. */
    val smile: Float = 0.1f,
    val mouthOpen: Float = 0f,
    val blush: Float = 0f, val sweat: Float = 0f, val tear: Float = 0f, val bags: Float = 0f,
    val gazeX: Float = 0f, val gazeY: Float = 0f,
) {
    companion object {
        val NEUTRAL = Face()
        val HAPPY = Face(tilt = 0.15f, liftL = 0.2f, liftR = 0.2f, eyeOpen = 0.8f, smile = 1f, mouthOpen = 0.25f, blush = 0.55f)
        val SMILE = Face(tilt = 0.1f, liftL = 0.1f, liftR = 0.1f, eyeOpen = 0.95f, smile = 0.65f, blush = 0.25f)
        val WORRIED = Face(tilt = -0.85f, liftL = 0.3f, liftR = 0.3f, eyeOpen = 1.05f, smile = -0.5f)
        val STRESSED = Face(tilt = -1f, liftL = 0.4f, liftR = 0.4f, eyeOpen = 1.12f, smile = -0.75f, mouthOpen = 0.3f, sweat = 1f, bags = 0.3f)
        val SAD = Face(tilt = -0.7f, eyeOpen = 0.7f, smile = -0.95f, tear = 1f, gazeY = 0.5f, bags = 0.25f)
        val SURPRISED = Face(liftL = 0.9f, liftR = 0.9f, eyeOpen = 1.25f, smile = 0f, mouthOpen = 0.85f)
        val RELIEVED = Face(tilt = 0.3f, liftL = 0.15f, liftR = 0.15f, eyeOpen = 0.12f, smile = 0.8f, mouthOpen = 0.1f, blush = 0.3f)
        val FOCUSED = Face(tilt = 0.45f, liftL = -0.1f, liftR = -0.1f, eyeOpen = 0.85f, smile = 0f)
        val SUSPICIOUS = Face(tilt = 0.2f, liftL = 0.75f, liftR = -0.15f, eyeOpen = 0.72f, smile = -0.25f)
        val SLEEPY = Face(tilt = -0.2f, eyeOpen = 0.3f, smile = 0f, mouthOpen = 0.08f, bags = 0.6f)
        val TIRED = Face(tilt = -0.45f, eyeOpen = 0.5f, smile = -0.3f, bags = 1f, gazeY = 0.3f)
        val ALARMED = Face(tilt = -0.6f, liftL = 0.7f, liftR = 0.7f, eyeOpen = 1.2f, smile = -0.4f, mouthOpen = 0.55f, sweat = 0.6f)

        fun mix(a: Face, b: Face, p: Float): Face {
            val k = clamp01(p)
            return Face(lerp(a.tilt, b.tilt, k), lerp(a.liftL, b.liftL, k), lerp(a.liftR, b.liftR, k), lerp(a.eyeOpen, b.eyeOpen, k), lerp(a.smile, b.smile, k),
                lerp(a.mouthOpen, b.mouthOpen, k), lerp(a.blush, b.blush, k), lerp(a.sweat, b.sweat, k), lerp(a.tear, b.tear, k), lerp(a.bags, b.bags, k),
                lerp(a.gazeX, b.gazeX, k), lerp(a.gazeY, b.gazeY, k))
        }
    }
}

/**
 * Pose du corps. Les angles des bras sont ABSOLUS, en degrés : 0 = vers le bas, 90 = à l'horizontale vers l'extérieur, 180 = vers le haut,
 * négatif = vers l'intérieur (croisé devant le corps). Premier nombre = bras, second = avant-bras. `L` = côté gauche de l'écran.
 */
internal data class Body(
    val armL: Pair<Float, Float> = 8f to 6f, val armR: Pair<Float, Float> = 8f to 6f,
    val tilt: Float = 0f,           // inclinaison de la tête (degrés)
    val lean: Float = 0f,           // translation du haut du corps (fraction de h)
    val step: Float = 0f,           // −1..1 : pas alternés en place
    val seated: Boolean = false,
    val crouch: Float = 0f,         // 0..1 : tassé (tête dans les épaules)
    /** Cible de la main (cinématique inverse) : remplace les angles du bras si elle est renseignée. */
    val handL: Hand? = null, val handR: Hand? = null,
) {
    companion object {
        val STAND = Body()
        fun mix(a: Body, b: Body, p: Float): Body {
            val k = clamp01(p)
            fun m(x: Pair<Float, Float>, y: Pair<Float, Float>) = lerp(x.first, y.first, k) to lerp(x.second, y.second, k)
            fun mh(x: Hand?, y: Hand?): Hand? = if (x != null && y != null && x.head == y.head) Hand(lerp(x.x, y.x, k), lerp(x.y, y.y, k), x.head) else if (k < 0.5f) x else y
            return Body(m(a.armL, b.armL), m(a.armR, b.armR), lerp(a.tilt, b.tilt, k), lerp(a.lean, b.lean, k), lerp(a.step, b.step, k), if (k < 0.5f) a.seated else b.seated, lerp(a.crouch, b.crouch, k),
                mh(a.handL, b.handL), mh(a.handR, b.handR))
        }
    }
}

/**
 * Cible de main. `head = true` : coordonnées en rayons de tête depuis le centre de la tête (x vers l'extérieur du côté de la main : + = vers
 * l'extérieur) ; sinon en fractions de la hauteur, depuis le centre des épaules (x + = vers l'extérieur, y + = vers le bas).
 */
internal data class Hand(val x: Float, val y: Float, val head: Boolean = false)

/** Positions utiles après le dessin : mains et centre de la tête, pour poser un accessoire. */
internal class Anchors(val lx: Float, val ly: Float, val rx: Float, val ry: Float, val headX: Float, val headY: Float, val hr: Float, val shoulderY: Float, val hipY: Float)

internal object Cast {
    /** Six apparences distinctes ; le même indice redonne toujours la même personne. */
    fun persona(i: Int): Persona = when (((i % 6) + 6) % 6) {
        0 -> Persona(rgb(0x7A4A30), rgb(0x16100C), Hair.SHORT, rgb(0x3F7CE0), rgb(0x2A3350), Outfit.SHIRT)
        1 -> Persona(rgb(0x8A5535), rgb(0x1A120D), Hair.WRAP, rgb(0xE9A23B), rgb(0x2A3350), Outfit.DRESS, accent = rgb(0x1F9D78))
        2 -> Persona(rgb(0x5B3623), rgb(0x0F0A08), Hair.CURLY, rgb(0x6C7487), rgb(0x2B3040), Outfit.HOODIE)
        3 -> Persona(rgb(0xB57A55), rgb(0x24160F), Hair.BRAIDS, rgb(0xE2675A), rgb(0x3A3F58), Outfit.TEE)
        4 -> Persona(rgb(0x6A4029), rgb(0xCFCFCF), Hair.BALD, rgb(0xF3F5F8), rgb(0x2F3D52), Outfit.SUIT, beard = true, accent = rgb(0x2F3D52))
        else -> Persona(rgb(0xD9A27C), rgb(0x2A1A12), Hair.BUN, rgb(0x2FA89B), rgb(0x2D3350), Outfit.BLOUSE, glasses = true)
    }

    // ───────────── Animation de repos ─────────────

    /** 0 = yeux ouverts, 1 = clignement (≈ 130 ms toutes les 3,4 s, décalé par personnage). */
    fun blink(t: Float, who: Int = 0): Float {
        val p = ((t + who * 1170f) % 3400f)
        return if (p < 130f) sin(p / 130f * PI.toFloat()) else 0f
    }

    fun breathe(t: Float, who: Int = 0): Float = sin((t + who * 700f) / 700f) * 0.5f + 0.5f

    // ───────────── Dessin ─────────────

    /**
     * @param x abscisse du centre, @param floor ordonnée des pieds, @param h hauteur totale (tête comprise).
     * Retourne les ancres (mains, tête).
     */
    fun draw(g: Gfx, x: Float, floor: Float, h: Float, lk: Persona, face: Face = Face.NEUTRAL, body: Body = Body.STAND, t: Float = 0f, who: Int = 0, mirror: Boolean = false): Anchors {
        val m = if (mirror) -1f else 1f
        val br = Cast.breathe(t, who)
        val hr = 0.105f * h
        val crouch = body.crouch
        val legLen = (if (body.seated) 0.30f else 0.43f) * h
        val hipY = floor - legLen
        val torsoH = 0.30f * h * (1f - 0.04f * crouch)
        val shY = hipY - torsoH - 0.004f * h * br
        val lx = x + m * body.lean * h
        val outline = mix(lk.skin, rgb(0x000000), 0.28f)
        val sk = lk.skin

        // ───── jambes ─────
        if (body.seated) {
            for (k in 0..1) {
                val sgn = if (k == 0) -1f else 1f
                val kx = x + sgn * 0.075f * h
                g.line(kx, hipY - 0.01f * h, kx, hipY + 0.08f * h, 0.085f * h, lk.bottom)
                g.line(kx, hipY + 0.07f * h, kx + sgn * 0.01f * h, floor - 0.035f * h, 0.07f * h, if (lk.outfit == Outfit.DRESS) sk else lk.bottom)
                g.fillRoundRect(kx - 0.05f * h, floor - 0.04f * h, kx + 0.07f * h, floor, 0.02f * h, lk.shoe)
            }
        } else {
            for (k in 0..1) {
                val sgn = if (k == 0) -1f else 1f
                val phase = body.step * sgn
                val lift = max(0f, phase) * 0.05f * h
                val fx = x + sgn * 0.07f * h + phase * 0.03f * h
                val hx = x + sgn * 0.055f * h
                val bottomSkin = lk.outfit == Outfit.DRESS
                val kneeY = hipY + legLen * 0.52f - lift * 0.5f
                g.line(hx, hipY, fx, kneeY, 0.082f * h, if (bottomSkin) sk else lk.bottom)
                g.line(fx, kneeY, fx, floor - 0.032f * h - lift, 0.072f * h, if (bottomSkin) sk else lk.bottom)
                g.fillRoundRect(fx - 0.05f * h, floor - 0.045f * h - lift, fx + 0.075f * h, floor - lift, 0.022f * h, lk.shoe)
            }
        }

        // ───── bras arrière, torse, bras avant ─────
        val shL = lx - 0.145f * h
        val shR = lx + 0.145f * h
        val sy = shY + 0.035f * h
        val armW = 0.058f * h
        val longSleeve = lk.outfit == Outfit.HOODIE || lk.outfit == Outfit.SUIT || lk.outfit == Outfit.SHIRT
        val shortSleeveFrac = 0.42f
        val l1 = 0.165f * h; val l2 = 0.15f * h
        val sleeveColor = if (lk.outfit == Outfit.SUIT) lk.accent else lk.top
        fun paint(sx: Float, ex: Float, ey: Float, hx: Float, hy: Float) {
            g.line(ex, ey, hx, hy, armW * 0.92f, if (longSleeve) sleeveColor else sk)
            if (longSleeve) g.line(ex + (hx - ex) * 0.8f, ey + (hy - ey) * 0.8f, hx, hy, armW * 0.9f, sk)
            g.line(sx, sy, ex, ey, armW, if (longSleeve) sleeveColor else sk)
            if (!longSleeve) g.line(sx, sy, sx + (ex - sx) * shortSleeveFrac, sy + (ey - sy) * shortSleeveFrac, armW * 1.04f, lk.top)
            g.fillCircle(hx, hy, 0.034f * h, sk)
        }
        fun armAngles(sx: Float, side: Float, a: Pair<Float, Float>): Pair<Float, Float> {
            val a1 = a.first * PI.toFloat() / 180f; val a2 = a.second * PI.toFloat() / 180f
            val ex = sx + side * sin(a1) * l1; val ey = sy + cos(a1) * l1
            val hx = ex + side * sin(a2) * l2; val hy = ey + cos(a2) * l2
            paint(sx, ex, ey, hx, hy)
            return hx to hy
        }
        fun armTo(sx: Float, side: Float, tx: Float, ty: Float): Pair<Float, Float> {
            val dx = tx - sx; val dy = ty - sy
            val reach = l1 + l2
            var d = kotlin.math.sqrt(dx * dx + dy * dy)
            val k = if (d > reach * 0.995f) reach * 0.995f / d else 1f
            val ttx = sx + dx * k; val tty = sy + dy * k
            d *= k
            val cosA = ((l1 * l1 + d * d - l2 * l2) / (2f * l1 * max(d, 0.001f))).coerceIn(-1f, 1f)
            val a = kotlin.math.acos(cosA)
            val th = kotlin.math.atan2(tty - sy, ttx - sx)
            val e1x = sx + cos(th + a) * l1; val e1y = sy + sin(th + a) * l1
            val e2x = sx + cos(th - a) * l1; val e2y = sy + sin(th - a) * l1
            // le coude le plus bas et le plus à l'extérieur est le plus naturel
            val s1 = e1y + side * (e1x - sx) * 0.4f; val s2 = e2y + side * (e2x - sx) * 0.4f
            val (ex, ey) = if (s1 > s2) e1x to e1y else e2x to e2y
            paint(sx, ex, ey, ttx, tty)
            return ttx to tty
        }
        // le bras du côté « arrière » est tracé avant le torse : ici les deux bras passent devant, sauf si repliés derrière (non utilisé)
    
        // capuche derrière la tête
        if (lk.outfit == Outfit.HOODIE) g.fillCircle(lx, shY - 0.045f * h, hr * 1.12f, mix(lk.top, rgb(0x000000), 0.15f))

        // jupe ou pan de robe
        if (lk.outfit == Outfit.DRESS) {
            val hem = floor - 0.2f * h
            val p = PathData().moveTo(lx - 0.125f * h, hipY - 0.12f * h).lineTo(lx + 0.125f * h, hipY - 0.12f * h)
                .cubicTo(lx + 0.2f * h, hipY, lx + 0.215f * h, hem - 0.03f * h, lx + 0.2f * h, hem)
                .lineTo(lx - 0.2f * h, hem).cubicTo(lx - 0.215f * h, hem - 0.03f * h, lx - 0.2f * h, hipY, lx - 0.125f * h, hipY - 0.12f * h).close()
            g.fillPath(p, lk.top)
            // motif : bandes et ronds d'une étoffe imprimée
            g.save(); g.clipPath(p)
            for (r in 0 until 3) for (c in 0 until 6) {
                val px = lx - 0.19f * h + c * 0.076f * h + (r % 2) * 0.038f * h; val py = hipY + r * 0.085f * h
                g.fillCircle(px, py, 0.016f * h, lk.accent); g.fillCircle(px, py, 0.007f * h, lk.top)
            }
            g.line(lx - 0.22f * h, hem - 0.025f * h, lx + 0.22f * h, hem - 0.025f * h, 0.018f * h, lk.accent)
            g.restore()
        }

        // torse
        val torso = PathData().moveTo(lx - 0.11f * h, shY)
            .cubicTo(lx - 0.15f * h, shY + 0.002f * h, lx - 0.165f * h, shY + 0.03f * h, lx - 0.15f * h, shY + 0.08f * h)
            .lineTo(lx - 0.125f * h, hipY + 0.01f * h)
            .lineTo(lx + 0.125f * h, hipY + 0.01f * h).lineTo(lx + 0.15f * h, shY + 0.08f * h)
            .cubicTo(lx + 0.165f * h, shY + 0.03f * h, lx + 0.15f * h, shY + 0.002f * h, lx + 0.11f * h, shY).close()
        val topColor = if (lk.outfit == Outfit.SUIT) lk.accent else lk.top
        g.fillPath(torso, topColor)
        when (lk.outfit) {
            Outfit.SHIRT -> {
                g.line(lx, shY + 0.03f * h, lx, hipY, 0.006f * h + 1f, mix(topColor, rgb(0x000000), 0.25f))
                for (b in 0 until 4) g.fillCircle(lx + 0.012f * h, shY + 0.07f * h + b * 0.062f * h, 0.006f * h + 0.7f, mix(topColor, rgb(0xFFFFFF), 0.5f))
            }
            Outfit.SUIT -> {
                val v = PathData().moveTo(lx - 0.05f * h, shY).lineTo(lx, shY + 0.15f * h).lineTo(lx + 0.05f * h, shY).close()
                g.fillPath(v, lk.top)
                g.line(lx - 0.05f * h, shY, lx - 0.012f * h, shY + 0.13f * h, 0.014f * h, mix(topColor, rgb(0x000000), 0.3f))
                g.line(lx + 0.05f * h, shY, lx + 0.012f * h, shY + 0.13f * h, 0.014f * h, mix(topColor, rgb(0x000000), 0.3f))
                g.fillRoundRect(lx - 0.008f * h, shY + 0.04f * h, lx + 0.008f * h, shY + 0.15f * h, 0.004f * h, rgb(0xC0392B))
            }
            Outfit.HOODIE -> {
                g.strokeRoundRect(lx - 0.07f * h, hipY - 0.1f * h, lx + 0.07f * h, hipY - 0.01f * h, 0.015f * h, 0.006f * h + 1f, mix(topColor, rgb(0x000000), 0.3f))
                g.line(lx - 0.025f * h, shY + 0.01f * h, lx - 0.03f * h, shY + 0.09f * h, 0.006f * h + 1f, mix(topColor, rgb(0xFFFFFF), 0.5f))
                g.line(lx + 0.025f * h, shY + 0.01f * h, lx + 0.03f * h, shY + 0.09f * h, 0.006f * h + 1f, mix(topColor, rgb(0xFFFFFF), 0.5f))
            }
            Outfit.BLOUSE -> {
                val v = PathData().moveTo(lx - 0.045f * h, shY).lineTo(lx, shY + 0.07f * h).lineTo(lx + 0.045f * h, shY).close()
                g.fillPath(v, sk)
            }
            Outfit.DRESS -> {
                val v = PathData().moveTo(lx - 0.05f * h, shY).cubicTo(lx - 0.05f * h, shY + 0.05f * h, lx + 0.05f * h, shY + 0.05f * h, lx + 0.05f * h, shY).close()
                g.fillPath(v, sk)
                g.line(lx - 0.13f * h, hipY - 0.12f * h, lx + 0.13f * h, hipY - 0.12f * h, 0.02f * h, lk.accent)
            }
            Outfit.TEE -> {
                val v = PathData().moveTo(lx - 0.045f * h, shY).cubicTo(lx - 0.045f * h, shY + 0.045f * h, lx + 0.045f * h, shY + 0.045f * h, lx + 0.045f * h, shY).close()
                g.fillPath(v, sk)
            }
        }
        // pantalon : ceinture et haut des cuisses
        if (lk.outfit != Outfit.DRESS) g.fillRoundRect(lx - 0.127f * h, hipY - 0.025f * h, lx + 0.127f * h, hipY + 0.045f * h, 0.015f * h, lk.bottom)
        // cou
        g.fillRoundRect(lx - 0.03f * h, shY - 0.055f * h, lx + 0.03f * h, shY + 0.012f * h, 0.012f * h, mix(sk, rgb(0x000000), 0.1f))

        // bras à angles : derrière le visage ; bras à cible (mains au visage) : après la tête
        var hL = if (body.handL == null) armAngles(shL, -1f, body.armL) else (shL to sy)
        var hR = if (body.handR == null) armAngles(shR, 1f, body.armR) else (shR to sy)

        // ───── tête ─────
        val tiltRad = (body.tilt * m) * PI.toFloat() / 180f
        val headCy = shY - 0.05f * h - hr * 0.92f + 0.03f * h * crouch
        val hx = lx + sin(tiltRad) * hr * 0.35f
        val hy = headCy
        g.save(); g.rotate(body.tilt * m, lx, shY - 0.03f * h)
        drawHead(g, hx - (hx - lx) * 0f, hy, hr, lk, face, t, who, m)
        g.restore()
        fun target(hd: Hand, side: Float): Pair<Float, Float> =
            if (hd.head) (hx + side * m * hd.x * hr) to (hy + hd.y * hr) else (lx + side * hd.x * h) to (shY + 0.035f * h + hd.y * h)
        body.handL?.let { val (tx, ty) = target(it, -1f); hL = armTo(shL, -1f, tx, ty) }
        body.handR?.let { val (tx, ty) = target(it, 1f); hR = armTo(shR, 1f, tx, ty) }
        return Anchors(hL.first, hL.second, hR.first, hR.second, hx, hy, hr, shY, hipY)
    }

    private fun drawHead(g: Gfx, hx: Float, hy: Float, hr: Float, lk: Persona, face: Face, t: Float, who: Int, m: Float) {
        val sk = lk.skin
        val dark = rgb(0x2A1810)
        val hairC = lk.hair
        // cheveux derrière le visage
        when (lk.hairStyle) {
            Hair.LONG -> g.fillRoundRect(hx - hr * 1.08f, hy - hr * 1.05f, hx + hr * 1.08f, hy + hr * 2.1f, hr * 0.9f, hairC)
            Hair.CURLY -> {
                for (k in 0 until 11) {
                    val a = (PI * (1.0 + k / 10.0)).toFloat()
                    g.fillCircle(hx + cos(a) * hr * 1.0f, hy - hr * 0.05f + sin(a) * hr * 1.0f, hr * 0.42f, hairC)
                }
                g.fillCircle(hx - hr * 1.0f, hy + hr * 0.1f, hr * 0.36f, hairC); g.fillCircle(hx + hr * 1.0f, hy + hr * 0.1f, hr * 0.36f, hairC)
            }
            Hair.BRAIDS -> for (s in intArrayOf(-1, 1)) for (k in 0 until 3) {
                val bx = hx + s * (hr * 0.95f + k * hr * 0.17f)
                for (j in 0 until 6) g.fillCircle(bx + sin(j * 1.3f + k) * hr * 0.03f, hy + hr * 0.1f + j * hr * 0.34f, hr * 0.12f, hairC)
            }
            else -> {}
        }
        // oreilles
        g.fillCircle(hx - hr * 0.93f, hy + hr * 0.08f, hr * 0.19f, sk); g.fillCircle(hx + hr * 0.93f, hy + hr * 0.08f, hr * 0.19f, sk)
        // visage
        g.fillRoundRect(hx - hr * 0.9f, hy - hr * 1.0f, hx + hr * 0.9f, hy + hr * 1.06f, hr * 0.86f, sk)
        // cheveux devant
        when (lk.hairStyle) {
            Hair.SHORT, Hair.LONG, Hair.BUN, Hair.BRAIDS -> {
                val p = PathData().moveTo(hx - hr * 0.94f, hy - hr * 0.12f)
                    .cubicTo(hx - hr * 1.12f, hy - hr * 1.0f, hx - hr * 0.5f, hy - hr * 1.28f, hx, hy - hr * 1.2f)
                    .cubicTo(hx + hr * 0.5f, hy - hr * 1.28f, hx + hr * 1.12f, hy - hr * 1.0f, hx + hr * 0.94f, hy - hr * 0.12f)
                    .cubicTo(hx + hr * 0.7f, hy - hr * 0.62f, hx + hr * 0.2f, hy - hr * 0.76f, hx - hr * 0.1f, hy - hr * 0.72f)
                    .cubicTo(hx - hr * 0.5f, hy - hr * 0.7f, hx - hr * 0.8f, hy - hr * 0.5f, hx - hr * 0.94f, hy - hr * 0.12f).close()
                g.fillPath(p, hairC)
                if (lk.hairStyle == Hair.BUN) { g.fillCircle(hx, hy - hr * 1.38f, hr * 0.38f, hairC) }
            }
            Hair.CURLY -> {
                val p = PathData().moveTo(hx - hr * 0.9f, hy - hr * 0.3f)
                    .cubicTo(hx - hr * 0.9f, hy - hr * 1.05f, hx + hr * 0.9f, hy - hr * 1.05f, hx + hr * 0.9f, hy - hr * 0.3f)
                    .cubicTo(hx + hr * 0.5f, hy - hr * 0.7f, hx - hr * 0.5f, hy - hr * 0.7f, hx - hr * 0.9f, hy - hr * 0.3f).close()
                g.fillPath(p, hairC)
            }
            Hair.WRAP -> {
                val wrap = lk.accent
                val p = PathData().moveTo(hx - hr * 0.98f, hy - hr * 0.05f)
                    .cubicTo(hx - hr * 1.1f, hy - hr * 1.15f, hx - hr * 0.4f, hy - hr * 1.35f, hx + hr * 0.1f, hy - hr * 1.28f)
                    .cubicTo(hx + hr * 0.8f, hy - hr * 1.25f, hx + hr * 1.12f, hy - hr * 0.9f, hx + hr * 0.98f, hy - hr * 0.05f)
                    .lineTo(hx + hr * 0.88f, hy - hr * 0.36f).cubicTo(hx + hr * 0.4f, hy - hr * 0.62f, hx - hr * 0.4f, hy - hr * 0.62f, hx - hr * 0.88f, hy - hr * 0.36f).close()
                g.fillPath(p, wrap)
                g.save(); g.clipPath(p)
                for (k in 0 until 7) g.fillCircle(hx - hr * 0.9f + k * hr * 0.3f, hy - hr * 0.9f + (k % 2) * hr * 0.2f, hr * 0.07f, lk.top)
                g.restore()
                g.fillCircle(hx + hr * 0.75f, hy - hr * 1.12f, hr * 0.3f, wrap); g.fillCircle(hx + hr * 0.95f, hy - hr * 1.38f, hr * 0.2f, wrap)
            }
            Hair.BALD -> {
                // crâne dégarni : un fin liseré gris sur les tempes
                g.strokeArc(hx, hy - hr * 0.1f, hr * 0.97f, 160f, 40f, hr * 0.12f, hairC); g.strokeArc(hx, hy - hr * 0.1f, hr * 0.97f, 340f, 40f, hr * 0.12f, hairC)
            }
        }
        // visage : œil, sourcils
        val blinkK = blink(t, who)
        val open = (face.eyeOpen * (1f - blinkK)).coerceIn(0f, 1.3f)
        val ey = hy + hr * 0.02f + face.gazeY * hr * 0.05f
        for (side in intArrayOf(-1, 1)) {
            val ex = hx + side * hr * 0.4f + face.gazeX * hr * 0.07f
            val eh = hr * 0.27f * open
            if (open < 0.22f) {
                // paupière fermée : arc (souriant si le visage sourit)
                val up = if (face.smile > 0.3f) -1f else 1f
                g.strokeArc(ex, ey + up * hr * 0.06f, hr * 0.15f, if (up < 0) 200f else 20f, 140f, hr * 0.065f + 1f, dark)
            } else {
                g.fillRoundRect(ex - hr * 0.11f, ey - eh / 2f, ex + hr * 0.11f, ey + eh / 2f, hr * 0.11f, dark)
                g.fillCircle(ex + hr * 0.035f, ey - eh * 0.2f, hr * 0.036f, rgb(0xFFFFFF))
            }
            // cernes
            if (face.bags > 0.02f) g.strokeArc(ex, ey + hr * 0.2f, hr * 0.17f, 20f, 140f, hr * 0.045f + 0.5f, mix(sk, rgb(0x4A2A5A), 0.45f * face.bags))
            // sourcils : bout intérieur vers le centre du visage
            val lift = if (side < 0) face.liftL else face.liftR
            val base = ey - hr * 0.33f - lift * hr * 0.17f
            val inner = base + face.tilt * hr * 0.13f
            val outer = base - face.tilt * hr * 0.13f
            val ix = hx + side * hr * 0.2f; val ox = hx + side * hr * 0.64f
            g.line(ix, inner, ox, outer, hr * 0.085f + 0.8f, if (lk.hairStyle == Hair.BALD && lk.beard) mix(lk.hair, rgb(0x000000), 0.4f) else mix(lk.hair, rgb(0x000000), 0.15f))
            // larme
            if (face.tear > 0.05f) {
                val p = ((t + side * 400f) % 2200f) / 2200f
                if (side > 0 || face.tear > 0.7f) {
                    val ty = ey + hr * 0.12f + p * hr * 0.9f
                    g.fillCircle(ex + side * hr * 0.05f, ty, hr * 0.05f * face.tear, rgb(0x9AD8FF).withAlpha(0.9f * (1f - p * 0.6f)))
                }
            }
        }
        if (lk.glasses) {
            val gc = rgb(0x1E2430)
            g.strokeRoundRect(hx - hr * 0.68f, ey - hr * 0.2f, hx - hr * 0.1f, ey + hr * 0.28f, hr * 0.14f, hr * 0.06f + 1f, gc)
            g.strokeRoundRect(hx + hr * 0.1f, ey - hr * 0.2f, hx + hr * 0.68f, ey + hr * 0.28f, hr * 0.14f, hr * 0.06f + 1f, gc)
            g.line(hx - hr * 0.1f, ey, hx + hr * 0.1f, ey, hr * 0.05f + 1f, gc)
        }
        // nez
        g.strokeArc(hx, hy + hr * 0.3f, hr * 0.1f, 20f, 140f, hr * 0.05f + 0.6f, mix(sk, rgb(0x000000), 0.28f))
        // joues
        if (face.blush > 0.02f) { for (s in intArrayOf(-1, 1)) g.fillCircle(hx + s * hr * 0.58f, hy + hr * 0.38f, hr * 0.17f, rgb(0xFF6F7A).withAlpha(0.38f * face.blush)) }
        // barbe
        if (lk.beard) {
            val b = PathData().moveTo(hx - hr * 0.86f, hy + hr * 0.1f).cubicTo(hx - hr * 0.8f, hy + hr * 1.12f, hx + hr * 0.8f, hy + hr * 1.12f, hx + hr * 0.86f, hy + hr * 0.1f)
                .cubicTo(hx + hr * 0.6f, hy + hr * 0.55f, hx - hr * 0.6f, hy + hr * 0.55f, hx - hr * 0.86f, hy + hr * 0.1f).close()
            g.fillPath(b, lk.hair)
        }
        // bouche
        val my = hy + hr * 0.62f
        val s = face.smile
        val mw = hr * 0.3f
        val mouthColor = rgb(0x4A1D1D)
        if (face.mouthOpen > 0.12f) {
            val oh = hr * (0.1f + 0.34f * face.mouthOpen)
            if (s > 0.2f) {
                // sourire ouvert : demi-lune
                val p = PathData().moveTo(hx - mw * 1.05f, my - oh * 0.35f).lineTo(hx + mw * 1.05f, my - oh * 0.35f)
                    .cubicTo(hx + mw * 1.0f, my + oh * 1.1f, hx - mw * 1.0f, my + oh * 1.1f, hx - mw * 1.05f, my - oh * 0.35f).close()
                g.fillPath(p, mouthColor)
                g.fillRoundRect(hx - mw * 0.7f, my - oh * 0.35f, hx + mw * 0.7f, my - oh * 0.35f + oh * 0.28f, oh * 0.1f, rgb(0xFFFFFF))
            } else {
                g.fillRoundRect(hx - mw * (0.8f - 0.25f * face.mouthOpen), my - oh / 2f, hx + mw * (0.8f - 0.25f * face.mouthOpen), my + oh / 2f, oh * 0.5f, mouthColor)
            }
        } else {
            val curve = hr * 0.22f * s
            val p = PathData().moveTo(hx - mw, my - curve * 0.2f).cubicTo(hx - mw * 0.4f, my + curve, hx + mw * 0.4f, my + curve, hx + mw, my - curve * 0.2f)
            g.strokePath(p, hr * 0.075f + 1f, mouthColor)
        }
        // sueur
        if (face.sweat > 0.05f) {
            val p = ((t % 1600f) / 1600f)
            val sx = hx + hr * 0.86f; val sy = hy - hr * 0.5f + p * hr * 0.5f
            val drop = PathData().moveTo(sx, sy - hr * 0.2f).cubicTo(sx + hr * 0.15f, sy, sx + hr * 0.14f, sy + hr * 0.2f, sx, sy + hr * 0.2f)
                .cubicTo(sx - hr * 0.14f, sy + hr * 0.2f, sx - hr * 0.15f, sy, sx, sy - hr * 0.2f).close()
            g.fillPath(drop, rgb(0x8FD3FF).withAlpha(face.sweat * (1f - p * 0.5f)))
        }
    }
}

/** Poses prêtes à l'emploi : un geste = une fonction du temps. */
internal object Poses {
    private fun w(t: Float, period: Float, off: Float = 0f) = sin((t + off) / period * 2f * PI.toFloat())

    val STAND = Body(armL = 6f to 4f, armR = 6f to 4f)
    fun idle(t: Float) = Body(armL = 6f + 1.5f * w(t, 3000f) to 4f, armR = 6f - 1.5f * w(t, 3000f) to 4f)
    fun wave(t: Float) = Body(armL = 6f to 4f, armR = 150f + 6f to 175f + 25f * w(t, 700f))
    fun point(t: Float) = Body(armL = 6f to 4f, armR = 85f to 85f + 4f * w(t, 900f))
    fun shrug(t: Float) = Body(armL = 55f to 95f + 8f * w(t, 1400f), armR = 55f to 95f - 8f * w(t, 1400f))
    fun handsUp(t: Float) = Body(armL = 150f + 6f * w(t, 600f) to 170f, armR = 150f - 6f * w(t, 600f) to 170f)
    fun thinking(t: Float) = Body(armL = 6f to 4f, armR = 38f to -150f, tilt = -6f + 2f * w(t, 2600f))
    /** Mains devant le buste : téléphone ou carnet. */
    fun holdFront(t: Float) = Body(armL = 28f to -62f + 3f * w(t, 1800f), armR = 28f to -62f - 3f * w(t, 1800f), tilt = 4f)
    fun typing(t: Float) = Body(armL = 34f to -78f + 5f * w(t, 260f), armR = 34f to -78f - 5f * w(t, 260f, 90f), seated = true, tilt = 3f)
    fun headInHands(t: Float) = Body(tilt = 8f, crouch = 1f, seated = true, handL = Hand(0.95f, 0.55f, true), handR = Hand(0.95f, 0.55f, true))
    fun handsOnHead(t: Float) = Body(tilt = 3f, crouch = 0.4f, handL = Hand(1.05f, -0.7f + 0.03f * w(t, 900f), true), handR = Hand(1.05f, -0.7f - 0.03f * w(t, 900f), true))
    fun hand2Chin(t: Float) = Body(armL = 6f to 4f, tilt = -6f, handR = Hand(0.25f, 0.95f, true))
    fun phoneAtEar(t: Float) = Body(armL = 6f to 4f, tilt = 6f, handR = Hand(1.0f, 0.15f, true))
    fun facePalm(t: Float) = Body(armL = 6f to 4f, tilt = 6f, crouch = 0.3f, handR = Hand(0.1f, 0.0f, true))
    fun crossed(t: Float) = Body(armL = 20f to -112f, armR = 20f to -112f)
    fun walk(t: Float, speed: Float = 520f) = Body(armL = 10f + 14f * w(t, speed) to 8f, armR = 10f - 14f * w(t, speed) to 8f, step = w(t, speed))
    fun carry(t: Float, speed: Float = 520f) = Body(armL = 8f to 4f, armR = 14f to -4f, step = w(t, speed))
    fun hug(t: Float) = Body(armL = 40f to -100f, armR = 40f to -100f)
    fun slump(t: Float) = Body(armL = 4f to 2f, armR = 4f to 2f, tilt = 12f, crouch = 0.8f, lean = 0.01f)
    fun open(t: Float) = Body(armL = 60f to 62f, armR = 60f to 62f)
}
