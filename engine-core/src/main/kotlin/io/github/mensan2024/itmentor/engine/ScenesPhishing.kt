// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hameçonnage (messages piégés) : le leurre, l'anatomie du message, le clic, la vérification, le passage par le site officiel,
 * la réaction après un clic. Les messages et adresses dessinés sont des EXEMPLES FICTIFS (domaine « .example », réservé par
 * la norme RFC 2606 : il ne mène nulle part) et sont annoncés comme tels.
 */
internal object PhishingScenes {
    private const val FAKE_URL = "http://bnq-verif.example/ok"
    private const val REAL_URL = "https://banque.example"

    /** Bulle de message (fictif) dans l'écran d'un téléphone centré en (cx, cy). Retourne les rectangles (urgence, code, lien). */
    private fun smsBubble(c: StageCtx, cx: Float, cy: Float, pw: Float, ph: Float, reveal: Float, hl: FloatArray = floatArrayOf(0f, 0f, 0f)): Array<FloatArray> {
        val g = c.g; val th = c.th
        val l = cx - pw / 2f + 26f; val r = cx + pw / 2f - 26f
        val top = cy - ph / 2f + 70f
        SK.text(g, "BANQUE INFO", l, top, 20f, th.muted)
        val bl = l; val bt = top + 16f; val br = r; val bb = bt + 270f
        g.withPose(Pose(alpha = reveal, dy = (1f - reveal) * 24f), cx, cy) {
            g.fillRoundRect(bl, bt, br, bb, 22f, th.panelHi)
            val f = SK.font(23f, 600)
            val x = bl + 18f
            g.drawText("Compte suspendu", x, bt + 44f, f, th.ink)
            g.drawText("sous 24 h.", x, bt + 74f, f, th.ink)
            g.drawText("Confirmez votre", x, bt + 128f, f, th.ink)
            g.drawText("code ici :", x, bt + 158f, f, th.ink)
            val mf = SK.font(20f, 400, FontFamily.MONO)
            g.drawText("http://bnq-", x, bt + 214f, mf, th.bad)
            g.drawText("verif.example/ok", x, bt + 242f, mf, th.bad)
            for ((k, rect) in listOf(floatArrayOf(x - 6f, bt + 50f, br - 14f, bt + 84f), floatArrayOf(x - 6f, bt + 104f, br - 14f, bt + 170f), floatArrayOf(x - 6f, bt + 194f, br - 14f, bt + 252f)).withIndex()) {
                val a = hl[k]
                if (a > 0.01f) g.strokeRoundRect(rect[0], rect[1], rect[2], rect[3], 10f, 4f, th.warn.withAlpha(a))
            }
        }
        return arrayOf(floatArrayOf(bl + 18f, bt + 67f), floatArrayOf(bl + 18f, bt + 143f), floatArrayOf(bl + 18f, bt + 223f))
    }

    private val bait = beat("bait", "Le leurre arrive", setOf(Role.INTRO), setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Thing.SMS, Actor.ATTAQUANT, Act.ENVOYER), needs = cueSet(Thing.SMS)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SIMULATION  -  EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        SK.glow(g, 150f, 380f, 260f, th.bad, 0.16f)
        SK.glow(g, 830f, 380f, 260f, th.accent, 0.12f)
        val appear = SK.pop(t, 100f, 700f)
        g.withPose(Pose(alpha = appear, scale = 0.85f + 0.15f * appear), 150f, 380f) {
            SK.hood(g, 150f, 330f, 78f, rgb(0x1B2233), th.bad)
            SK.laptop(g, 150f, 540f, 170f, th, screen = th.bad.withAlpha(0.35f))
        }
        SK.pill(g, "ATTAQUANT", 150f, 640f, th.bad, th.bgTop.withAlpha(0.8f), 26f, appear)
        // Le téléphone vibre à chaque arrivée.
        val period = 3000f
        var shake = 0f; var badge = 0f
        for (k in 0 until 3) {
            val u = SK.loop(t, period, k * period / 3f)
            if (u > 0.93f) shake = maxOf(shake, 1f - (u - 0.93f) / 0.07f)
        }
        if (t > 2000f) badge = SK.pop(t, 2000f, 400f)
        val px = 830f + sin(t / 38f) * 5f * shake
        SK.phone(g, px, 380f, 190f, 380f, th)
        g.fillRoundRect(px - 80f, 300f, px + 80f, 345f, 12f, th.panelHi)
        g.fillRoundRect(px - 80f, 360f, px + 40f, 396f, 12f, th.panelHi)
        if (badge > 0f) {
            g.fillCircle(px + 86f, 205f, 22f * badge, th.bad)
            if (badge > 0.8f) SK.text(g, "1", px + 86f, 213f, 24f, rgb(0xFFFFFF), center = true)
        }
        SK.pill(g, "TOI", 830f, 640f, th.ink, th.bgTop.withAlpha(0.8f), 26f, appear)
        for (k in 0 until 3) {
            val u = SK.loop(t - 700f, period, k * period / 3f)
            if (t < 700f + k * period / 3f) continue
            val x = lerp(250f, 735f, u); val y = lerp(400f, 360f, u) - sin(u * PI.toFloat()) * 120f
            val a = clamp01(u / 0.08f) * clamp01((1f - u) / 0.06f)
            SK.envelope(g, x, y, 70f, th.panelHi, th.warn, a)
        }
        SK.dashed(g, 250f, 410f, 735f, 370f, 3f, th.bad.withAlpha(0.25f), phase = t / 30f)
    }

    private val anatomy = beat("anatomy", "Anatomie du message", setOf(Role.RECOGNIZE, Role.DEFINE), setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Thing.SMS, Thing.LIEN, Thing.CODE), needs = cueSet(Thing.SMS)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        val pcx = 270f; val pcy = 360f
        SK.glow(g, pcx, pcy, 330f, th.accent, 0.10f)
        SK.phone(g, pcx, pcy, 340f, 640f, th)
        val reveal = SK.ease(t, 150f, 600f, Easing.EXPO_OUT)
        val hl = floatArrayOf(0f, 0f, 0f)
        val flags = listOf("URGENCE" to "on te presse d'agir", "DEMANDE DE CODE" to "on te réclame une information", "LIEN SUSPECT" to "adresse qui n'est pas le site officiel")
        for (k in 0 until 3) {
            val s = 1500f + k * 1900f
            hl[k] = SK.ease(t, s, 400f) * (0.6f + 0.4f * sin(t / 220f).let { if (t > s) it else 1f }) * (if (t < s + 1800f || k == 2) 1f else 0.35f)
        }
        val anchors = smsBubble(c, pcx, pcy, 340f, 640f, reveal, hl)
        for (k in 0 until 3) {
            val s = 1500f + k * 1900f
            val p = SK.pop(t, s + 150f, 500f)
            if (p <= 0f) continue
            val cy = 205f + k * 150f
            val lx = 560f
            g.withPose(Pose(alpha = clamp01(p), dx = (1f - clamp01(p)) * 60f), lx, cy) {
                g.fillRoundRect(lx, cy - 58f, 985f, cy + 58f, 20f, th.panel)
                g.strokeRoundRect(lx, cy - 58f, 985f, cy + 58f, 20f, 3f, th.warn.withAlpha(0.8f))
                g.fillCircle(lx + 44f, cy, 26f, th.warn)
                SK.text(g, (k + 1).toString(), lx + 44f, cy + 10f, 30f, th.bgBottom, center = true)
                SK.text(g, flags[k].first, lx + 88f, cy - 6f, 30f, th.ink)
                SK.text(g, flags[k].second, lx + 88f, cy + 30f, 21f, th.muted, 500)
                g.line(lx, cy, anchors[k][0] + 150f, anchors[k][1], 3f, th.warn.withAlpha(0.55f))
            }
        }
    }

    private val click = beat("click", "Le clic qui piège", setOf(Role.MECHANISM, Role.PROCEDURE), setOf(Goal.COMPRENDRE_MECANISME),
        cueSet(Act.CLIQUER, Act.SAISIR, Thing.LIEN)) { c ->
        val g = c.g; val th = c.th
        val t = c.t % 11000f
        SK.caption(g, "SIMULATION  -  EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        val pcx = 330f; val pcy = 370f; val pw = 340f; val ph = 640f
        SK.glow(g, pcx, pcy, 330f, th.accent, 0.10f)
        SK.phone(g, pcx, pcy, pw, ph, th)
        val phase2 = SK.ease(t, 3000f, 500f)
        val l = pcx - pw / 2f + 26f; val r = pcx + pw / 2f - 26f
        if (phase2 < 0.5f) {
            smsBubble(c, pcx, pcy, pw, ph, 1f)
            // Le curseur se déplace vers le lien puis clique.
            val u = SK.ease(t, 700f, 1500f)
            val cx = lerp(pcx + 90f, l + 90f, u); val cy = lerp(pcy + 230f, pcy + 130f, u)
            SK.cursor(g, cx, cy, 26f, rgb(0xFFFFFF), rgb(0x10151C))
            val ck = window(t, 2300f, 600f)
            if (ck > 0f && ck < 1f) g.strokeCircle(cx, cy, 14f + 60f * ck, 4f, th.warn.withAlpha(1f - ck))
        } else {
            // Fausse page de connexion (copie grossière d'une banque).
            val top = pcy - ph / 2f + 70f
            g.fillRoundRect(l, top, r, top + 70f, 12f, th.bad.withAlpha(0.8f))
            SK.text(g, "BANQUE", (l + r) / 2f, top + 46f, 28f, rgb(0xFFFFFF), center = true)
            SK.text(g, "Identifiant", l + 6f, top + 120f, 20f, th.muted, 500)
            g.fillRoundRect(l, top + 132f, r, top + 184f, 10f, th.bgBottom)
            SK.text(g, "Code", l + 6f, top + 226f, 20f, th.muted, 500)
            g.fillRoundRect(l, top + 238f, r, top + 290f, 10f, th.bgBottom)
            val n1 = (SK.ease(t, 3800f, 1300f, Easing.LINEAR) * 8f).toInt(); val n2 = (SK.ease(t, 5300f, 1000f, Easing.LINEAR) * 6f).toInt()
            for (i in 0 until n1) g.fillCircle(l + 24f + i * 22f, top + 158f, 6f, th.ink)
            for (i in 0 until n2) g.fillCircle(l + 24f + i * 22f, top + 264f, 6f, th.ink)
            val press = SK.ease(t, 6400f, 300f)
            g.fillRoundRect(l, top + 330f, r, top + 390f, 14f, mix(th.line, th.bad, press))
            SK.text(g, "VALIDER", (l + r) / 2f, top + 369f, 24f, rgb(0xFFFFFF), center = true)
        }
        // Les données partent chez l'attaquant.
        val send = window(t, 6700f, 2600f)
        val hx = 820f; val hy = 190f
        SK.glow(g, hx, hy + 20f, 220f, th.bad, 0.10f + 0.20f * send)
        SK.hood(g, hx, hy + 40f, 70f, rgb(0x1B2233), if (send > 0f) th.bad else th.muted)
        SK.pill(g, "ATTAQUANT", hx, hy + 170f, th.bad, th.bgTop.withAlpha(0.8f), 24f)
        if (send > 0f) {
            for (k in 0 until 5) {
                val u = clamp01(send * 1.4f - k * 0.12f)
                if (u <= 0f || u >= 1f) continue
                val x = lerp(pcx + 90f, hx - 20f, u); val y = lerp(pcy - 40f, hy + 80f, u) - sin(u * PI.toFloat()) * 60f
                SK.packet(g, x, y, 11f, th.bad)
            }
            SK.pill(g, "DONNEES VOLEES", 720f, 560f, rgb(0xFFFFFF), th.bad, 28f, SK.pop(t, 8200f, 500f).coerceAtMost(1f))
        }
    }

    private val verify = beat("verify", "Vérifier l'expéditeur et l'adresse", setOf(Role.ADVICE), setOf(Goal.APPRENDRE_REFLEXE),
        cueSet(Act.VERIFIER, Thing.LIEN, Thing.MESSAGE)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        fun row(y: Float, label: String, url: String, ok: Boolean, start: Float) {
            val p = SK.ease(t, start, 600f, Easing.EXPO_OUT)
            g.withPose(Pose(alpha = p, dx = (1f - p) * -70f), 500f, y) {
                g.fillRoundRect(60f, y - 90f, 940f, y + 90f, 26f, th.panel)
                g.strokeRoundRect(60f, y - 90f, 940f, y + 90f, 26f, 3f, th.line)
                SK.text(g, label, 100f, y - 40f, 24f, th.muted)
                SK.text(g, url, 100f, y + 22f, 40f, if (ok) th.good else th.bad, 400, family = FontFamily.MONO)
            }
        }
        row(200f, "ADRESSE REÇUE DANS LE MESSAGE", FAKE_URL, false, 200f)
        row(470f, "SITE OFFICIEL (TAPÉ OU ENREGISTRÉ PAR TOI)", REAL_URL, true, 900f)
        // La loupe parcourt l'adresse reçue, puis un verdict apparaît.
        val u = SK.ease(t, 1600f, 2400f, Easing.EASE_IN_OUT)
        val mx = lerp(130f, 760f, u); val my = 222f + sin(u * 6f) * 8f
        if (t > 1500f && t < 4800f) {
            g.strokeCircle(mx, my, 44f, 8f, th.accent)
            g.line(mx + 31f, my + 31f, mx + 70f, my + 70f, 11f, th.accent)
            g.fillCircle(mx, my, 44f, th.accent.withAlpha(0.12f))
        }
        val v1 = SK.pop(t, 4200f, 500f)
        if (v1 > 0f) {
            g.fillCircle(880f, 200f, 40f * v1, th.bad)
            SK.cross(g, 880f, 200f, 28f * v1, rgb(0xFFFFFF), 1f)
            SK.pill(g, "NE CORRESPOND PAS", 500f, 330f, rgb(0xFFFFFF), th.bad, 26f, clamp01(v1))
        }
        val v2 = SK.pop(t, 5200f, 500f)
        if (v2 > 0f) { g.fillCircle(880f, 470f, 40f * v2, th.good); SK.check(g, 880f, 470f, 26f * v2, th.bgBottom, 1f) }
    }

    private val direct = beat("direct", "Passer par le site officiel", setOf(Role.ADVICE), setOf(Goal.APPRENDRE_REFLEXE),
        cueSet(Act.NAVIGUER_DIRECT, Thing.LIEN, Act.CLIQUER)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        // À gauche : le lien du message, barré.
        val a = SK.ease(t, 150f, 500f)
        g.withPose(Pose(alpha = a), 150f, 350f) {
            SK.phone(g, 150f, 360f, 220f, 420f, th)
            g.fillRoundRect(60f, 310f, 240f, 380f, 12f, th.panelHi)
            SK.text(g, "lien du", 78f, 338f, 20f, th.muted, 500)
            SK.text(g, "message", 78f, 366f, 20f, th.muted, 500)
        }
        val cr = SK.ease(t, 900f, 500f)
        if (cr > 0f) { g.line(50f, 345f, 50f + 200f * cr, 345f + 0f, 8f, th.bad); SK.cross(g, 150f, 345f, 70f * cr, th.bad, 1f, 12f) }
        // À droite : le navigateur où l'on tape soi-même l'adresse.
        val bp = SK.ease(t, 600f, 700f, Easing.EXPO_OUT)
        g.withPose(Pose(alpha = bp, dx = (1f - bp) * 80f), 650f, 350f) {
            val typed = SK.ease(t, 1700f, 2200f, Easing.LINEAR)
            val done = typed >= 1f
            val top = SK.browser(g, 330f, 150f, 960f, 600f, th, "banque.example", typed, secure = if (done) true else null)
            val load = SK.ease(t, 4200f, 900f)
            if (load > 0f) {
                g.fillRoundRect(366f, top + 20f, 366f + 560f * load, top + 74f, 12f, th.good.withAlpha(0.8f))
                g.fillRoundRect(366f, top + 100f, 926f, top + 150f, 12f, th.panelHi.withAlpha(load))
                g.fillRoundRect(366f, top + 170f, 800f, top + 220f, 12f, th.panelHi.withAlpha(load))
                g.fillRoundRect(366f, top + 240f, 700f, top + 290f, 12f, th.panelHi.withAlpha(load))
            }
        }
        SK.pill(g, "TU TAPES L'ADRESSE TOI-MÊME", 640f, 650f, th.ink, th.bgTop.withAlpha(0.8f), 26f, SK.ease(t, 2400f, 500f))
    }

    private val recover = beat("recover", "Réagir après un clic", setOf(Role.ADVICE), setOf(Goal.APPRENDRE_REFLEXE),
        cueSet(Act.CHANGER, Act.SIGNALER, Thing.MOT_DE_PASSE, Actor.BANQUE)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 500f, 330f, 430f, th.good, 0.10f)
        val pulse = 0.5f + 0.5f * sin(t / 600f)
        SK.shield(g, 500f, 190f, 120f * SK.pop(t, 100f, 700f).coerceAtMost(1.2f), th.good.withAlpha(0.15f + 0.10f * pulse), th.good, SK.ease(t, 4800f, 700f))
        val steps = listOf("CHANGER LE MOT DE PASSE", "PRÉVENIR LA BANQUE", "SIGNALER LE MESSAGE")
        for (k in 0 until 3) {
            val cx = 190f + k * 310f; val cy = 480f
            val s = 1000f + k * 1300f
            val p = SK.pop(t, s, 600f)
            if (p <= 0f) continue
            g.withPose(Pose(alpha = clamp01(p), scale = 0.8f + 0.2f * clamp01(p), dy = (1f - clamp01(p)) * 30f), cx, cy) {
                g.fillRoundRect(cx - 145f, cy - 120f, cx + 145f, cy + 120f, 24f, th.panel)
                g.strokeRoundRect(cx - 145f, cy - 120f, cx + 145f, cy + 120f, 24f, 3f, th.accent.withAlpha(0.7f))
                when (k) {
                    0 -> { val rot = SK.ease(t, s + 300f, 900f) * 90f; g.save(); g.rotate(rot, cx, cy - 25f); Icons.draw(g, IconId.KEY, cx, cy - 25f, 86f, th.accent, 2.2f); g.restore() }
                    1 -> { // bâtiment de banque : fronton + colonnes
                        val roof = PathData().moveTo(cx - 52f, cy - 40f).lineTo(cx, cy - 78f).lineTo(cx + 52f, cy - 40f).close(); g.fillPath(roof, th.accent)
                        for (i in 0 until 4) g.fillRect(cx - 44f + i * 29f, cy - 32f, cx - 32f + i * 29f, cy + 16f, th.accent)
                        g.fillRect(cx - 56f, cy + 22f, cx + 56f, cy + 34f, th.accent)
                    }
                    else -> Icons.draw(g, IconId.MEGAPHONE, cx, cy - 25f, 86f, th.accent, 2.2f)
                }
                SK.text(g, steps[k], cx, cy + 86f, 21f, th.ink, center = true)
                val ck = SK.ease(t, s + 1000f, 400f)
                if (ck > 0f) { g.fillCircle(cx + 120f, cy - 100f, 24f * ck, th.good); SK.check(g, cx + 120f, cy - 100f, 14f * ck, th.bgBottom) }
            }
        }
    }

    /** Plan final commun : un message piégé arrêté par un bouclier. */
    private val recap = beat("recap", "À retenir : vérifier avant de cliquer", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Act.VERIFIER, Act.CLIQUER, Thing.LIEN)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        recapShield(c, th.good) { x, y, a ->
            SK.phone(g, x, y, 170f, 320f, th)
            SK.envelope(g, x, y - 20f, 90f, th.panelHi, th.warn, a)
        }
    }

    internal fun recapShield(c: StageCtx, color: Int, left: (Float, Float, Float) -> Unit) {
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 600f, 350f, 420f, color, 0.14f + 0.04f * sin(t / 500f))
        val a = SK.ease(t, 100f, 700f, Easing.EXPO_OUT)
        left(210f, 350f, a)
        val u = SK.loop(t, 2400f)
        val ix = lerp(320f, 470f, u)
        val pk = clamp01(u / 0.12f) * (1f - clamp01((u - 0.8f) / 0.2f))
        SK.packet(g, ix, 350f, 12f, th.bad, pk * a)
        val s = SK.pop(t, 400f, 800f).coerceAtMost(1.15f)
        g.save(); g.scale(s, 640f, 350f)
        SK.shield(g, 640f, 350f, 190f, color.withAlpha(0.22f), color, SK.ease(t, 1200f, 800f))
        g.restore()
        if (u > 0.74f && u < 1f) SK.burst(g, 505f, 350f, 40f, th.bad, (u - 0.74f) / 0.26f)
        for (k in 0 until 6) {
            val ang = t / 1800f + k * PI.toFloat() / 3f
            SK.spark(g, 640f + cos(ang) * 260f, 350f + sin(ang) * 230f, 11f + 4f * sin(t / 300f + k), color.withAlpha(0.7f))
        }
    }

    /** Le courriel frauduleux : expéditeur qui imite une banque, urgence, lien qui ne mène pas où il prétend, puis les trois vérifications. */
    private val mail = beat("mail", "Courriel frauduleux, lien suspect, protections", setOf(Role.INTRO, Role.DEFINE, Role.RECOGNIZE), setOf(Goal.IDENTIFIER_MENACE, Goal.APPRENDRE_REFLEXE),
        cueSet(Actor.ATTAQUANT, Thing.EMAIL, Thing.LIEN), minMs = 5000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "EXEMPLE FICTIF  -  ADRESSES DE DOCUMENTATION", 72f, 24f, th.accent.withAlpha(0.85f))
        val a = SK.ease(t, 0f, 600f, Easing.EXPO_OUT)
        g.withPose(Pose(alpha = a, dy = (1f - a) * 30f), 330f, 330f) {
            g.fillRoundRect(40f, 60f, 650f, 640f, 24f, th.panel)
            g.strokeRoundRect(40f, 60f, 650f, 640f, 24f, 3f, th.line)
            g.fillRoundRect(40f, 60f, 650f, 120f, 24f, th.panelHi)
            for (k in 0 until 3) g.fillCircle(78f + k * 26f, 90f, 7f, th.line)
            SK.text(g, "BOITE DE RECEPTION", 190f, 98f, 20f, th.muted)
            val f = SK.font(22f, 500)
            val mf = SK.font(21f, 500, FontFamily.MONO)
            g.drawText("De :", 66f, 170f, f, th.muted)
            g.drawText("service@banque-verif.example", 130f, 170f, mf, th.ink)
            g.drawText("Objet :", 66f, 214f, f, th.muted)
            g.drawText("Action requise sous 24 h", 150f, 214f, f, th.ink)
            g.line(66f, 236f, 624f, 236f, 2f, th.line)
            g.drawText("Votre compte sera suspendu.", 66f, 290f, f, th.ink)
            g.drawText("Confirmez vos informations :", 66f, 330f, f, th.ink)
            // bouton-lien
            g.fillRoundRect(66f, 370f, 380f, 440f, 18f, th.accent)
            SK.text(g, "VERIFIER MAINTENANT", 223f, 414f, 24f, th.bgBottom, center = true)
            // 1 : expéditeur
            val s1 = SK.ease(t, 1400f, 400f)
            if (s1 > 0.01f) { g.strokeRoundRect(120f, 146f, 500f, 182f, 10f, 4f, th.bad.withAlpha(s1)); SK.pill(g, "EXPEDITEUR DOUTEUX", 560f, 198f, th.bgBottom, th.bad, 17f, s1) }
            // 2 : lien
            val s2 = SK.ease(t, 3000f, 400f)
            if (s2 > 0.01f) {
                SK.cursor(g, 300f, 410f, 20f, th.ink, th.bgBottom)
                g.fillRoundRect(66f, 460f, 624f, 520f, 12f, th.bgBottom.withAlpha(0.95f * s2))
                g.drawText("http://bnq-verif.example/ok", 82f, 498f, mf, th.bad.withAlpha(s2))
                SK.pill(g, "LE LIEN NE CORRESPOND PAS", 420f, 548f, th.bgBottom, th.bad, 17f, s2)
            }
        }
        // les trois protections
        val items = listOf("VERIFIER L'EXPEDITEUR", "SURVOLER LE LIEN SANS CLIQUER", "PASSER PAR LE SITE OFFICIEL")
        for ((i, s) in items.withIndex()) {
            val p = SK.pop(t, 4200f + i * 900f, 500f)
            if (p <= 0f) continue
            val y = 170f + i * 150f
            g.withPose(Pose(alpha = clamp01(p), dx = (1f - clamp01(p)) * 60f), 800f, y) {
                g.fillRoundRect(676f, y - 52f, 980f, y + 52f, 22f, th.panel)
                g.strokeRoundRect(676f, y - 52f, 980f, y + 52f, 22f, 3f, th.good.withAlpha(0.9f))
                SK.shield(g, 722f, y, 26f, th.good.withAlpha(0.25f), th.good, 1f)
                val fm = SK.font(19f, 700)
                val words = s.split(' ')
                val half = (words.size + 1) / 2
                g.drawText(words.take(half).joinToString(" "), 764f, y - 4f, fm, th.ink)
                g.drawText(words.drop(half).joinToString(" "), 764f, y + 24f, fm, th.ink)
            }
        }
    }

    val kind = SceneKind("phishing", "Hameçonnage", setOf(Concept.PHISHING), listOf(bait, mail, anatomy, click, verify, direct, recover, recap))
}
