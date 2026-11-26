// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.sin

/**
 * Démonstrations visuelles propres à un domaine, pour le format Infographie.
 * Chacune montre un mécanisme réel et porte la mention « Simulation » : rien n'est présenté comme une capture réelle.
 *  - PACKETS  : réseaux, trajet d'un paquet (le TTL diminue à chaque routeur) ;
 *  - PROMPT   : IA, un prompt vague et un prompt précis ne donnent pas la même réponse ;
 *  - EDITOR   : développement, écrire, exécuter, lire l'erreur, corriger ;
 *  - PULSE    : santé, rythme cardiaque et quatre habitudes ;
 *  - PHISHING : cybersécurité, SMS piégé, faux site, données volées.
 * Les textes dessinés restent dans le rectangle donné ; l'explication complète de l'auteur est dans le cadre du bas.
 */
internal class DomainDiagrams(
    private val ink: Int, private val muted: Int, private val accent: Int,
    private val danger: Int, private val ok: Int, private val warn: Int,
) {
    private val tf = FontSpec(FontFamily.TEXT, 800, 30f)
    private val sf = FontSpec(FontFamily.TEXT, 600, 24f)
    private val mf = FontSpec(FontFamily.MONO, 600, 26f)

    private fun card(g: Gfx, l: Float, tp: Float, r: Float, bt: Float, border: Int) {
        g.fillRoundRect(l, tp, r, bt, 18f, 0x2A000000)
        g.strokeRoundRect(l, tp, r, bt, 18f, 2.5f, border)
    }

    /** Texte centré, jamais plus large que maxW. */
    private fun centered(g: Gfx, s: String, cx: Float, baseline: Float, font: FontSpec, color: Int, maxW: Float) {
        val f = fitSize(g, s, font, maxW)
        g.drawText(s, cx - g.measureText(s, f) / 2, baseline, f, color)
    }

    private fun left(g: Gfx, s: String, x: Float, baseline: Float, font: FontSpec, color: Int, maxW: Float) {
        g.drawText(s, x, baseline, fitSize(g, s, font, maxW), color)
    }

    private fun tag(g: Gfx, l: Float, top: Float, text: String = "SIMULATION") {
        val f = FontSpec(FontFamily.TEXT, 800, 20f)
        val w = g.measureText(text, f) + 28f
        g.fillRoundRect(l, top, l + w, top + 34f, 17f, 0x33FFFFFF)
        g.drawText(text, l + 14f, top + 25f, f, muted)
    }

    private fun appear(t: Float, start: Float, dur: Float = 350f) = Easing.BACK_OUT.ease(clamp01((t - start) / dur))

    // ───────────────────────────── Réseaux ─────────────────────────────

    fun packets(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top)
        val cy = top + 190f
        val xs = floatArrayOf(l + 120f, (l + r) / 2, r - 120f)
        val names = listOf("Ton PC", "Routeur", "Serveur")
        val icons = listOf(IconId.CPU, IconId.ROUTER, IconId.GLOBE)
        for (i in 0..1) g.line(xs[i] + 70f, cy, xs[i + 1] - 70f, cy, 4f, 0x55FFFFFF)
        for (i in 0..2) {
            val k = appear(t, 100f + i * 150f)
            g.withPose(Pose(alpha = clamp01(k), scale = lerp(0.8f, 1f, k)), xs[i], cy) {
                g.fillCircle(xs[i], cy, 62f, accent.withAlpha(0.18f))
                g.strokeCircle(xs[i], cy, 62f, 3f, accent)
                Icons.draw(g, icons[i], xs[i], cy, 40f, accent, 2.8f)
                centered(g, names[i], xs[i], cy + 108f, sf, ink, 190f)
            }
        }
        // le paquet va du PC au serveur en boucle ; le TTL baisse à chaque saut
        val period = 3600f
        val p = ((t - 500f).coerceAtLeast(0f) % period) / period
        if (t > 500f) {
            val sx = xs[0] + 70f; val ex = xs[2] - 70f
            val x = lerp(sx, ex, clamp01(p * 1.15f))
            val ttl = if (x < xs[1]) 64 else 63
            g.fillRoundRect(x - 52f, cy - 48f, x + 52f, cy - 12f, 8f, warn)
            val pf = FontSpec(FontFamily.MONO, 800, 20f)
            val s = "TTL $ttl"
            g.drawText(s, x - g.measureText(s, pf) / 2, cy - 22f, pf, rgb(0x14161C))
        }
        centered(g, "192.168.1.10  →  93.184.216.34", (l + r) / 2, cy + 190f, mf, muted, r - l - 40f)
        centered(g, "Chaque routeur retire 1 au TTL : à 0, le paquet est abandonné.", (l + r) / 2, cy + 250f, sf.withSize(22f), ink, r - l - 40f)
    }

    fun vlan(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top)
        val cx = (l + r) / 2
        val sy = top + 120f
        val v10 = accent; val v20 = warn
        val pcx = floatArrayOf(l + 110f, l + 330f, r - 330f, r - 110f)
        val pcy = top + 330f
        val cols = intArrayOf(v10, v10, v20, v20)
        // commutateur
        g.fillRoundRect(cx - 150f, sy - 40f, cx + 150f, sy + 40f, 14f, 0x33FFFFFF)
        g.strokeRoundRect(cx - 150f, sy - 40f, cx + 150f, sy + 40f, 14f, 3f, ink)
        centered(g, "Commutateur", cx, sy + 11f, sf, ink, 260f)
        for (i in 0..3) {
            g.line(pcx[i], pcy - 50f, cx + (i - 1.5f) * 80f, sy + 40f, 4f, cols[i].withAlpha(0.7f))
            g.fillCircle(pcx[i], pcy, 48f, cols[i].withAlpha(0.18f))
            g.strokeCircle(pcx[i], pcy, 48f, 3f, cols[i])
            Icons.draw(g, IconId.CPU, pcx[i], pcy, 28f, cols[i], 2.6f)
            centered(g, "PC ${i + 1}", pcx[i], pcy + 90f, sf.withSize(22f), ink, 160f)
        }
        centered(g, "VLAN 10", (pcx[0] + pcx[1]) / 2, pcy + 140f, tf.withSize(26f), v10, 300f)
        centered(g, "VLAN 20", (pcx[2] + pcx[3]) / 2, pcy + 140f, tf.withSize(26f), v20, 300f)
        // deux envois en boucle : PC1 → PC2 (même VLAN), PC1 → PC3 (autre VLAN)
        val c = (t % 6000f)
        val first = c < 3000f
        val dst = if (first) 1 else 2
        val p = ((c % 3000f) / 1500f).coerceIn(0f, 2f)
        val sxp = pcx[0]; val syp = pcy
        val hx = cx + (0 - 1.5f) * 80f
        val ex = cx + (dst - 1.5f) * 80f
        val (x, y) = when {
            p < 1f -> lerp(sxp, hx, p) to lerp(syp - 50f, sy + 40f, p)
            first -> lerp(hx, pcx[dst], p - 1f) to lerp(sy + 40f, pcy - 50f, p - 1f)
            else -> hx to sy + 40f
        }
        g.fillCircle(x, y, 14f, if (first || p < 1f) v10 else danger)
        if (!first && p >= 1f) {
            centered(g, "Bloqué : autre VLAN", cx, sy - 70f, tf.withSize(24f), danger, r - l - 40f)
        }
        if (first && p >= 1.9f) centered(g, "Livré : même VLAN", cx, sy - 70f, tf.withSize(24f), ok, r - l - 40f)
        centered(g, "Sans routeur, un VLAN ne voit pas les autres.", cx, pcy + 210f, sf.withSize(22f), ink, r - l - 40f)
    }

    // ───────────────────────────── Intelligence artificielle ─────────────────────────────

    fun prompt(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top)
        data class Row(val q: String, val a: String, val good: Boolean)
        val rows = listOf(
            Row("« Parle de sécurité »", "Réponse générale, peu utile", false),
            Row("« 3 règles MFA pour une PME »", "Réponse ciblée et exploitable", true),
        )
        val h = 190f; var y = top + 60f
        for ((i, row) in rows.withIndex()) {
            val start = 200f + i * 1300f
            val k = appear(t, start)
            if (k > 0f) g.withPose(Pose(alpha = clamp01(k), scale = lerp(0.9f, 1f, k)), (l + r) / 2, y + h / 2) {
                val col = if (row.good) ok else danger
                card(g, l + 6f, y, r - 6f, y + h, col.withAlpha(0.85f))
                left(g, "Prompt", l + 34f, y + 44f, sf.withSize(20f), muted, 200f)
                left(g, row.q, l + 34f, y + 86f, tf.withSize(28f), ink, r - l - 80f)
                val ax = clamp01((t - start - 350f) / 450f)
                g.line(l + 34f, y + 112f, lerp(l + 34f, l + 200f, ax), y + 112f, 4f, col)
                Icons.draw(g, IconId.BRAIN, l + 250f, y + 112f, 22f, col, 2.4f)
                if (t > start + 900f) {
                    left(g, "Réponse", l + 34f, y + 142f, sf.withSize(20f), muted, 200f)
                    left(g, row.a, l + 34f, y + 176f, tf.withSize(28f), col, r - l - 80f)
                }
            }
            y += h + 36f
        }
        centered(g, "Plus ton prompt est précis, plus la réponse l'est.", (l + r) / 2, y + 30f, sf.withSize(22f), ink, r - l - 40f)
    }

    // ───────────────────────────── Développement ─────────────────────────────

    fun editor(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top)
        val ey = top + 52f; val eb = ey + 330f
        g.fillRoundRect(l + 6f, ey, r - 6f, eb, 14f, rgb(0x0A0F1C))
        g.strokeRoundRect(l + 6f, ey, r - 6f, eb, 14f, 1.5f, 0x2AFFFFFF)
        listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).forEachIndexed { i, c -> g.fillCircle(l + 32f + i * 22f, ey + 24f, 7f, c) }
        left(g, "app.py", l + 130f, ey + 31f, sf.withSize(20f), muted, 200f)
        val c = (t % 6000f)
        val buggy = c < 3000f
        val fn = if (buggy) "totl" else "total"
        val code = listOf("def total(prix, qte):", "    return prix * qte", "", "print($fn(2, 3))")
        val typed = clamp01(c / 1200f)
        val shown = if (c < 3000f) typed else 1f
        val total = code.sumOf { it.length }
        var budget = (total * shown).toInt()
        for ((i, line) in code.withIndex()) {
            val n = budget.coerceIn(0, line.length); budget -= line.length
            if (n > 0 || line.isEmpty()) {
                val txt = line.take(n)
                g.drawText("${i + 1}", l + 30f, ey + 92f + i * 40f, mf.withSize(20f), muted.withAlpha(0.7f))
                g.drawText(txt, l + 66f, ey + 92f + i * 40f, mf, if (i == 3 && !buggy) ok else ink)
            }
        }
        // bouton exécuter + sortie
        val runAt = if (buggy) 1500f else 4200f
        val bx = r - 190f
        g.fillRoundRect(bx, ey + 250f, bx + 160f, ey + 300f, 10f, if (c > runAt) accent else accent.withAlpha(0.45f))
        g.drawText("▶ Exécuter", bx + 14f, ey + 284f, FontSpec(FontFamily.TEXT, 800, 22f), rgb(0x0A0F1C))
        val outTop = eb + 24f
        g.fillRoundRect(l + 6f, outTop, r - 6f, outTop + 120f, 12f, 0x22000000)
        if (buggy && c > 2000f) {
            left(g, "NameError: name 'totl' is not defined", l + 30f, outTop + 52f, mf.withSize(24f), danger, r - l - 60f)
            if (c > 2500f) left(g, "→ une faute de frappe dans le nom", l + 30f, outTop + 96f, sf.withSize(22f), warn, r - l - 60f)
        } else if (!buggy && c > 4800f) {
            left(g, "6", l + 30f, outTop + 52f, mf.withSize(30f), ok, r - l - 60f)
            left(g, "✓ Corrigé : le programme s'exécute", l + 30f, outTop + 96f, sf.withSize(22f), ok, r - l - 60f)
        }
    }

    // ───────────────────────────── Santé ─────────────────────────────

    fun pulse(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top, "ILLUSTRATION")
        val cy = top + 160f
        val beat = (t % 900f) / 900f
        val s = 1f + 0.14f * sin(beat * PI.toFloat()).coerceAtLeast(0f) * (if (beat < 0.5f) 1f else 0f)
        val hx = l + 110f
        g.withPose(Pose(scale = s), hx, cy) { Icons.draw(g, IconId.HEART, hx, cy, 56f, danger, 3.2f) }
        // ligne de pouls défilante
        val x0 = hx + 90f; val x1 = r - 30f
        var px = x0; var py = cy
        val period = 150f
        var x = x0
        while (x < x1) {
            val ph = (((x - x0) + t * 0.25f) % period) / period
            val y = cy + when {
                ph < 0.30f -> 0f
                ph < 0.38f -> -18f * ((ph - 0.30f) / 0.08f)
                ph < 0.46f -> -18f + 100f * ((ph - 0.38f) / 0.08f)
                ph < 0.54f -> 82f - 110f * ((ph - 0.46f) / 0.08f)
                ph < 0.62f -> -28f + 28f * ((ph - 0.54f) / 0.08f)
                else -> 0f
            }
            if (x > x0) g.line(px, py, x, y, 4f, ok)
            px = x; py = y; x += 6f
        }
        // quatre habitudes
        val items = listOf(IconId.LEAF to "Alimentation", IconId.DROP to "Hydratation", IconId.MOON to "Sommeil", IconId.SUN to "Activité")
        val gy = cy + 250f
        val cw = (r - l - 20f) / 4f
        for ((i, it) in items.withIndex()) {
            val k = appear(t, 400f + i * 220f)
            val cx = l + 10f + cw * (i + 0.5f)
            g.withPose(Pose(alpha = clamp01(k), scale = lerp(0.8f, 1f, k)), cx, gy) {
                g.fillCircle(cx, gy, 54f, accent.withAlpha(0.16f))
                g.strokeCircle(cx, gy, 54f, 3f, accent)
                Icons.draw(g, it.first, cx, gy, 32f, accent, 2.8f)
                centered(g, it.second, cx, gy + 100f, sf.withSize(22f), ink, cw - 8f)
            }
        }
    }

    // ───────────────────────────── Hameçonnage ─────────────────────────────

    fun phishing(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top)
        val w = r - l - 12f
        // 1. SMS
        val k1 = appear(t, 150f)
        val y1 = top + 56f
        g.withPose(Pose(alpha = clamp01(k1), scale = lerp(0.92f, 1f, k1)), (l + r) / 2, y1 + 70f) {
            card(g, l + 6f, y1, r - 6f, y1 + 150f, warn)
            Icons.draw(g, IconId.MAIL, l + 54f, y1 + 52f, 26f, warn, 2.6f)
            left(g, "SMS d'un numéro inconnu", l + 100f, y1 + 48f, sf.withSize(20f), muted, w - 120f)
            left(g, "Colis bloqué, paie 1,99 € ici :", l + 34f, y1 + 94f, tf.withSize(26f), ink, w - 50f)
            left(g, "http://colis-suivi.example/x", l + 34f, y1 + 134f, mf.withSize(24f), danger, w - 50f)
        }
        // 2. faux site
        val y2 = y1 + 186f
        val k2 = appear(t, 1300f)
        if (k2 > 0f) g.withPose(Pose(alpha = clamp01(k2), scale = lerp(0.92f, 1f, k2)), (l + r) / 2, y2 + 110f) {
            card(g, l + 6f, y2, r - 6f, y2 + 240f, danger)
            left(g, "Faux site : imite un vrai service", l + 34f, y2 + 42f, sf.withSize(20f), muted, w - 50f)
            val n = ((t - 2000f) / 70f).toInt().coerceIn(0, 12)
            g.fillRoundRect(l + 34f, y2 + 62f, r - 34f, y2 + 112f, 8f, 0x22FFFFFF)
            left(g, "N° de carte : " + "4".repeat(n.coerceAtMost(4)) + "•".repeat((n - 4).coerceIn(0, 8)), l + 48f, y2 + 96f, mf.withSize(22f), ink, w - 90f)
            g.fillRoundRect(l + 34f, y2 + 128f, r - 34f, y2 + 178f, 8f, 0x22FFFFFF)
            left(g, "Code reçu par SMS : ••••••", l + 48f, y2 + 162f, mf.withSize(22f), ink, w - 90f)
            if (t > 3100f) {
                val kk = appear(t, 3100f)
                g.withPose(Pose(alpha = clamp01(kk), scale = lerp(1.3f, 1f, kk)), (l + r) / 2, y2 + 200f) {
                    centered(g, "Données envoyées à l'attaquant", (l + r) / 2, y2 + 214f, tf.withSize(24f), danger, w - 40f)
                }
            }
        }
        centered(g, "Vérifie l'expéditeur : ne clique pas.", (l + r) / 2, y2 + 296f, sf.withSize(22f), ink, w - 40f)
    }

    // ───────────────────────────── Pare-feu ─────────────────────────────

    /** Trafic réseau filtré : les paquets conformes aux règles passent, les autres sont refusés. Règles d'exemple, étiquetées. */
    fun firewall(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top, "SIMULATION · RÈGLES D'EXEMPLE")
        val cy = top + 190f
        val wx = (l + r) / 2
        centered(g, "Internet", l + 100f, cy - 120f, sf, muted, 170f)
        centered(g, "Réseau interne", r - 110f, cy - 120f, sf, muted, 200f)
        // le mur
        val kw = appear(t, 100f)
        g.withPose(Pose(alpha = clamp01(kw), scale = lerp(0.8f, 1f, kw)), wx, cy) {
            g.fillRoundRect(wx - 30f, cy - 100f, wx + 30f, cy + 100f, 12f, accent.withAlpha(0.35f))
            g.strokeRoundRect(wx - 30f, cy - 100f, wx + 30f, cy + 100f, 12f, 3f, accent)
            Icons.draw(g, IconId.SHIELD, wx, cy, 24f, accent, 2.6f)
        }
        // trois paquets : passe, passe, refusé, en boucle
        val period = 4200f
        val lanes = listOf(cy - 60f to true, cy to false, cy + 60f to true)
        for ((i, lane) in lanes.withIndex()) {
            val u = (((t - 600f - i * 450f).coerceAtLeast(0f)) % period) / period
            if (t < 600f + i * 450f) continue
            val startX = l + 150f; val endX = r - 150f
            val (ly, ok1) = lane
            val x = if (u < 0.5f) lerp(startX, wx - 50f, u / 0.5f)
                else if (ok1) lerp(wx - 50f, endX, (u - 0.5f) / 0.5f)
                else lerp(wx - 50f, wx - 130f, ((u - 0.5f) / 0.25f).coerceAtMost(1f))
            val alpha = if (!ok1 && u > 0.75f) 1f - (u - 0.75f) / 0.25f else 1f
            g.fillRoundRect(x - 26f, ly - 16f, x + 26f, ly + 16f, 6f, (if (ok1) ok else danger).withAlpha(alpha))
            if (!ok1 && u > 0.5f) g.drawText("✕", wx - 24f, ly + 9f, FontSpec(FontFamily.TEXT, 800, 28f), danger.withAlpha(alpha))
        }
        // règles
        val ky = appear(t, 1500f)
        g.withPose(Pose(alpha = clamp01(ky)), wx, cy + 170f) {
            left(g, "443  HTTPS  →  autorisé", l + 60f, cy + 170f, mf.withSize(26f), ok, r - l - 100f)
            left(g, "23   Telnet →  refusé", l + 60f, cy + 214f, mf.withSize(26f), danger, r - l - 100f)
        }
        centered(g, "Le pare-feu compare chaque paquet à ses règles.", wx, cy + 275f, sf.withSize(22f), ink, r - l - 40f)
    }

    // ───────────────────────────── Marketing : tunnel ─────────────────────────────

    /** Tunnel de conversion générique : le public se réduit à chaque étage. Aucune valeur chiffrée. */
    fun funnel(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top, "SCHÉMA · SANS CHIFFRES")
        val labels = listOf("Visiteurs", "Contacts", "Clients", "Clients fidèles")
        val cx = (l + r) / 2
        val maxW = r - l - 60f
        for (i in labels.indices) {
            val k = appear(t, 150f + i * 450f, 500f)
            val wTop = maxW * (1f - i * 0.2f); val wBot = maxW * (1f - (i + 1) * 0.2f)
            val y0 = top + 70f + i * 96f; val y1 = y0 + 84f
            g.withPose(Pose(alpha = clamp01(k), dy = (1 - k) * -30f), cx, y0) {
                val p = PathData().moveTo(cx - wTop / 2, y0).lineTo(cx + wTop / 2, y0).lineTo(cx + wBot / 2, y1).lineTo(cx - wBot / 2, y1).close()
                g.fillPath(p, (if (i % 2 == 0) accent else warn).withAlpha(0.35f))
                g.strokePath(p, 3f, if (i % 2 == 0) accent else warn)
                centered(g, labels[i], cx, y0 + 54f, tf.withSize(28f), ink, wBot - 30f)
            }
        }
        centered(g, "Chaque étape retient une partie du public précédent.", cx, top + 70f + 4 * 96f + 40f, sf.withSize(22f), ink, r - l - 40f)
    }

    // ───────────────────────────── API REST ─────────────────────────────

    /** Échange client-serveur : une requête, une réponse. Exemple écrit à la main, jamais exécuté. */
    fun api(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top, "SIMULATION · EXEMPLE, NON EXÉCUTÉ")
        val cy = top + 130f
        val lx = l + 110f; val rx = r - 110f
        for ((i, p) in listOf(lx to "Client", rx to "Serveur").withIndex()) {
            val k = appear(t, 100f + i * 150f)
            g.withPose(Pose(alpha = clamp01(k), scale = lerp(0.8f, 1f, k)), p.first, cy) {
                g.fillCircle(p.first, cy, 62f, accent.withAlpha(0.18f)); g.strokeCircle(p.first, cy, 62f, 3f, accent)
                Icons.draw(g, if (i == 0) IconId.CODE else IconId.CPU, p.first, cy, 38f, accent, 2.8f)
                centered(g, p.second, p.first, cy + 106f, sf, ink, 190f)
            }
        }
        val mid = (lx + rx) / 2
        val k1 = appear(t, 900f)
        g.withPose(Pose(alpha = clamp01(k1)), mid, cy) {
            g.line(lx + 80f, cy - 14f, rx - 80f, cy - 14f, 4f, warn)
            g.line(rx - 80f, cy - 14f, rx - 100f, cy - 28f, 4f, warn); g.line(rx - 80f, cy - 14f, rx - 100f, cy + 0f, 4f, warn)
        }
        val k2 = appear(t, 2200f)
        g.withPose(Pose(alpha = clamp01(k2)), mid, cy) {
            g.line(rx - 80f, cy + 22f, lx + 80f, cy + 22f, 4f, ok)
            g.line(lx + 80f, cy + 22f, lx + 100f, cy + 8f, 4f, ok); g.line(lx + 80f, cy + 22f, lx + 100f, cy + 36f, 4f, ok)
        }
        val w = r - l - 12f
        val ky = cy + 150f
        val ka = appear(t, 900f)
        g.withPose(Pose(alpha = clamp01(ka), dy = (1 - ka) * 20f), mid, ky) {
            card(g, l + 6f, ky, r - 6f, ky + 100f, warn)
            left(g, "Requête", l + 30f, ky + 34f, sf.withSize(20f), muted, w - 40f)
            left(g, "GET /api/users/1 HTTP/1.1", l + 30f, ky + 78f, mf.withSize(26f), ink, w - 50f)
        }
        val ky2 = ky + 124f
        val kb = appear(t, 2200f)
        g.withPose(Pose(alpha = clamp01(kb), dy = (1 - kb) * 20f), mid, ky2) {
            card(g, l + 6f, ky2, r - 6f, ky2 + 150f, ok)
            left(g, "Réponse", l + 30f, ky2 + 34f, sf.withSize(20f), muted, w - 40f)
            left(g, "200 OK", l + 30f, ky2 + 76f, mf.withSize(26f), ok, w - 50f)
            left(g, "{ \"id\": 1, \"nom\": \"Exemple\" }", l + 30f, ky2 + 120f, mf.withSize(24f), ink, w - 50f)
        }
    }

    // ───────────────────────────── Habitudes : progression ─────────────────────────────

    /** Progression jour après jour : des points qui se remplissent. Illustration, aucune donnée ni promesse. */
    fun streak(g: Gfx, t: Float, l: Float, top: Float, r: Float, b: Float) {
        tag(g, l + 10f, top, "ILLUSTRATION")
        val cx = (l + r) / 2
        val cols = 7; val rows = 3
        val gap = (r - l - 120f) / (cols - 1)
        val rad = (gap * 0.32f).coerceAtMost(34f)
        var n = 0
        for (row in 0 until rows) for (c in 0 until cols) {
            val x = l + 60f + c * gap
            val y = top + 120f + row * (rad * 2 + 46f)
            val on = appear(t, 300f + n * 120f, 300f)
            g.strokeCircle(x, y, rad, 3f, accent.withAlpha(0.5f))
            if (on > 0f) g.fillCircle(x, y, rad * clamp01(on), accent)
            n++
        }
        val my = top + 120f + rows * (rad * 2 + 46f) + 30f
        centered(g, "Petit à petit, la progression devient visible.", cx, my, sf.withSize(24f), ink, r - l - 40f)
    }
}
