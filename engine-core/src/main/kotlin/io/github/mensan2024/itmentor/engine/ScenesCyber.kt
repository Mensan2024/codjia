// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.sin

/**
 * Force brute : essais en boucle, vitesse comparée, verrouillage après échecs, second facteur, surveillance des journaux,
 * couches de défense. Les mots de passe et lignes de journal dessinés sont des exemples fictifs (adresse de documentation
 * 203.0.113.0/24, réservée par la RFC 5737) et sont annoncés comme tels.
 */
internal object BruteForceScenes {
    private val GUESSES = listOf("123456", "azerty", "password", "qwerty", "admin", "Soleil2020", "abc123", "letmein", "111111", "root")

    private val guess = beat("guess", "Des milliers d'essais", setOf(Role.INTRO, Role.DEFINE), setOf(Goal.IDENTIFIER_MENACE, Goal.COMPRENDRE_MECANISME),
        cueSet(Act.DEVINER, Thing.MOT_DE_PASSE, Actor.ATTAQUANT)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SIMULATION  -  EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        val a = SK.pop(t, 100f, 600f)
        SK.glow(g, 140f, 340f, 250f, th.bad, 0.14f)
        g.withPose(Pose(alpha = clamp01(a)), 140f, 340f) {
            SK.hood(g, 140f, 290f, 70f, rgb(0x1B2233), th.bad)
            SK.laptop(g, 140f, 480f, 160f, th, screen = th.bad.withAlpha(0.3f))
        }
        SK.pill(g, "ATTAQUANT", 140f, 600f, th.bad, th.bgTop.withAlpha(0.8f), 24f, clamp01(a))
        // Le cadenas encaisse chaque essai.
        val period = 520f
        val n = (t / period).toInt()
        val u = (t % period) / period
        val hit = if (u < 0.22f) 1f - u / 0.22f else 0f
        SK.glow(g, 800f, 330f, 260f, th.bad, 0.10f + 0.22f * hit)
        SK.lock(g, 800f, 300f, 170f, th.line, th.ink.withAlpha(0.9f), 0f, sin(t / 18f) * 6f * hit)
        for (k in 0 until 4) {
            val idx = n - k
            if (idx < 0) continue
            val uu = ((t - idx * period) / (period * 4.2f)).coerceIn(0f, 1f)
            val x = lerp(290f, 700f, uu); val y = 250f + ((idx * 53) % 7 - 3) * 34f
            SK.pill(g, GUESSES[idx % GUESSES.size], x, y, th.ink, th.panelHi, 22f, clamp01((1f - uu) * 3f) * clamp01(uu * 8f))
        }
        SK.text(g, "TENTATIVES", 800f, 530f, 22f, th.muted, center = true)
        val count = (t / 5.5f).toInt() * 7 + 41
        SK.text(g, count.toString().padStart(6, '0'), 800f, 590f, 54f, th.bad, 700, center = true, family = FontFamily.MONO)
    }

    private val speed = beat("speed", "Court contre long", setOf(Role.MECHANISM, Role.PROCEDURE, Role.DEFINE), setOf(Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.MOT_DE_PASSE, Act.DEVINER)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "ILLUSTRATION QUALITATIVE", 72f, 24f, th.accent.withAlpha(0.85f))
        fun bar(y: Float, label: String, pass: String, fast: Boolean, start: Float) {
            val p = SK.ease(t, start, 500f, Easing.EXPO_OUT)
            g.withPose(Pose(alpha = p, dx = (1f - p) * -60f), 500f, y) {
                g.fillRoundRect(40f, y - 120f, 960f, y + 120f, 28f, th.panel)
                SK.text(g, label, 80f, y - 62f, 26f, th.muted)
                SK.text(g, pass, 80f, y - 6f, 44f, th.ink, 400, family = FontFamily.MONO)
                g.fillRoundRect(80f, y + 40f, 860f, y + 78f, 19f, th.bgBottom)
                val fill = if (fast) SK.ease(t, start + 900f, 1700f, Easing.EASE_IN) else 0.035f + 0.015f * SK.ease(t, start + 900f, 6000f, Easing.LINEAR)
                g.fillRoundRect(80f, y + 40f, 80f + 780f * fill, y + 78f, 19f, if (fast) th.bad else th.good)
                val done = fast && fill >= 0.999f
                SK.lock(g, 905f, y - 30f, 56f, if (done) th.bad else th.line, th.ink, if (done) 1f else 0f)
                SK.text(g, if (fast) "VITE DEVINE" else "TRES LONG A DEVINER", 850f, y + 106f, 22f, if (fast) th.bad else th.good, center = true)
            }
        }
        bar(190f, "MOT DE PASSE COURT ET COURANT", "azerty12", true, 200f)
        bar(500f, "MOT DE PASSE LONG ET UNIQUE", "Cafe-Lune-Pluie-Rouge-47", false, 1000f)
    }

    private val lockout = beat("lockout", "Verrouillage après trop d'échecs", setOf(Role.ADVICE), setOf(Goal.APPRENDRE_REFLEXE),
        cueSet(Act.LIMITER, Act.BLOQUER, Thing.JOURNAUX_SYSTEME)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SIMULATION  -  EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        SK.hood(g, 130f, 320f, 66f, rgb(0x1B2233), th.bad)
        SK.pill(g, "ATTAQUANT", 130f, 450f, th.bad, th.bgTop.withAlpha(0.8f), 22f)
        val locked = SK.ease(t, 3600f, 500f)
        SK.glow(g, 720f, 330f, 260f, mix(th.bad, th.good, locked), 0.16f)
        SK.lock(g, 720f, 300f, 190f, mix(th.line, th.good.withAlpha(0.9f), locked), th.ink, 0f)
        // Cinq essais, un par seconde ; le cinquième déclenche le verrouillage.
        for (i in 0 until 5) {
            val on = t > 700f + i * 600f
            val x = 460f + i * 56f
            g.fillCircle(x, 560f, 20f, if (on) th.bad else th.panelHi)
            if (on) SK.cross(g, x, 560f, 14f, rgb(0xFFFFFF), 1f, 5f)
        }
        SK.text(g, "ECHECS", 600f, 625f, 22f, th.muted, center = true)
        for (k in 0 until 6) {
            val u = SK.loop(t, 1200f, k * 200f)
            val start = 200f; val stop = if (t > 3600f) 590f else 640f
            val x = lerp(start, stop, u); val y = 330f + (k % 3 - 1) * 26f
            val blocked = t > 3600f && u > 0.93f
            SK.packet(g, x, y, 9f, th.bad, clamp01(u / 0.1f) * (if (blocked) 0f else 1f))
        }
        if (t > 3600f) {
            SK.shield(g, 590f, 330f, 100f * SK.pop(t, 3600f, 600f).coerceAtMost(1.1f), th.good.withAlpha(0.25f), th.good)
            SK.pill(g, "ACCES BLOQUE", 720f, 130f, rgb(0xFFFFFF), th.good.withAlpha(0.95f), 28f, clamp01(locked))
            val u = SK.loop(t, 1200f)
            if (u > 0.75f) SK.burst(g, 575f, 330f, 36f, th.bad, (u - 0.75f) / 0.25f)
        }
    }

    private val mfa = beat("mfa", "Second verrou : la double authentification", setOf(Role.ADVICE), setOf(Goal.APPRENDRE_REFLEXE),
        cueSet(Act.ACTIVER_MFA, Thing.CODE, Thing.MOT_DE_PASSE)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SIMULATION  -  EXEMPLE FICTIF", 72f, 24f, th.accent.withAlpha(0.85f))
        SK.hood(g, 110f, 330f, 60f, rgb(0x1B2233), th.bad)
        // Porte 1 : le mot de passe a été deviné, elle s'ouvre.
        val open1 = SK.ease(t, 1200f, 600f)
        SK.glow(g, 380f, 330f, 200f, th.warn, 0.10f)
        SK.lock(g, 380f, 300f, 130f, th.warn, th.ink, open1)
        SK.pill(g, "MOT DE PASSE", 380f, 470f, th.ink, th.panelHi, 22f)
        // Porte 2 : un code qui change sur le téléphone.
        SK.glow(g, 700f, 330f, 230f, th.good, 0.14f)
        SK.lock(g, 700f, 300f, 130f, th.good, th.ink, 0f)
        SK.pill(g, "CODE UNIQUE", 700f, 470f, th.ink, th.panelHi, 22f)
        SK.phone(g, 900f, 340f, 150f, 300f, th)
        val slot = (t / 1500f).toInt()
        val code = (0 until 6).joinToString("") { (SK.hash01(slot * 6 + it, 3) * 10f).toInt().toString() }
        SK.text(g, code, 900f, 340f, 30f, th.good, 700, center = true, family = FontFamily.MONO)
        val prog = 1f - (t % 1500f) / 1500f
        g.fillRoundRect(840f, 372f, 840f + 120f * prog, 380f, 4f, th.good)
        val u = SK.ease(t, 1800f, 1800f, Easing.EASE_IN_OUT)
        val x = lerp(190f, 590f, u)
        SK.packet(g, x, 330f, 11f, th.bad, if (t > 1800f) 1f else 0.9f)
        if (u >= 1f) SK.burst(g, 610f, 330f, 40f, th.bad, SK.loop(t, 1400f))
        if (u >= 1f) SK.cross(g, 700f, 150f, 50f, th.bad, SK.ease(t, 3700f, 400f), 10f)
    }

    private val monitor = beat("monitor", "Lire les journaux", setOf(Role.ADVICE, Role.PROCEDURE), setOf(Goal.APPRENDRE_REFLEXE),
        cueSet(Act.SURVEILLER, Thing.JOURNAUX_SYSTEME)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "EXEMPLE FICTIF  -  ADRESSE DE DOCUMENTATION", 72f, 24f, th.accent.withAlpha(0.85f))
        SK.terminal(g, 40f, 60f, 960f, 480f, th)
        val f = SK.font(22f, 400, FontFamily.MONO)
        val rows = 9
        val shift = (t / 620f).toInt()
        for (r in 0 until rows) {
            val idx = shift + r
            val y = 118f + r * 42f
            val bad = idx % 3 != 2
            val ip = if (bad) "203.0.113.7" else "198.51.100." + (10 + idx % 40)
            val user = if (bad) listOf("admin", "root", "test", "oracle")[idx % 4] else "marie"
            val line = if (bad) "Failed password for $user from $ip" else "Accepted password for $user from $ip"
            val a = clamp01(1f - (r.toFloat() / rows) * 0.2f)
            val x = 70f
            g.drawText(line, x, y, f, if (bad) th.bad.withAlpha(a) else th.good.withAlpha(a))
        }
        val bell = SK.pop(t, 2400f, 500f)
        if (bell > 0f) {
            g.fillRoundRect(560f, 570f, 940f, 656f, 22f, th.warn.withAlpha(clamp01(bell)))
            SK.text(g, "ALERTE : TROP D'ECHECS", 750f, 624f, 26f, th.bgBottom, center = true)
        }
        val u = SK.loop(t, 1000f)
        g.strokeRoundRect(60f, 100f, 900f, 140f, 8f, 3f, th.warn.withAlpha(0.4f + 0.4f * sin(u * 2f * PI.toFloat())))
    }

    private val layers = beat("layers", "Des couches de défense", setOf(Role.RECAP, Role.ADVICE, Role.PROCEDURE), setOf(Goal.MEMORISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Thing.MOT_DE_PASSE, Act.ACTIVER_MFA, Act.LIMITER, Act.SURVEILLER)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val cx = 330f; val cy = 350f
        SK.glow(g, cx, cy, 330f, th.good, 0.12f)
        // Les anneaux apparaissent au rythme des lignes du texte (700 ms + 600 ms par ligne).
        val labels = listOf("password" to "MOT DE PASSE", "mfa" to "DOUBLE FACTEUR", "limit" to "LIMITATION", "monitor" to "SURVEILLANCE")
        val motifs = c.intent.itemMotifs
        val shown = labels.mapIndexedNotNull { k, (m, name) ->
            val at = motifs.indexOf(m)
            when {
                at >= 0 -> Triple(m, name, 700f + at * 600f)
                motifs.none { it.isNotEmpty() } -> Triple(m, name, 700f + k * 600f)
                else -> null
            }
        }
        SK.lock(g, cx, cy - 40f, 100f, th.line, th.ink, 0f)
        for ((k, s) in shown.withIndex()) {
            val r = 120f + k * 52f
            val p = SK.ease(t, s.third, 700f, Easing.EXPO_OUT)
            if (p <= 0f) continue
            val col = listOf(th.good, th.accent, th.warn, th.glow)[k % 4]
            g.strokeCircle(cx, cy, r * p, 8f, col.withAlpha(0.55f + 0.25f * sin(t / 400f + k)))
            val ly = 130f + k * 110f
            g.line(cx + r * 0.7f, cy - r * 0.7f, 660f, ly, 2.5f, col.withAlpha(0.6f * p))
            g.fillCircle(cx + r * 0.7f, cy - r * 0.7f, 7f, col)
            g.fillRoundRect(660f, ly - 34f, 990f, ly + 34f, 18f, th.panel)
            g.fillCircle(690f, ly, 14f, col)
            SK.text(g, s.second, 716f, ly + 9f, 21f, th.ink)
        }
        val u = SK.loop(t, 2200f)
        val ox = lerp(30f, cx - 120f - shown.size * 52f, u)
        SK.packet(g, ox, 350f, 11f, th.bad, clamp01(u / 0.1f) * (1f - clamp01((u - 0.85f) / 0.15f)))
        if (u > 0.82f) SK.burst(g, cx - 120f - shown.size * 52f, 350f, 40f, th.bad, (u - 0.82f) / 0.18f)
    }

    val kind = SceneKind("bruteforce", "Force brute", setOf(Concept.BRUTE_FORCE), listOf(guess, speed, lockout, mfa, monitor, layers))
}

/** Pare-feu : tri des paquets, table de règles, zones. */
internal object FirewallScenes {
    private fun wall(g: Gfx, x: Float, top: Float, bottom: Float, gateTop: Float, gateBottom: Float, th: StageTheme) {
        val rows = ((bottom - top) / 34f).toInt()
        for (r in 0 until rows) {
            val y = top + r * 34f
            if (y + 34f > gateTop && y < gateBottom) continue
            val off = if (r % 2 == 0) 0f else 22f
            for (k in -1..1) g.fillRoundRect(x - 36f + k * 44f + off - 22f, y + 2f, x - 36f + k * 44f + off + 20f, y + 32f, 5f, mix(th.line, th.bad, 0.18f))
        }
    }

    private val filter = beat("filter", "Tri des paquets", setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM), setOf(Goal.COMPRENDRE_MECANISME, Goal.IDENTIFIER_MENACE),
        cueSet(Thing.PAQUET, Act.FILTRER, Act.BLOQUER, Act.AUTORISER, Actor.PARE_FEU)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        SK.cloud(g, 120f, 330f, 92f, th.panelHi)
        SK.pill(g, "INTERNET", 120f, 450f, th.ink, th.bgTop.withAlpha(0.8f), 24f)
        val wx = 500f
        SK.glow(g, wx, 380f, 260f, th.warn, 0.08f)
        wall(g, wx, 110f, 650f, 335f, 425f, th)
        g.fillRoundRect(wx - 6f, 330f, wx + 6f, 430f, 4f, th.good.withAlpha(0.5f))
        SK.glow(g, wx, 380f, 110f, th.good, 0.28f)
        SK.pill(g, "PARE-FEU", wx, 90f, th.bgBottom, th.warn, 26f)
        // Réseau interne
        SK.glow(g, 860f, 380f, 220f, th.good, 0.10f)
        SK.pc(g, 880f, 300f, 74f, th); SK.pc(g, 880f, 460f, 74f, th)
        SK.server(g, 790f, 380f, 80f, 120f, th, c.tg)
        SK.pill(g, "RESEAU INTERNE", 850f, 600f, th.ink, th.bgTop.withAlpha(0.8f), 24f)
        for (k in 0 until 7) {
            val u = SK.loop(t, 4200f, k * 600f)
            val ok = k % 2 == 0
            val y0 = 250f + ((k * 97) % 5) * 40f
            val x = lerp(215f, if (ok) 760f else 468f, if (ok) u else clamp01(u / 0.57f))
            val y = if (ok) lerp(y0, 380f, clamp01((x - 215f) / 285f)) else y0
            val col = if (ok) th.good else th.bad
            if (!ok && u > 0.57f) { SK.burst(g, 468f, y, 36f, th.bad, (u - 0.57f) / 0.2f); continue }
            SK.packet(g, x, y, 11f, col, clamp01(u / 0.05f) * (if (ok) 1f - clamp01((u - 0.92f) / 0.08f) else 1f))
        }
        SK.pill(g, "AUTORISE", 620f, 560f, th.bgBottom, th.good, 24f)
        SK.pill(g, "BLOQUE", 360f, 560f, rgb(0xFFFFFF), th.bad, 24f)
    }

    private val rules = beat("rules", "Table de règles", setOf(Role.MECHANISM, Role.PROCEDURE, Role.DEFINE), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE),
        cueSet(Thing.REGLE, Act.AUTORISER, Act.BLOQUER)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "EXEMPLE DE REGLES  -  SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val rows = listOf(Triple("1", "AUTORISER", "navigation web"), Triple("2", "AUTORISER", "messagerie"), Triple("3", "BLOQUER", "tout le reste"))
        val top = 110f
        for ((i, r) in rows.withIndex()) {
            val y = top + i * 120f
            g.fillRoundRect(300f, y, 960f, y + 100f, 20f, th.panel)
            g.strokeRoundRect(300f, y, 960f, y + 100f, 20f, 2f, th.line)
            SK.text(g, r.first, 340f, y + 66f, 40f, th.muted, center = true)
            val allow = r.second == "AUTORISER"
            SK.pill(g, r.second, 520f, y + 50f, if (allow) th.bgBottom else rgb(0xFFFFFF), if (allow) th.good else th.bad, 26f)
            SK.text(g, r.third, 650f, y + 62f, 32f, th.ink, 600)
        }
        // Trois paquets défilent : chacun s'arrête sur la première règle qui le concerne.
        val labels = listOf("web", "mail", "inconnu")
        val cyc = 3600f
        for (k in 0 until 3) {
            val s = k * cyc
            val tt = t % (cyc * 3f)
            val local = tt - s
            if (local < 0f || local > cyc) continue
            val target = k
            val enter = SK.ease(local, 0f, 700f)
            val px = lerp(60f, 250f, enter)
            // le paquet descend le long des règles jusqu'à celle qui lui correspond
            val py = lerp(top + 50f, top + 50f + target * 120f, SK.ease(local, 700f, 300f + 600f * target, Easing.LINEAR))
            val matched = local > 1000f + 600f * target
            val col = if (k == 2) th.bad else th.good
            SK.packet(g, px, py, 14f, col)
            SK.text(g, labels[k], px, py + 44f, 20f, th.muted, center = true)
            if (matched) {
                g.strokeRoundRect(300f, top + target * 120f, 960f, top + target * 120f + 100f, 20f, 5f, col)
                SK.burst(g, 300f, py, 36f, col, ((local - 1000f - 600f * target) / 800f).coerceIn(0f, 1f))
            }
        }
    }

    private val zones = beat("zones", "Zones et sens de circulation", setOf(Role.DEFINE, Role.MECHANISM, Role.PROCEDURE), setOf(Goal.VOIR_ARCHITECTURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.PARE_FEU, Actor.SERVEUR, Thing.PAQUET)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val zx = listOf(40f to 290f, 355f to 645f, 710f to 960f)
        val names = listOf("INTERNET", "ZONE EXPOSEE", "RESEAU INTERNE")
        for ((i, z) in zx.withIndex()) {
            val p = SK.ease(t, 150f + i * 300f, 600f, Easing.EXPO_OUT)
            g.withPose(Pose(alpha = p, dy = (1f - p) * 30f), (z.first + z.second) / 2f, 350f) {
                g.fillRoundRect(z.first, 90f, z.second, 620f, 26f, th.panel.withAlpha(0.8f))
                g.strokeRoundRect(z.first, 90f, z.second, 620f, 26f, 2f, th.line)
                SK.text(g, names[i], (z.first + z.second) / 2f, 138f, 22f, th.muted, center = true)
            }
        }
        SK.cloud(g, 165f, 350f, 80f, th.panelHi)
        SK.server(g, 500f, 350f, 150f, 160f, th, c.tg)
        SK.pc(g, 780f, 270f, 70f, th); SK.pc(g, 880f, 400f, 70f, th); SK.pc(g, 780f, 520f, 70f, th)
        for (x in listOf(322f, 678f)) { wall(g, x, 150f, 570f, 330f, 380f, th) }
        SK.pill(g, "PARE-FEU", 322f, 610f, th.bgBottom, th.warn, 20f); SK.pill(g, "PARE-FEU", 678f, 610f, th.bgBottom, th.warn, 20f)
        val s1 = SK.ease(t, 1400f, 500f)
        SK.arrow(g, 250f, 355f, 250f + 70f * s1, 355f, 7f, th.good.withAlpha(s1), 22f)
        val s2 = SK.ease(t, 2400f, 500f)
        SK.arrow(g, 580f, 355f, 580f + 90f * s2, 355f, 7f, th.warn.withAlpha(s2), 22f)
        val s3 = SK.ease(t, 3400f, 500f)
        if (s3 > 0f) { SK.cross(g, 322f, 220f, 30f, th.bad, s3, 8f); SK.pill(g, "REFUSE", 322f, 170f, rgb(0xFFFFFF), th.bad, 20f, s3) }
        for (k in 0 until 3) {
            val u = SK.loop(t - 1400f, 2400f, k * 800f)
            if (t > 1400f) SK.packet(g, lerp(100f, 480f, u), 355f, 9f, th.good, clamp01(u / 0.1f) * (1f - clamp01((u - 0.85f) / 0.15f)))
        }
    }

    private val recap = beat("recap", "À retenir : un filtre à l'entrée", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Act.FILTRER, Act.BLOQUER, Act.AUTORISER)) { c ->
        PhishingScenes.recapShield(c, c.th.good) { x, y, a ->
            SK.cloud(c.g, x, y, 80f * a, c.th.panelHi)
        }
    }

    val kind = SceneKind("firewall", "Pare-feu", setOf(Concept.FIREWALL), listOf(filter, rules, zones, recap), fallbackFor = setOf(Concept.NETFLOW))
}
