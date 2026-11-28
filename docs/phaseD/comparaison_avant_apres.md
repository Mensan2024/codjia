# VIDEO24 — Phase D.2 : comparaison avant / après (validation par la pertinence texte ↔ image)

Date : 28/11/2026. Cinq sujets : Phishing, OSPF, Ransomware, Active Directory, Pare-feu. Niveau « intermédiaire ». Voix de rendu : Piper (voix d'atelier uniquement ; l'application utilise la synthèse vocale de l'appareil).

## Critère de validation (changé)
Une phase n'est plus « réussie » parce que les tests passent, que chaque scène a une image ou que le contrôle qualité est vide. Pour **chaque scène** on vérifie : (1) ce que dit la voix, (2) le texte affiché, (3) les objets/acteurs dessinés, (4) l'animation et l'action, (5) la cohérence des personnages/équipements/environnements, (6) le calage voix/animation. Un plan n'est admis que si le texte cite ce que ce plan montre (garde sémantique), et jamais parce qu'un mot de son nom ressemble à un mot du texte.

## Résultat en chiffres (audit du plan final, `audit_*.md`)
| Vidéo | Épisodes | Durée totale | Scènes | Plan dédié au mécanisme cité | Cartes d'ouverture (affiche de série) | Scènes refusées | Scènes sans image |
|---|---|---|---|---|---|---|---|
| OSPF | 4 | 136 s | 14 | 10 | 4 | 0 | 0 |
| Ransomware | 4 | 170 s | 16 | 16 | 0 | 0 | 0 |
| Active Directory | 4 | 143 s | 13 | 9 | 4 | 0 | 0 |
| Pare-feu | 3 | 122 s | 12 | 10 | 2 | 0 | 0 |
| Phishing | 5 | 207 s | 21 | 17 | 4 | 0 | 0 |

Chaque épisode dure moins de 50 s (limite 59 s) ; marge minimale voix/animation : 250 ms sur tous les épisodes.

## Problèmes corrigés (exemples relevés dans l'audit avant D.2 → après)
| Vidéo | Texte de la scène | Avant | Après |
|---|---|---|---|
| OSPF | « Les routeurs ont trouvé un autre chemin sans qu'un technicien intervienne » | plan « coûts des liens » choisi sans que le texte parle de coûts | plan « plus court chemin, panne, recalcul » |
| OSPF | « OSPFv2 route l'IPv4 et OSPFv3 route l'IPv6 » | plan générique « carte du réseau » | plan dédié aux deux versions |
| OSPF | « Intervalles Hello, aire ou masque différents : l'adjacence ne se forme pas » | affiche générique | plan dédié : deux voisins, paramètres qui diffèrent, adjacence refusée |
| Ransomware | « Les outils de détection (EDR) repèrent le chiffrement massif » | **simulation 3D d'attaque sans rapport (refusée)** | plan « détecter le chiffrement massif et isoler le poste » |
| Ransomware | « Comment s'en protéger » | plan « segmenter » (un seul des cinq conseils) | plan « couches de protection » |
| Ransomware | « Les conséquences » | plan « tout s'arrête » (une seule conséquence) | plan « conséquences : arrêt, fuite, paiement non garanti » |
| Active Directory | « Active Directory est l'annuaire… il centralise les comptes, les groupes et les ordinateurs » | **plan « stratégie de groupe » (refusé)** | plan « le domaine : contrôleurs, UO, groupes » |
| Active Directory | « Comment ça fonctionne (1/2) » et « Erreurs fréquentes » | **plan « domaine » (refusé) pour deux textes différents** | plans dédiés : domaine ; droits d'administrateur au quotidien ; contrôleur de domaine unique |
| Active Directory | « En quelques clics, l'informatique lui crée un compte… ouvrir une session » | affiche générique | plan « ouverture de session : identifiants, ticket Kerberos, accès » |
| Phishing | « Un courriel de "sa banque" lui annonce que son compte sera suspendu sous 24 heures » | plan « appeler par le numéro officiel » (choisi parce que le texte contient « banque » et « compte », alors que la scène décrit le courriel reçu) | **nouveau plan dédié** « le message presse » : la phrase « sera suspendu sous 24 h » s'allume, une horloge tourne, la personne s'inquiète |
| Phishing | survol du lien, en-têtes SPF/DKIM/DMARC, saisie manuelle de l'adresse, double authentification, clé de sécurité/passkey, réinitialisation du mot de passe, variantes (spear phishing, whaling, smishing, vishing) | aucun plan dédié à ces mécanismes (ils retombaient sur des plans voisins : indices, courriel, site officiel) | **8 nouveaux plans dédiés** avec le précédent (clic sur le lien, variantes, en-têtes, adresse saisie à la main, double authentification, passkey, réinitialisation) + plan « survol » restreint à son texte |
| Phishing | « Comment le reconnaître » (4 signes) | 3 signes dessinés sur 4 | **4 signes dessinés** (expéditeur, urgence, lien, formule générique) |
| Tous | listes dont une virgule d'énumération était prise pour un séparateur titre/détail | « …comptes d'utilisateurs : d'ordinateurs et de groupes… », « …immuables : et teste leur restauration » | phrase conservée entière |

Le plan « indices du message » (4 signes) est désormais réservé aux scènes qui enseignent à reconnaître et ne sert qu'une fois par vidéo.

Mécanismes de contrôle ajoutés : garde sémantique (un plan n'illustre que ce que le texte cite ; un plan générique n'est permis que pour l'ouverture, la définition et le récapitulatif ; un plan propre à un sujet ne sert jamais un autre sujet ; un mécanisme nommé exclut les affiches et les plans d'un autre mécanisme) ; commandes `audit` (verdict par scène) et `why` (pourquoi chaque plan du catalogue est admis ou refusé pour une scène) ; tests `SemanticReuseTest`.

## Ce qui reste réutilisé, et pourquoi (à valider)
- **Cartes d'ouverture d'épisode** : l'affiche du sujet est reprise au début de chaque épisode comme identité de série (14 cartes sur 76 scènes, marquées « déjà montré » ; règle ajoutée : une affiche ne se répète que d'un épisode à l'autre, jamais comme illustration d'une scène ordinaire). Si vous préférez un plan nouveau par épisode, c'est un choix à trancher.
- **Même mécanisme revu dans un épisode ultérieur** : OSPF 3.2 (plus court chemin, déjà vu en 1.2), OSPF 4.3 et AD 4.4 (récapitulatif repris du plan « carte du réseau » / « une identité, des groupes, des règles »), AD 2.3 et 3.2, ransomware 2.4 (message de rançon, déjà vu en 1.2), pare-feu 2.3 (paquet filtré), phishing 3.3 (indices du message). Dans chaque cas le texte parle du même mécanisme ; aucun ne sert un concept différent. À votre appréciation s'ils doivent devenir des plans distincts.
- La scène « mail » du phishing ne dessine pas la pièce jointe citée dans le 4ᵉ signe (« ou une pièce jointe que tu n'attendais pas ») : seul « Cher client » (formule générique) est marqué.

## Limites connues
- **Pas de sous-titres incrustés** : le moteur n'en produit pas ; le texte affiché à l'écran est celui de la scène, pas la transcription mot à mot de la voix. Le calage voix/animation est contrôlé (marge ≥ 250 ms par scène, scènes allongées jusqu'à la fin de la phrase).
- Voix Piper = voix de démonstration de l'atelier ; sur Android, la voix de l'appareil sera différente (durées de parole variables, d'où l'allongement automatique des scènes).
- Le ralentissement (scène allongée de 5 à 7 s pour finir la phrase) laisse l'animation en fin de boucle sur quelques scènes (ex. OSPF 3.2 : +7 s).
- Les plans santé mentale, humanitaire et marketing restent dans l'application, strictement réservés à leurs sujets (garde sémantique).

## Régressions détectées par la suite de tests et corrigées dans la foulée
- Phishing : le plan « indices du message » servait deux fois dans une même vidéo → désormais non répétable et réservé aux scènes « reconnaître » ; la scène « le message presse » a reçu son propre plan.
- Marketing (plans non informatiques conservés) : la phrase « corrige la fuite » du tunnel de vente n'avait plus d'image ; elle est maintenant reconnue comme lecture de l'entonnoir (le plan « où partent les clients »).
- Humanitaire / santé mentale / marketing : l'affiche de chaque famille peut aussi servir de récapitulatif de son propre sujet (jamais d'un autre sujet).
- Pare-feu : le test de « famille de plans » ignorait la famille 3D ; mis à jour.
