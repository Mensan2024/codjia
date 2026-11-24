// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Démonstrations sur écran simulé : description de ce qui est affiché à chaque étape (bureau, menu de
 * recherche, fenêtre de réglages, terminal) et de l'action montrée (clic, interrupteur, commande tapée).
 *
 * Les libellés d'interface et les sorties de commandes viennent de la bibliothèque [Procedures] :
 * ils reproduisent ce que l'utilisateur verra réellement. Aucune sortie n'est inventée à partir du texte.
 */
enum class DemoOs(val label: String) { WINDOWS("Windows"), MAC("macOS"), UBUNTU("Linux (Ubuntu)"), CISCO("Cisco IOS") }

/** Application dont la fenêtre est affichée. */
enum class DemoApp { NONE, WIN_SETTINGS, WIN_SECURITY, MAC_SETTINGS, GNOME_SETTINGS, TERMINAL }

sealed interface DemoControl {
    data class Toggle(val on: Boolean) : DemoControl
    data class Button(val label: String) : DemoControl
    data class Status(val text: String, val ok: Boolean) : DemoControl
    data object Chevron : DemoControl
    data object None : DemoControl
}

data class DemoRow(val label: String, val detail: String? = null, val control: DemoControl = DemoControl.Chevron, val icon: IconId? = null)

/** Ligne de terminal : commande (tapée au clavier) ou sortie (affichée d'un coup). */
data class TermLine(val text: String, val command: Boolean = false, val prompt: String? = null)

/** Ce que montre l'écran à une étape. */
sealed interface DemoScreen {
    /** Bureau seul (ouverture d'un chapitre « Sur Windows… »). */
    data object Desktop : DemoScreen

    /** Menu Démarrer / Spotlight / Activités avec une recherche tapée. */
    data class Search(val query: String, val result: String, val resultKind: String, val icon: IconId) : DemoScreen

    /** Fenêtre de réglages : élément de navigation sélectionné, titre de la page, lignes. */
    data class Settings(val app: DemoApp, val nav: String, val title: String, val rows: List<DemoRow>, val navItems: List<String>) : DemoScreen

    /** Terminal : lignes déjà présentes + nouvelles lignes animées. */
    data class Terminal(val title: String, val prompt: String, val history: List<TermLine>, val lines: List<TermLine>) : DemoScreen
}

/** Action montrée pendant l'étape. */
sealed interface DemoAction {
    data object None : DemoAction
    /** Clic sur le résultat de recherche. */
    data object OpenResult : DemoAction
    /** Clic sur la ligne `row` ; si `toggleTo` est donné, l'interrupteur bascule. */
    data class ClickRow(val row: Int, val toggleTo: Boolean? = null, val statusAfter: String? = null) : DemoAction
}

data class DemoStep(
    val os: DemoOs,
    val screen: DemoScreen,
    val action: DemoAction = DemoAction.None,
    /** Étape précédente du même chapitre (caméra et curseur continus), ou `null` en début de chapitre. */
    val previous: DemoStep? = null,
)
