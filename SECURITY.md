# Politique de sécurité de CODJIA

## Versions suivies

| Version | Corrections de sécurité |
| --- | --- |
| `main` (dernière version) | ✅ |
| Branches `version/*` (archives) | ❌ Elles sont conservées pour l'historique uniquement |

## Signaler une vulnérabilité

**N'ouvre pas de ticket public.** Utilise le signalement privé de GitHub : onglet *Security*, puis *Report a vulnerability*.

Indique :
- la version (*Réglages › À propos* ou `versionName`) ;
- le téléphone et la version d'Android ;
- les étapes pour reproduire le problème, avec un fichier d'exemple s'il le faut (texte, fichier audio).

Délais visés :
- accusé de réception sous 72 h ;
- premier diagnostic sous 7 jours ;
- correction ou mesure d'atténuation sous 30 jours pour une faille grave.

Les signalements de bonne foi sont bienvenus, et leurs auteurs sont remerciés dans les notes de version s'ils le souhaitent.

## Principes

- **Hors ligne.** L'application ne demande aucune permission. La permission INTERNET est retirée du manifeste fusionné, et l'intégration continue échoue si elle réapparaît.
- **Aucune donnée collectée.** Il n'y a ni traceur, ni publicité, ni compte. Les sauvegardes cloud et les transferts entre appareils sont désactivés.
- **Aucun secret dans le dépôt.** La clé de développement `codjia-debug.keystore` est publique par choix et ne sert qu'aux tests. La clé de publication n'est jamais versionnée.
- **Contrôles automatiques.** CodeQL (`security-extended`) à chaque modification de `main` et chaque semaine, Dependabot pour les dépendances et les actions GitHub, vérification du Gradle Wrapper.

Le modèle de menaces détaillé se trouve dans [docs/SECURITE.md](docs/SECURITE.md).
