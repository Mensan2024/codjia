// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Bibliothèque de démonstrations vérifiées : pour un sujet (désactiver le Bureau à distance, activer le
 * pare-feu…), le chemin exact dans chaque système, avec les libellés d'interface et les commandes réelles.
 *
 * Sources des parcours : interfaces de Windows 11 (version française), macOS Sonoma/Sequoia (français),
 * Ubuntu 24.04 LTS (français) et Cisco IOS. Les sorties de commandes sont celles des outils en anglais
 * (langue par défaut des serveurs) ; les compteurs (nombre de paquets…) sont des exemples réalistes.
 */
object Procedures {
    data class Step(val title: String, val caption: String, val screen: DemoScreen, val action: DemoAction = DemoAction.None)
    data class Procedure(val os: DemoOs, val steps: List<Step>)
    data class Topic(val id: String, val label: String, val keywords: List<Pair<String, Int>>, val procedures: List<Procedure>)

    // ───────────────────────────── Menus de navigation ─────────────────────────────

    private val WIN_NAV = listOf("Système", "Bluetooth et appareils", "Réseau et Internet", "Personnalisation", "Applications",
        "Comptes", "Heure et langue", "Jeux", "Accessibilité", "Confidentialité et sécurité", "Windows Update")
    private val WINSEC_NAV = listOf("Accueil", "Protection contre les virus et menaces", "Protection du compte",
        "Pare-feu et protection du réseau", "Contrôle des applications et du navigateur", "Sécurité de l'appareil",
        "Performances et intégrité de l'appareil", "Options de contrôle parental")
    private val MAC_NAV = listOf("Wi-Fi", "Bluetooth", "Réseau", "Notifications", "Son", "Concentration", "Temps d'écran", "Général",
        "Apparence", "Accessibilité", "Centre de contrôle", "Siri et Spotlight", "Confidentialité et sécurité", "Bureau et Dock")
    private val GNOME_NAV = listOf("Wi-Fi", "Réseau", "Bluetooth", "Écrans", "Son", "Énergie", "Multitâche", "Apparence",
        "Applications", "Notifications", "Recherche", "Partage", "Clavier", "Accessibilité", "Confidentialité et sécurité", "Système")

    private fun winSearch(q: String, icon: IconId = IconId.GEAR) =
        Step("Ouvre « $q »", "Clique sur Démarrer et tape « $q », puis ouvre l'application.", DemoScreen.Search(q, q, "Application", icon), DemoAction.OpenResult)
    private fun macSearch() =
        Step("Ouvre les Réglages Système", "Appuie sur Cmd + Espace (Spotlight), tape « Réglages Système » et valide.",
            DemoScreen.Search("Réglages Système", "Réglages Système", "Application", IconId.GEAR), DemoAction.OpenResult)
    private fun ubuntuSearch(q: String, icon: IconId = IconId.GEAR) =
        Step("Ouvre « $q »", "Appuie sur la touche Super (Activités), tape « $q » et ouvre l'application.",
            DemoScreen.Search(q, q, "Application", icon), DemoAction.OpenResult)

    private fun row(label: String, detail: String? = null, c: DemoControl = DemoControl.Chevron, icon: IconId? = null) = DemoRow(label, detail, c, icon)
    private fun on() = DemoControl.Toggle(true)
    private fun off() = DemoControl.Toggle(false)
    private fun ok(t: String) = DemoControl.Status(t, true)
    private fun ko(t: String) = DemoControl.Status(t, false)

    // ───────────────────────────── Bureau à distance ─────────────────────────────

    private val RDP = Topic("rdp", "Bureau à distance",
        listOf("rdp" to 3, "bureau à distance" to 3, "remote desktop" to 3, "3389" to 2, "partage d'écran" to 3, "prise en main à distance" to 2),
        listOf(
            Procedure(DemoOs.WINDOWS, listOf(
                winSearch("Paramètres"),
                Step("Système > *Bureau à distance*", "Dans Système, descends jusqu'à « Bureau à distance ».",
                    DemoScreen.Settings(DemoApp.WIN_SETTINGS, "Système", "Système", listOf(
                        row("Affichage", "Moniteurs, luminosité, éclairage nocturne"), row("Son", "Volume, sortie, entrée"),
                        row("Alimentation"), row("Stockage", "Espace de stockage, lecteurs"),
                        row("Bureau à distance", "Utilisateurs, autorisations")), WIN_NAV),
                    DemoAction.ClickRow(4)),
                Step("*Désactive* le Bureau à distance", "Mets l'interrupteur sur Désactivé si tu ne te connectes pas à ce PC à distance.",
                    DemoScreen.Settings(DemoApp.WIN_SETTINGS, "Système", "Système  ›  Bureau à distance", listOf(
                        row("Bureau à distance", "Se connecter et utiliser ce PC depuis un autre appareil", on()),
                        row("Utilisateurs du Bureau à distance", "Choisir qui peut se connecter")), WIN_NAV),
                    DemoAction.ClickRow(0, toggleTo = false)),
            )),
            Procedure(DemoOs.MAC, listOf(
                macSearch(),
                Step("Général > *Partage*", "Dans la barre latérale, choisis Général, puis Partage.",
                    DemoScreen.Settings(DemoApp.MAC_SETTINGS, "Général", "Général", listOf(
                        row("Informations"), row("Mise à jour de logiciels"), row("Stockage"), row("AirDrop et Handoff"),
                        row("Langue et région"), row("Date et heure"), row("Partage")), MAC_NAV),
                    DemoAction.ClickRow(6)),
                Step("Coupe le *Partage d'écran*", "Désactive « Partage d'écran » : plus personne ne peut voir ni contrôler ton Mac.",
                    DemoScreen.Settings(DemoApp.MAC_SETTINGS, "Général", "Partage", listOf(
                        row("Partage de fichiers", c = off()), row("Partage des médias", c = off()), row("Partage d'écran", c = on()),
                        row("Session à distance", c = off()), row("Gestion à distance", c = on())), MAC_NAV),
                    DemoAction.ClickRow(2, toggleTo = false)),
                Step("Et la *Gestion à distance*", "Désactive aussi « Gestion à distance » si ton Mac n'est pas géré par ton entreprise.",
                    DemoScreen.Settings(DemoApp.MAC_SETTINGS, "Général", "Partage", listOf(
                        row("Partage de fichiers", c = off()), row("Partage des médias", c = off()), row("Partage d'écran", c = off()),
                        row("Session à distance", c = off()), row("Gestion à distance", c = on())), MAC_NAV),
                    DemoAction.ClickRow(4, toggleTo = false)),
            )),
            Procedure(DemoOs.UBUNTU, listOf(
                ubuntuSearch("Paramètres"),
                Step("Système > *Bureau à distance*", "Dans Paramètres, ouvre Système, puis Bureau à distance.",
                    DemoScreen.Settings(DemoApp.GNOME_SETTINGS, "Système", "Système", listOf(
                        row("Région et langue"), row("Date et heure"), row("Utilisateurs"), row("Bureau à distance"),
                        row("Mises à jour des logiciels"), row("À propos")), GNOME_NAV),
                    DemoAction.ClickRow(3)),
                Step("*Désactive* le partage de bureau", "Coupe « Partage de bureau » : ta session n'est plus accessible à distance.",
                    DemoScreen.Settings(DemoApp.GNOME_SETTINGS, "Système", "Bureau à distance", listOf(
                        row("Partage de bureau", "Voir et contrôler cette session à distance", on()),
                        row("Contrôle à distance", "Autoriser l'utilisation de la souris et du clavier", on())), GNOME_NAV),
                    DemoAction.ClickRow(0, toggleTo = false)),
            )),
        ))

    // ───────────────────────────── Pare-feu ─────────────────────────────

    private val UFW_STATUS = listOf(
        TermLine("Status: active"), TermLine("Logging: on (low)"),
        TermLine("Default: deny (incoming), allow (outgoing), disabled (routed)"), TermLine("New profiles: skip"), TermLine(""),
        TermLine("To                         Action      From"), TermLine("--                         ------      ----"),
        TermLine("22/tcp                     ALLOW IN    Anywhere"), TermLine("22/tcp (v6)                ALLOW IN    Anywhere (v6)"))

    private val FIREWALL = Topic("pare-feu", "Pare-feu",
        listOf("pare-feu" to 3, "firewall" to 3, "ufw" to 3, "coupe-feu" to 3, "defender" to 1),
        listOf(
            Procedure(DemoOs.WINDOWS, listOf(
                winSearch("Sécurité Windows", IconId.SHIELD),
                Step("Ouvre *Pare-feu et protection du réseau*", "Sur l'accueil, une croix rouge signale un réseau sans pare-feu.",
                    DemoScreen.Settings(DemoApp.WIN_SECURITY, "Accueil", "Sécurité en un coup d'œil", listOf(
                        row("Protection contre les virus et menaces", c = ok("Aucune action requise"), icon = IconId.SHIELD),
                        row("Protection du compte", c = ok("Aucune action requise"), icon = IconId.USER),
                        row("Pare-feu et protection du réseau", c = ko("Action recommandée"), icon = IconId.WIFI),
                        row("Contrôle des applications", c = ok("Aucune action requise"), icon = IconId.GLOBE)), WINSEC_NAV),
                    DemoAction.ClickRow(2)),
                Step("Choisis le *réseau public*", "C'est le réseau actif (Wi-Fi public, hôtel…) : le pare-feu y est désactivé.",
                    DemoScreen.Settings(DemoApp.WIN_SECURITY, "Pare-feu et protection du réseau", "Pare-feu et protection du réseau", listOf(
                        row("Réseau de domaine", c = ok("Le pare-feu est activé.")), row("Réseau privé", c = ok("Le pare-feu est activé.")),
                        row("Réseau public (actif)", c = ko("Le pare-feu est désactivé."))), WINSEC_NAV),
                    DemoAction.ClickRow(2)),
                Step("*Active* le pare-feu", "Active « Pare-feu Microsoft Defender » : les connexions entrantes non sollicitées sont bloquées.",
                    DemoScreen.Settings(DemoApp.WIN_SECURITY, "Pare-feu et protection du réseau", "Réseau public", listOf(
                        row("Pare-feu Microsoft Defender", "Protège ton appareil sur le réseau public", off()),
                        row("Connexions entrantes", "Bloque toutes les connexions entrantes", off())), WINSEC_NAV),
                    DemoAction.ClickRow(0, toggleTo = true)),
            )),
            Procedure(DemoOs.UBUNTU, listOf(
                Step("Vérifie l'état du *pare-feu*", "Ouvre un terminal (Ctrl + Alt + T). ufw est le pare-feu simple d'Ubuntu.",
                    DemoScreen.Terminal("codjia@ubuntu: ~", "codjia@ubuntu:~$ ", emptyList(), listOf(
                        TermLine("sudo ufw status", true), TermLine("Status: inactive")))),
                Step("Bloque *tout ce qui entre*", "Politique par défaut : refuser l'entrant, autoriser le sortant. Garde SSH si tu administres à distance.",
                    DemoScreen.Terminal("codjia@ubuntu: ~", "codjia@ubuntu:~$ ", listOf(TermLine("sudo ufw status", true), TermLine("Status: inactive")), listOf(
                        TermLine("sudo ufw default deny incoming", true), TermLine("Default incoming policy changed to 'deny'"),
                        TermLine("(be sure to update your rules accordingly)"),
                        TermLine("sudo ufw allow ssh", true), TermLine("Rules updated"), TermLine("Rules updated (v6)")))),
                Step("*Active* et vérifie", "ufw enable démarre le pare-feu, aussi à chaque démarrage. status verbose affiche les règles.",
                    DemoScreen.Terminal("codjia@ubuntu: ~", "codjia@ubuntu:~$ ", emptyList(), listOf(
                        TermLine("sudo ufw enable", true),
                        TermLine("Command may disrupt existing ssh connections. Proceed with operation (y|n)? y"),
                        TermLine("Firewall is active and enabled on system startup"),
                        TermLine("sudo ufw status verbose", true)) + UFW_STATUS)),
            )),
            Procedure(DemoOs.MAC, listOf(
                macSearch(),
                Step("Réseau > *Coupe-feu*", "Dans Réseau, le coupe-feu apparaît sous tes connexions. Ici, il est inactif.",
                    DemoScreen.Settings(DemoApp.MAC_SETTINGS, "Réseau", "Réseau", listOf(
                        row("Wi-Fi", c = ok("Connecté"), icon = IconId.WIFI), row("Thunderbolt Bridge", c = ko("Non connecté")),
                        row("VPN"), row("Coupe-feu", "Inactif", icon = IconId.SHIELD)), MAC_NAV),
                    DemoAction.ClickRow(3)),
                Step("*Active* le coupe-feu", "Active le coupe-feu : macOS bloque les connexions entrantes non autorisées.",
                    DemoScreen.Settings(DemoApp.MAC_SETTINGS, "Réseau", "Coupe-feu", listOf(
                        row("Coupe-feu", "Bloquer les connexions entrantes non désirées", off()), row("Options…")), MAC_NAV),
                    DemoAction.ClickRow(0, toggleTo = true)),
            )),
        ))

    // ───────────────────────────── Mises à jour ─────────────────────────────

    private val UPDATES = Topic("mises-a-jour", "Mises à jour",
        listOf("mise à jour" to 2, "mises à jour" to 2, "windows update" to 3, "apt upgrade" to 3, "apt update" to 3, "correctif" to 2, "patch" to 1, "à jour" to 1),
        listOf(
            Procedure(DemoOs.WINDOWS, listOf(
                winSearch("Paramètres"),
                Step("Va dans *Windows Update*", "Tout en bas du menu de gauche : Windows Update.",
                    DemoScreen.Settings(DemoApp.WIN_SETTINGS, "Windows Update", "Windows Update", listOf(
                        row("Des mises à jour sont disponibles", "Dernière recherche : il y a 6 jours", DemoControl.Button("Rechercher des mises à jour"), IconId.CLOCK),
                        row("Suspendre les mises à jour", "Pendant 1 semaine"), row("Historique des mises à jour"), row("Options avancées")), WIN_NAV),
                    DemoAction.ClickRow(0)),
                Step("*Installe* et redémarre", "Windows télécharge et installe les correctifs de sécurité. Redémarre quand il le demande.",
                    DemoScreen.Settings(DemoApp.WIN_SETTINGS, "Windows Update", "Windows Update", listOf(
                        row("Mise à jour cumulative de sécurité", "Téléchargement et installation…", DemoControl.Button("Tout installer"), IconId.SHIELD),
                        row("Recevoir les dernières mises à jour dès qu'elles sont disponibles", c = off()),
                        row("Historique des mises à jour"), row("Options avancées")), WIN_NAV),
                    DemoAction.ClickRow(0)),
            )),
            Procedure(DemoOs.UBUNTU, listOf(
                Step("Mets à jour la *liste des paquets*", "Dans un terminal : apt update interroge les dépôts d'Ubuntu.",
                    DemoScreen.Terminal("codjia@ubuntu: ~", "codjia@ubuntu:~$ ", emptyList(), listOf(
                        TermLine("sudo apt update", true),
                        TermLine("Hit:1 http://archive.ubuntu.com/ubuntu noble InRelease"),
                        TermLine("Get:2 http://security.ubuntu.com/ubuntu noble-security InRelease"),
                        TermLine("Reading package lists... Done"), TermLine("Building dependency tree... Done"),
                        TermLine("12 packages can be upgraded. Run 'apt list --upgradable' to see them.")))),
                Step("*Installe* les mises à jour", "apt upgrade installe les nouvelles versions, dont les correctifs de sécurité.",
                    DemoScreen.Terminal("codjia@ubuntu: ~", "codjia@ubuntu:~$ ", emptyList(), listOf(
                        TermLine("sudo apt upgrade -y", true),
                        TermLine("Reading package lists... Done"), TermLine("Calculating upgrade... Done"),
                        TermLine("The following packages will be upgraded:"), TermLine("  libssl3t64 openssl openssh-client ..."),
                        TermLine("12 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.")))),
                Step("Vérifie s'il faut *redémarrer*", "Si ce fichier existe, un redémarrage termine l'installation (noyau, bibliothèques).",
                    DemoScreen.Terminal("codjia@ubuntu: ~", "codjia@ubuntu:~$ ", emptyList(), listOf(
                        TermLine("cat /var/run/reboot-required", true), TermLine("*** System restart required ***")))),
            )),
            Procedure(DemoOs.MAC, listOf(
                macSearch(),
                Step("Général > *Mise à jour de logiciels*", "Dans Général, ouvre « Mise à jour de logiciels ».",
                    DemoScreen.Settings(DemoApp.MAC_SETTINGS, "Général", "Général", listOf(
                        row("Informations"), row("Mise à jour de logiciels"), row("Stockage"), row("AirDrop et Handoff")), MAC_NAV),
                    DemoAction.ClickRow(1)),
                Step("*Mets à jour* maintenant", "Installe la mise à jour proposée et active les mises à jour automatiques.",
                    DemoScreen.Settings(DemoApp.MAC_SETTINGS, "Général", "Mise à jour de logiciels", listOf(
                        row("Mise à jour disponible", "Mise à jour de sécurité de macOS", DemoControl.Button("Mettre à jour maintenant"), IconId.SHIELD),
                        row("Mises à jour automatiques", "Sécurité et fichiers système uniquement")), MAC_NAV),
                    DemoAction.ClickRow(0)),
            )),
        ))

    // ───────────────────────────── VLAN (Cisco IOS) ─────────────────────────────

    private val VLAN = Topic("vlan", "VLAN",
        listOf("vlan" to 3, "switchport" to 3, "commutateur" to 1, "ccna" to 1),
        listOf(
            Procedure(DemoOs.CISCO, listOf(
                Step("Entre en *configuration*", "Sur la console du commutateur : mode privilégié, puis configuration globale.",
                    DemoScreen.Terminal("Console — SW1", "SW1#", emptyList(), listOf(
                        TermLine("enable", true, "SW1>"), TermLine("configure terminal", true, "SW1#"),
                        TermLine("Enter configuration commands, one per line.  End with CNTL/Z.")))),
                Step("*Crée* le VLAN 10", "On crée le VLAN et on lui donne un nom parlant.",
                    DemoScreen.Terminal("Console — SW1", "SW1#", listOf(TermLine("configure terminal", true, "SW1#")), listOf(
                        TermLine("vlan 10", true, "SW1(config)#"), TermLine("name COMPTA", true, "SW1(config-vlan)#"),
                        TermLine("exit", true, "SW1(config-vlan)#")))),
                Step("*Affecte* un port au VLAN", "Le port Fa0/5 devient un port d'accès du VLAN 10.",
                    DemoScreen.Terminal("Console — SW1", "SW1#", emptyList(), listOf(
                        TermLine("interface fastEthernet 0/5", true, "SW1(config)#"),
                        TermLine("switchport mode access", true, "SW1(config-if)#"),
                        TermLine("switchport access vlan 10", true, "SW1(config-if)#"), TermLine("end", true, "SW1(config-if)#")))),
                Step("*Vérifie* avec show vlan", "Le VLAN 10 « COMPTA » est actif et contient bien le port Fa0/5.",
                    DemoScreen.Terminal("Console — SW1", "SW1#", emptyList(), listOf(
                        TermLine("show vlan brief", true, "SW1#"), TermLine(""),
                        TermLine("VLAN Name                 Status    Ports"),
                        TermLine("---- -------------------- --------- -----------------"),
                        TermLine("1    default              active    Fa0/1, Fa0/2, Fa0/3"),
                        TermLine("10   COMPTA               active    Fa0/5")))),
            )),
        ))

    val ALL: List<Topic> = listOf(RDP, FIREWALL, UPDATES, VLAN)

    /** Sujet de démonstration correspondant au texte (score ≥ 3), ou `null`. */
    fun match(text: String): Topic? {
        val low = text.lowercase()
        return ALL.map { tp -> tp to tp.keywords.sumOf { (k, w) -> if (low.contains(k)) w else 0 } }
            .filter { it.second >= 3 }.maxByOrNull { it.second }?.first
    }

    /** Système nommé dans le texte, s'il y en a un. */
    fun osIn(text: String): DemoOs? {
        val low = text.lowercase()
        return when {
            Regex("windows|powershell").containsMatchIn(low) -> DemoOs.WINDOWS
            Regex("macos|mac os|\\bmac\\b|macbook").containsMatchIn(low) -> DemoOs.MAC
            Regex("linux|ubuntu|debian|ufw|\\bapt\\b").containsMatchIn(low) -> DemoOs.UBUNTU
            Regex("cisco|switch|commutateur|routeur").containsMatchIn(low) -> DemoOs.CISCO
            else -> null
        }
    }

    /** Procédure à montrer : le système du texte si possible, sinon un système différent selon la graine. */
    fun pick(topic: Topic, text: String, seed: Long): Procedure {
        val wanted = osIn(text)
        topic.procedures.firstOrNull { it.os == wanted }?.let { return it }
        return topic.procedures[(Rand.of(seed, 0x05E1).nextInt() ushr 1) % topic.procedures.size]
    }

    /** Durée d'une étape : le temps de l'action, et assez pour lire la légende. */
    internal fun durationOf(step: Step): Int {
        val action = when (val s = step.screen) {
            DemoScreen.Desktop -> 2200f
            is DemoScreen.Search -> 900f + s.query.length * 70f + 900f + 1100f
            is DemoScreen.Settings -> if (step.action is DemoAction.ClickRow) 1900f + 2200f else 3200f
            is DemoScreen.Terminal -> termDuration(s.lines) + 1800f
        }
        val words = (step.title + " " + step.caption).split(' ').count { it.isNotBlank() }
        val reading = 1200f + words * 300f
        return maxOf(action, reading).toInt().coerceIn(3500, 9000)
    }

    /** Scènes de démonstration (une par étape), caméra et curseur continus d'une étape à l'autre. */
    fun scenes(proc: Procedure): List<Scene> {
        var prev: DemoStep? = null
        return proc.steps.mapIndexed { i, st ->
            val demo = DemoStep(proc.os, st.screen, st.action, prev)
            prev = demo
            Scene(id = "d-${proc.os.name.lowercase()}-$i", layout = Layout.SCREEN, title = st.title, subtitle = st.caption,
                kicker = "${proc.os.label} – étape ${i + 1}/${proc.steps.size}", durationMs = durationOf(st), demo = demo,
                transitionOut = Transition.CUT)
        }
    }
}
