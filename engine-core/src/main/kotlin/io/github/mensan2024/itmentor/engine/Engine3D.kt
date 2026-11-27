// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Petit moteur 3D logiciel, 100 % Kotlin commun : mêmes images sur Android, navigateur et tests, aucune permission,
 * aucune bibliothèque. C'est de la 3D « stylisée » (objets à facettes, éclairage par face, ombre au sol) dessinée
 * avec les polygones 2D du moteur : pas de texture, pas de reflet, pas d'ombre portée précise, pas de personnage animé.
 *
 * Principe : chaque objet est une liste de [Part] (solides convexes : boîtes, cylindres, segments de tube). Pour chaque
 * image, les sommets sont transformés (rotation locale, éclatement, orbite de caméra, perspective), les faces tournées
 * vers l'arrière sont écartées, les solides sont triés du plus loin au plus proche (algorithme du peintre), puis chaque
 * face est dessinée avec une teinte selon son orientation par rapport à la lumière.
 */

/**
 * Solide convexe. [v] = sommets (x, y, z à la suite), [f] = faces (indices de sommets, dans l'ordre du contour).
 * [decalOf] ≥ 0 : détail posé sur un autre solide (touche, voyant, puce), dessiné juste après lui.
 * [explode] : déplacement (x, y, z) à pleine vue éclatée. [rotX] : rotation fixe en degrés autour de [pivot] (charnière).
 * [blink] ≥ 0 : voyant qui clignote (phase). [label] : nom affiché sur la vue éclatée.
 */
internal class Part(
    val name: String,
    val v: FloatArray,
    val f: Array<IntArray>,
    val color: Int,
    val emissive: Float = 0f,
    val explode: FloatArray = ZERO,
    val decalOf: Int = -1,
    val rotX: Float = 0f,
    val pivot: FloatArray = ZERO,
    val blink: Float = -1f,
    val label: String? = null,
) {
    companion object { val ZERO = floatArrayOf(0f, 0f, 0f) }
}

/** Objet 3D complet. [groundY] : hauteur du sol ; [centerY] : hauteur visée par la caméra (assemblé → éclaté). */
internal class Model3D(
    val parts: List<Part>,
    val groundY: Float,
    val footprint: Float,
    val centerAssembled: Float,
    val centerExploded: Float,
    /** Unités visibles en hauteur, assemblé puis éclaté : sert à calculer l'échelle pour remplir la scène. */
    val fitAssembled: Float,
    val fitExploded: Float,
)

/** Caméra en orbite : [s] pixels par unité au point visé, [dist] distance (perspective), centre d'écran ([cx], [cy]). */
internal class Cam3D(val yaw: Float, val pitch: Float, val dist: Float, val s: Float, val cx: Float, val cy: Float, val targetY: Float)

internal object Solids {
    private val BOX_FACES = arrayOf(
        intArrayOf(0, 2, 6, 4), intArrayOf(1, 3, 7, 5), intArrayOf(0, 1, 5, 4),
        intArrayOf(2, 3, 7, 6), intArrayOf(0, 1, 3, 2), intArrayOf(4, 5, 7, 6),
    )

    fun box(name: String, cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, color: Int,
            emissive: Float = 0f, explode: FloatArray = Part.ZERO, decalOf: Int = -1, rotX: Float = 0f,
            pivot: FloatArray = Part.ZERO, blink: Float = -1f, label: String? = null): Part {
        val v = FloatArray(24)
        for (i in 0 until 8) {
            v[i * 3] = cx + (if (i and 1 == 0) -sx else sx) / 2f
            v[i * 3 + 1] = cy + (if (i shr 1 and 1 == 0) -sy else sy) / 2f
            v[i * 3 + 2] = cz + (if (i shr 2 and 1 == 0) -sz else sz) / 2f
        }
        return Part(name, v, BOX_FACES, color, emissive, explode, decalOf, rotX, pivot, blink, label)
    }

    /** Cylindre vertical à [n] pans. */
    fun cylinder(name: String, cx: Float, cy: Float, cz: Float, r: Float, h: Float, n: Int, color: Int,
                 emissive: Float = 0f, explode: FloatArray = Part.ZERO, decalOf: Int = -1, label: String? = null): Part {
        val v = FloatArray(n * 2 * 3)
        for (i in 0 until n) {
            val a = (2.0 * PI * i / n).toFloat()
            v[i * 3] = cx + r * cos(a); v[i * 3 + 1] = cy - h / 2f; v[i * 3 + 2] = cz + r * sin(a)
            v[(n + i) * 3] = cx + r * cos(a); v[(n + i) * 3 + 1] = cy + h / 2f; v[(n + i) * 3 + 2] = cz + r * sin(a)
        }
        val faces = ArrayList<IntArray>()
        faces += IntArray(n) { it }
        faces += IntArray(n) { n + it }
        for (i in 0 until n) { val j = (i + 1) % n; faces += intArrayOf(i, j, n + j, n + i) }
        return Part(name, v, faces.toTypedArray(), color, emissive, explode, decalOf, 0f, Part.ZERO, -1f, label)
    }

    /**
     * Tube à section rectangulaire le long d'une ligne brisée du plan (x, y) : un solide par segment, anneaux partagés
     * entre segments voisins (pas de fente). [a] = demi-épaisseur dans le plan, [d] = profondeur en z.
     */
    fun tube(name: String, pts: List<Pair<Float, Float>>, a: Float, d: Float, color: Int, explode: FloatArray = Part.ZERO): List<Part> {
        val n = pts.size
        // normale de chaque point : moyenne des normales des segments voisins
        val nx = FloatArray(n); val ny = FloatArray(n)
        for (i in 0 until n) {
            val p0 = pts[maxOf(i - 1, 0)]; val p1 = pts[minOf(i + 1, n - 1)]
            var tx = p1.first - p0.first; var ty = p1.second - p0.second
            val l = sqrt(tx * tx + ty * ty).coerceAtLeast(1e-4f); tx /= l; ty /= l
            nx[i] = -ty; ny[i] = tx
        }
        fun ring(i: Int): FloatArray {
            val (px, py) = pts[i]
            return floatArrayOf(
                px + a * nx[i], py + a * ny[i], d / 2f,
                px - a * nx[i], py - a * ny[i], d / 2f,
                px - a * nx[i], py - a * ny[i], -d / 2f,
                px + a * nx[i], py + a * ny[i], -d / 2f,
            )
        }
        val out = ArrayList<Part>()
        for (i in 0 until n - 1) {
            val r0 = ring(i); val r1 = ring(i + 1)
            val v = r0 + r1
            val faces = ArrayList<IntArray>()
            for (k in 0 until 4) faces += intArrayOf(k, (k + 1) % 4, 4 + (k + 1) % 4, 4 + k)
            if (i == 0) faces += intArrayOf(0, 1, 2, 3)
            if (i == n - 2) faces += intArrayOf(4, 5, 6, 7)
            out += Part("$name$i", v, faces.toTypedArray(), color, 0f, explode)
        }
        return out
    }
}

internal object Render3D {
    private class Poly(val path: PathData, val fill: Int, val edge: Int)

    /** Lumière attachée à la caméra (studio) : vers le haut, la gauche et la caméra. */
    private val LX = -0.45f; private val LY = 0.75f; private val LZ = -0.50f
    private val LL = sqrt(LX * LX + LY * LY + LZ * LZ)

    private fun shade(color: Int, brightness: Float, emissive: Float): Int {
        val b = brightness.coerceIn(0.10f, 1.15f)
        val lit = mix(0xFF000000.toInt(), color, b.coerceAtMost(1f))
        val boosted = if (b > 1f) mix(lit, 0xFFFFFFFF.toInt(), (b - 1f) * 0.6f) else lit
        return if (emissive > 0f) mix(boosted, color, emissive) else boosted
    }

    /** Transforme un point du monde en espace caméra : (x, y, z) → tableau de 3 floats écrit dans [out]. */
    private fun toCam(cam: Cam3D, x: Float, y: Float, z: Float, out: FloatArray, o: Int) {
        val yy = y - cam.targetY
        val cy = cos(cam.yaw); val sy = sin(cam.yaw)
        val x1 = x * cy + z * sy
        val z1 = -x * sy + z * cy
        val cp = cos(cam.pitch); val sp = sin(cam.pitch)
        out[o] = x1
        out[o + 1] = yy * cp + z1 * sp          // vu d'en haut : les points proches (z < 0) descendent à l'écran
        out[o + 2] = -yy * sp + z1 * cp         // et les points hauts se rapprochent de la caméra
    }

    private fun px(cam: Cam3D, x: Float, z: Float): Float = cam.cx + cam.s * cam.dist / (z + cam.dist) * x
    private fun py(cam: Cam3D, y: Float, z: Float): Float = cam.cy - cam.s * cam.dist / (z + cam.dist) * y

    /** Ombre de contact et anneau lumineux au sol. */
    fun floor(g: Gfx, m: Model3D, cam: Cam3D, explode: Float, accent: Int) {
        val o = FloatArray(3)
        fun ring(r: Float): PathData {
            val p = PathData()
            for (i in 0..40) {
                val a = (2.0 * PI * i / 40).toFloat()
                toCam(cam, r * cos(a), m.groundY, r * sin(a), o, 0)
                val x = px(cam, o[0], o[2]); val y = py(cam, o[1], o[2])
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            return p.close()
        }
        val r = m.footprint
        val lift = 1f - 0.45f * explode
        g.fillPath(ring(r * 1.25f), accent.withAlpha(0.07f * lift))
        g.fillPath(ring(r * 1.0f), 0xFF000000.toInt().withAlpha(0.18f * lift))
        g.fillPath(ring(r * 0.72f), 0xFF000000.toInt().withAlpha(0.22f * lift))
        g.strokePath(ring(r * 1.25f), 2f, accent.withAlpha(0.35f))
    }

    /** Dessine l'objet. Renvoie, pour chaque pièce avec [Part.label], sa position à l'écran (pour les repères). */
    fun draw(g: Gfx, m: Model3D, cam: Cam3D, explode: Float, tMs: Float): Map<String, Pair<Float, Float>> {
        val parts = m.parts
        val n = parts.size
        val cv = Array(n) { FloatArray(parts[it].v.size) }
        val centers = FloatArray(n * 3)
        val tmp = FloatArray(3)
        for ((i, p) in parts.withIndex()) {
            val vc = cv[i]
            val ex = p.explode[0] * explode; val ey = p.explode[1] * explode; val ez = p.explode[2] * explode
            val rad = (p.rotX * PI / 180.0).toFloat()
            val cr = cos(rad); val sr = sin(rad)
            var sx = 0f; var sy = 0f; var sz = 0f
            for (k in 0 until p.v.size / 3) {
                var x = p.v[k * 3]; var y = p.v[k * 3 + 1]; var z = p.v[k * 3 + 2]
                if (p.rotX != 0f) {
                    val dy = y - p.pivot[1]; val dz = z - p.pivot[2]
                    y = p.pivot[1] + dy * cr - dz * sr
                    z = p.pivot[2] + dy * sr + dz * cr
                }
                toCam(cam, x + ex, y + ey, z + ez, tmp, 0)
                vc[k * 3] = tmp[0]; vc[k * 3 + 1] = tmp[1]; vc[k * 3 + 2] = tmp[2]
                sx += tmp[0]; sy += tmp[1]; sz += tmp[2]
            }
            val cnt = (p.v.size / 3).toFloat()
            centers[i * 3] = sx / cnt; centers[i * 3 + 1] = sy / cnt; centers[i * 3 + 2] = sz / cnt
        }
        // Ordre : du plus loin au plus proche ; un détail suit toujours le solide qui le porte.
        val key = FloatArray(n) { i ->
            val p = parts[i]
            if (p.decalOf >= 0) centers[p.decalOf * 3 + 2] - 0.001f * (1 + i % 50) else centers[i * 3 + 2]
        }
        val order = (0 until n).sortedWith(compareBy({ -key[it] }, { it }))
        val labels = HashMap<String, Pair<Float, Float>>()
        for (i in order) {
            val p = parts[i]
            val vc = cv[i]
            val cx0 = centers[i * 3]; val cy0 = centers[i * 3 + 1]; val cz0 = centers[i * 3 + 2]
            p.label?.let { labels[it] = px(cam, cx0, cz0) to py(cam, cy0, cz0) }
            val emissive = if (p.blink >= 0f) { if (((tMs / 520f + p.blink) % 1f) < 0.55f) 1f else 0.15f } else p.emissive
            for (face in p.f) {
                val a = face[0]; val b = face[1]; val c = face[2]
                val ax = vc[a * 3]; val ay = vc[a * 3 + 1]; val az = vc[a * 3 + 2]
                var nx = (vc[b * 3 + 1] - ay) * (vc[c * 3 + 2] - az) - (vc[b * 3 + 2] - az) * (vc[c * 3 + 1] - ay)
                var ny = (vc[b * 3 + 2] - az) * (vc[c * 3] - ax) - (vc[b * 3] - ax) * (vc[c * 3 + 2] - az)
                var nz = (vc[b * 3] - ax) * (vc[c * 3 + 1] - ay) - (vc[b * 3 + 1] - ay) * (vc[c * 3] - ax)
                var fx = 0f; var fy = 0f; var fz = 0f
                for (k in face) { fx += vc[k * 3]; fy += vc[k * 3 + 1]; fz += vc[k * 3 + 2] }
                fx /= face.size; fy /= face.size; fz /= face.size
                // Normale sortante : elle s'éloigne du centre du solide (valable pour des solides convexes).
                if (nx * (fx - cx0) + ny * (fy - cy0) + nz * (fz - cz0) < 0f) { nx = -nx; ny = -ny; nz = -nz }
                val nl = sqrt(nx * nx + ny * ny + nz * nz)
                if (nl < 1e-6f) continue
                nx /= nl; ny /= nl; nz /= nl
                // Face tournée vers l'arrière : écartée.
                if (nx * fx + ny * fy + nz * (fz + cam.dist) >= 0f) continue
                val lambert = ((nx * LX + ny * LY + nz * LZ) / LL).coerceAtLeast(0f)
                val fill = shade(p.color, 0.34f + 0.78f * lambert, emissive)
                val path = PathData()
                for ((q, k) in face.withIndex()) {
                    val x = px(cam, vc[k * 3], vc[k * 3 + 2]); val y = py(cam, vc[k * 3 + 1], vc[k * 3 + 2])
                    if (q == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                g.fillPath(path, fill)
                g.strokePath(path, 1.4f, mix(fill, 0xFF000000.toInt(), 0.35f))
            }
        }
        return labels
    }
}

/** Objets de la bibliothèque 3D. Chaque modèle dépend de la palette du format. */
internal object Models3D {
    enum class Kind { LAPTOP, SERVER, LOCK }

    class Pal(val body: Int, val dark: Int, val accent: Int, val screen: Int, val glow: Int)

    fun build(kind: Kind, p: Pal): Model3D = when (kind) {
        Kind.LAPTOP -> laptop(p)
        Kind.SERVER -> server(p)
        Kind.LOCK -> padlock(p)
    }

    private fun e(x: Float, y: Float, z: Float) = floatArrayOf(x, y, z)

    /** Ordinateur portable ouvert ; en vue éclatée : écran, clavier, carte mère, batterie, châssis se séparent. */
    private fun laptop(p: Pal): Model3D {
        val parts = ArrayList<Part>()
        fun add(x: Part): Int { parts += x; return parts.lastIndex }
        val pivot = floatArrayOf(0f, 0.10f, 1.40f)
        val ang = 103f
        // Châssis (fond + parois)
        val bottomE = e(0f, -1.45f, 0f)
        val bottom = add(Solids.box("bas", 0f, -0.10f, 0f, 4.0f, 0.06f, 2.8f, p.dark, explode = bottomE, label = "Châssis"))
        add(Solids.box("paroi-g", -1.97f, -0.02f, 0f, 0.06f, 0.20f, 2.8f, p.dark, explode = bottomE, decalOf = bottom))
        add(Solids.box("paroi-d", 1.97f, -0.02f, 0f, 0.06f, 0.20f, 2.8f, p.dark, explode = bottomE, decalOf = bottom))
        add(Solids.box("paroi-av", 0f, -0.02f, -1.37f, 3.9f, 0.20f, 0.06f, p.dark, explode = bottomE, decalOf = bottom))
        add(Solids.box("paroi-ar", 0f, -0.02f, 1.37f, 3.9f, 0.20f, 0.06f, p.dark, explode = bottomE, decalOf = bottom))
        // Batterie
        val bat = add(Solids.box("batterie", 0f, -0.04f, -0.10f, 2.6f, 0.07f, 1.2f, mix(p.dark, p.accent, 0.35f), explode = e(0f, -0.75f, 0f), label = "Batterie"))
        add(Solids.box("batterie-b", 0f, 0.0f, -0.10f, 2.0f, 0.02f, 0.5f, p.accent, 0.5f, e(0f, -0.75f, 0f), decalOf = bat))
        // Carte mère + processeur + mémoire + ventilateur
        val board = add(Solids.box("carte", 0f, 0.02f, 0.15f, 3.5f, 0.04f, 2.2f, 0xFF1F6B4A.toInt(), explode = e(0f, 0f, 0f), label = "Carte mère"))
        add(Solids.box("cpu", -0.5f, 0.07f, 0.1f, 0.62f, 0.07f, 0.62f, 0xFFB9C2CC.toInt(), explode = e(0f, 0.35f, 0f), decalOf = board, label = "Processeur"))
        add(Solids.box("cpu-d", -0.5f, 0.11f, 0.1f, 0.34f, 0.02f, 0.34f, p.accent, 0.6f, e(0f, 0.35f, 0f), decalOf = board))
        add(Solids.box("ram1", 0.95f, 0.065f, 0.55f, 1.1f, 0.05f, 0.22f, 0xFF2B2F36.toInt(), explode = e(0f, 0.25f, 0f), decalOf = board, label = "Mémoire"))
        add(Solids.box("ram2", 0.95f, 0.065f, 0.15f, 1.1f, 0.05f, 0.22f, 0xFF2B2F36.toInt(), explode = e(0f, 0.25f, 0f), decalOf = board))
        add(Solids.cylinder("ventilo", 1.1f, 0.075f, -0.55f, 0.42f, 0.07f, 14, 0xFF59616B.toInt(), explode = e(0f, 0.3f, 0f), decalOf = board))
        // Clavier (plateau + touches)
        val deckE = e(0f, 1.35f, 0f)
        val deck = add(Solids.box("plateau", 0f, 0.115f, 0f, 4.0f, 0.06f, 2.8f, p.body, explode = deckE, label = "Clavier"))
        for (r in 0 until 5) for (c in 0 until 11) {
            val kx = -1.65f + c * 0.33f; val kz = -0.55f + r * 0.30f
            add(Solids.box("t", kx, 0.16f, kz, 0.26f, 0.045f, 0.22f, mix(p.dark, p.body, 0.15f), explode = deckE, decalOf = deck))
        }
        add(Solids.box("trackpad", 0f, 0.15f, -1.02f, 1.3f, 0.012f, 0.6f, mix(p.dark, p.accent, 0.25f), 0f, deckE, decalOf = deck))
        // Écran : couvercle + dalle + interface
        val lidE = e(0f, 2.45f, 0.9f)
        val lid = add(Solids.box("capot", 0f, 0.15f, 0f, 4.0f, 0.10f, 2.8f, p.body, explode = lidE, rotX = ang, pivot = pivot, label = "Écran"))
        val scr = add(Solids.box("dalle", 0f, 0.085f, 0f, 3.7f, 0.02f, 2.5f, p.screen, 0.9f, lidE, decalOf = lid, rotX = ang, pivot = pivot))
        add(Solids.box("fenetre", -0.4f, 0.07f, 0.1f, 2.4f, 0.012f, 1.5f, mix(p.screen, 0xFFFFFFFF.toInt(), 0.12f), 1f, lidE, decalOf = scr, rotX = ang, pivot = pivot))
        add(Solids.box("barre", -0.4f, 0.058f, 0.78f, 2.4f, 0.012f, 0.22f, p.accent, 1f, lidE, decalOf = scr, rotX = ang, pivot = pivot))
        for (k in 0 until 4) add(Solids.box("ligne$k", -0.85f + (k % 2) * 0.1f, 0.058f, 0.30f - k * 0.30f, 1.5f - (k % 2) * 0.3f, 0.012f, 0.09f, p.glow, 1f, lidE, decalOf = scr, rotX = ang, pivot = pivot))
        add(Solids.box("icone", 1.25f, 0.058f, 0.55f, 0.5f, 0.012f, 0.5f, p.glow, 1f, lidE, decalOf = scr, rotX = ang, pivot = pivot))
        return Model3D(parts, groundY = -0.12f, footprint = 2.9f, centerAssembled = 1.2f, centerExploded = 1.0f, fitAssembled = 4.6f, fitExploded = 8.3f)
    }

    /** Baie de serveurs : les serveurs coulissent hors de la baie en vue éclatée. */
    private fun server(p: Pal): Model3D {
        val parts = ArrayList<Part>()
        fun add(x: Part): Int { parts += x; return parts.lastIndex }
        val h = 4.8f
        add(Solids.box("fond", 0f, h / 2, 0.95f, 2.5f, h, 0.1f, p.dark))
        add(Solids.box("cote-g", -1.2f, h / 2, 0f, 0.1f, h, 2.0f, p.dark))
        add(Solids.box("cote-d", 1.2f, h / 2, 0f, 0.1f, h, 2.0f, p.dark))
        add(Solids.box("haut", 0f, h + 0.05f, 0f, 2.5f, 0.1f, 2.0f, p.body))
        add(Solids.box("bas", 0f, -0.05f, 0f, 2.5f, 0.1f, 2.0f, p.body))
        add(Solids.box("pied-g", -1.0f, -0.2f, -0.7f, 0.2f, 0.2f, 0.2f, p.dark))
        add(Solids.box("pied-d", 1.0f, -0.2f, -0.7f, 0.2f, 0.2f, 0.2f, p.dark))
        val units = 8
        for (k in 0 until units) {
            val y = 0.42f + k * 0.54f
            val out = if (k % 2 == 0) 1.55f else 0.9f
            val ex = e(0f, 0f, -out - 0.08f * k)
            val u = add(Solids.box("unit$k", 0f, y, -0.1f, 2.2f, 0.44f, 1.7f, if (k == 5) mix(p.body, p.accent, 0.25f) else p.body, explode = ex))
            add(Solids.box("face$k", 0f, y, -0.97f, 2.2f, 0.44f, 0.06f, mix(p.dark, p.body, 0.35f), explode = ex, decalOf = u))
            for (s in 0 until 5) add(Solids.box("vent$k$s", -0.55f - s * 0.0f + (s - 2) * 0.17f, y, -1.01f, 0.07f, 0.26f, 0.02f, p.dark, explode = ex, decalOf = u))
            add(Solids.box("led$k", 0.82f, y + 0.08f, -1.0f, 0.1f, 0.1f, 0.04f, p.glow, 1f, ex, decalOf = u, blink = (k * 0.173f) % 1f))
            add(Solids.box("led2$k", 0.95f, y + 0.08f, -1.0f, 0.1f, 0.1f, 0.04f, p.accent, 1f, ex, decalOf = u, blink = (k * 0.311f + 0.4f) % 1f))
        }
        return Model3D(parts, groundY = -0.32f, footprint = 2.6f, centerAssembled = h / 2, centerExploded = h / 2, fitAssembled = 6.4f, fitExploded = 7.2f)
    }

    /** Cadenas : le corps reste en place, l'anse se lève (ouverture) en vue éclatée. */
    private fun padlock(p: Pal): Model3D {
        val parts = ArrayList<Part>()
        fun add(x: Part): Int { parts += x; return parts.lastIndex }
        val open = e(0f, 0.95f, 0f)
        val pts = ArrayList<Pair<Float, Float>>()
        pts += (-0.85f to 1.55f)
        pts += (-0.85f to 2.4f)
        val steps = 10
        for (i in 0..steps) {
            val a = (PI - PI * i / steps).toFloat()
            pts += (0.85f * cos(a) to 2.4f + 0.85f * sin(a))
        }
        pts += (0.85f to 2.4f)
        pts += (0.85f to 1.55f)
        val metal = mix(0xFFB9C2CC.toInt(), p.body, 0.25f)
        parts += Solids.tube("anse", pts, 0.2f, 0.42f, metal, open)
        val body = add(Solids.box("corps", 0f, 0.95f, 0f, 3.0f, 1.9f, 1.2f, p.accent))
        add(Solids.box("plaque", 0f, 0.95f, -0.62f, 2.6f, 1.5f, 0.04f, mix(p.accent, 0xFFFFFFFF.toInt(), 0.18f), decalOf = body))
        add(Solids.box("trou", 0f, 1.12f, -0.66f, 0.42f, 0.42f, 0.03f, p.dark, decalOf = body))
        add(Solids.box("fente", 0f, 0.72f, -0.66f, 0.16f, 0.5f, 0.03f, p.dark, decalOf = body))
        add(Solids.box("vis-g", -1.15f, 0.25f, -0.66f, 0.12f, 0.12f, 0.03f, p.dark, decalOf = body))
        add(Solids.box("vis-d", 1.15f, 0.25f, -0.66f, 0.12f, 0.12f, 0.03f, p.dark, decalOf = body))
        return Model3D(parts, groundY = -0.02f, footprint = 2.2f, centerAssembled = 1.55f, centerExploded = 1.9f, fitAssembled = 4.1f, fitExploded = 5.0f)
    }
}
