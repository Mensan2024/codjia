// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.sin

/**
 * Phase D.2 — plans d'hameçonnage dédiés à UN mécanisme chacun (variantes, en-têtes d'authentification, adresse saisie à la main,
 * double authentification, passkey). Chaque plan n'est admis que si le texte de la scène parle de ce mécanisme (indice obligatoire « phish-… »).
 * Tous les noms de domaine et adresses dessinés sont des EXEMPLES FICTIFS (« .example », RFC 2606).
 */
internal object SitPhishing2 {
    private const val FAKE_FROM = "service@banque-verif.example"
    private const val REAL = "banque.example"
    private const val FAKE = "banque-verif.example"
    private val GOALS = setOf(Goal.COMPRENDRE_MECANISME, Goal.IDENTIFIER_MENACE, Goal.APPRENDRE_REFLEXE, Goal.SUIVRE_PROCEDURE)

    /** Carte d'une variante : cadre, icône dessinée par l'appelant, nom et précision. */
    private fun cell(c: StageCtx, i: Int, p: Float, active: Boolean, name: String, sub: String, icon: (Float, Float) -> Unit) {
        val g = c.g; val th = c.th
        val x0 = 40f + (i % 2) * 480f; val y0 = 60f + (i / 2) * 300f; val w = 440f; val h = 280f
        val cx = x0 + w / 2f
        g.withPose(Pose(alpha = p, scale = 0.85f + 0.15f * p), cx, y0 + h / 2f) {
            g.fillRoundRect(x0, y0, x0 + w, y0 + h, 22f, th.panel)
            g.strokeRoundRect(x0, y0, x0 + w, y0 + h, 22f, if (active) 5f else 3f, if (active) th.warn else th.line)
            icon(cx, y0 + 112f)
            SK.pill(g, name, cx, y0 + h - 66f, th.warn, th.bgTop.withAlpha(0.85f), 26f)
            SK.text(g, sub, cx, y0 + h - 22f, 24f, th.muted, 600, center = true)
        }
    }

    private val clicklink = beat("clicklink", "Le message demande de confirmer ses informations en cliquant sur un lien", SitKit.BODY, GOALS, emptySet(),
        minMs = 4000, needs = setOf("topic:phish-click")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c); SitKit.fiction(c)
        val click = SK.ease(t, 2800f, 300f)
        val body = SitPhishing.mail(c, 60f, 70f, 600f, 560f, link = click * (0.5f + 0.5f * sin(t / 200f)), showClock = false)
        // Le pointeur va vers le bouton du message.
        val bx = body[0] + 175f; val by = body[1] + 128f + 31f
        val u = SK.ease(t, 1200f, 1500f)
        SK.cursor(g, lerp(560f, bx, u), lerp(520f, by, u), 26f, rgb(0xFFFFFF), rgb(0x111111))
        val look = Props.gaze(Face.mix(Face.FOCUSED, Face.WORRIED, SK.ease(t, 2600f, 500f)), 840f, 330f, 300f, 250f)
        Cast.draw(g, 830f, floor, 370f, Cast.persona(0), look, Poses.holdFront(t), t, 0, mirror = true)
    }

    private val variants = beat("variants", "Les variantes : spear phishing, whaling, smishing, vishing", SitKit.BODY, GOALS, emptySet(),
        minMs = 6000, needs = setOf("topic:phish-variants")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SitKit.office(c); SitKit.fiction(c)
        val lead = 900f; val span = (c.scene.durationMs - lead - 700f).coerceAtLeast(3000f)
        val cum = floatArrayOf(0f, 0.38f, 0.58f, 0.80f)
        val start = FloatArray(4) { lead + span * cum[it] }
        val shown = (0..3).count { t >= start[it] }
        fun env(x: Float, y: Float, w: Float) = SK.envelope(g, x, y, w, th.panelHi, th.warn)
        cell(c, 0, SK.pop(t, start[0], 450f), shown == 1, "SPEAR PHISHING", "message personnalisé et ciblé") { cx, cy ->
            env(cx - 110f, cy, 84f); SK.arrow(g, cx - 62f, cy, cx + 20f, cy, 5f, th.bad, 16f)
            SK.person(g, cx + 80f, cy + 4f, 34f, th.ink)
            for (k in 1..3) g.strokeCircle(cx + 80f, cy + 4f, 22f + k * 17f, 3f, th.bad.withAlpha(0.9f - k * 0.2f))
        }
        cell(c, 1, SK.pop(t, start[1], 450f), shown == 2, "WHALING", "vise les dirigeants") { cx, cy ->
            env(cx - 110f, cy, 84f); SK.arrow(g, cx - 62f, cy, cx + 10f, cy, 5f, th.bad, 16f)
            SK.person(g, cx + 70f, cy + 2f, 44f, th.accent)
            g.fillRoundRect(cx + 108f, cy + 26f, cx + 160f, cy + 62f, 7f, th.warn)
            g.strokeArc(cx + 134f, cy + 26f, 14f, 180f, 180f, 5f, th.warn)
        }
        cell(c, 2, SK.pop(t, start[2], 450f), shown == 3, "SMISHING", "par SMS") { cx, cy ->
            SK.phone(g, cx, cy, 108f, 190f, th)
            g.fillRoundRect(cx - 40f, cy - 46f, cx + 40f, cy + 6f, 10f, th.bad)
            SK.text(g, "SMS", cx, cy - 22f, 20f, rgb(0xFFFFFF), 800, center = true)
            SK.text(g, "suivi.example", cx, cy - 4f, 14f, rgb(0xFFFFFF), 500, center = true, family = FontFamily.MONO)
        }
        cell(c, 3, SK.pop(t, start[3], 450f), shown == 4, "VISHING", "par téléphone") { cx, cy ->
            SK.phone(g, cx, cy, 108f, 190f, th)
            g.fillCircle(cx, cy - 20f, 22f, th.warn)
            SK.text(g, "Appel", cx, cy + 32f, 20f, th.ink, 700, center = true)
            for (k in 0..2) { val u = SK.loop(t, 1500f, k * 500f); g.strokeArc(cx + 62f, cy - 20f, 22f + u * 44f, -50f, 100f, 5f, th.warn.withAlpha(1f - u)) }
        }
    }

    private val headers = beat("headers", "En-tête du message : SPF, DKIM et DMARC en échec", SitKit.BODY, GOALS, emptySet(),
        minMs = 6000, needs = setOf("topic:phish-headers")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SitKit.office(c); SitKit.fiction(c)
        val lead = 900f; val span = (c.scene.durationMs - lead - 700f).coerceAtLeast(3000f)
        val at = floatArrayOf(lead + span * 0.12f, lead + span * 0.34f, lead + span * 0.56f, lead + span * 0.78f)
        val l = 50f; val r = 950f
        g.fillRoundRect(l, 70f, r, 330f, 20f, th.panel); g.strokeRoundRect(l, 70f, r, 330f, 20f, 3f, th.line)
        g.fillRoundRect(l, 70f, r, 118f, 20f, th.panelHi); g.fillRect(l, 98f, r, 118f, th.panelHi)
        SK.text(g, "En-tête technique du message (extrait)", l + 26f, 104f, 24f, th.muted, 700)
        val mono = FontFamily.MONO
        SK.text(g, "From: $FAKE_FROM", l + 30f, 170f, 26f, th.bad, 500, family = mono)
        SK.text(g, "To: awa@entreprise.example", l + 30f, 212f, 26f, th.muted, 500, family = mono)
        SK.text(g, "Authentication-Results:", l + 30f, 262f, 26f, th.ink, 600, family = mono)
        val names = listOf("spf", "dkim", "dmarc")
        var x = l + 54f
        for ((k, n) in names.withIndex()) {
            val p = SK.ease(t, at[k], 350f)
            val txt = "$n=fail"
            val w = txt.length * 15.6f
            if (p > 0.01f) {
                g.fillRoundRect(x - 8f, 276f, x + w + 8f, 312f, 9f, th.bad.withAlpha(0.22f * p))
                g.strokeRoundRect(x - 8f, 276f, x + w + 8f, 312f, 9f, 3f, th.bad.withAlpha(p))
            }
            SK.text(g, if (p > 0.5f) txt else "$n=...", x, 302f, 26f, if (p > 0.5f) th.bad else th.muted, 700, family = mono)
            x += w + 40f
        }
        val what = listOf("SPF" to "le serveur d'envoi est-il autorisé ?", "DKIM" to "la signature est-elle valide ?", "DMARC" to "la politique du domaine est-elle respectée ?")
        for ((k, wv) in what.withIndex()) {
            val p = SK.ease(t, at[k], 450f)
            val cx = 50f + 150f + k * 300f
            g.withPose(Pose(alpha = p, dy = (1f - p) * 14f), cx, 440f) {
                g.fillRoundRect(cx - 142f, 360f, cx + 142f, 520f, 18f, th.panel); g.strokeRoundRect(cx - 142f, 360f, cx + 142f, 520f, 18f, 3f, th.bad)
                SK.text(g, wv.first, cx, 416f, 34f, th.bad, 800, center = true)
                val words = wv.second.split(' ')
                val mid = (words.size + 1) / 2
                SK.text(g, words.take(mid).joinToString(" "), cx, 462f, 22f, th.ink, 600, center = true)
                SK.text(g, words.drop(mid).joinToString(" "), cx, 492f, 22f, th.ink, 600, center = true)
                SK.cross(g, cx + 108f, 396f, 20f, th.bad)
            }
        }
        SK.pill(g, "ECHEC  =  EXPEDITEUR PEUT-ETRE USURPE", 500f, 590f, th.warn, th.bgTop.withAlpha(0.9f), 26f, SK.ease(t, at[3], 450f))
    }

    private val typed = beat("typed", "Taper soi-même l'adresse officielle plutôt que cliquer", SitKit.BODY, GOALS, emptySet(),
        minMs = 5000, needs = setOf("topic:phish-typed")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SitKit.office(c); SitKit.fiction(c)
        // À gauche : le message et son lien, qu'on n'ouvre pas.
        val out = SK.ease(t, 700f, 600f)
        g.withPose(Pose(alpha = 1f - 0.45f * out, scale = 1f - 0.12f * out), 235f, 250f) {
            SK.envelope(g, 235f, 230f, 250f, th.panelHi, th.warn)
            g.fillRoundRect(120f, 330f, 350f, 372f, 10f, th.bad)
            SK.text(g, "CONFIRMER MAINTENANT", 235f, 359f, 18f, rgb(0xFFFFFF), 700, center = true)
            SK.text(g, "http://$FAKE/ok", 235f, 400f, 17f, th.bad, 500, center = true, family = FontFamily.MONO)
        }
        if (out > 0.4f) { g.line(110f, 140f, 360f, 420f, 8f, th.bad.withAlpha(out)); g.line(360f, 140f, 110f, 420f, 8f, th.bad.withAlpha(out)) }
        SK.pill(g, "NE PAS CLIQUER", 235f, 470f, th.bad, th.bgTop.withAlpha(0.9f), 24f, out)
        // À droite : l'adresse officielle tapée à la main.
        val typing = SK.ease(t, 1500f, 1700f)
        val done = typing > 0.98f
        val ct = SK.browser(g, 470f, 110f, 960f, 420f, th, REAL, typing, th.ink, if (done) true else null)
        SK.text(g, "Mon espace client", 715f, ct + 70f, 30f, th.ink, 700, center = true)
        g.fillRoundRect(600f, ct + 100f, 830f, ct + 150f, 12f, if (done) th.good else th.line)
        SK.text(g, "Se connecter", 715f, ct + 134f, 24f, rgb(0xFFFFFF), 700, center = true)
        if (done) SK.check(g, 880f, ct + 126f, 36f, th.good, SK.ease(t, 3300f, 350f))
        if (typing < 0.98f) SK.cursor(g, 640f + 160f * typing, 330f, 24f, rgb(0xFFFFFF), rgb(0x111111))
        SK.pill(g, "ADRESSE TAPEE A LA MAIN", 715f, 480f, th.good, th.bgTop.withAlpha(0.9f), 24f, SK.ease(t, 3000f, 400f))
    }

    private val mfa = beat("mfa", "Double authentification : le mot de passe seul ne suffit plus", SitKit.BODY, GOALS, emptySet(),
        minMs = 6000, needs = setOf("topic:phish-mfa")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SitKit.office(c); SitKit.fiction(c)
        val lead = 800f; val span = (c.scene.durationMs - lead - 700f).coerceAtLeast(3000f)
        val s1 = lead + span * 0.08f; val s2 = lead + span * 0.34f; val s3 = lead + span * 0.55f; val s4 = lead + span * 0.74f
        val ct = SK.browser(g, 40f, 80f, 560f, 450f, th, REAL, 1f, th.ink, true)
        // Premier facteur.
        g.fillRoundRect(80f, ct + 24f, 520f, ct + 78f, 12f, th.bgBottom); g.strokeRoundRect(80f, ct + 24f, 520f, ct + 78f, 12f, 3f, th.line)
        SK.text(g, "Mot de passe  ••••••••", 100f, ct + 60f, 26f, th.ink, 600)
        SK.check(g, 484f, ct + 51f, 30f, th.good, SK.ease(t, s1, 350f))
        // Second facteur.
        val p2 = SK.ease(t, s2, 400f)
        g.withPose(Pose(alpha = p2, dy = (1f - p2) * 12f), 300f, ct + 120f) {
            g.fillRoundRect(80f, ct + 100f, 520f, ct + 154f, 12f, th.bgBottom); g.strokeRoundRect(80f, ct + 100f, 520f, ct + 154f, 12f, 3f, if (t >= s4) th.good else th.warn)
            SK.text(g, if (t >= s4) "Code  4 8 1 9 0 2" else "Code à 6 chiffres  ......", 100f, ct + 136f, 26f, th.ink, 600)
        }
        SK.check(g, 484f, ct + 127f, 30f, th.good, SK.ease(t, s4, 350f))
        // Le téléphone de la personne affiche le code.
        val p3 = SK.pop(t, s3, 500f)
        g.withPose(Pose(alpha = p3, scale = 0.8f + 0.2f * p3), 800f, 270f) {
            SK.phone(g, 800f, 270f, 190f, 330f, th)
            SK.text(g, "Votre code", 800f, 250f, 22f, th.muted, 600, center = true)
            SK.text(g, "481 902", 800f, 306f, 40f, th.accent, 800, center = true, family = FontFamily.MONO)
        }
        if (t >= s3) SK.arrow(g, 700f, 270f, 580f, ct + 126f, 4f, th.accent.withAlpha(SK.ease(t, s3 + 300f, 400f)), 14f)
        // Un mot de passe volé ne suffit pas.
        val p5 = SK.ease(t, lead + span * 0.84f, 450f)
        if (p5 > 0.01f) {
            g.withPose(Pose(alpha = p5), 200f, 560f) {
                SK.hood(g, 150f, 560f, 30f, rgb(0x2A3550), th.bad)
                SK.pill(g, "MOT DE PASSE VOLE", 370f, 535f, th.bad, th.bgTop.withAlpha(0.9f), 22f)
                SK.pill(g, "SANS LE CODE : REFUSE", 400f, 590f, th.warn, th.bgTop.withAlpha(0.9f), 22f)
                SK.cross(g, 610f, 562f, 30f, th.bad)
            }
        }
    }

    private val passkey = beat("passkey", "Passkey ou clé de sécurité : elle refuse un faux site", SitKit.BODY, GOALS, emptySet(),
        minMs = 6000, needs = setOf("topic:phish-passkey")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SitKit.office(c); SitKit.fiction(c)
        val lead = 800f; val span = (c.scene.durationMs - lead - 700f).coerceAtLeast(3000f)
        val sReal = lead + span * 0.12f; val sFake = lead + span * 0.52f
        // La clé : anneau + tige + dents.
        val kx = 130f; val ky = 330f
        g.fillCircle(kx, ky, 46f, th.warn); g.fillCircle(kx, ky, 18f, th.bgBottom)
        g.fillRect(kx + 40f, ky - 9f, kx + 150f, ky + 9f, th.warn)
        g.fillRect(kx + 110f, ky + 9f, kx + 124f, ky + 34f, th.warn); g.fillRect(kx + 132f, ky + 9f, kx + 146f, ky + 26f, th.warn)
        SK.pill(g, "PASSKEY / CLE DE SECURITE", 200f, 440f, th.warn, th.bgTop.withAlpha(0.9f), 20f)
        // Site officiel : la clé est acceptée.
        val ct1 = SK.browser(g, 400f, 60f, 960f, 290f, th, REAL, 1f, th.ink, true)
        val a1 = SK.ease(t, sReal, 500f)
        SK.arrow(g, 290f, ky - 20f, 390f, 170f, 5f, th.good.withAlpha(a1), 16f)
        SK.text(g, "Clé reconnue pour ce site", 680f, ct1 + 56f, 26f, th.ink.withAlpha(0.35f + 0.65f * a1), 600, center = true)
        SK.check(g, 680f, ct1 + 100f, 38f, th.good, SK.ease(t, sReal + 500f, 350f))
        // Faux site : la clé refuse de s'authentifier, le domaine ne correspond pas.
        val ct2 = SK.browser(g, 400f, 340f, 960f, 570f, th, FAKE, 1f, th.bad, false)
        val a2 = SK.ease(t, sFake, 500f)
        SK.arrow(g, 290f, ky + 20f, 390f, 450f, 5f, th.bad.withAlpha(a2), 16f)
        SK.text(g, "Le domaine ne correspond pas", 680f, ct2 + 56f, 26f, th.ink.withAlpha(0.35f + 0.65f * a2), 600, center = true)
        SK.cross(g, 680f, ct2 + 100f, 38f, th.bad, SK.ease(t, sFake + 500f, 350f))
        SK.pill(g, "LA CLE REFUSE", 680f, 600f, th.bad, th.bgTop.withAlpha(0.9f), 24f, SK.ease(t, sFake + 700f, 400f))
    }

    private val reset = beat("reset", "Déjà saisi son mot de passe : le changer sur le site officiel et prévenir", SitKit.BODY, GOALS, emptySet(),
        minMs = 6000, needs = setOf("topic:phish-reset")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SitKit.office(c); SitKit.fiction(c)
        val lead = 800f; val span = (c.scene.durationMs - lead - 700f).coerceAtLeast(3000f)
        val s1 = lead + span * 0.10f; val s2 = lead + span * 0.38f; val s3 = lead + span * 0.62f
        // 1. Sur le site officiel (adresse connue, cadenas) : nouveau mot de passe.
        val ct = SK.browser(g, 40f, 80f, 580f, 400f, th, REAL, 1f, th.ink, true)
        SK.text(g, "Changer le mot de passe", 310f, ct + 38f, 26f, th.ink, 700, center = true)
        g.fillRoundRect(80f, ct + 62f, 540f, ct + 112f, 12f, th.bgBottom); g.strokeRoundRect(80f, ct + 62f, 540f, ct + 112f, 12f, 3f, th.line)
        val typing = SK.ease(t, s1, 1300f)
        SK.text(g, "Nouveau  " + "•".repeat((10 * typing).toInt()), 100f, ct + 96f, 26f, th.ink, 600)
        SK.check(g, 500f, ct + 87f, 30f, th.good, SK.ease(t, s1 + 1400f, 350f))
        SK.pill(g, "1  CHANGER, SUR LE SITE OFFICIEL", 310f, 450f, th.good, th.bgTop.withAlpha(0.9f), 22f, SK.ease(t, s1, 400f))
        // 2. Prévenir la banque ou le service informatique.
        val p2 = SK.pop(t, s2, 500f)
        g.withPose(Pose(alpha = p2, scale = 0.8f + 0.2f * p2), 800f, 250f) {
            SK.phone(g, 800f, 250f, 190f, 330f, th)
            g.fillRoundRect(722f, 215f, 878f, 275f, 14f, th.warn)
            SK.text(g, "Alerte envoyée", 800f, 242f, 22f, th.bgTop, 800, center = true)
            SK.text(g, "banque / informatique", 800f, 266f, 16f, th.bgTop, 600, center = true)
        }
        SK.pill(g, "2  PREVENIR LA BANQUE", 800f, 450f, th.warn, th.bgTop.withAlpha(0.9f), 22f, SK.ease(t, s2, 400f))
        if (t >= s3) SK.arrow(g, 620f, 240f, 690f, 240f, 4f, th.warn.withAlpha(SK.ease(t, s3, 300f)), 14f)
        val hp = SK.ease(t, s3, 500f)
        if (hp > 0.01f) {
            SK.hood(g, 150f, 590f, 28f, rgb(0x2A3550), th.bad.withAlpha(hp))
            SK.pill(g, "L'ANCIEN MOT DE PASSE NE SERT PLUS", 600f, 590f, th.bad, th.bgTop.withAlpha(0.9f), 22f, hp)
        }
    }

    private val urgent = beat("urgent", "Le message presse : compte suspendu sous 24 heures", SitKit.BODY, GOALS, emptySet(),
        minMs = 4000, needs = setOf("topic:phish-urgent")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = SitKit.office(c); SitKit.fiction(c)
        val body = SitPhishing.mail(c, 60f, 70f, 600f, 560f, showClock = false)
        // La phrase qui fixe le délai s'allume ; une horloge tourne à côté : la pression du temps est le mécanisme montré.
        val p = SK.ease(t, 1000f, 500f)
        g.strokeRoundRect(body[0] - 10f, body[1] + 30f, 576f, body[1] + 72f, 12f, 4f, th.bad.withAlpha(p * (0.7f + 0.3f * sin(t / 220f))))
        val q = SK.pop(t, 1600f, 600f)
        g.withPose(Pose(alpha = q, scale = 0.8f + 0.2f * q), 810f, 110f) {
            Props.clock(g, 810f, 110f, 56f, th, t, th.bad)
            SK.pill(g, "SOUS 24 H", 810f, 192f, th.bad, th.bgTop.withAlpha(0.85f), 26f)
        }
        val face = Face.mix(Face.mix(Face.FOCUSED, Face.WORRIED, SK.ease(t, 1400f, 500f)), Face.SUSPICIOUS, SK.ease(t, 3600f, 600f))
        val look = Props.gaze(face, 840f, 330f, 300f, 250f)
        Cast.draw(g, 850f, floor, 340f, Cast.persona(0), look, Poses.holdFront(t), t, 0, mirror = true)
    }

    val beats: List<Beat> = listOf(clicklink, variants, headers, typed, mfa, passkey, reset, urgent)
}
