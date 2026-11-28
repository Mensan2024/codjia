// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Outil d'inspection (banc d'essai) : dessine un plan du registre seul, sur le cadre virtuel 1000 × 700, sans texte ni mise
 * en page. Sert à juger la qualité visuelle de chaque plan et à produire les planches de la phase C.
 */
object Gallery {
    /** Clés « genre/plan » de tous les plans 2D du registre. */
    fun keys(): List<String> = SceneRegistry.kinds.flatMap { k -> k.beats.filter { it.draw != null }.map { k.id + "/" + it.id } } + listOf("test/emotions", "test/poses")

    fun label(key: String): String = SceneRegistry.find(key)?.second?.label ?: key

    /** Durée minimale utile d'un plan (ms). */
    fun minMs(key: String): Int = SceneRegistry.find(key)?.second?.minMs ?: 0

    /** Dessine le plan [key] à l'instant [t] (ms) ; `dark` choisit le fond sombre habituel du format Scénario. */
    fun draw(g: Gfx, key: String, t: Float, concept: Concept = Concept.NONE) {
        val th = StageTheme(rgb(0x0B1220), rgb(0x16264A), rgb(0xF2F6FF), rgb(0xA9B8D6), rgb(0x57C7FF), rgb(0xA8E4FF))
        g.fillRect(0f, 0f, 1000f, 700f, th.bgBottom)
        if (key.startsWith("test/")) { testCards(g, key, t, th); return }
        val (k, b) = SceneRegistry.find(key) ?: return
        val intent = SceneIntent(Domain.GENERAL, ContentFamily.PERSONAL, concept, emptyList(), Role.DEFINE, emptySet(), emptySet(), emptySet(), emptySet(), null,
            Context.PERSONNEL, Relation.AUCUNE, Goal.COMPRENDRE_MECANISME, emptyMap(), emptyList(), 1f, emptyList())
        val sc = Scene("gallery", Layout.DEFINITION, k.label, body = b.label, durationMs = 8000)
        b.draw?.invoke(StageCtx(g, t, t, th, intent, sc, 1L))
    }

    private fun testCards(g: Gfx, key: String, t: Float, th: StageTheme) {
        if (key == "test/emotions") {
            val faces = listOf("neutre" to Face.NEUTRAL, "joie" to Face.HAPPY, "inquiet" to Face.WORRIED, "stress" to Face.STRESSED, "triste" to Face.SAD, "surpris" to Face.SURPRISED,
                "soulagé" to Face.RELIEVED, "méfiant" to Face.SUSPICIOUS, "fatigué" to Face.TIRED, "concentré" to Face.FOCUSED, "alarmé" to Face.ALARMED, "somnolent" to Face.SLEEPY)
            for ((i, f) in faces.withIndex()) {
                val cx = 90f + (i % 6) * 165f; val cy = 200f + (i / 6) * 330f
                Cast.draw(g, cx, cy + 130f, 300f, Cast.persona(i), f.second, Poses.idle(t), t, i)
                SK.text(g, f.first, cx, cy + 160f, 22f, th.ink, center = true)
            }
        } else {
            val poses = listOf<Pair<String, (Float) -> Body>>("repos" to Poses::idle, "salue" to Poses::wave, "face-palm" to Poses::facePalm, "mains sur tête" to Poses::handsOnHead, "bras levés" to Poses::handsUp,
                "réfléchit" to Poses::thinking, "tient" to Poses::holdFront, "tape" to Poses::typing, "tête dans les mains" to Poses::headInHands, "menton" to Poses::hand2Chin,
                "marche" to { tt -> Poses.walk(tt) }, "téléphone" to Poses::phoneAtEar)
            for ((i, p) in poses.withIndex()) {
                val cx = 90f + (i % 6) * 165f; val cy = 200f + (i / 6) * 330f
                Cast.draw(g, cx, cy + 130f, 300f, Cast.persona(i + 1), if (i == 8) Face.SAD else Face.SMILE, p.second(t), t, i)
                SK.text(g, p.first, cx, cy + 160f, 22f, th.ink, center = true)
            }
        }
    }
}
