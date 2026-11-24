# CODJIA — Plan de développement de l'application complète (phase D)

**Condition de départ :** la version 0.3 du prototype doit être validée sur le Galaxy A16 (voir RAPPORT_VALIDATION §5).

**Méthode :**
- On développe un module à la fois. Chaque module est testé, puis documenté.
- Il y a une démonstration sur le téléphone à la fin de chaque étape.
- On ne publie rien tant que les critères de qualité (§4) ne sont pas remplis.

## 1. Architecture cible

Elle reprend la conception validée en phase 1 (dossier de conception technique). Le moteur actuel devient les modules `core:motion`, `core:render`, `core:export` et `core:audio`, sans réécriture.

| Couche | Modules |
| --- | --- |
| Application | `app` (navigation, injection Hilt, variantes `play` et `fdroid`) |
| Écrans (Jetpack Compose) | `feature:onboarding`, `feature:home`, `feature:create`, `feature:templates`, `feature:music`, `feature:preview`, `feature:render`, `feature:library`, `feature:settings` |
| Domaine | `core:model`, `core:text` (analyse du texte), `core:storyboard` (planificateur ≤ 40 s), `core:domain` (détection, recommandation) |
| Moteur | `core:motion` (= engine-core actuel), `core:render` (Canvas + Media3), `core:export`, `core:audio` (musique, décodage, mixage) |
| Plateforme | `core:data` (Room, DataStore), `core:security` (clés d'API chiffrées par le Keystore), `core:ai` (seul accès réseau), `core:designsystem` (thème CODJIA) |

Les règles du prototype restent valables :
- le moteur ne dépend jamais de l'interface ;
- l'aperçu et l'export passent par la même fonction de rendu ;
- seul `core:ai` utilise le réseau.

## 2. Étapes

| Étape | Contenu | Livrable testable sur le téléphone | Tests |
| --- | --- | --- | --- |
| **D1. Socle** | Projet multi-module, Hilt, Compose, thème CODJIA, navigation, Room et DataStore, intégration continue (tests, lint, analyse des licences) | L'application s'ouvre : démarrage, accueil vide, réglages | Compilation des 2 variantes, tests de navigation |
| **D2. Du texte au storyboard** | Mode texte : collage, nettoyage, découpage en idées, choix des compositions (définition, étapes, comparaison, chiffre, à retenir), mots-clés mis en valeur automatiquement, respect des 40 s | Un texte collé devient un storyboard affiché scène par scène | Textes de référence (astuces 021 à 027), durées, aucune perte de contenu, textes trop longs |
| **D3. Modèles** | Galerie des 8 templates avec vignettes réelles, recommandation par domaine, possibilité de changer à tout moment | Le choix de modèle avec aperçu immédiat | Captures de référence, contraste, zone sûre |
| **D4. Musique** | Bibliothèque par catégories, écoute avec forme d'onde, import de fichiers, volume, « sans musique », extension du catalogue par JSON | Le choix de musique façon Reels | Décodage MP3, AAC et OGG ; fondus ; synchronisation |
| **D5. Aperçu et éditeur** | Aperçu animé avec musique, modification d'un texte ou de l'ordre des scènes, durée totale affichée | Corriger une scène puis revoir l'aperçu | Le rendu reste identique entre aperçu et export |
| **D6. Génération** | Service au premier plan, notification de progression, reprise après interruption, contrôle du MP4 (codec, audio, durée), passage en 720p sur les appareils modestes | Générer avec l'écran éteint | Interruption, stockage plein, encodeur absent, téléphone chaud |
| **D7. Historique, exports, partage** | Bibliothèque des vidéos, renommer, dupliquer, supprimer, partager vers LinkedIn, WhatsApp, Facebook | Retrouver et repartager une vidéo | Base de données, gestion des fichiers |
| **D8. Mode IA** | Fournisseurs configurables (Gemini en premier), clés chiffrées, réponse validée par un schéma, mention « contenu généré par IA » et bouton de signalement, repli sur le mode texte | Une consigne courte devient une vidéo | Clé invalide, hors ligne, réponse invalide, aucune clé dans les journaux |
| **D9. Finition** | Accessibilité (TalkBack, tailles de texte), animations de l'interface, fiches Google Play et F-Droid, politique de confidentialité, captures | Version candidate | Revue d'accessibilité, lint, compilation reproductible |
| **D10. Test fermé** | 12 testeurs pendant 14 jours (compte Play personnel), corrections | Version 1.0 | Rapports de plantage exportés par les testeurs |

## 3. Ordre et dépendances

```
D1 ──► D2 ──► D3 ──► D4 ──► D5 ──► D6 ──► D7 ──► D9 ──► D10
                 └───────────────► D8 (en parallèle de D5 à D7)
```

D2 est le chantier le plus important : c'est lui qui transforme « n'importe quel texte » en une belle vidéo. Les templates et la musique sont déjà prêts.

## 4. Critères de qualité avant publication

- Tous les tests passent, sur la JVM et sur un émulateur.
- Le rendu reste dans la zone sûre, aucun texte n'en chevauche un autre, les contrastes sont conformes.
- Vérifié sur au moins 2 téléphones réels, dont un d'entrée de gamme : export de 40 s en 1080p en moins de 2 minutes, sans plantage sur 20 générations à la suite.
- Jamais de vidéo muette quand une musique a été choisie ; écart audio/vidéo inférieur à une image.
- Aucune clé d'API dans le code, les journaux ou les sauvegardes. Aucun traceur ni Google Play Services dans la variante F-Droid.
- Licence vérifiée pour chaque dépendance, police et musique (inventaire automatique dans l'intégration continue).
- Avis favorable de ta part sur une série de 10 vidéos réelles (astuces cybersécurité, santé, agriculture, marketing).

## 5. Décisions à prendre avant D1

1. L'identifiant définitif sur Google Play : `io.github.mensan2024.codjia` est proposé. Il ne pourra plus changer après la publication.
2. Le nom de domaine ou la page du projet pour la politique de confidentialité (une page GitHub suffit).
3. Le compte développeur Google Play à utiliser (personnel ou organisation).
