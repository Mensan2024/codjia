// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Phase D.2 — plans 3D animés pour l'informatique : routage OSPF, annuaire Active Directory, pare-feu, rançongiciel.
 * Chaque plan ne sert que le sujet dont il parle (`needs` = indice de sujet obligatoire) : un plan OSPF ne s'affiche jamais pour un autre texte.
 * Les noms et adresses affichés sont fictifs (domaine « entreprise.example », RFC 2606).
 */
internal object Scenes3DNet {
    private val BODY = SitKit.BODY + setOf(Role.RECAP)
    private val MECH = setOf(Goal.COMPRENDRE_MECANISME, Goal.VOIR_ARCHITECTURE, Goal.SUIVRE_PROCEDURE)

    private fun bg(c: StageCtx) {
        val th = c.th
        c.g.fillVerticalGradient(0f, 0f, 1000f, 700f, mix(th.bgTop, th.bgBottom, 0.25f), th.bgBottom)
    }

    private fun caption(c: StageCtx, s: String) = SK.pill(c.g, s, 500f, 46f, c.th.ink, c.th.bgTop.withAlpha(0.9f), 24f)

    private fun step(c: StageCtx, labels: List<String>, active: Int, y: Float = 650f) {
        val n = labels.size; val w = 1000f / n
        for ((i, s) in labels.withIndex()) {
            val on = i == active
            val col = if (on) c.th.ink else c.th.muted
            SK.pill(c.g, s, w * (i + 0.5f), y, col, if (on) c.th.accent.withAlpha(0.55f) else c.th.bgTop.withAlpha(0.8f), 19f, if (i <= active) 1f else 0.55f)
        }
    }

    private fun view(c: StageCtx, yaw: Float, pitch: Float, scale: Float, cy: Float = 390f) =
        Net3D.View(c, yaw + 0.10f * sin(c.tg / 5200f), pitch, scale, cy)

    private fun lerpPt(x0: Float, z0: Float, x1: Float, z1: Float, u: Float) = lerp(x0, x1, u) to lerp(z0, z1, u)

    // ───────────────────────── OSPF ─────────────────────────

    private class Node(val name: String, val x: Float, val z: Float)
    private val R = listOf(Node("R1", -4.2f, -1.7f), Node("R2", 4.2f, -1.7f), Node("R3", 4.2f, 2.5f), Node("R4", -4.2f, 2.5f))
    /** Liens (indices dans [R]) et coûts : R1-R2 10, R2-R3 10, R3-R4 20, R4-R1 10, diagonale R1-R3 40. */
    private val E = listOf(Triple(0, 1, 10), Triple(1, 2, 10), Triple(2, 3, 20), Triple(3, 0, 10), Triple(0, 2, 40))

    private fun ospfBase(c: StageCtx, glow: FloatArray = FloatArray(4) { 0.3f }): Net3D.View {
        val v = view(c, 0f, 0.82f, 90f, 385f)
        v.grid(6f, 4f)
        return v.also { vv -> for ((i, n) in R.withIndex()) Net3D.router(vv, n.x, n.z, glow[i]) }
    }

    private fun names(v: Net3D.View) { for (n in R) v.tag(n.name, n.x, 1.1f, n.z, v.th.ink, 22f) }

    private val ospfOverview = beat("ospf-overview", "Des routeurs qui partagent une carte du réseau", BODY + setOf(Role.DEFINE, Role.INTRO), MECH + setOf(Goal.MEMORISER),
        emptySet(), repeatable = true, minMs = 3500, shows = setOf("topic:ospf-map"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = ospfBase(c, FloatArray(4) { 0.25f + 0.25f * (0.5f + 0.5f * sin(t / 600f + it * 1.6f)) })
        for ((a, b, _) in E) v.link(R[a].x, R[a].z, R[b].x, R[b].z, th.line, 0.9f)
        // trafic : R1 → R2 → R3 en continu
        for (k in 0 until 3) {
            val u = SK.loop(t, 2600f, k * 870f)
            val seg = if (u < 0.5f) 0 else 1
            val (x, z) = if (seg == 0) lerpPt(R[0].x, R[0].z, R[1].x, R[1].z, u * 2f) else lerpPt(R[1].x, R[1].z, R[2].x, R[2].z, (u - 0.5f) * 2f)
            Net3D.packet(v, x, 0.5f, z, th.accent)
        }
        v.draw(); names(v)
        v.tag("Aire 0", 0f, 0.1f, 0.4f, th.muted, 20f, 0.9f)
        caption(c, "OSPF : un protocole à état de liens")
    }

    private val ospfHello = beat("ospf-hello", "Les paquets Hello : se découvrir entre voisins", BODY, MECH,
        cueSet(Actor.ROUTEUR, Thing.PAQUET) + setOf("topic:ospf-hello"), minMs = 6000, needs = setOf("topic:ospf-hello"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val up = BooleanArray(E.size) { t > 700f + it * 900f + 1100f }
        val v = ospfBase(c)
        var adj = 0
        for ((i, e) in E.withIndex()) {
            if (up[i]) adj++
            v.link(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, if (up[i]) th.good else th.line, if (up[i]) 1f else 0.7f)
        }
        for ((i, e) in E.withIndex()) {
            val s = 700f + i * 900f
            if (t in s..(s + 1100f)) {
                val u = (t - s) / 1100f
                val (x, z) = lerpPt(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, u)
                val (x2, z2) = lerpPt(R[e.second].x, R[e.second].z, R[e.first].x, R[e.first].z, u)
                Net3D.packet(v, x, 0.55f, z, th.accent); Net3D.packet(v, x2, 0.55f, z2, th.accent)
                v.tag("Hello", (x + x2) / 2f, 1.0f, (z + z2) / 2f, th.accent, 18f)
            }
        }
        v.draw(); names(v)
        SK.pill(c.g, "Voisins découverts : $adj / ${E.size}", 500f, 650f, th.ink, th.bgTop.withAlpha(0.9f), 22f)
        caption(c, "1 · Les routeurs se découvrent (Hello)")
    }

    private val ospfLsa = beat("ospf-lsa", "Les LSA : chacun annonce l'état de ses liens", BODY, MECH,
        cueSet(Actor.ROUTEUR, Thing.PAQUET) + setOf("topic:ospf-lsa"), minMs = 6000, needs = setOf("topic:ospf-lsa"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        // Chaque routeur émet son LSA à son tour ; une copie atteint un voisin après 900 ms et est retransmise.
        val emit = floatArrayOf(500f, 2300f, 4100f, 5900f)
        val known = IntArray(4)
        for (o in 0 until 4) for (r in 0 until 4) {
            val dist = hops(o, r)
            if (t > emit[o] + 900f * dist) known[r]++
        }
        val glow = FloatArray(4) { 0.25f + 0.2f * known[it] }
        val v = ospfBase(c, glow)
        for ((a, b, _) in E) v.link(R[a].x, R[a].z, R[b].x, R[b].z, th.good, 0.85f)
        for (o in 0 until 4) for ((a, b, _) in E) {
            for ((from, to) in listOf(a to b, b to a)) {
                val d0 = emit[o] + 900f * hops(o, from)
                if (hops(o, to) == hops(o, from) + 1 && t in d0..(d0 + 900f)) {
                    val (x, z) = lerpPt(R[from].x, R[from].z, R[to].x, R[to].z, (t - d0) / 900f)
                    Net3D.packet(v, x, 0.6f, z, th.warn)
                }
            }
        }
        // la base d'états de liens : une pile de plaques par LSA connu
        for (r in 0 until 4) for (k in 0 until known[r]) v.add(Solids.box("lsdb", R[r].x, 0.75f + k * 0.12f, R[r].z, 0.7f, 0.08f, 0.5f, th.good, 0.6f))
        v.draw(); names(v)
        v.tag("LSA", R[0].x + 0.8f, 1.5f, R[0].z, th.warn, 18f, SK.ease(t, 500f, 400f) * (1f - SK.ease(t, 3000f, 600f)))
        SK.pill(c.g, "LSA connus par R1 : ${known[0]} / 4", 500f, 650f, th.ink, th.bgTop.withAlpha(0.9f), 22f)
        caption(c, "2 · Chacun annonce l'état de ses liens (LSA)")
    }

    /** Distance en sauts entre deux routeurs de l'anneau avec la diagonale R1-R3. */
    private fun hops(a: Int, b: Int): Int {
        if (a == b) return 0
        val adj = E.map { it.first to it.second }
        var frontier = setOf(a); var seen = setOf(a); var d = 0
        while (b !in frontier && d < 6) {
            frontier = frontier.flatMap { n -> adj.filter { it.first == n || it.second == n }.map { if (it.first == n) it.second else it.first } }.toSet() - seen
            seen = seen + frontier; d++
        }
        return d
    }

    private val ospfSpf = beat("ospf-spf", "Le plus court chemin, puis la panne et le recalcul", BODY, MECH,
        cueSet(Actor.ROUTEUR, Thing.PAQUET) + setOf("topic:ospf-spf"), repeatable = true, minMs = 8000, needs = setOf("topic:ospf-spf"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val failAt = 4600f
        val failed = t > failAt
        val v = ospfBase(c)
        // chemin retenu : R1-R2-R3 (coût 20) ; après la panne de R2-R3 : R1-R4-R3 (coût 30)
        val path = if (failed && t > failAt + 1500f) listOf(0, 3, 2) else listOf(0, 1, 2)
        val phase = SK.ease(t, 300f, 2200f)
        for ((i, e) in E.withIndex()) {
            val dead = failed && i == 1
            val onPath = (0 until path.size - 1).any { k -> (e.first == path[k] && e.second == path[k + 1]) || (e.second == path[k] && e.first == path[k + 1]) }
            val col = when { dead -> th.bad; onPath && phase > 0.9f -> th.good; phase > 0.2f -> mix(th.line, th.warn, 0.5f); else -> th.line }
            v.link(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, col, 1f, if (onPath && phase > 0.9f && !dead) 10f else 7f)
            val mx = (R[e.first].x + R[e.second].x) / 2f; val mz = (R[e.first].z + R[e.second].z) / 2f
            v.tag(if (dead) "coupé" else "${e.third}", mx, 0.35f, mz + if (i == 4) 0.5f else 0f, if (dead) th.bad else th.ink, 17f, SK.ease(t, 300f + i * 250f, 400f))
        }
        // paquet sur le chemin retenu
        val seg = path.size - 1
        val u = SK.loop(t, 3000f) * seg
        val k = u.toInt().coerceAtMost(seg - 1)
        val (x, z) = lerpPt(R[path[k]].x, R[path[k]].z, R[path[k + 1]].x, R[path[k + 1]].z, u - k)
        if (phase > 0.9f) Net3D.packet(v, x, 0.6f, z, th.good)
        v.draw(); names(v)
        val msg = when {
            t < 300f -> ""
            !failed -> "Meilleur chemin R1 → R3 : coût 20"
            t < failAt + 1500f -> "Lien R2–R3 coupé : nouveau calcul SPF…"
            else -> "Nouveau chemin R1 → R3 : coût 30"
        }
        if (msg.isNotEmpty()) SK.pill(c.g, msg, 500f, 650f, if (failed) th.warn else th.ink, th.bgTop.withAlpha(0.92f), 22f)
        caption(c, "3 · Le plus court chemin, et la panne")
    }

    private val ospfCosts = beat("ospf-costs", "Chaque lien a un coût, le chemin retenu est le moins cher", BODY, MECH,
        emptySet(), repeatable = true, minMs = 3500, shows = setOf("topic:ospf-cost"), excludes = setOf("topic:ospf-spf"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = ospfBase(c)
        val best = listOf(0, 1)
        for ((i, e) in E.withIndex()) {
            val on = i in best
            v.link(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, if (on) th.good else th.line, 1f, if (on) 10f else 7f)
            v.tag("${e.third}", (R[e.first].x + R[e.second].x) / 2f, 0.35f, (R[e.first].z + R[e.second].z) / 2f + if (i == 4) 0.5f else 0f, th.ink, 18f, SK.ease(t, 200f + i * 220f, 400f))
        }
        val u = SK.loop(t, 2800f) * 2f; val k = u.toInt().coerceAtMost(1)
        val (x, z) = lerpPt(R[best[k]].x, R[best[k]].z, R[best[k] + 1].x, R[best[k] + 1].z, u - k)
        Net3D.packet(v, x, 0.6f, z, th.good)
        v.draw(); names(v)
        SK.pill(c.g, "R1 → R3 : 10 + 10 = 20 (le plus bas)", 500f, 650f, th.good, th.bgTop.withAlpha(0.92f), 22f, SK.ease(t, 1400f, 500f))
        caption(c, "Le coût de chaque lien")
    }

    private val ospfPoster = beat("poster", "Affiche : un réseau de routeurs qui se parlent", setOf(Role.INTRO, Role.RECAP, Role.DEFINE), setOf(Goal.IDENTIFIER_MENACE, Goal.MEMORISER, Goal.COMPRENDRE_MECANISME),
        emptySet(), repeatable = true, minMs = 1500, onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = Net3D.View(c, 0.35f * sin(c.tg / 3600f), 0.7f, 96f, 360f)
        v.grid(6f, 4f)
        for ((a, b, _) in E) v.link(R[a].x, R[a].z, R[b].x, R[b].z, th.accent, 0.8f, 8f)
        for ((i, n) in R.withIndex()) Net3D.router(v, n.x, n.z, 0.5f + 0.4f * sin(t / 500f + i))
        for (k in 0 until 4) { val e = E[k]; val u = SK.loop(t, 2200f, k * 550f); val (x, z) = lerpPt(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, u); Net3D.packet(v, x, 0.6f, z, th.warn) }
        v.draw()
    }


    /**
     * Phase en cours d'un plan qui illustre `n` énoncés lus l'un après l'autre : le découpage suit la longueur des puces de la scène
     * (la voix les lit dans l'ordre, au prorata de leur longueur), pour que l'image change quand la voix change de sujet.
     * Retourne (numéro de phase, temps écoulé dans la phase en ms).
     */
    private fun phaseOf(c: StageCtx, n: Int): Pair<Int, Float> {
        val lead = 1100f; val tail = 700f
        val span = (c.scene.durationMs - lead - tail).coerceAtLeast(1500f)
        val w = if (c.scene.items.size == n) c.scene.items.map { it.length.toFloat() + 6f } else List(n) { 1f }
        val tot = w.sum()
        var acc = lead
        for (i in 0 until n) {
            val d = span * w[i] / tot
            if (c.t < acc + d || i == n - 1) return i to (c.t - acc).coerceAtLeast(0f)
            acc += d
        }
        return 0 to 0f
    }

    private fun failedLink(v: Net3D.View, i: Int, th: StageTheme, a: Float) {
        val e = E[i]
        v.link(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, mix(th.line, th.bad, a), 1f, 7f)
    }

    private val ospfVersions = beat("ospf-versions", "OSPFv2 route l'IPv4, OSPFv3 route l'IPv6", BODY, MECH,
        cueSet(Actor.ROUTEUR) + setOf("topic:ospf-ver"), minMs = 5000, needs = setOf("topic:ospf-ver"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.8f, 92f, 360f)
        v.grid(6f, 3.5f)
        val ax = -3.6f; val bx = 3.6f; val z2 = -1.3f; val z3 = 1.3f
        Net3D.router(v, ax, 0f, 0.5f); Net3D.router(v, bx, 0f, 0.5f)
        v.link(ax, z2, bx, z2, th.accent, SK.ease(t, 300f, 600f), 8f)
        v.link(ax, z3, bx, z3, th.warn, SK.ease(t, 2400f, 600f), 8f)
        for (k in 0 until 3) {
            val u = SK.loop(t, 2400f, k * 800f)
            if (t > 900f) Net3D.packet(v, lerp(ax, bx, u), 0.5f, z2, th.accent)
            if (t > 2900f) Net3D.packet(v, lerp(ax, bx, u), 0.5f, z3, th.warn)
        }
        v.draw()
        v.tag("R1", ax, 1.1f, 0f, th.ink, 22f); v.tag("R2", bx, 1.1f, 0f, th.ink, 22f)
        v.tag("OSPFv2 · routes IPv4", 0f, 0.4f, z2 - 0.55f, th.accent, 20f, SK.ease(t, 600f, 500f))
        v.tag("OSPFv3 · routes IPv6", 0f, 0.4f, z3 + 0.55f, th.warn, 20f, SK.ease(t, 2700f, 500f))
        SK.pill(c.g, "IPv4 : 192.0.2.0/24", 250f, 575f, th.accent, th.bgTop.withAlpha(0.9f), 20f, SK.ease(t, 900f, 500f))
        SK.pill(c.g, "IPv6 : 2001:db8::/32", 750f, 575f, th.warn, th.bgTop.withAlpha(0.9f), 20f, SK.ease(t, 3000f, 500f))
        SK.pill(c.g, "Protocole ouvert, normalisé par l'IETF (RFC 2328, RFC 5340)", 500f, 650f, th.ink, th.accent.withAlpha(0.45f), 21f, SK.ease(t, 4800f, 600f))
        caption(c, "Deux versions, un même principe")
    }

    private val ospfBenefits = beat("ospf-benefits", "À quoi sert OSPF : routes automatiques, reprise sur panne, aires", BODY, MECH + setOf(Goal.MEMORISER),
        emptySet(), minMs = 7000, needs = setOf("topic:purpose"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val th = c.th
        bg(c)
        val (ph, lt) = phaseOf(c, 3)
        val v = ospfBase(c)
        when (ph) {
            0 -> { // les liens se découvrent un à un, sans rien saisir à la main
                for ((i, e) in E.withIndex()) {
                    val a = SK.ease(lt, 200f + i * 450f, 500f)
                    v.link(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, mix(th.line, th.good, a), 0.45f + 0.55f * a, 7f)
                }
                val u = SK.loop(lt, 1800f)
                Net3D.packet(v, lerp(R[0].x, R[1].x, u), 0.55f, lerp(R[0].z, R[1].z, u), th.accent)
            }
            1 -> { // un lien tombe, un autre chemin est calculé
                val cut = lt > 1200f
                for ((i, e) in E.withIndex()) {
                    val onNew = cut && lt > 2600f && (i == 2 || i == 3)
                    val col = if (cut && i == 1) th.bad else if (onNew) th.good else if (!cut && (i == 0 || i == 1)) th.good else th.line
                    v.link(R[e.first].x, R[e.first].z, R[e.second].x, R[e.second].z, col, 1f, if (col == th.line) 6f else 9f)
                }
                val path = if (cut && lt > 2600f) listOf(0, 3, 2) else listOf(0, 1, 2)
                if (!(cut && lt <= 2600f)) {
                    val u = SK.loop(lt, 2400f) * 2f; val k = u.toInt().coerceAtMost(1)
                    Net3D.packet(v, lerp(R[path[k]].x, R[path[k + 1]].x, u - k), 0.6f, lerp(R[path[k]].z, R[path[k + 1]].z, u - k), th.good)
                }
                if (cut) v.tag("coupé", (R[1].x + R[2].x) / 2f, 0.4f, (R[1].z + R[2].z) / 2f, th.bad, 18f)
            }
            else -> { // le réseau est découpé en aires : chacune a une table plus petite
                for ((a, b, _) in E) v.link(R[a].x, R[a].z, R[b].x, R[b].z, th.line, 0.9f)
                val z0 = SK.ease(lt, 200f, 700f); val z1 = SK.ease(lt, 1200f, 700f)
                val p0 = v.pt(-4.2f, 0f, -1.7f); val p1 = v.pt(4.2f, 0f, -1.7f)
                val q0 = v.pt(-4.2f, 0f, 2.5f); val q1 = v.pt(4.2f, 0f, 2.5f)
                // Aire 0 : routeurs du haut ; aire 1 : routeurs du bas.
                if (z0 > 0.01f) { c.g.fillRoundRect(p0[0] - 80f, p0[1] - 86f, p1[0] + 80f, p1[1] + 34f, 26f, th.accent.withAlpha(0.14f * z0)); c.g.strokeRoundRect(p0[0] - 80f, p0[1] - 86f, p1[0] + 80f, p1[1] + 34f, 26f, 3f, th.accent.withAlpha(0.8f * z0)) }
                if (z1 > 0.01f) { c.g.fillRoundRect(q0[0] - 80f, q0[1] - 70f, q1[0] + 80f, q1[1] + 50f, 26f, th.warn.withAlpha(0.14f * z1)); c.g.strokeRoundRect(q0[0] - 80f, q0[1] - 70f, q1[0] + 80f, q1[1] + 50f, 26f, 3f, th.warn.withAlpha(0.8f * z1)) }
            }
        }
        v.draw(); names(v)
        if (ph == 2) {
            val lt2 = lt
            SK.pill(c.g, "Aire 0", 880f, 560f, th.accent, th.bgTop.withAlpha(0.92f), 22f, SK.ease(lt2, 300f, 400f))
            SK.pill(c.g, "Aire 1", 880f, 130f, th.warn, th.bgTop.withAlpha(0.92f), 22f, SK.ease(lt2, 1300f, 400f))
        }
        step(c, listOf("1 · Routes automatiques", "2 · Reprise sur panne", "3 · Aires"), ph)
        caption(c, listOf("Les routes se découvrent toutes seules", "Un lien tombe : un autre chemin est calculé", "Le réseau découpé en aires")[ph])
    }

    private val ospfDr = beat("ospf-dr", "Réseau partagé : le DR et le BDR limitent les adjacences", BODY, MECH,
        cueSet(Actor.ROUTEUR) + setOf("topic:ospf-dr"), minMs = 6000, needs = setOf("topic:ospf-dr"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.86f, 84f, 365f)
        v.grid(6f, 3.6f)
        val n = 6
        val xs = FloatArray(n) { 4.3f * cos(2f * PI.toFloat() * it / n + PI.toFloat() / 6f) }
        val zs = FloatArray(n) { 2.3f * sin(2f * PI.toFloat() * it / n + PI.toFloat() / 6f) }
        val elected = t > 4300f
        val fade = SK.ease(t, 4300f, 900f)
        Net3D.switchBox(v, 0f, 0f)
        for (i in 0 until n) v.link(0f, 0f, xs[i], zs[i], th.line, 0.55f, 5f)
        var count = 0
        var k = 0
        for (i in 0 until n) for (j in i + 1 until n) {
            val keep = i <= 1 || j <= 1
            val appear = SK.ease(t, 300f + k * 180f, 400f)
            k++
            val a = if (keep) appear else appear * (1f - fade)
            if (a > 0.02f) v.link(xs[i], zs[i], xs[j], zs[j], if (keep && elected) th.good else th.accent, 0.85f * a, if (keep && elected) 6f else 3.5f)
            if (keep || !elected) count++
        }
        for (i in 0 until n) Net3D.router(v, xs[i], zs[i], 0.35f, when { elected && i == 0 -> th.warn; elected && i == 1 -> th.good; else -> th.accent })
        v.draw()
        for (i in 0 until n) v.tag(if (elected && i == 0) "DR" else if (elected && i == 1) "BDR" else "R${i + 1}", xs[i], 1.15f, zs[i], if (elected && i <= 1) th.ink else th.muted, if (elected && i <= 1) 23f else 19f)
        v.tag("Réseau Ethernet partagé", 0f, 0.7f, 0f, th.muted, 17f)
        val full = SK.ease(t, 300f + 14 * 180f, 400f)
        SK.pill(c.g, if (!elected) "Sans DR : chaque routeur avec chacun (15 adjacences)" else "Avec DR et BDR : 9 adjacences", 500f, 650f, if (elected) th.good else th.warn, th.bgTop.withAlpha(0.92f), 22f, if (elected) 1f else full)
        caption(c, if (elected) "Un routeur désigné et son remplaçant" else "6 routeurs sur le même réseau")
    }

    private val ospfMismatch = beat("ospf-mismatch", "Hello, délai de mort, aire et masque : ils doivent être identiques", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Actor.ROUTEUR) + setOf("topic:ospf-mismatch"), minMs = 6000, needs = setOf("topic:ospf-mismatch"), onlyFor = setOf(Concept.ROUTING)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        // 0 : tout est identique ; 1 : Hello différent ; 2 : aire différente ; 3 : masque différent
        val cyc = 14000f
        val tt = SK.loop(t, cyc) * cyc
        val ph = (tt / 3500f).toInt().coerceAtMost(3)
        val rows = listOf("Intervalle Hello" to ("10 s" to if (ph == 1) "30 s" else "10 s"), "Délai de mort" to ("40 s" to if (ph == 1) "120 s" else "40 s"),
            "Aire" to ("0" to if (ph == 2) "1" else "0"), "Masque" to ("/24" to if (ph == 3) "/25" else "/24"))
        val bad = BooleanArray(4) { rows[it].second.first != rows[it].second.second }
        val ok = bad.none { it }
        for (side in 0..1) {
            val x0 = if (side == 0) 24f else 616f
            c.g.fillRoundRect(x0, 110f, x0 + 360f, 560f, 20f, th.panel.withAlpha(0.88f))
            c.g.strokeRoundRect(x0, 110f, x0 + 360f, 560f, 20f, 2.5f, th.line)
            SK.text(c.g, if (side == 0) "R1" else "R2", x0 + 180f, 160f, 36f, th.ink, 800, center = true)
            for ((i, r) in rows.withIndex()) {
                val y = 235f + i * 100f
                val value = if (side == 0) r.second.first else r.second.second
                SK.text(c.g, value, x0 + 180f, y, 38f, if (bad[i]) th.bad else th.ink, 800, center = true, family = FontFamily.MONO)
                if (side == 1 && bad[i]) SK.cross(c.g, x0 + 330f, y - 12f, 26f, th.bad)
            }
        }
        for ((i, r) in rows.withIndex()) SK.text(c.g, r.first, 500f, 235f + i * 100f, 21f, th.muted, 700, center = true)
        val lc = if (ok) th.good else th.bad
        c.g.line(392f, 285f, 608f, 285f, 7f, lc.withAlpha(if (ok) 1f else 0.35f))
        if (!ok) { SK.dashed(c.g, 392f, 285f, 608f, 285f, 5f, th.bad, 14f, 14f, t / 40f) }
        SK.pill(c.g, if (ok) "Paramètres identiques : l'adjacence se forme" else "Paramètre différent : l'adjacence ne se forme pas", 500f, 650f, if (ok) th.good else th.bad, th.bgTop.withAlpha(0.92f), 22f)
        caption(c, "Deux voisins doivent être d'accord")
    }

    val ospfKind = SceneKind("net3d-ospf", "Routage OSPF en 3D", setOf(Concept.ROUTING), listOf(ospfOverview, ospfCosts, ospfHello, ospfLsa, ospfSpf, ospfVersions, ospfBenefits, ospfDr, ospfMismatch, ospfPoster), domains = emptySet())

    // ───────────────────────── Pare-feu ─────────────────────────

    private fun fwScene(c: StageCtx, wallHeat: Float): Net3D.View {
        val v = view(c, -0.3f, 0.62f, 68f, 400f)
        v.grid(6.5f, 3.5f)
        Net3D.pc(v, -4.6f, 0f, v.th.accent)
        Net3D.pc(v, -4.6f, -1.7f, v.th.bad)
        Net3D.server(v, 4.6f, 0f)
        Net3D.wall(v, 0f, 2.6f, 0.4f, wallHeat)
        v.link(-4.6f, 0f, 4.6f, 0f, v.th.line, 0.35f, 4f)
        return v
    }

    private val fwFlows = beat("fw-flows", "Le pare-feu laisse passer ou bloque chaque paquet", BODY, MECH,
        cueSet(Actor.PARE_FEU, Thing.PAQUET, Act.BLOQUER, Act.AUTORISER) + setOf("topic:fw"), minMs = 5000, shows = setOf("topic:fw-packet"), onlyFor = setOf(Concept.FIREWALL), repeatable = true, excludes = setOf("topic:purpose")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val loop = SK.loop(t, 7200f) * 7200f
        val v = fwScene(c, 0.3f)
        var rule = -1
        // 1 : HTTPS passe par la porte ; 2 : Telnet se heurte au mur ; 3 : un paquet inconnu est refusé
        val a = (loop / 2400f).coerceIn(0f, 1f)
        if (loop < 2600f) { Net3D.packet(v, lerp(-4.0f, 4.0f, a), 0.55f, 0f, th.good, 0.38f); rule = if (a in 0.4f..0.7f) 0 else -1 }
        val b = ((loop - 2600f) / 2000f)
        if (b in 0f..1f) { val x = lerp(-4.0f, -0.5f, b.coerceAtMost(1f)); Net3D.packet(v, x, 0.55f, -1.7f, th.bad, 0.38f); rule = if (b > 0.75f) 1 else -1 }
        val d = ((loop - 4800f) / 2000f)
        if (d in 0f..1f) { val x = if (d < 0.6f) lerp(-4.0f, -0.5f, d / 0.6f) else lerp(-0.5f, -2.4f, (d - 0.6f) / 0.4f); Net3D.packet(v, x, 0.55f, 1.4f, th.muted, 0.38f); rule = if (d in 0.5f..0.9f) 2 else -1 }
        v.draw()
        v.tag("Internet", -4.6f, 1.5f, -0.4f, th.muted, 19f); v.tag("Serveur web", 4.6f, 2.5f, 0f, th.ink, 19f)
        v.tag("Pare-feu", 0f, 2.0f, 0f, th.warn, 21f)
        val b2 = b
        if (b2 > 0.85f && b2 < 1.2f) SK.burst(c.g, v.pt(-0.3f, 0.6f, -1.7f)[0], v.pt(-0.3f, 0.6f, -1.7f)[1], 46f, th.bad, ((b2 - 0.85f) / 0.35f).coerceIn(0f, 1f))
        val labels = listOf("1 · Autoriser · HTTPS (443)" to th.good, "2 · Bloquer · Telnet (23)" to th.bad, "3 · Refuser · tout le reste" to th.warn)
        for ((i, l) in labels.withIndex()) {
            val on = rule == i
            SK.pill(c.g, l.first, 250f + (i * 250f), 650f, if (on) th.ink else th.muted, if (on) l.second.withAlpha(0.6f) else th.bgTop.withAlpha(0.8f), if (on) 19f else 17f)
        }
        caption(c, "Le pare-feu trie chaque paquet")
    }

    private val fwRules = beat("fw-rules", "Les règles se lisent de haut en bas : la première qui correspond s'applique", BODY, MECH,
        cueSet(Actor.PARE_FEU, Thing.REGLE, Act.FILTRER) + setOf("topic:fw-rule"), minMs = 7000, needs = setOf("topic:fw-rule"), onlyFor = setOf(Concept.FIREWALL), excludes = setOf("topic:purpose")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, -0.55f, 0.6f, 62f, 330f, ).also { }
        // table de règles, à gauche
        val rows = listOf("Autoriser" to "HTTPS (TCP 443)", "Bloquer" to "Telnet (TCP 23)", "Autoriser" to "DNS (UDP 53)", "Refuser" to "tout le reste")
        // paquet évalué : DNS d'abord (3e règle), puis un paquet inconnu (règle par défaut)
        val cycle = SK.loop(t, 9000f) * 9000f
        val second = cycle >= 4500f
        val target = if (second) 3 else 2
        val local = if (second) cycle - 4500f else cycle
        val cursor = (local / 800f).toInt().coerceAtMost(target)
        val settled = local > 800f * target + 600f
        val x0 = 40f; val w = 560f
        SK.pill(c.g, if (second) "Paquet : TCP 8080 (inconnu)" else "Paquet : UDP 53 (DNS)", x0 + w / 2f, 120f, th.ink, th.accent.withAlpha(0.5f), 22f)
        for ((i, r) in rows.withIndex()) {
            val y = 160f + i * 95f
            val hit = settled && i == target
            val passed = i < cursor || (i == cursor && !settled && local > 800f * i + 400f && i != target)
            val fill = when { hit -> (if (r.first == "Autoriser") th.good else th.bad).withAlpha(0.35f); i == cursor && !settled -> th.accent.withAlpha(0.28f); else -> th.panel.withAlpha(0.85f) }
            c.g.fillRoundRect(x0, y, x0 + w, y + 78f, 14f, fill)
            c.g.strokeRoundRect(x0, y, x0 + w, y + 78f, 14f, 2.5f, if (hit) (if (r.first == "Autoriser") th.good else th.bad) else th.line)
            SK.text(c.g, "${i + 1}", x0 + 22f, y + 50f, 34f, th.accent, 800)
            SK.text(c.g, r.first, x0 + 70f, y + 36f, 26f, if (r.first == "Autoriser") th.good else th.bad, 800)
            SK.text(c.g, r.second, x0 + 70f, y + 66f, 22f, th.ink, 600, family = FontFamily.MONO)
            if (passed) SK.text(c.g, "ne correspond pas", x0 + w - 200f, y + 48f, 19f, th.muted, 600)
            if (hit) SK.text(c.g, if (r.first == "Autoriser") "correspond : passe" else "correspond : bloqué", x0 + w - 250f, y + 48f, 21f, th.ink, 800)
        }
        // mur 3D à droite, avec le paquet qui passe ou rebondit
        val v2 = Net3D.View(c, -0.5f + 0.1f * sin(c.tg / 5200f), 0.62f, 60f, 420f, 790f)
        v2.grid(3.2f, 2.5f)
        Net3D.wall(v2, 0f, 1.7f, 0.4f, 0.3f)
        val u = clamp01((local - 800f * target - 500f) / 1500f)
        val pass = target == 2
        val px = if (!settled) -2.6f else if (pass) lerp(-2.6f, 2.8f, u) else if (u < 0.5f) lerp(-2.6f, -0.5f, u * 2f) else lerp(-0.5f, -1.6f, (u - 0.5f) * 2f)
        Net3D.packet(v2, px, 0.55f, if (pass) 0f else 1.0f, if (pass) th.good else if (settled) th.bad else th.muted, 0.38f)
        v2.draw()
        caption(c, "Ordre des règles : de haut en bas")
    }


    /** Position le long d'une ligne brisée (x, z), u de 0 à 1. */
    private fun along(pts: List<Pair<Float, Float>>, u: Float): Pair<Float, Float> {
        val len = FloatArray(pts.size - 1) { hypot(pts[it + 1].first - pts[it].first, pts[it + 1].second - pts[it].second) }
        var d = clamp01(u) * len.sum()
        for (i in len.indices) {
            if (d <= len[i] || i == len.lastIndex) { val f = if (len[i] > 0f) (d / len[i]).coerceIn(0f, 1f) else 0f; return lerp(pts[i].first, pts[i + 1].first, f) to lerp(pts[i].second, pts[i + 1].second, f) }
            d -= len[i]
        }
        return pts.last()
    }

    private fun zone(v: Net3D.View, x0: Float, z0: Float, x1: Float, z1: Float, color: Int, a: Float) {
        val p = listOf(v.pt(x0, 0.02f, z0), v.pt(x1, 0.02f, z0), v.pt(x1, 0.02f, z1), v.pt(x0, 0.02f, z1))
        val path = PathData().moveTo(p[0][0], p[0][1]).lineTo(p[1][0], p[1][1]).lineTo(p[2][0], p[2][1]).lineTo(p[3][0], p[3][1]).close()
        v.c.g.fillPath(path, color.withAlpha(a))
        v.c.g.strokePath(path, 2.5f, color.withAlpha(0.75f))
    }

    private val fwZones = beat("fw-zones", "Le pare-feu relie des zones : Internet, réseau interne, DMZ", BODY, MECH,
        cueSet(Actor.PARE_FEU) + setOf("topic:fw-zones"), minMs = 6000, needs = setOf("topic:fw-zones"), onlyFor = setOf(Concept.FIREWALL), excludes = setOf("topic:purpose")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, -0.25f, 0.66f, 64f, 400f)
        v.grid(6.5f, 3.6f)
        zone(v, -6.3f, -3.4f, -1.0f, 3.4f, th.muted, 0.14f)
        zone(v, 1.0f, -3.4f, 6.3f, 0.4f, th.good, 0.15f)
        zone(v, 1.0f, 0.9f, 6.3f, 3.4f, th.warn, 0.17f)
        Net3D.pc(v, -4.4f, 0f, th.bad)
        Net3D.pc(v, 3.0f, -1.7f, th.accent); Net3D.pc(v, 4.8f, -1.7f, th.accent)
        Net3D.server(v, 4.2f, 2.3f, th.warn)
        Net3D.wall(v, 0f, 3.3f, 0.4f, 0.3f)
        val cyc = SK.loop(t, 9000f) * 9000f
        val which = (cyc / 3000f).toInt().coerceAtMost(2)
        val u = ((cyc - which * 3000f) / 2400f).coerceIn(0f, 1f)
        val (pts, col, msg) = when (which) {
            0 -> Triple(listOf(-4.4f to 0f, -0.2f to 0f, 1.2f to 1.0f, 3.4f to 2.3f), th.good, "Internet → DMZ : HTTPS vers le serveur public, autorisé")
            1 -> Triple(listOf(-4.4f to -1.7f, -0.9f to -1.7f, -2.0f to -1.7f), th.bad, "Internet → réseau interne : refusé")
            else -> Triple(listOf(3.0f to -1.2f, 1.0f to -0.6f, 0f to 0f, -4.0f to 0f), th.good, "Réseau interne → Internet : autorisé")
        }
        // le paquet bloqué avance jusqu'au mur puis recule
        val uu = if (which == 1) (if (u < 0.55f) u / 0.55f * 0.76f else 0.76f - (u - 0.55f) / 0.45f * 0.24f) else u
        val (x, z) = along(pts, uu.coerceIn(0f, 1f))
        Net3D.packet(v, x, 0.55f, z, col, 0.38f)
        v.draw()
        v.tag("Internet", -3.6f, 1.6f, 2.4f, th.muted, 21f)
        v.tag("Réseau interne", 3.9f, 1.5f, -2.8f, th.good, 21f)
        v.tag("DMZ : serveurs publics", 3.9f, 2.9f, 3.0f, th.warn, 21f)
        v.tag("Pare-feu", 0f, 1.9f, 0f, th.ink, 21f)
        SK.pill(c.g, msg, 500f, 650f, col, th.bgTop.withAlpha(0.92f), 22f)
        caption(c, "Trois zones, des règles entre elles")
    }

    private val fwState = beat("fw-state", "Pare-feu à états : la réponse à une requête sortante est autorisée", BODY, MECH,
        cueSet(Actor.PARE_FEU) + setOf("topic:fw-state"), minMs = 7000, needs = setOf("topic:fw-state"), onlyFor = setOf(Concept.FIREWALL)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, -0.3f, 0.62f, 66f, 420f)
        v.grid(6.5f, 3.6f)
        Net3D.server(v, -4.6f, 0f, th.muted)
        Net3D.pc(v, 4.6f, 0f, th.accent)
        Net3D.wall(v, 0f, 2.7f, 0.4f, 0.3f)
        v.link(-4.6f, 0f, 4.6f, 0f, th.line, 0.35f, 4f)
        val cyc = SK.loop(t, 10000f) * 10000f
        var col = th.muted; var msg = ""
        var known = cyc > 1700f
        if (cyc < 2400f) { val (x, z) = along(listOf(4.4f to 0f, -4.4f to 0f), cyc / 2400f); Net3D.packet(v, x, 0.55f, z, th.good, 0.38f); col = th.good; msg = "Requête sortante : autorisée, la connexion est notée" }
        else if (cyc in 2800f..5200f) { val (x, z) = along(listOf(-4.4f to 0f, 4.4f to 0f), (cyc - 2800f) / 2400f); Net3D.packet(v, x, 0.55f, z, th.good, 0.38f); col = th.good; msg = "Réponse : autorisée automatiquement, la connexion est connue" }
        else if (cyc in 5800f..8400f) {
            val u = (cyc - 5800f) / 2600f
            val (x, z) = along(listOf(-4.4f to -1.7f, -0.9f to -1.7f, -2.0f to -1.7f), if (u < 0.6f) u / 0.6f * 0.76f else 0.76f - (u - 0.6f) / 0.4f * 0.24f)
            Net3D.packet(v, x, 0.55f, z, th.bad, 0.38f); col = th.bad; msg = "Connexion entrante que personne n'a demandée : bloquée"
        }
        v.draw()
        v.tag("Site web (Internet)", -4.6f, 2.7f, 0f, th.muted, 19f); v.tag("Poste interne", 4.6f, 1.6f, 0f, th.ink, 19f); v.tag("Pare-feu", 0f, 1.9f, 0f, th.ink, 21f)
        // table des connexions
        c.g.fillRoundRect(160f, 84f, 840f, 168f, 16f, th.panel.withAlpha(0.9f))
        c.g.strokeRoundRect(160f, 84f, 840f, 168f, 16f, 2f, th.line)
        SK.text(c.g, "Table des connexions suivies", 184f, 112f, 18f, th.muted, 700)
        if (known) SK.text(c.g, "192.168.1.20 → 203.0.113.5 : TCP 443   établie", 184f, 148f, 24f, th.good, 700, family = FontFamily.MONO)
        else SK.text(c.g, "(vide)", 184f, 148f, 24f, th.muted, 600, family = FontFamily.MONO)
        if (msg.isNotEmpty()) SK.pill(c.g, msg, 500f, 650f, col, th.bgTop.withAlpha(0.92f), 22f)
        caption(c, "Un pare-feu à états suit les connexions")
    }

    private val fwMistake = beat("fw-mistake", "Une règle trop large ou mal placée masque les règles précises", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Actor.PARE_FEU, Thing.REGLE) + setOf("topic:fw-mistake"), minMs = 6000, needs = setOf("topic:fw-mistake"), onlyFor = setOf(Concept.FIREWALL)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val cyc = 11000f
        val tt = SK.loop(t, cyc) * cyc
        val fixed = tt >= 5800f
        val lt = if (fixed) tt - 5800f else tt
        val rules = if (!fixed) listOf("Autoriser" to "tout le trafic", "Bloquer" to "Telnet (TCP 23)", "Autoriser" to "HTTPS (TCP 443)", "Refuser" to "tout le reste")
        else listOf("Bloquer" to "Telnet (TCP 23)", "Autoriser" to "HTTPS (TCP 443)", "Refuser" to "tout le reste")
        SK.pill(c.g, "Paquet : Telnet (TCP 23)", 500f, 112f, th.ink, th.accent.withAlpha(0.5f), 24f)
        val x0 = 120f; val w = 760f
        val settled = lt > 1100f
        for ((i, r) in rules.withIndex()) {
            val y = 160f + i * 100f
            val hit = settled && i == 0
            val masked = settled && i > 0
            val allow = r.first == "Autoriser"
            val fill = when { hit -> (if (!fixed) th.bad else th.good).withAlpha(0.35f); else -> th.panel.withAlpha(if (masked && !fixed) 0.45f else 0.85f) }
            c.g.fillRoundRect(x0, y, x0 + w, y + 82f, 14f, fill)
            c.g.strokeRoundRect(x0, y, x0 + w, y + 82f, 14f, 2.5f, if (hit) (if (!fixed) th.bad else th.good) else th.line)
            SK.text(c.g, "${i + 1}", x0 + 24f, y + 54f, 34f, th.accent, 800)
            SK.text(c.g, r.first, x0 + 76f, y + 38f, 27f, if (allow) th.good else th.bad, 800)
            SK.text(c.g, r.second, x0 + 76f, y + 69f, 23f, th.ink, 600, family = FontFamily.MONO)
            if (hit && !fixed) SK.text(c.g, "correspond : Telnet passe", x0 + w - 340f, y + 50f, 23f, th.ink, 800)
            if (hit && fixed) SK.text(c.g, "correspond : Telnet bloqué", x0 + w - 340f, y + 50f, 23f, th.ink, 800)
            if (masked && !fixed) SK.text(c.g, "jamais lue", x0 + w - 190f, y + 50f, 21f, th.muted, 700)
            if (i == 0 && !fixed) SK.pill(c.g, "trop large", x0 + w - 110f, y + 18f, th.bad, th.bgTop.withAlpha(0.9f), 15f)
        }
        SK.pill(c.g, if (!fixed) "Règle générale placée trop haut : les règles précises ne servent plus" else "Règles précises d'abord, règle générale à la fin", 500f, 650f, if (fixed) th.good else th.bad, th.bgTop.withAlpha(0.92f), 21f)
        caption(c, if (!fixed) "Une règle trop large" else "Le bon ordre")
    }

    private val fwPurpose = beat("fw-purpose", "À quoi sert un pare-feu : bloquer l'entrant, séparer les zones, garder une trace", BODY, MECH + setOf(Goal.MEMORISER),
        emptySet(), minMs = 7000, needs = setOf("topic:purpose"), onlyFor = setOf(Concept.FIREWALL)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val (ph, lt) = phaseOf(c, 3)
        val v = fwScene(c, 0.25f)
        if (ph < 2) {
            val z = if (ph == 0) 0f else -1.7f
            for (k in 0 until 2) {
                val u = SK.loop(lt, 2400f, k * 1200f)
                Net3D.packet(v, lerp(-4.2f, -0.9f, (u / 0.7f).coerceAtMost(1f)) - if (u > 0.7f) (u - 0.7f) * 2.5f else 0f, 0.55f, if (k == 0) z else z + 1.5f, th.bad, 0.38f)
            }
        }
        v.draw()
        if (ph == 0) { v.tag("Internet", -4.6f, 1.5f, 0f, th.muted, 19f); v.tag("Réseau interne", 4.6f, 2.7f, 0f, th.ink, 19f) }
        if (ph == 1) { v.tag("Invités", -4.6f, 1.5f, -0.9f, th.warn, 19f); v.tag("Serveurs", 4.6f, 2.7f, 0f, th.ink, 19f) }
        if (ph == 2) {
            c.g.fillRoundRect(40f, 330f, 960f, 600f, 18f, th.bgBottom.withAlpha(0.94f))
            c.g.strokeRoundRect(40f, 330f, 960f, 600f, 18f, 2f, th.line)
            SK.text(c.g, "Journal du pare-feu", 66f, 366f, 20f, th.muted, 700)
            val lines = listOf(
                Triple("10:02:14", "AUTORISÉ  192.168.1.20 → 203.0.113.5   TCP 443", th.good),
                Triple("10:02:15", "REFUSÉ    198.51.100.7 → 192.168.1.10  TCP 23", th.bad),
                Triple("10:02:19", "REFUSÉ    198.51.100.7 → 192.168.1.10  TCP 445", th.bad),
                Triple("10:02:21", "AUTORISÉ  192.168.1.31 → 198.51.100.20  UDP 53", th.good))
            for ((i, l) in lines.withIndex()) {
                val a = SK.ease(lt, 300f + i * 700f, 400f)
                SK.text(c.g, l.first + "  " + l.second, 66f, 420f + i * 46f, 21f, l.third.withAlpha(a), 700, family = FontFamily.MONO)
            }
            SK.text(c.g, "EXEMPLE FICTIF : adresses de documentation", 66f, 584f, 16f, th.muted, 600)
        }
        step(c, listOf("1 · Bloquer l'entrant", "2 · Séparer les zones", "3 · Garder une trace"), ph)
        caption(c, listOf("Connexions venues d'Internet : refusées", "Chaque zone n'atteint que le permis", "Chaque décision est journalisée")[ph])
    }

    private val fwPoster = beat("poster", "Affiche : un mur qui filtre les paquets", setOf(Role.INTRO, Role.RECAP, Role.DEFINE), setOf(Goal.IDENTIFIER_MENACE, Goal.MEMORISER, Goal.COMPRENDRE_MECANISME),
        emptySet(), repeatable = true, minMs = 1500, onlyFor = setOf(Concept.FIREWALL)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = Net3D.View(c, -0.3f + 0.25f * sin(c.tg / 3600f), 0.62f, 90f, 380f)
        v.grid(5f, 3f)
        Net3D.wall(v, 0f, 2.6f, 0.4f, 0.4f)
        for (k in 0 until 3) { val u = SK.loop(t, 2400f, k * 800f); Net3D.packet(v, lerp(-4.2f, 4.2f, u), 0.55f, 0f, th.good, 0.36f) }
        for (k in 0 until 2) { val u = SK.loop(t, 2400f, k * 1200f); Net3D.packet(v, lerp(-4.2f, -0.5f, u.coerceAtMost(1f)), 0.55f, if (k == 0) -1.4f else 1.4f, th.bad, 0.36f) }
        v.draw()
    }

    val fwKind = SceneKind("net3d-fw", "Pare-feu en 3D", setOf(Concept.FIREWALL), listOf(fwFlows, fwRules, fwZones, fwState, fwMistake, fwPurpose, fwPoster))

    // ───────────────────────── Active Directory ─────────────────────────

    private val adDomain = beat("ad-domain", "Le domaine : contrôleurs, unités d'organisation, groupes et règles", BODY, MECH + setOf(Goal.MEMORISER),
        emptySet(), repeatable = true, minMs = 5000, shows = setOf("topic:ad-domain"), onlyFor = setOf(Concept.DIRECTORY), excludes = setOf("topic:ad-central")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.78f, 78f, 395f)
        v.grid(6.5f, 4f)
        // anneau du domaine au sol
        val ring = (0 until 49).map { i -> val a = (2.0 * PI * i / 48).toFloat(); v.pt(6.2f * cos(a), 0.03f, 3.9f * sin(a) + 0.2f) }
        for (i in 0 until 48) c.g.line(ring[i][0], ring[i][1], ring[i + 1][0], ring[i + 1][1], 4f, th.accent.withAlpha(0.55f))
        // deux contrôleurs de domaine et leur réplication
        Net3D.server(v, -1.6f, 2.9f); Net3D.server(v, 1.6f, 2.9f, th.good)
        v.link(-1.6f, 2.9f, 1.6f, 2.9f, th.line, 0.9f, 5f)
        val rp = SK.loop(t, 2200f)
        Net3D.packet(v, lerp(-1.6f, 1.6f, rp), 1.0f, 2.9f, th.good, 0.26f); Net3D.packet(v, lerp(1.6f, -1.6f, rp), 1.2f, 2.9f, th.accent, 0.26f)
        // trois unités d'organisation : tapis au sol et deux postes chacune
        val ou = listOf("Comptabilité" to -4.0f, "Ventes" to 0f, "Technique" to 4.0f)
        for ((i, o) in ou.withIndex()) {
            val x = o.second
            v.link(x, -0.4f, 0f, 2.9f, th.line, 0.5f, 3f)
            Net3D.pc(v, x - 0.7f, -1.0f, mix(th.accent, th.good, i / 2f)); Net3D.pc(v, x + 0.7f, -1.0f, mix(th.accent, th.good, i / 2f))
        }
        // règles de groupe : une onde part des contrôleurs
        val wv = SK.loop(t, 3600f)
        if (wv > 0.1f) { val pr = v.pt(0f, 0.04f, 2.9f); c.g.strokeCircle(pr[0], pr[1] + 8f, 40f + 380f * wv, 3f, th.warn.withAlpha(0.7f * (1f - wv))) }
        v.draw()
        for ((i, o) in ou.withIndex()) v.tag(o.first, o.second, 1.2f, -1.0f, th.ink, 19f)
        v.tag("Contrôleurs de domaine", 0f, 2.7f, 2.9f, th.accent, 20f)
        v.tag("Réplication", 0f, 0.1f, 2.0f, th.good, 17f, 0.9f)
        v.tag("entreprise.example", 4.6f, 0.1f, 3.9f, th.muted, 18f)
        SK.pill(c.g, "Stratégies de groupe : appliquées à tous les postes", 500f, 650f, th.warn, th.bgTop.withAlpha(0.9f), 20f)
        caption(c, "Le domaine et son annuaire")
    }

    private val adAuth = beat("ad-auth", "L'ouverture de session : identifiants, ticket Kerberos, accès", BODY, MECH,
        cueSet(Actor.SERVEUR, Actor.UTILISATEUR, Thing.COMPTE, Thing.MOT_DE_PASSE) + setOf("topic:ad-auth"), minMs = 8000, needs = setOf("topic:ad-auth"), onlyFor = setOf(Concept.DIRECTORY), excludes = setOf("topic:ad-central")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.8f, 84f, 390f)
        v.grid(6.5f, 4f)
        val pcX = -4.0f; val pcZ = -0.5f; val dcX = 0.2f; val dcZ = 2.7f; val sx = 4.0f; val sz = -0.5f
        val tgt = SK.ease(t, 6400f, 600f)
        Net3D.pc(v, pcX, pcZ, th.accent); Net3D.server(v, dcX, dcZ, th.accent); Net3D.server(v, sx, sz, mix(th.accent, th.good, tgt), 1.5f, 0.4f + 0.6f * tgt)
        v.link(pcX, pcZ, dcX, dcZ, th.line, 0.9f); v.link(pcX, pcZ, sx, sz, th.line, 0.9f); v.link(dcX, dcZ, sx, sz, th.line, 0.4f, 4f)
        val s1 = (t - 700f) / 1500f; val s2 = (t - 2700f) / 1500f; val s3 = (t - 4700f) / 1500f
        if (s1 in 0f..1f) { val (x, z) = lerpPt(pcX, pcZ, dcX, dcZ, s1); Net3D.packet(v, x, 0.9f, z, th.accent, 0.34f) }
        if (s2 in 0f..1f) { val (x, z) = lerpPt(dcX, dcZ, pcX, pcZ, s2); Net3D.packet(v, x, 0.9f, z, th.warn, 0.4f) }
        val hasTicket = t > 4200f
        if (hasTicket && s3 < 0f) Net3D.packet(v, pcX, 1.55f, pcZ, th.warn, 0.4f)
        if (s3 in 0f..1f) { val (x, z) = lerpPt(pcX, pcZ, sx, sz, s3); Net3D.packet(v, x, 0.9f, z, th.warn, 0.4f) }
        v.draw()
        v.tag("Poste", pcX, 1.7f, pcZ, th.ink, 19f); v.tag("Contrôleur de domaine", dcX, 2.6f, dcZ, th.accent, 19f); v.tag("Serveur de fichiers", sx - 0.4f, 2.5f, sz, th.ink, 19f)
        if (hasTicket) v.tag("Ticket Kerberos", pcX + 0.5f, 2.4f, pcZ, th.warn, 18f, SK.ease(t, 4200f, 400f))
        val active = when { t < 2600f -> 0; t < 4600f -> 1; t < 6300f -> 2; else -> 3 }
        if (t > 6500f) SK.check(c.g, v.pt(sx, 2.3f, sz)[0] + 70f, v.pt(sx, 2.3f, sz)[1], 40f, th.good, SK.ease(t, 6500f, 400f))
        step(c, listOf("1 Identifiants", "2 Ticket remis", "3 Ticket présenté", "4 Accès accordé"), active)
        caption(c, "Ouvrir une session dans le domaine")
    }


    private val adGpo = beat("ad-gpo", "Une règle de sécurité appliquée à tous les postes d'un coup", BODY, MECH,
        emptySet(), repeatable = true, minMs = 4000, shows = setOf("topic:ad-gpo"), onlyFor = setOf(Concept.DIRECTORY), excludes = setOf("topic:ad-central", "topic:ad-domain")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.8f, 84f, 390f)
        v.grid(6.5f, 4f)
        Net3D.server(v, 0f, 2.6f, th.warn)
        val xs = listOf(-4.4f, -1.5f, 1.5f, 4.4f)
        for ((i, x) in xs.withIndex()) {
            val u = SK.ease(t, 900f + i * 500f, 900f)
            v.link(0f, 2.6f, x, -0.8f, th.warn, 0.35f + 0.55f * u, 5f)
            Net3D.pc(v, x, -0.8f, mix(th.accent, th.good, u))
            if (u in 0.02f..0.98f) { val (px, pz) = lerpPt(0f, 2.6f, x, -0.8f, u); Net3D.packet(v, px, 0.9f, pz, th.warn, 0.32f) }
        }
        v.draw()
        v.tag("Stratégie de groupe", 0f, 2.7f, 2.6f, th.warn, 20f)
        for ((i, x) in xs.withIndex()) v.tag("appliquée", x, 1.6f, -0.8f, th.good, 16f, SK.ease(t, 1700f + i * 500f, 400f))
        caption(c, "Une règle, tout le parc")
    }

    private val adGroups = beat("ad-groups", "Les utilisateurs sont dans des groupes, les groupes ont les droits", BODY, MECH,
        emptySet(), minMs = 4500, needs = setOf("topic:ad-group"), onlyFor = setOf(Concept.DIRECTORY), excludes = setOf("topic:ad-central")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.78f, 84f, 390f)
        v.grid(6.5f, 4f)
        Net3D.pc(v, -4.3f, -1.4f, th.accent); Net3D.pc(v, -4.3f, 0.6f, th.accent); Net3D.pc(v, -4.3f, 2.6f, th.good)
        Net3D.server(v, 4.3f, 0.6f, th.good)
        for (f in 0 until 4) Net3D.file(v, 3.7f + (f % 2) * 0.55f, 2.2f + (f / 2) * 0.1f, 0.5f, 0f)
        val g1 = v.pt(-1.0f, 0.5f, 0.6f)
        val ok = SK.ease(t, 1200f, 600f); val no = SK.ease(t, 2600f, 600f)
        for (z in listOf(-1.4f, 0.6f)) v.link(-4.3f, z, 4.3f, 0.6f, th.good, ok, 6f)
        v.link(-4.3f, 2.6f, 4.3f, 0.6f, th.bad, no * 0.8f, 5f)
        v.draw()
        v.tag("Groupe Comptabilité", -4.3f, 1.6f, -0.4f, th.good, 18f, SK.ease(t, 500f, 500f))
        v.tag("Dossier compta : autorisé", 0f, 0.1f, -0.6f, th.good, 18f, ok)
        v.tag("Hors du groupe : refusé", -1.0f, 0.1f, 2.0f, th.bad, 18f, no)
        v.tag("Serveur de fichiers", 4.3f, 2.5f, 0.6f, th.ink, 18f)
        if (g1[0] < 0f) return@beat
        caption(c, "Des groupes plutôt que des droits individuels")
    }


    private fun sub(c: StageCtx, lt: Float) = StageCtx(c.g, lt, c.tg, c.th, c.intent, c.scene, c.seed, c.w, c.h)

    private val adBenefits = beat("ad-benefits", "À quoi sert l'annuaire : une identité, des groupes, des règles pour tout le parc", BODY, MECH + setOf(Goal.MEMORISER),
        emptySet(), minMs = 7000, needs = setOf("topic:ad-central"), onlyFor = setOf(Concept.DIRECTORY)) { c ->
        val th = c.th
        val (ph, lt) = phaseOf(c, 3)
        if (ph == 0) {
            bg(c)
            val v = view(c, 0f, 0.8f, 84f, 390f)
            v.grid(6.5f, 4f)
            Net3D.server(v, 0f, 2.6f, th.accent)
            val xs = listOf(-4.5f, -1.5f, 1.5f, 4.5f)
            for ((i, x) in xs.withIndex()) {
                val u = SK.ease(lt, 700f + i * 450f, 700f)
                v.link(0f, 2.6f, x, -0.8f, th.good, 0.3f + 0.6f * u, 5f)
                Net3D.pc(v, x, -0.8f, mix(th.line, th.good, u))
            }
            v.draw()
            v.tag("Un seul compte dans l'annuaire", 0f, 2.7f, 2.6f, th.accent, 20f)
            for ((i, x) in xs.withIndex()) v.tag("session ouverte", x, 1.6f, -0.8f, th.good, 16f, SK.ease(lt, 1300f + i * 450f, 400f))
            caption(c, "Une seule identité pour tout")
        } else if (ph == 1) adGroups.draw!!(sub(c, lt)) else adGpo.draw!!(sub(c, lt))
        step(c, listOf("1 · Une identité", "2 · Des groupes", "3 · Des règles"), ph)
    }

    private val adAdmin = beat("ad-admin", "Un compte du quotidien administrateur du domaine : un seul poste compromis expose tout", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Actor.UTILISATEUR) + setOf("topic:ad-admin"), minMs = 6000, needs = setOf("topic:ad-admin"), onlyFor = setOf(Concept.DIRECTORY)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val cyc = 12000f
        val tt = SK.loop(t, cyc) * cyc
        val separated = tt >= 6200f
        val lt = if (separated) tt - 6200f else tt
        val v = view(c, 0f, 0.8f, 84f, 390f)
        v.grid(6.5f, 4f)
        val hit = lt > 1600f
        val spread = SK.ease(lt, 2200f, 1400f)
        Net3D.server(v, -1.6f, 2.7f, if (!separated && spread > 0.9f) th.bad else th.accent); Net3D.server(v, 1.6f, 2.7f, if (!separated && spread > 0.9f) th.bad else th.good)
        val pcs = listOf(-4.4f, -1.5f, 1.5f, 4.4f)
        for ((i, x) in pcs.withIndex()) {
            val infected = if (i == 0) hit else (!separated && spread > 0.4f + 0.15f * i)
            v.link(x, -0.8f, 0f, 2.7f, if (infected && !(separated && i == 0)) th.bad else th.line, 0.8f, 4f)
            Net3D.pc(v, x, -0.8f, if (infected) th.bad else th.accent)
        }
        if (!hit) Net3D.packet(v, lerp(-6.2f, -4.4f, SK.ease(lt, 500f, 1000f)), 0.9f, -0.8f, th.bad, 0.34f)
        if (!separated && spread in 0.02f..0.98f) { val (px, pz) = lerpPt(-4.4f, -0.8f, 0f, 2.7f, spread); Net3D.packet(v, px, 0.9f, pz, th.bad, 0.32f) }
        v.draw()
        v.tag(if (separated) "Compte ordinaire" else "Compte du quotidien", -4.4f, 1.7f, -0.8f, if (separated) th.good else th.warn, 18f)
        SK.pill(c.g, if (separated) "Un compte d'administration distinct, utilisé seulement pour administrer" else "Ce compte a les droits d'administrateur du domaine", 500f, 106f, if (separated) th.good else th.warn, th.bgTop.withAlpha(0.92f), 19f)
        v.tag("Contrôleurs de domaine", 0f, 2.7f, 2.7f, th.ink, 18f)
        if (separated) v.tag("Compte d'administration à part", 0f, 0.3f, 1.1f, th.good, 17f, SK.ease(lt, 600f, 500f))
        SK.pill(c.g, if (!separated) (if (spread > 0.9f) "Un poste compromis : tout le domaine est exposé" else "Le poste est compromis…") else "L'infection reste sur le poste", 500f, 650f, if (separated) th.good else th.bad, th.bgTop.withAlpha(0.92f), 22f)
        caption(c, if (separated) "Comptes séparés" else "Des droits d'administrateur au quotidien")
    }

    private val adDcFail = beat("ad-dcfail", "Un seul contrôleur de domaine : s'il tombe, plus personne ne s'authentifie", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Actor.SERVEUR) + setOf("topic:ad-dcfail"), minMs = 6000, needs = setOf("topic:ad-dcfail"), onlyFor = setOf(Concept.DIRECTORY)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val cyc = 12000f
        val tt = SK.loop(t, cyc) * cyc
        val two = tt >= 6200f
        val lt = if (two) tt - 6200f else tt
        val down = lt > 1800f
        val v = view(c, 0f, 0.8f, 84f, 390f)
        v.grid(6.5f, 4f)
        val dcs = if (two) listOf(-1.6f, 1.6f) else listOf(0f)
        for ((i, x) in dcs.withIndex()) Net3D.server(v, x, 2.7f, if (i == 0 && down) th.bad else th.good, 1.9f, if (i == 0 && down) 0f else 0.5f)
        val pcs = listOf(-4.4f, -1.5f, 1.5f, 4.4f)
        for ((i, x) in pcs.withIndex()) {
            val ok = two || !down
            for ((k, dx) in dcs.withIndex()) {
                val alive = !(k == 0 && down)
                v.link(x, -0.8f, dx, 2.7f, if (alive) th.line else th.bad.withAlpha(0.35f), if (alive) 0.7f else 0.4f, 4f)
            }
            Net3D.pc(v, x, -0.8f, if (ok) th.good else th.bad, if (ok) 0.6f else 0.2f)
            val target = dcs.firstOrNull { true }?.let { if (two && down) dcs[1] else it } ?: 0f
            val u = SK.loop(t, 2200f, i * 450f)
            if (ok) { val (px, pz) = lerpPt(x, -0.8f, target, 2.7f, u); Net3D.packet(v, px, 0.8f, pz, th.good, 0.28f) }
        }
        v.draw()
        for ((i, x) in dcs.withIndex()) v.tag(if (i == 0 && down) "hors service" else "Contrôleur de domaine", x, 2.9f, 2.7f, if (i == 0 && down) th.bad else th.ink, 18f)
        if (!two && down) for (x in pcs) v.tag("connexion impossible", x, 1.6f, -0.8f, th.bad, 15f)
        SK.pill(c.g, when { !two && !down -> "Un seul contrôleur : tout fonctionne… tant qu'il tient"; !two -> "Plus personne ne peut s'authentifier"; else -> "Deux contrôleurs : l'autre répond, personne n'est bloqué" }, 500f, 650f, if (two || !down) th.good else th.bad, th.bgTop.withAlpha(0.92f), 21f)
        caption(c, if (two) "Avec deux contrôleurs de domaine" else "Un seul contrôleur de domaine")
    }

    private val adPoster = beat("poster", "Affiche : un annuaire et ses contrôleurs de domaine", setOf(Role.INTRO, Role.RECAP, Role.DEFINE), setOf(Goal.IDENTIFIER_MENACE, Goal.MEMORISER, Goal.COMPRENDRE_MECANISME),
        emptySet(), repeatable = true, minMs = 1500, onlyFor = setOf(Concept.DIRECTORY)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = Net3D.View(c, 0.35f * sin(c.tg / 3600f), 0.68f, 100f, 370f)
        v.grid(6f, 4f)
        Net3D.server(v, -1.4f, 1.0f, th.accent, 2.4f); Net3D.server(v, 1.4f, 1.0f, th.good, 2.4f)
        for (i in 0 until 5) { val x = -4.0f + i * 2.0f; v.link(x, -1.8f, 0f, 1.0f, th.line, 0.8f, 4f); Net3D.pc(v, x, -1.8f, mix(th.accent, th.good, i / 4f)) }
        val wv = SK.loop(t, 3000f)
        val pr = v.pt(0f, 0.04f, 1.0f); c.g.strokeCircle(pr[0], pr[1], 30f + 420f * wv, 3f, th.accent.withAlpha(0.6f * (1f - wv)))
        v.draw()
    }

    val adKind = SceneKind("net3d-ad", "Active Directory en 3D", setOf(Concept.DIRECTORY), listOf(adDomain, adGpo, adAuth, adGroups, adBenefits, adAdmin, adDcFail, adPoster))

    // ───────────────────────── Rançongiciel ─────────────────────────

    private fun ransomNet(c: StageCtx, infected: BooleanArray, tFiles: FloatArray): Net3D.View {
        val v = view(c, 0.1f, 0.78f, 82f, 395f)
        v.grid(6.5f, 4f)
        val th = v.th
        val nodes = listOf(-4.0f to -0.5f, 0f to 2.0f, 4.0f to -0.5f)
        for (i in 0..1) v.link(nodes[i].first, nodes[i].second, nodes[i + 1].first, nodes[i + 1].second, if (infected[i + 1]) th.bad else th.line, 0.9f)
        Net3D.pc(v, nodes[0].first, nodes[0].second, if (infected[0]) th.bad else th.accent, 0.8f)
        Net3D.switchBox(v, nodes[1].first, nodes[1].second)
        Net3D.server(v, nodes[2].first, nodes[2].second, if (infected[2]) th.bad else th.accent, 1.9f, if (infected[2]) 1f else 0.3f)
        for ((i, n) in listOf(nodes[0], nodes[2]).withIndex()) {
            val dev = if (i == 0) 0 else 2
            for (f in 0 until 6) {
                val enc = infected[dev] && tFiles[dev] > f * 260f
                Net3D.file(v, n.first - 0.55f + (f % 3) * 0.55f, 1.9f + (f / 3) * 0.12f, n.second + (f / 3) * 0.1f - 0.1f, if (enc) 1f else 0f)
            }
        }
        return v
    }

    private val ransomOverview = beat("ransom-overview", "Un poste infecté, des fichiers qui se verrouillent", setOf(Role.INTRO, Role.DEFINE), MECH + setOf(Goal.IDENTIFIER_MENACE),
        emptySet(), repeatable = true, minMs = 3500, needs = setOf("topic:ransom"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val cyc = SK.loop(t, 7000f) * 7000f
        val inf = booleanArrayOf(cyc > 900f, false, cyc > 3600f)
        val v = ransomNet(c, inf, floatArrayOf(cyc - 900f, 0f, cyc - 3600f))
        val u = (cyc - 2400f) / 1200f
        if (u in 0f..1f) { val (x, z) = if (u < 0.5f) lerpPt(-4.0f, -0.5f, 0f, 2.0f, u * 2f) else lerpPt(0f, 2.0f, 4.0f, -0.5f, (u - 0.5f) * 2f); Net3D.packet(v, x, 0.7f, z, th.bad, 0.4f) }
        v.draw()
        v.tag("Poste", -4.0f, 2.9f, -0.5f, if (inf[0]) th.bad else th.ink, 19f); v.tag("Serveur de fichiers", 4.0f, 3.0f, -0.5f, if (inf[2]) th.bad else th.ink, 19f)
        caption(c, "Le rançongiciel chiffre les fichiers")
    }

    private val ransomSpread = beat("ransom-spread", "Le rançongiciel se propage et chiffre les fichiers", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Actor.ATTAQUANT, Thing.ATTAQUE, Act.CHIFFRER) + setOf("topic:ransom-spread"), minMs = 7000, needs = setOf("topic:ransom-spread"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val inf = booleanArrayOf(t > 800f, false, t > 4400f)
        val tf = floatArrayOf(t - 800f, 0f, t - 4400f)
        val v = ransomNet(c, inf, tf)
        val u = (t - 2600f) / 1800f
        if (u in 0f..1f) { val (x, z) = if (u < 0.5f) lerpPt(-4.0f, -0.5f, 0f, 2.0f, u * 2f) else lerpPt(0f, 2.0f, 4.0f, -0.5f, (u - 0.5f) * 2f); Net3D.packet(v, x, 0.7f, z, th.bad, 0.4f) }
        v.draw()
        v.tag("Poste infecté", -4.0f, 2.9f, -0.5f, th.bad, 19f, SK.ease(t, 800f, 400f)); v.tag("Serveur de fichiers", 4.0f, 3.0f, -0.5f, th.ink, 19f)
        if (t > 4400f) v.tag("Partage chiffré", 4.0f, 3.5f, -0.5f, th.bad, 19f, SK.ease(t, 4400f, 400f))
        step(c, listOf("1 Entrée sur un poste", "2 Exploration du réseau", "3 Chiffrement"), if (t < 2600f) 0 else if (t < 4400f) 1 else 2)
        caption(c, "L'infection se propage")
    }

    private val ransomNote = beat("ransom-note", "Le message de rançon et son compte à rebours", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        cueSet(Actor.ATTAQUANT, Thing.MESSAGE, Thing.ATTAQUE) + setOf("topic:ransom-note"), minMs = 4500, needs = setOf("topic:ransom-note"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val x0 = 130f; val x1 = 870f; val y0 = 120f; val y1 = 560f
        c.g.fillRoundRect(x0, y0, x1, y1, 22f, mix(th.bgBottom, th.bad, 0.18f)); c.g.strokeRoundRect(x0, y0, x1, y1, 22f, 4f, th.bad)
        c.g.fillRoundRect(x0, y0, x1, y0 + 56f, 22f, th.bad.withAlpha(0.85f)); c.g.fillRect(x0, y0 + 30f, x1, y0 + 56f, th.bad.withAlpha(0.85f))
        SK.text(c.g, "SIMULATION · EXEMPLE FICTIF", 500f, y0 + 38f, 22f, th.ink, 800, center = true)
        SK.lock(c.g, 280f, 330f, 120f, th.warn, th.ink, 0f, if (t % 1400f < 160f) 4f else 0f)
        SK.text(c.g, "Tes fichiers sont chiffrés", 450f, 260f, 38f, th.ink, 800)
        SK.text(c.g, "Un paiement est exigé pour recevoir la clé.", 450f, 310f, 24f, th.ink, 600)
        val left = (72f * 3600f - t * 900f).coerceAtLeast(0f).toInt() / 1000
        fun two(n: Int) = if (n < 10) "0$n" else "$n"
        val sec = 72 * 3600 - t.toInt() / 10
        SK.text(c.g, "${two(sec / 3600)}:${two(sec / 60 % 60)}:${two(sec % 60)}", 450f, 400f, 62f, th.warn, 800, family = FontFamily.MONO)
        SK.text(c.g, "Délai avant que le prix double", 450f, 440f, 21f, th.muted, 600)
        for (i in 0 until 6) run { SK.pill(c.g, "rapport$i.docx.locked", 250f + (i % 3) * 245f, 495f + (i / 3) * 40f, th.muted, th.bgTop.withAlpha(0.85f), 16f) }
        SK.pill(c.g, "Ne paie pas avant d'avoir l'avis d'experts", 500f, 620f, th.ink, th.good.withAlpha(0.45f), 22f, SK.ease(t, 2400f, 500f))
        caption(c, "Le message de rançon")
        if (left < 0) return@beat
    }

    private val ransomBackup = beat("ransom-backup", "La sauvegarde hors ligne permet de restaurer", BODY, MECH + setOf(Goal.APPRENDRE_REFLEXE),
        cueSet(Thing.DONNEES, Actor.SERVEUR) + setOf("topic:backup"), minMs = 6500, needs = setOf("topic:backup"), onlyFor = setOf(Concept.MALWARE), excludes = setOf("topic:protect")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0.1f, 0.78f, 86f, 395f)
        v.grid(6.5f, 4f)
        val restore = SK.ease(t, 3200f, 2600f)
        val srvX = -3.4f; val bkX = 3.4f; val z = 0.5f
        Net3D.server(v, srvX, z, mix(th.bad, th.good, restore), 1.9f, 0.6f)
        // disque de sauvegarde, déconnecté puis relié
        val d = v.add(Solids.box("disque", bkX, 0.3f, z, 1.5f, 0.6f, 1.0f, v.body))
        v.add(Solids.box("disque-d", bkX, 0.62f, z, 1.1f, 0.04f, 0.6f, th.good, 0.7f, decalOf = d))
        for (f in 0 until 6) Net3D.file(v, srvX - 0.55f + (f % 3) * 0.55f, 2.1f + (f / 3) * 0.12f, z + (f / 3) * 0.1f, if (restore * 6f > f + 1f) 0f else 1f)
        val linked = t > 2600f
        if (linked) v.link(srvX, z, bkX, z, th.good, SK.ease(t, 2600f, 400f), 8f) else {
            val a = v.pt(srvX, 0.05f, z); val b = v.pt(bkX, 0.05f, z)
            SK.dashed(c.g, a[0], a[1], lerp(a[0], b[0], 0.35f), a[1], 5f, th.line, 14f, 10f)
            SK.dashed(c.g, lerp(a[0], b[0], 0.65f), a[1], b[0], b[1], 5f, th.line, 14f, 10f)
        }
        val u = (t - 3200f) / 2600f
        if (u in 0f..1f) for (k in 0 until 3) { val uu = ((u + k * 0.33f) % 1f); Net3D.packet(v, lerp(bkX, srvX, uu), 0.8f, z, th.good, 0.32f) }
        v.draw()
        v.tag("Serveur chiffré", srvX, 3.2f, z, th.bad, 19f, 1f - restore); v.tag("Fichiers restaurés", srvX, 3.2f, z, th.good, 19f, restore)
        v.tag("Sauvegarde hors ligne", bkX, 1.4f, z, th.ink, 19f)
        if (!linked) v.tag("déconnectée : intacte", 0.1f, 0.1f, z, th.muted, 17f)
        step(c, listOf("3 copies", "2 supports", "1 hors ligne"), if (t < 1500f) 0 else if (t < 3000f) 1 else 2)
        caption(c, "Restaurer sans payer")
    }

    private val ransomEntry = beat("ransom-entry", "Trois portes d'entrée : courriel, accès à distance, faille", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        emptySet(), minMs = 4500, needs = setOf("topic:ransom-entry"), onlyFor = setOf(Concept.MALWARE), excludes = setOf("topic:protect")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.78f, 86f, 390f)
        v.grid(6.5f, 4f)
        val lanes = listOf(-2.4f to "Courriel piégé", 0f to "Accès à distance", 2.4f to "Faille non corrigée")
        val hit = t > 1600f
        Net3D.pc(v, 3.4f, 0f, if (hit) th.bad else th.accent, 0.8f)
        for ((i, l) in lanes.withIndex()) {
            v.link(-4.6f, l.first, 3.4f, 0f, th.bad, 0.5f, 4f)
            val u = SK.loop(t, 3000f, i * 900f)
            val (x, z) = lerpPt(-4.6f, l.first, 3.4f, 0f, u)
            Net3D.packet(v, x, 0.6f, z, th.bad, 0.36f)
        }
        v.draw()
        for (l in lanes) v.tag(l.second, -4.6f, 0.9f, l.first, th.ink, 18f)
        v.tag("Poste visé", 3.4f, 1.9f, 0f, if (hit) th.bad else th.ink, 19f)
        caption(c, "Comment le rançongiciel entre")
    }

    private val ransomDetect = beat("ransom-detect", "Détecter le chiffrement massif et isoler le poste", BODY, MECH,
        emptySet(), minMs = 3800, needs = setOf("topic:ransom-detect"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val isolated = t > 3600f
        val v = view(c, 0f, 0.78f, 88f, 390f)
        v.grid(6.5f, 4f)
        Net3D.pc(v, -3.2f, 0f, th.bad, 0.9f); Net3D.switchBox(v, 1.2f, 0f); Net3D.server(v, 4.2f, 0f)
        v.link(-3.2f, 0f, 1.2f, 0f, if (isolated) th.line else th.bad, if (isolated) 0.3f else 1f, 6f)
        v.link(1.2f, 0f, 4.2f, 0f, th.line, 0.9f, 6f)
        val n = (t / 90f).toInt().coerceAtMost(if (isolated) 24 else 400)
        for (f in 0 until 6) Net3D.file(v, -3.75f + (f % 3) * 0.55f, 1.9f + (f / 3) * 0.12f, (f / 3) * 0.1f - 0.1f, if (n > f * 3) 1f else 0f)
        v.draw()
        v.tag("Fichiers modifiés : $n", -3.2f, 2.9f, 0f, th.bad, 19f)
        if (t > 2000f) v.tag("Alerte EDR", -3.2f, 3.6f, 0f, th.warn, 21f, SK.ease(t, 2000f, 400f))
        if (isolated) v.tag("Poste isolé du réseau", -1.0f, 0.1f, 0f, th.good, 19f, SK.ease(t, 3600f, 400f))
        caption(c, "Détecter, puis isoler")
    }

    private val ransomSegment = beat("ransom-segment", "Segmenter et limiter les droits : l'infection s'arrête", BODY, MECH + setOf(Goal.APPRENDRE_REFLEXE),
        emptySet(), minMs = 4500, needs = setOf("topic:ransom-segment"), onlyFor = setOf(Concept.MALWARE), excludes = setOf("topic:protect")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, -0.3f, 0.65f, 70f, 395f)
        v.grid(6.5f, 3.5f)
        Net3D.pc(v, -4.4f, -1.2f, th.bad, 0.9f); Net3D.pc(v, -4.4f, 1.2f, th.accent)
        Net3D.server(v, 4.4f, 0f, th.good)
        Net3D.wall(v, 0f, 2.6f, 0f, 0.2f)
        val u = SK.loop(t, 3200f)
        val x = lerp(-4.0f, -0.5f, (u / 0.7f).coerceAtMost(1f))
        Net3D.packet(v, x, 0.6f, -1.0f, th.bad, 0.38f)
        v.draw()
        v.tag("Zone des postes", -4.4f, 2.0f, 0f, th.ink, 19f); v.tag("Zone des serveurs", 4.4f, 2.8f, 0f, th.good, 19f); v.tag("Réseau segmenté", 0f, 2.0f, 0f, th.warn, 20f)
        if (u > 0.7f) { val p = v.pt(-0.4f, 0.7f, -1.0f); SK.burst(c.g, p[0], p[1], 46f, th.bad, ((u - 0.7f) / 0.3f).coerceIn(0f, 1f)) }
        caption(c, "L'infection ne passe pas le mur")
    }



    private val ransomSigns = beat("ransom-signs", "Les signes : fichiers illisibles, message, poste lent, accès perdus", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        emptySet(), minMs = 8000, needs = setOf("topic:signs"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val th = c.th
        val (ph, lt) = phaseOf(c, 4)
        when (ph) {
            0 -> {
                bg(c)
                val names = listOf("devis_client.docx" to "a9f3q", "rapport_annuel.pdf" to "m2x7k", "photos_equipe.zip" to "t8p4z", "budget_2027.xlsx" to "q1w6e", "contrats.pdf" to "r5y0u")
                for ((i, n) in names.withIndex()) {
                    val y = 118f + i * 84f
                    val enc = lt > 500f + i * 650f
                    c.g.fillRoundRect(120f, y, 880f, y + 66f, 14f, if (enc) th.bad.withAlpha(0.22f) else th.panel.withAlpha(0.85f))
                    c.g.strokeRoundRect(120f, y, 880f, y + 66f, 14f, 2.5f, if (enc) th.bad else th.line)
                    SK.text(c.g, n.first + if (enc) "." + n.second else "", 150f, y + 43f, 26f, if (enc) th.bad else th.ink, 700, family = FontFamily.MONO)
                    if (enc) SK.pill(c.g, "extension inconnue · ne s'ouvre plus", 700f, y + 33f, th.ink, th.bad.withAlpha(0.55f), 16f)
                }
                caption(c, "Des fichiers qui ne s'ouvrent plus")
            }
            1 -> ransomNote.draw!!(sub(c, lt))
            2 -> {
                bg(c)
                val bars = listOf("Activité du disque" to 1f, "Processeur" to 0.93f)
                for ((i, b) in bars.withIndex()) {
                    val y = 210f + i * 130f
                    SK.text(c.g, b.first, 130f, y - 18f, 26f, th.ink, 700)
                    c.g.fillRoundRect(130f, y, 870f, y + 48f, 24f, th.panel.withAlpha(0.9f))
                    val fill = (SK.ease(lt, 200f + i * 300f, 1200f) * (b.second - 0.04f * kotlin.math.abs(sin(lt / 140f + i)))).coerceIn(0f, 1f)
                    c.g.fillRoundRect(130f, y, 130f + 740f * fill, y + 48f, 24f, th.bad)
                }
                val sp = lt / 4f
                c.g.strokeArc(500f, 520f, 46f, sp, 260f, 9f, th.warn)
                SK.pill(c.g, "Le poste ralentit : activité disque inhabituelle", 500f, 610f, th.warn, th.bgTop.withAlpha(0.92f), 22f)
                caption(c, "Un poste très lent")
            }
            else -> {
                bg(c)
                val v = view(c, 0f, 0.8f, 84f, 390f)
                v.grid(6.5f, 4f)
                Net3D.pc(v, 0f, -1.2f, th.accent)
                Net3D.server(v, -4.2f, 2.3f, th.bad, 1.9f, 0.1f)
                Net3D.server(v, 4.2f, 2.3f, th.bad, 1.9f, 0.1f)
                val a = SK.ease(lt, 600f, 800f)
                v.link(0f, -1.2f, -4.2f, 2.3f, th.bad, 0.25f + 0.4f * a, 5f); v.link(0f, -1.2f, 4.2f, 2.3f, th.bad, 0.25f + 0.4f * a, 5f)
                v.draw()
                v.tag("Dossiers partagés : inaccessibles", -4.2f, 2.5f, 2.3f, th.bad, 18f, a)
                v.tag("Sauvegardes : inaccessibles", 4.2f, 2.5f, 2.3f, th.bad, 18f, SK.ease(lt, 1400f, 500f))
                caption(c, "Partages et sauvegardes coupés")
            }
        }
        step(c, listOf("1 · Fichiers illisibles", "2 · Message", "3 · Poste lent", "4 · Accès perdus"), ph, 676f)
    }

    private val ransomConsequences = beat("ransom-consequences", "Les conséquences : arrêt d'activité, fuite de données, aucune garantie en payant", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        emptySet(), minMs = 8000, needs = setOf("topic:consequences"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val th = c.th
        val (ph, lt) = phaseOf(c, 3)
        when (ph) {
            0 -> ransomImpact.draw!!(sub(c, lt))
            1 -> ransomLeak.draw!!(sub(c, lt))
            else -> {
                bg(c)
                val v = view(c, 0f, 0.78f, 86f, 390f)
                v.grid(6.5f, 4f)
                Net3D.pc(v, -3.8f, 0.4f, th.bad, 0.9f)
                for (f in 0 until 3) Net3D.file(v, -4.3f + f * 0.55f, 1.7f, 0.4f, 1f)
                Net3D.server(v, 3.8f, 0.4f, th.bad, 1.9f, 0.9f)
                v.link(-3.8f, 0.4f, 3.8f, 0.4f, th.warn, 0.6f, 5f)
                val u = SK.ease(lt, 500f, 1800f)
                if (u in 0.01f..0.99f) Net3D.packet(v, lerp(-3.8f, 3.8f, u), 0.9f, 0.4f, th.warn, 0.36f)
                v.draw()
                v.tag("Rançon payée", 0f, 1.5f, 0.4f, th.warn, 20f, SK.ease(lt, 500f, 400f))
                v.tag("Attaquant", 3.8f, 2.8f, 0.4f, th.bad, 19f)
                v.tag("Fichiers toujours chiffrés ?", -3.8f, 2.7f, 0.4f, th.bad, 19f, SK.ease(lt, 2500f, 500f))
                SK.pill(c.g, "Aucune garantie de récupérer les fichiers", 500f, 600f, th.bad, th.bgTop.withAlpha(0.92f), 23f, SK.ease(lt, 2900f, 500f))
                SK.pill(c.g, "Et le paiement finance les criminels", 500f, 650f, th.warn, th.bgTop.withAlpha(0.92f), 21f, SK.ease(lt, 3800f, 500f))
                caption(c, "Payer ne règle rien")
            }
        }
        step(c, listOf("1 · Activité à l'arrêt", "2 · Données publiées", "3 · Payer ne garantit rien"), ph, 690f)
    }

    private val PROTECT = listOf("Sauvegardes hors ligne", "Mises à jour", "Pièces jointes prudentes", "Droits limités", "Double authentification", "Segmentation")
    private val PROTECT_RE = listOf("sauvegarde", "mets a jour|correctif|mises? a jour", "pieces? jointes?|liens", "limite les droits|droits", "double authentification|mfa|2fa", "segmente|segmentation")

    private val ransomProtect = beat("ransom-protect", "Se protéger : des couches successives autour du poste", BODY, MECH + setOf(Goal.APPRENDRE_REFLEXE),
        emptySet(), minMs = 8000, needs = setOf("topic:protect"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val items = c.scene.items.map { DomainDetector.normalize(it) }
        val order: List<Int> = items.mapNotNull { it -> PROTECT_RE.indexOfFirst { re -> Regex(re).containsMatchIn(it) }.takeIf { k -> k >= 0 } }.distinct().ifEmpty { listOf(0, 1, 2, 3, 4) }
        val (ph, lt) = phaseOf(c, if (c.scene.items.size == order.size) order.size else 5)
        val shown = (ph + 1).coerceAtMost(order.size)
        val cx = 500f; val cy = 340f; val step = 36f; val r0 = 64f
        val rOuter = r0 + order.size * step
        for (k in shown - 1 downTo 0) {
            val r = r0 + (k + 1) * step
            val cur = k == shown - 1
            val a = if (cur) SK.ease(lt, 100f, 600f) else 1f
            c.g.strokeCircle(cx, cy, r, if (cur) 11f else 8f, (if (cur) th.accent else th.good).withAlpha(0.85f * a))
        }
        c.g.fillCircle(cx, cy, r0 - 6f, th.panel.withAlpha(0.95f)); c.g.strokeCircle(cx, cy, r0 - 6f, 3f, th.ink.withAlpha(0.7f))
        SK.text(c.g, "Poste", cx, cy + 9f, 26f, th.ink, 800, center = true)
        // l'attaque arrive de loin, elle bute sur la couche la plus extérieure
        val rr = r0 + shown * step
        val u = SK.loop(t, 2600f)
        val dist = lerp(rOuter + 150f, rr + 8f, (u / 0.7f).coerceAtMost(1f))
        val ang = -0.6f
        val px = cx + dist * cos(ang); val py = cy + dist * sin(ang)
        c.g.fillCircle(px, py, 11f, th.bad)
        if (u > 0.7f) SK.burst(c.g, px, py, 40f, th.bad, ((u - 0.7f) / 0.3f).coerceIn(0f, 1f))
        for ((k, idx) in order.withIndex()) {
            if (k >= shown) break
            val left = k % 2 == 0
            val row = k / 2
            val y = 150f + row * 120f
            val cur = k == shown - 1
            val x = if (left) 252f else 748f
            val label = PROTECT[idx]
            val w = c.g.measureText(label, SK.font(18f)) + 40f
            val bx = if (left) x - w else x
            c.g.fillRoundRect(bx, y - 22f, bx + w, y + 22f, 22f, (if (cur) th.accent else th.good).withAlpha(0.28f))
            c.g.strokeRoundRect(bx, y - 22f, bx + w, y + 22f, 22f, 2f, if (cur) th.accent else th.good)
            SK.text(c.g, label, bx + 20f, y + 7f, 18f, th.ink, 700)
        }
        caption(c, "Chaque couche réduit le risque")
    }

    private val ransomLock = beat("ransom-lock", "À retenir : verrouillé par l'attaquant, restauré par la sauvegarde", setOf(Role.RECAP), setOf(Goal.MEMORISER, Goal.APPRENDRE_REFLEXE),
        emptySet(), repeatable = true, minMs = 2500, onlyFor = setOf(Concept.MALWARE), needs = setOf("topic:ransom")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val model = Models3D.build(Models3D.Kind.LOCK, Models3D.Pal(mix(th.bgBottom, th.ink, 0.7f), mix(th.bgBottom, th.ink, 0.25f), th.warn, th.bgTop, th.good))
        val cam = Cam3D(-0.5f + 0.5f * sin(c.tg / 3000f), 0.38f, 15f, 520f / model.fitAssembled, 500f, 330f, model.centerAssembled)
        Render3D.draw(c.g, model, cam, 0f, t)
        SK.pill(c.g, "Sauvegarde hors ligne, testée : on restaure sans payer", 500f, 640f, th.ink, th.good.withAlpha(0.45f), 22f)
        caption(c, "À retenir")
    }

    private val ransomRecap = beat("ransom-recap", "À retenir : verrouillé par l'attaquant, restauré par la sauvegarde", setOf(Role.RECAP), setOf(Goal.MEMORISER, Goal.APPRENDRE_REFLEXE),
        emptySet(), minMs = 2500, onlyFor = setOf(Concept.MALWARE), needs = setOf("topic:ransom")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val model = Models3D.build(Models3D.Kind.LOCK, Models3D.Pal(mix(th.bgBottom, th.ink, 0.7f), mix(th.bgBottom, th.ink, 0.25f), th.warn, th.bgTop, th.good))
        val cam = Cam3D(0.6f - 0.5f * sin(c.tg / 3000f), 0.38f, 15f, 520f / model.fitAssembled, 500f, 330f, model.centerAssembled)
        Render3D.draw(c.g, model, cam, 0f, t)
        SK.pill(c.g, "Sauvegarde hors ligne, testée : on restaure sans payer", 500f, 640f, th.ink, th.good.withAlpha(0.45f), 22f)
        caption(c, "À retenir : la sauvegarde")
    }


    private val ransomImpact = beat("ransom-impact", "Tout s'arrête : postes et serveurs verrouillés", BODY, MECH,
        emptySet(), minMs = 4000, needs = setOf("topic:ransom-impact"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = ransomNet(c, booleanArrayOf(true, true, true), floatArrayOf(t + 1500f, 0f, t + 1500f))
        v.draw()
        v.tag("Postes verrouillés", -4.0f, 2.9f, -0.5f, th.bad, 19f); v.tag("Serveurs à l'arrêt", 4.0f, 3.0f, -0.5f, th.bad, 19f)
        SK.pill(c.g, "Activité à l'arrêt", 500f, 650f, th.warn, th.bgTop.withAlpha(0.92f), 24f, SK.ease(t, 800f, 500f))
        caption(c, "Les conséquences")
    }

    private val ransomLeak = beat("ransom-leak", "Double extorsion : les données partent avant le chiffrement", BODY, MECH + setOf(Goal.IDENTIFIER_MENACE),
        emptySet(), minMs = 4500, needs = setOf("topic:ransom-leak"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = view(c, 0f, 0.78f, 86f, 390f)
        v.grid(6.5f, 4f)
        Net3D.server(v, -3.4f, 0.4f, th.bad, 1.9f, 0.9f)
        Net3D.pc(v, 3.6f, 0.4f, th.bad, 0.9f)
        v.link(-3.4f, 0.4f, 3.6f, 0.4f, th.bad, 0.7f, 5f)
        for (k in 0 until 4) { val u = SK.loop(t, 2800f, k * 700f); Net3D.packet(v, lerp(-3.4f, 3.6f, u), 0.9f, 0.4f, th.warn, 0.34f) }
        v.draw()
        v.tag("Données de l'entreprise", -3.4f, 2.8f, 0.4f, th.ink, 19f); v.tag("Attaquant", 3.6f, 1.9f, 0.4f, th.bad, 19f)
        v.tag("Copiées avant le chiffrement", 0f, 1.2f, 0.4f, th.warn, 19f, SK.ease(t, 900f, 500f))
        if (t > 3800f) v.tag("Menace de publication", 3.6f, 2.7f, 0.4f, th.bad, 19f, SK.ease(t, 3800f, 400f))
        caption(c, "Double extorsion")
    }

    private val ransomReact = beat("ransom-react", "Réagir : débrancher, prévenir, ne pas payer", BODY, MECH + setOf(Goal.APPRENDRE_REFLEXE),
        emptySet(), minMs = 4500, needs = setOf("topic:ransom-react"), onlyFor = setOf(Concept.MALWARE)) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val cut = t > 1500f
        val v = view(c, 0f, 0.78f, 88f, 390f)
        v.grid(6.5f, 4f)
        Net3D.pc(v, -3.4f, 0f, th.bad, 0.9f); Net3D.switchBox(v, 1.0f, 0f); Net3D.server(v, 4.2f, 0f)
        v.link(1.0f, 0f, 4.2f, 0f, th.line, 0.9f, 6f)
        if (cut) {
            val a = v.pt(-3.4f, 0.05f, 0f); val b = v.pt(1.0f, 0.05f, 0f)
            SK.dashed(c.g, a[0], a[1], lerp(a[0], b[0], 0.3f), a[1], 5f, th.line, 12f, 10f); SK.dashed(c.g, lerp(a[0], b[0], 0.7f), a[1], b[0], b[1], 5f, th.line, 12f, 10f)
        } else v.link(-3.4f, 0f, 1.0f, 0f, th.bad, 1f, 6f)
        v.draw()
        v.tag(if (cut) "Poste isolé" else "Poste touché", -3.4f, 2.0f, 0f, if (cut) th.good else th.bad, 19f)
        step(c, listOf("1 Débrancher du réseau", "2 Prévenir l'informatique", "3 Ne pas payer seul"), if (t < 1500f) 0 else if (t < 3200f) 1 else 2)
        caption(c, "Le bon réflexe")
    }

    private val ransomPoster = beat("poster", "Affiche : un poste dont les fichiers se verrouillent", setOf(Role.INTRO, Role.RECAP, Role.DEFINE), setOf(Goal.IDENTIFIER_MENACE, Goal.MEMORISER, Goal.COMPRENDRE_MECANISME),
        emptySet(), repeatable = true, minMs = 1500, onlyFor = setOf(Concept.MALWARE), needs = setOf("topic:ransom")) { c ->
        val t = c.t; val th = c.th
        bg(c)
        val v = Net3D.View(c, 0.3f * sin(c.tg / 3600f), 0.62f, 120f, 380f)
        v.grid(5f, 3f)
        Net3D.pc(v, 0f, 0f, th.bad, 1f)
        for (f in 0 until 9) Net3D.file(v, -1.1f + (f % 3) * 1.1f, 1.7f + (f / 3) * 0.14f, -0.9f + (f / 3) * 0.6f, if (t > 500f + f * 220f) 1f else 0f)
        v.draw()
    }

    val ransomKind = SceneKind("net3d-ransom", "Rançongiciel en 3D", setOf(Concept.MALWARE), listOf(ransomOverview, ransomSpread, ransomNote, ransomBackup, ransomEntry, ransomDetect, ransomSegment, ransomSigns, ransomConsequences, ransomProtect, ransomLock, ransomRecap, ransomImpact, ransomLeak, ransomReact, ransomPoster))
}
