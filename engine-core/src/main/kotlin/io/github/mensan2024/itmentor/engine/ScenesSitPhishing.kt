// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase C — hameçonnage en SITUATION : une personne, un bureau, un courriel frauduleux, un lien suspect, un appel de vérification.
 * Tous les messages, adresses et sites dessinés sont des EXEMPLES FICTIFS (domaine « .example », réservé par la norme RFC 2606).
 */
internal object SitKit {
    /** Rôles où un plan de situation est admis : c'est le sujet précis (indices obligatoires) qui le désigne, pas la place dans le récit. */
    val BODY = setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.PROCEDURE, Role.ADVICE, Role.RECOGNIZE)

    /** Pièce de bureau aux couleurs du thème : mur, sol, plante. Retourne l'ordonnée du sol. */
    fun office(c: StageCtx, floorY: Float = 585f): Float {
        val th = c.th
        val f = Props.room(c.g, mix(th.bgBottom, th.accent, 0.16f), mix(th.bgTop, th.bgBottom, 0.35f), floorY)
        Props.plant(c.g, 52f, f, 150f)
        return f
    }

    fun fiction(c: StageCtx) = SK.caption(c.g, "SIMULATION  -  EXEMPLE FICTIF", 72f, 26f, c.th.accent.withAlpha(0.85f))

    /** Personne assise derrière un bureau avec un portable vu de dos. Retourne les ancres de la personne. */
    fun worker(c: StageCtx, x: Float, floor: Float, h: Float, who: Int, face: Face, body: Body, deskW: Float = 400f): Anchors {
        val a = Cast.draw(c.g, x, floor, h, Cast.persona(who), face, body.copy(seated = true), c.t, who)
        val deskTop = floor - 0.30f * h + 0.035f * h
        Props.laptopBack(c.g, x, deskTop, deskW * 0.34f)
        Props.desk(c.g, x, deskTop, floor, deskW)
        return a
    }
}

internal object SitPhishing {
    private const val FAKE_FROM = "service@banque-verif.example"
    private const val FAKE_URL = "http://banque-verif.example/ok"

    private fun mail(c: StageCtx, l: Float, t: Float, r: Float, b: Float, link: Float = 0f, showClock: Boolean = true) {
        val g = c.g; val th = c.th
        val body = Props.mailWindow(g, l, t, r, b, th, FAKE_FROM, "URGENT : compte suspendu", fromColor = th.bad)
        val x = body[0]
        SK.text(g, "Cher client,", x, body[1] + 18f, 26f, th.muted, 600)
        SK.text(g, "Votre compte sera suspendu sous 24 h.", x, body[1] + 58f, 26f, th.ink, 600)
        SK.text(g, "Confirmez vos informations :", x, body[1] + 98f, 26f, th.ink, 600)
        val by = body[1] + 128f
        g.fillRoundRect(x, by, x + 350f, by + 62f, 14f, th.bad)
        if (link > 0.01f) g.strokeRoundRect(x - 6f, by - 6f, x + 356f, by + 68f, 18f, 4f, th.warn.withAlpha(link))
        SK.text(g, "CONFIRMER MAINTENANT", x + 175f, by + 40f, 24f, rgb(0xFFFFFF), center = true)
        SK.text(g, FAKE_URL, x, by + 98f, 21f, th.bad, 500, family = FontFamily.MONO)
        if (showClock) {
            Props.clock(g, r - 60f, b - 62f, 30f, th, c.t)
            SK.text(g, "24 h", r - 60f, b - 10f, 20f, th.warn, center = true)
        }
    }

    private val inbox = beat("inbox", "Une personne reçoit un courriel urgent", SitKit.BODY + setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM), setOf(Goal.IDENTIFIER_MENACE, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.EMAIL, Thing.MESSAGE, Act.ENVOYER, Actor.ATTAQUANT, Actor.UTILISATEUR) + setOf("topic:inbox", "topic:urgency"), minMs = 3500, needs = setOf("topic:inbox")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c)
        SitKit.fiction(c)
        val pop = SK.pop(t, 900f, 700f)
        val face = Face.mix(Face.mix(Face.FOCUSED, Face.WORRIED, SK.ease(t, 1500f, 500f)), Face.SUSPICIOUS, SK.ease(t, 3200f, 600f))
        val look = Props.gaze(face, 240f, 300f, 700f, 280f)
        val body = Body.mix(Poses.typing(t), Poses.hand2Chin(t).copy(seated = true), SK.ease(t, 2800f, 500f))
        val a = SitKit.worker(c, 235f, floor, 440f, 0, look, body, 380f)
        // L'enveloppe de notification sort du portable avant que la fenêtre s'ouvre.
        if (t in 200f..1100f) { val u = window(t, 200f, 900f); SK.envelope(g, 235f, 450f - u * 120f, 60f, th.panelHi, th.warn, 1f - clamp01((u - 0.8f) * 5f)) }
        g.withPose(Pose(alpha = pop, scale = 0.7f + 0.3f * pop), 710f, 300f) {
            SK.glow(g, 710f, 300f, 360f, th.bad, 0.12f)
            mail(c, 460f, 70f, 960f, 560f, link = if (t > 2500f) 0.5f + 0.5f * sin(t / 220f) else 0f)
        }
        Props.notifBadge(g, 340f, 240f, 1, SK.pop(t, 1000f, 300f) * (1f - SK.ease(t, 3000f, 300f)), th.bad)
        if (a.hr < 0f) return@beat
    }

    private val clues = beat("clues", "Les indices qui trahissent le message", SitKit.BODY + setOf(Role.RECOGNIZE, Role.DEFINE, Role.MECHANISM), setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Thing.EMAIL, Thing.LIEN, Act.VERIFIER) + setOf("topic:sender", "topic:urgency", "topic:link", "topic:inbox"), minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c)
        SitKit.fiction(c)
        mail(c, 60f, 90f, 560f, 570f, showClock = false)
        // Trois repères s'allument l'un après l'autre : expéditeur, urgence, lien.
        val marks = listOf(Triple(floatArrayOf(84f, 176f, 540f, 214f), "1  EXPEDITEUR", 1100f), Triple(floatArrayOf(84f, 296f, 540f, 338f), "2  URGENCE", 2300f), Triple(floatArrayOf(84f, 460f, 540f, 500f), "3  LIEN", 3500f))
        for ((k, m) in marks.withIndex()) {
            val p = SK.ease(t, m.third, 450f)
            if (p <= 0.01f) continue
            val r = m.first
            g.strokeRoundRect(r[0], r[1], r[2], r[3], 12f, 4f, th.warn.withAlpha(p * (0.65f + 0.35f * sin(t / 240f))))
            SK.arrow(g, 566f, (r[1] + r[3]) / 2f, 616f, 140f + k * 150f, 3f, th.warn.withAlpha(p), 12f)
            SK.pill(g, m.second, 725f, 140f + k * 150f, th.warn, th.bgTop.withAlpha(0.85f), 24f, p)
        }
        val look = Props.gaze(Face.mix(Face.FOCUSED, Face.SUSPICIOUS, SK.ease(t, 1000f, 600f)), 850f, 330f, 300f, 330f)
        Cast.draw(g, 880f, floor, 360f, Cast.persona(5), look, Poses.holdFront(t), t, 5, mirror = true)
        Props.magnifier(g, 800f + 8f * sin(t / 600f), 410f + 6f * cos(t / 700f), 34f, th.accent)
    }

    private val hover = beat("hover", "Survoler le lien avant de cliquer", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.RECOGNIZE, Role.MECHANISM), setOf(Goal.APPRENDRE_REFLEXE, Goal.SUIVRE_PROCEDURE),
        cueSet(Thing.LIEN, Act.VERIFIER, Act.CLIQUER, Thing.EMAIL) + setOf("topic:link", "topic:sender"), minMs = 4000, needs = setOf("topic:link")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c)
        SitKit.fiction(c)
        // Un navigateur : le texte du lien dit une chose, l'adresse réelle en dit une autre.
        val l = 40f; val r = 650f; val top = 80f; val b = 400f
        g.fillRoundRect(l, top, r, b, 20f, th.panel); g.strokeRoundRect(l, top, r, b, 20f, 3f, th.line)
        g.fillRoundRect(l, top, r, top + 56f, 20f, th.panelHi); g.fillRect(l, top + 36f, r, top + 56f, th.panelHi)
        SK.text(g, "Mon espace client - Banque", l + 30f, top + 130f, 32f, th.ink)
        val lx = l + 30f; val ly = top + 175f
        SK.text(g, "banque.example/mon-compte", lx, ly + 28f, 30f, th.accent, 700)
        g.line(lx, ly + 36f, lx + 400f, ly + 36f, 3f, th.accent)
        val cp = SK.ease(t, 800f, 1400f)
        SK.cursor(g, lerp(560f, lx + 190f, cp), lerp(380f, ly + 14f, cp), 26f, rgb(0xFFFFFF), rgb(0x111111))
        val tip = SK.ease(t, 2400f, 400f)
        if (tip > 0.01f) {
            val tx = lx + 40f; val ty = ly + 70f
            g.withPose(Pose(alpha = tip, dy = (1f - tip) * 10f), tx, ty) {
                g.fillRoundRect(tx, ty, tx + 530f, ty + 52f, 12f, rgb(0x22262F)); g.strokeRoundRect(tx, ty, tx + 530f, ty + 52f, 12f, 3f, th.bad)
                SK.text(g, FAKE_URL, tx + 18f, ty + 35f, 24f, th.bad, 600, family = FontFamily.MONO)
            }
        }
        val look = Props.gaze(Face.mix(Face.FOCUSED, Face.SUSPICIOUS, SK.ease(t, 2400f, 500f)), 840f, 330f, 300f, 250f)
        Cast.draw(g, 830f, floor, 370f, Cast.persona(3), look, Body.mix(Poses.holdFront(t), Body(armL = 80f to 84f + 4f * sin(t / 500f), armR = 6f to 4f), SK.ease(t, 2000f, 500f)), t, 3, mirror = true)
        SK.pill(g, "ADRESSE REELLE  <>  TEXTE AFFICHE", 345f, 470f, th.warn, th.bgTop.withAlpha(0.85f), 24f, SK.ease(t, 2800f, 400f))
    }

    private val callback = beat("callback", "Appeler par le numéro officiel", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE), setOf(Goal.APPRENDRE_REFLEXE, Goal.SUIVRE_PROCEDURE),
        cueSet(Act.NAVIGUER_DIRECT, Act.VERIFIER, Actor.BANQUE, Thing.COMPTE) + setOf("topic:callback", "topic:inbox"), minMs = 4000, needs = setOf("topic:callback")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c)
        SitKit.fiction(c)
        // La personne laisse le message de côté et appelle la banque avec le numéro de sa carte, pas celui du message.
        val tCall = SK.ease(t, 1200f, 600f)
        val face = Face.mix(Face.SUSPICIOUS, Face.RELIEVED, SK.ease(t, 3200f, 700f))
        val an = Cast.draw(g, 330f, floor, 400f, Cast.persona(1), face, Body.mix(Poses.hand2Chin(t), Poses.phoneAtEar(t), tCall), t, 1)
        if (tCall > 0.6f) g.fillRoundRect(an.rx - 11f, an.ry - 34f, an.rx + 11f, an.ry + 30f, 6f, rgb(0x20242E))
        // Message douteux, barré et rangé.
        val out = SK.ease(t, 600f, 700f)
        g.withPose(Pose(alpha = 1f - 0.5f * out, dx = -0f, dy = out * 10f, scale = 1f - 0.35f * out), 760f, 170f) {
            mail(c, 560f, 60f, 960f, 380f, showClock = false)
            if (out > 0.5f) { g.line(570f, 70f, 950f, 370f, 7f, th.bad.withAlpha(0.9f)); g.line(950f, 70f, 570f, 370f, 7f, th.bad.withAlpha(0.9f)) }
        }
        // Carte bancaire (générique) d'où vient le bon numéro.
        val cp = SK.ease(t, 1400f, 600f)
        g.withPose(Pose(alpha = cp, scale = 0.8f + 0.2f * cp), 760f, 500f) {
            g.fillRoundRect(620f, 440f, 900f, 570f, 18f, mix(th.accent, th.bgBottom, 0.4f))
            g.fillRect(620f, 470f, 900f, 496f, rgb(0x14202E))
            g.fillRoundRect(640f, 520f, 740f, 536f, 6f, Color.white(0.6f)); g.fillRoundRect(640f, 546f, 710f, 558f, 6f, Color.white(0.4f))
        }
        SK.pill(g, "NUMERO AU DOS DE LA CARTE", 760f, 600f, th.good, th.bgTop.withAlpha(0.85f), 22f, cp)
        // Ondes d'appel
        if (tCall > 0.8f) for (k in 0..2) { val u = SK.loop(t, 1500f, k * 500f); g.strokeArc(an.rx + 22f, an.ry - 4f, 24f + u * 60f, -50f, 100f, 5f, th.good.withAlpha(1f - u)) }
        if (SK.ease(t, 3300f, 400f) > 0.5f) SK.check(g, 520f, 350f, 44f, th.good)
    }

    private val report = beat("report", "Signaler puis supprimer", SitKit.BODY + setOf(Role.ADVICE, Role.RECAP, Role.PROCEDURE, Role.MECHANISM), setOf(Goal.APPRENDRE_REFLEXE, Goal.MEMORISER),
        cueSet(Act.SIGNALER, Act.CHANGER, Thing.EMAIL, Thing.MESSAGE) + setOf("topic:report", "topic:inbox"), minMs = 4000, needs = setOf("topic:report")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c)
        SitKit.fiction(c)
        val face = Face.mix(Face.WORRIED, Face.SMILE, SK.ease(t, 2600f, 600f))
        val body = Body.mix(Poses.typing(t), Poses.holdFront(t).copy(seated = true), SK.ease(t, 2400f, 500f))
        SitKit.worker(c, 235f, floor, 440f, 4, Props.gaze(face, 235f, 300f, 690f, 330f), body, 380f)
        // Le courriel part vers le service de signalement, puis la corbeille.
        val p1 = SK.ease(t, 700f, 1300f); val p2 = SK.ease(t, 2500f, 1100f)
        val mx = lerp(650f, 830f, p1); val my = lerp(300f, 210f, p1)
        if (p2 < 0.98f) {
            val fx = if (p2 > 0f) lerp(mx, 830f, p2) else mx; val fy = if (p2 > 0f) lerp(my, 480f, p2) else my
            SK.envelope(g, fx, fy, 120f * (1f - 0.4f * p2), th.panelHi, th.bad, 1f)
        }
        // Service de signalement (boîte aux lettres) et corbeille.
        g.fillRoundRect(740f, 120f, 930f, 250f, 16f, th.panelHi); g.strokeRoundRect(740f, 120f, 930f, 250f, 16f, 3f, th.good)
        SK.shield(g, 835f, 175f, 36f, th.good.withAlpha(0.25f), th.good, SK.ease(t, 1800f, 500f))
        SK.pill(g, "SIGNALER", 835f, 290f, th.good, th.bgTop.withAlpha(0.85f), 22f)
        g.fillRoundRect(770f, 440f, 890f, 560f, 12f, th.panelHi); g.strokeRoundRect(770f, 440f, 890f, 560f, 12f, 3f, th.warn)
        g.fillRoundRect(756f, 424f, 904f, 446f, 8f, th.warn.withAlpha(0.8f))
        for (k in 0..2) g.line(800f + k * 30f, 462f, 800f + k * 30f, 540f, 4f, th.warn.withAlpha(0.6f))
        SK.pill(g, "SUPPRIMER", 830f, 600f, th.warn, th.bgTop.withAlpha(0.85f), 22f)
    }

    private val victim = beat("victim", "Le mot de passe saisi part chez l'attaquant", SitKit.BODY + setOf(Role.MECHANISM, Role.DEFINE, Role.PROCEDURE, Role.RECOGNIZE), setOf(Goal.COMPRENDRE_MECANISME, Goal.IDENTIFIER_MENACE),
        cueSet(Act.SAISIR, Act.CLIQUER, Thing.MOT_DE_PASSE, Actor.ATTAQUANT) + setOf("topic:login", "topic:link"), minMs = 4000, needs = setOf("topic:login")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c)
        SitKit.fiction(c)
        // À gauche, la personne remplit un faux formulaire ; à droite, un attaquant reçoit ce qui est saisi.
        val typed = SK.ease(t, 900f, 2200f)
        val face = Face.mix(Face.FOCUSED, Face.SURPRISED, SK.ease(t, 3600f, 400f))
        val a = SitKit.worker(c, 160f, floor, 400f, 2, Props.gaze(face, 160f, 300f, 470f, 280f), Poses.typing(t), 300f)
        val l = 330f; val top = 100f; val r = 650f; val b = 470f
        g.fillRoundRect(l, top, r, b, 18f, th.panel); g.strokeRoundRect(l, top, r, b, 18f, 3f, th.bad)
        g.fillRoundRect(l, top, r, top + 52f, 18f, th.panelHi); g.fillRect(l, top + 34f, r, top + 52f, th.panelHi)
        SK.text(g, "banque-verif.example", l + 20f, top + 36f, 20f, th.bad, 600, family = FontFamily.MONO)
        SK.text(g, "Identifiant", l + 30f, top + 110f, 22f, th.muted, 600)
        g.fillRoundRect(l + 30f, top + 124f, r - 30f, top + 170f, 10f, th.bgBottom)
        SK.text(g, "Mot de passe", l + 30f, top + 214f, 22f, th.muted, 600)
        g.fillRoundRect(l + 30f, top + 228f, r - 30f, top + 274f, 10f, th.bgBottom)
        val dots = (typed * 9f).toInt()
        for (k in 0 until dots) g.fillCircle(l + 52f + k * 26f, top + 251f, 8f, th.ink)
        // Les caractères voyagent vers l'attaquant.
        for (k in 0 until 4) {
            val u = SK.loop(t - 1400f, 1600f, k * 400f)
            if (t < 1400f + k * 400f) continue
            val x = lerp(r - 20f, 800f, u); val y = lerp(top + 251f, 330f, u) - sin(u * PI.toFloat()) * 70f
            SK.packet(g, x, y, 12f, th.bad, clamp01(u / 0.1f) * clamp01((1f - u) / 0.1f))
        }
        SK.hood(g, 840f, 330f, 76f, rgb(0x1B2233), th.bad)
        SK.laptop(g, 840f, 520f, 190f, th, screen = th.bad.withAlpha(0.35f))
        SK.pill(g, "ATTAQUANT", 840f, 625f, th.bad, th.bgTop.withAlpha(0.85f), 24f, SK.pop(t, 500f))
        if (a.hr < 0f) return@beat
    }


    private fun along(pts: List<Pair<Float, Float>>, u: Float): Pair<Float, Float> {
        val seg = pts.size - 1; val p = u.coerceIn(0f, 0.9999f) * seg; val i = p.toInt(); val f = p - i
        return lerp(pts[i].first, pts[i + 1].first, f) to lerp(pts[i].second, pts[i + 1].second, f)
    }

    private val chain = beat("chain", "L'attaque de bout en bout : du fraudeur au compte volé", SitKit.BODY + setOf(Role.MECHANISM, Role.PROCEDURE), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE, Goal.IDENTIFIER_MENACE),
        cueSet(Actor.ATTAQUANT, Thing.EMAIL, Thing.LIEN, Thing.MOT_DE_PASSE, Act.ENVOYER) + setOf("topic:chain"), minMs = 8000, needs = setOf("topic:chain")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c, 600f)
        SitKit.fiction(c)
        val xs = listOf(100f, 300f, 500f, 705f, 900f); val y = 300f
        val stepMs = ((c.scene.durationMs - 1200f) / 5f).coerceIn(1200f, 3200f)
        fun lit(i: Int) = SK.ease(t, 500f + i * stepMs, 400f)
        for (i in 0..3) g.line(xs[i] + 40f, y + 90f, xs[i + 1] - 40f, y + 90f, 6f, th.line.withAlpha(0.7f))
        // 1 le fraudeur
        g.withPose(Pose(alpha = SK.pop(t, 200f), scale = 1f), xs[0], y) {
            SK.hood(g, xs[0], y - 40f, 58f, rgb(0x1B2233), th.bad); SK.laptop(g, xs[0], y + 150f, 150f, th, screen = th.bad.withAlpha(0.35f))
        }
        // 2 le faux message
        val p2 = SK.pop(t, 500f + stepMs)
        g.withPose(Pose(alpha = p2, scale = 0.8f + 0.2f * p2), xs[1], y) { SK.envelope(g, xs[1], y + 70f, 150f, th.bad, th.bgTop); SK.text(g, "!", xs[1] + 62f, y + 20f, 46f, th.warn, center = true) }
        // 3 la victime
        val p3 = SK.pop(t, 500f + 2 * stepMs)
        g.withPose(Pose(alpha = p3, scale = 0.85f + 0.15f * p3), xs[2], y) {
            Cast.draw(g, xs[2], y + 250f, 330f, Cast.persona(1), Face.mix(Face.NEUTRAL, Face.WORRIED, lit(3)), Poses.holdFront(t), t, 1)
        }
        // 4 la fausse page
        val p4 = SK.pop(t, 500f + 3 * stepMs)
        g.withPose(Pose(alpha = p4, scale = 0.85f + 0.15f * p4), xs[3], y) {
            g.fillRoundRect(xs[3] - 92f, y - 70f, xs[3] + 92f, y + 150f, 14f, th.panel); g.strokeRoundRect(xs[3] - 92f, y - 70f, xs[3] + 92f, y + 150f, 14f, 4f, th.bad)
            SK.text(g, "banque-verif.example", xs[3], y - 40f, 15f, th.bad, 600, center = true, family = FontFamily.MONO)
            g.fillRoundRect(xs[3] - 68f, y - 14f, xs[3] + 68f, y + 20f, 7f, th.bgBottom); g.fillRoundRect(xs[3] - 68f, y + 40f, xs[3] + 68f, y + 74f, 7f, th.bgBottom)
            val dots = (lit(3) * 8f).toInt(); for (k in 0 until dots) g.fillCircle(xs[3] - 56f + k * 16f, y + 57f, 5f, th.ink)
            g.fillRoundRect(xs[3] - 68f, y + 92f, xs[3] + 68f, y + 126f, 7f, th.bad)
        }
        // 5 le compte volé
        val p5 = SK.pop(t, 500f + 4 * stepMs)
        g.withPose(Pose(alpha = p5, scale = 0.85f + 0.15f * p5), xs[4], y) {
            g.fillRoundRect(xs[4] - 80f, y - 40f, xs[4] + 80f, y + 150f, 18f, th.panel); g.strokeRoundRect(xs[4] - 80f, y - 40f, xs[4] + 80f, y + 150f, 18f, 4f, mix(th.line, th.bad, lit(4)))
            SK.lock(g, xs[4], y + 70f, 66f, th.panelHi, mix(th.good, th.bad, lit(4)), open = lit(4), shake = 0f)
        }
        // L'information voyage : le message vers la victime, puis les identifiants jusqu'au compte volé.
        val u = clamp01((t - 500f) / (stepMs * 4f))
        if (t > 500f) { val (px, py) = along(xs.map { it to y + 90f }, u); SK.packet(g, px, py, 15f, if (u < 0.5f) th.warn else th.bad) }
        val names = listOf("FRAUDEUR", "FAUX MESSAGE", "VICTIME", "FAUSSE PAGE", "COMPTE")
        for (i in 0..4) SK.pill(g, names[i], xs[i], 520f, th.muted, th.bgTop.withAlpha(0.85f), 20f, SK.pop(t, 300f + i * stepMs))
        val labels = listOf("1  CHOISIT UNE MARQUE", "2  ENVOIE LE MESSAGE", "3  LA VICTIME CLIQUE", "4  SAISIT SES IDENTIFIANTS", "5  COMPTE VOLÉ")
        val cur = (((t - 500f) / stepMs).toInt()).coerceIn(0, 4)
        SK.pill(g, labels[cur], 500f, 600f, th.ink, th.bgTop.withAlpha(0.9f), 28f)
    }

    private val damage = beat("damage", "Ce que coûte un compte volé", SitKit.BODY + setOf(Role.MECHANISM, Role.RECAP), setOf(Goal.IDENTIFIER_MENACE, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.ATTAQUANT, Thing.COMPTE) + setOf("topic:damage"), minMs = 6000, needs = setOf("topic:damage")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c, 600f)
        SitKit.fiction(c)
        Cast.draw(g, 150f, floor + 20f, 380f, Cast.persona(1), Face.mix(Face.WORRIED, Face.ALARMED, SK.ease(t, 1800f, 500f)), Poses.holdFront(t), t, 1)
        // Le compte : un cadenas qui s'ouvre.
        val open = SK.ease(t, 700f, 600f)
        g.fillRoundRect(320f, 200f, 520f, 420f, 22f, th.panel); g.strokeRoundRect(320f, 200f, 520f, 420f, 22f, 4f, mix(th.line, th.bad, open))
        SK.lock(g, 420f, 300f, 70f, th.panelHi, mix(th.good, th.bad, open), open = open)
        SK.pill(g, "COMPTE", 420f, 450f, th.ink, th.bgTop.withAlpha(0.9f), 24f)
        // Trois conséquences qui partent du compte, une à la fois.
        val rows = listOf(120f to "DONNÉES VOLÉES", 285f to "VIREMENTS FRAUDULEUX", 470f to "CONTACTS PIÉGÉS")
        for ((i, rw) in rows.withIndex()) {
            val ts = 1600f + i * 1700f; val p = SK.ease(t, ts, 500f)
            val y = rw.first
            g.line(520f, 310f, 640f, y, 4f, th.bad.withAlpha(0.5f * p))
            g.withPose(Pose(alpha = p, scale = 0.8f + 0.2f * p), 740f, y) {
                when (i) {
                    0 -> { g.fillRoundRect(650f, y - 36f, 770f, y + 36f, 10f, th.panel); for (k in 0..2) g.line(662f, y - 18f + k * 18f, 758f - k * 14f, y - 18f + k * 18f, 3f, th.muted); SK.hood(g, 850f, y - 6f, 30f, rgb(0x1B2233), th.bad) }
                    1 -> { for (k in 0..2) g.fillCircle(670f + k * 34f, y + 8f - 10f * sin(t / 300f + k), 15f, th.warn); SK.hood(g, 850f, y - 6f, 30f, rgb(0x1B2233), th.bad) }
                    else -> { for (k in 0..2) { Cast.draw(g, 690f + k * 90f, y + 80f, 120f, Cast.persona(2 + k), Face.SURPRISED, Poses.idle(t), t, 2 + k); SK.envelope(g, 690f + k * 90f, y - 75f, 44f, th.bad, th.bgTop) } }
                }
            }
            SK.pill(g, rw.second, 740f, if (i == 2) y + 108f else y + 64f, th.bad, th.bgTop.withAlpha(0.9f), 20f, p)
        }
    }

    /** Affiche courte (cartes d'ouverture) : une personne, un message, un doute. */
    private val poster = beat("poster", "Affiche : un message qui fait douter", setOf(Role.INTRO), setOf(Goal.MEMORISER), emptySet(), repeatable = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c); SitKit.fiction(c)
        Cast.draw(g, 330f, floor, 430f, Cast.persona(5), Props.gaze(Face.mix(Face.FOCUSED, Face.SUSPICIOUS, SK.ease(t, 500f, 600f)), 330f, 300f, 700f, 250f), Poses.holdFront(t), t, 5)
        val p = SK.pop(t, 300f, 600f)
        g.withPose(Pose(alpha = p, scale = 0.6f + 0.4f * p), 700f, 290f) { SK.glow(g, 700f, 290f, 280f, th.bad, 0.14f); SK.envelope(g, 700f, 290f, 330f, th.panelHi, th.warn); Props.notifBadge(g, 840f, 190f, 1, 1f, th.bad); SK.text(g, "?", 700f, 330f, 120f, th.bad, 800, center = true) }
    }

    val kind = SceneKind("sit-phishing", "Hameçonnage en situation", setOf(Concept.PHISHING), listOf(inbox, clues, hover, callback, report, victim, chain, damage, poster), fallbackFor = setOf(Concept.MALWARE))
}
