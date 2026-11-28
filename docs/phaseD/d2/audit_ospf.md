# Audit de pertinence

## 1.1 `s-hero` · HERO · 6.2 s
- AFFICHÉ : ASTUCE RÉSEAUX ⏎ OSPF : comment les routeurs trouvent le meilleur chemin ⏎ Dans une entreprise de quatre sites, un câble est coupé un mardi matin.
- DIT : OSPF : comment les routeurs trouvent le meilleur chemin. Dans une entreprise de quatre sites, un câble est coupé un mardi matin.
- sujet/rôle : Routage dynamique (OSPF) · ouverture ; indices du texte : act:RELIER, actor:ROUTEUR, thing:OBJET_3D, topic:cabling, topic:gateway, topic:logistics, topic:office, topic:ospf, topic:ospf-spf
- PLAN : net3d-ospf/poster (Affiche : un réseau de routeurs qui se parlent) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Routage dynamique (OSPF) »

## 1.2 `s-def` · DEFINITION · 9.2 s
- AFFICHÉ : Pourtant personne ne perd sa connexion ⏎ Les routeurs ont trouvé un autre chemin sans qu'un technicien intervienne.
- DIT : Les routeurs ont trouvé un autre chemin sans qu'un technicien intervienne.
- sujet/rôle : Routage dynamique (OSPF) · définition ; indices du texte : actor:ROUTEUR, thing:OBJET_3D, topic:gateway, topic:login, topic:logistics, topic:ospf-spf
- PLAN : net3d-ospf/ospf-spf (Le plus court chemin, puis la panne et le recalcul) note 0.90 ; dessine : ospf-spf
- VERDICT : ok : le texte cite ospf-spf

## 1.3 `s-def0-1` · DEFINITION · 14.0 s
- AFFICHÉ : OSPF, pour Open Shortest Path First, est un protocole de routage dynamique à état de liens : les routeurs d'un même réseau d'entreprise partagent la carte du réseau et calculent eux-mêmes le plus court chemin.
- DIT : OSPF, pour Open Shortest Path First, est un protocole de routage dynamique à état de liens : les routeurs d'un même réseau d'entreprise partagent la carte du réseau et calculent eux-mêmes le plus court chemin.
- sujet/rôle : Routage dynamique (OSPF) · définition ; indices du texte : act:PARTAGER, actor:ROUTEUR, thing:LIEN, thing:OBJET_3D, topic:cabling, topic:gateway, topic:link, topic:logistics, topic:office, topic:ospf, topic:ospf-map, topic:ospf-spf, topic:talk
- PLAN : net3d-ospf/ospf-overview (Des routeurs qui partagent une carte du réseau) note 0.85 ; dessine : ospf-map
- VERDICT : ok : le texte cite ospf-map

## 1.4 `s-def0-2` · DEFINITION · 8.9 s
- AFFICHÉ : OSPFv2 route l'IPv4 et OSPFv3 route l'IPv6 ; c'est un protocole ouvert, normalisé par l'IETF.
- DIT : OSPFv2 route l'IPv4 et OSPFv3 route l'IPv6 ; c'est un protocole ouvert, normalisé par l'IETF.
- sujet/rôle : Routage dynamique (OSPF) · définition ; indices du texte : topic:logistics, topic:ospf-ver
- PLAN : net3d-ospf/ospf-versions (OSPFv2 route l'IPv4, OSPFv3 route l'IPv6) note 0.85 ; dessine : ospf-ver
- VERDICT : ok : le texte cite ospf-ver

## 2.1 `ep2-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 2/4 ⏎ OSPF : comment les routeurs trouvent le meilleur chemin
- DIT : OSPF : comment les routeurs trouvent le meilleur chemin.
- sujet/rôle : Routage dynamique (OSPF) · ouverture ; indices du texte : actor:ROUTEUR, thing:OBJET_3D, topic:gateway, topic:logistics, topic:ospf, topic:ospf-spf
- PLAN : net3d-ospf/poster (Affiche : un réseau de routeurs qui se parlent) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Routage dynamique (OSPF) » ; déjà montré en 1.1

## 2.2 `s-steps0` · STEPS · 12.2 s
- AFFICHÉ : À quoi ça sert ⏎ • Les routes se découvrent et se mettent à jour sans configuration manuelle réseau par réseau ⏎ • Quand un lien tombe : les routeurs recalculent un autre chemin sans intervention humaine ⏎ • En découpant le réseau en aires : on garde des tables de routage raisonnables même sur un grand réseau
- DIT : À quoi ça sert. Les routes se découvrent et se mettent à jour sans configuration manuelle réseau par réseau. Quand un lien tombe : les routeurs recalculent un autre chemin sans intervention humaine. En découpant le réseau en aires : on garde des tables de routage raisonnables même sur un grand réseau.
- sujet/rôle : Routage dynamique (OSPF) · procédure ; indices du texte : actor:ROUTEUR, thing:LIEN, thing:OBJET_3D, topic:food, topic:gateway, topic:link, topic:logistics, topic:ospf-area, topic:ospf-spf, topic:purpose
- PLAN : net3d-ospf/ospf-benefits (À quoi sert OSPF : routes automatiques, reprise sur panne, aires) note 1.00 ; dessine : purpose
- VERDICT : ok : le texte cite purpose

## 2.3 `s-steps2-0` · STEPS · 9.0 s
- AFFICHÉ : Comment ça fonctionne (1/4) ⏎ • Chaque routeur envoie des paquets Hello : pour découvrir ses voisins ⏎ • Deux voisins qui s'entendent forment une adjacence et synchronisent leurs informations
- DIT : Comment ça fonctionne (1/4). Chaque routeur envoie des paquets Hello : pour découvrir ses voisins. Deux voisins qui s'entendent forment une adjacence et synchronisent leurs informations.
- sujet/rôle : Routage dynamique (OSPF) · procédure ; indices du texte : act:ENVOYER, actor:ROUTEUR, thing:OBJET_3D, thing:PAQUET, topic:flow, topic:gateway, topic:logistics, topic:ospf-hello
- PLAN : net3d-ospf/ospf-hello (Les paquets Hello : se découvrir entre voisins) note 1.00 ; dessine : ospf-hello
- VERDICT : ok : le texte cite ospf-hello

## 2.4 `s-steps2-1` · STEPS · 9.0 s
- AFFICHÉ : Comment ça fonctionne (2/4) ⏎ • Chaque routeur annonce l'état de ses liens dans des messages appelés LSA, diffusés à tout le réseau ⏎ • Tous les routeurs d'une même aire obtiennent ainsi la même carte du réseau
- DIT : Comment ça fonctionne (2/4). Chaque routeur annonce l'état de ses liens dans des messages appelés LSA, diffusés à tout le réseau. Tous les routeurs d'une même aire obtiennent ainsi la même carte du réseau.
- sujet/rôle : Routage dynamique (OSPF) · procédure ; indices du texte : act:DEMANDER, actor:ROUTEUR, thing:LIEN, thing:MESSAGE, thing:OBJET_3D, topic:callback, topic:gateway, topic:link, topic:logistics, topic:ospf-lsa, topic:ospf-map
- PLAN : net3d-ospf/ospf-lsa (Les LSA : chacun annonce l'état de ses liens) note 0.90 ; dessine : ospf-lsa
- VERDICT : ok : le texte cite ospf-lsa

## 3.1 `ep3-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 3/4 ⏎ OSPF : comment les routeurs trouvent le meilleur chemin
- DIT : OSPF : comment les routeurs trouvent le meilleur chemin.
- sujet/rôle : Routage dynamique (OSPF) · ouverture ; indices du texte : actor:ROUTEUR, thing:OBJET_3D, topic:gateway, topic:logistics, topic:ospf, topic:ospf-spf
- PLAN : net3d-ospf/poster (Affiche : un réseau de routeurs qui se parlent) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Routage dynamique (OSPF) » ; déjà montré en 1.1

## 3.2 `s-steps2-2` · STEPS · 12.2 s
- AFFICHÉ : Comment ça fonctionne (3/4) ⏎ • Chacun calcule alors le plus court chemin vers chaque réseau, avec l'algorithme SPF de Dijkstra ⏎ • Le coût d'un lien dépend de sa bande passante : plus il est rapide, plus son coût est faible ⏎ • Les meilleurs chemins entrent dans la table de routage. Si un lien tombe, un nouveau LSA est diffusé et le calcul est refait
- DIT : Comment ça fonctionne (3/4). Chacun calcule alors le plus court chemin vers chaque réseau, avec l'algorithme SPF de Dijkstra. Le coût d'un lien dépend de sa bande passante : plus il est rapide, plus son coût est faible. Les meilleurs chemins entrent dans la table de routage. Si un lien tombe, un nouveau LSA est diffusé et le calcul est refait.
- sujet/rôle : Routage dynamique (OSPF) · procédure ; indices du texte : thing:LIEN, topic:health, topic:link, topic:ospf-cost, topic:ospf-lsa, topic:ospf-spf
- PLAN : net3d-ospf/ospf-spf (Le plus court chemin, puis la panne et le recalcul) note 0.80 ; dessine : ospf-spf
- VERDICT : ok : le texte cite ospf-spf ; déjà montré en 1.2

## 3.3 `s-steps2-3` · DEFINITION · 11.5 s
- AFFICHÉ : Comment ça fonctionne (4/4) ⏎ Sur un réseau Ethernet partagé : un routeur désigné, le DR, et son remplaçant, le BDR, limitent le nombre d'adjacences
- DIT : Sur un réseau Ethernet partagé : un routeur désigné, le DR, et son remplaçant, le BDR, limitent le nombre d'adjacences.
- sujet/rôle : Routage dynamique (OSPF) · définition ; indices du texte : act:ENVOYER, act:LIMITER, act:PARTAGER, actor:ROUTEUR, thing:OBJET_3D, topic:gateway, topic:logistics, topic:ospf-dr, topic:ospf-hello, topic:sender, topic:talk
- PLAN : net3d-ospf/ospf-dr (Réseau partagé : le DR et le BDR limitent les adjacences) note 1.00 ; dessine : ospf-dr
- VERDICT : ok : le texte cite ospf-dr

## 4.1 `ep4-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 4/4 ⏎ OSPF : comment les routeurs trouvent le meilleur chemin
- DIT : OSPF : comment les routeurs trouvent le meilleur chemin.
- sujet/rôle : Routage dynamique (OSPF) · ouverture ; indices du texte : actor:ROUTEUR, thing:OBJET_3D, topic:gateway, topic:logistics, topic:ospf, topic:ospf-spf
- PLAN : net3d-ospf/poster (Affiche : un réseau de routeurs qui se parlent) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Routage dynamique (OSPF) » ; déjà montré en 1.1

## 4.2 `s-def3-0` · DEFINITION · 10.3 s
- AFFICHÉ : Erreurs fréquentes ⏎ Des intervalles Hello, une aire ou un masque différents entre deux voisins : l'adjacence ne se forme pas.
- DIT : Des intervalles Hello, une aire ou un masque différents entre deux voisins : l'adjacence ne se forme pas.
- sujet/rôle : Routage dynamique (OSPF) · définition ; indices du texte : topic:ospf-hello, topic:ospf-mismatch
- PLAN : net3d-ospf/ospf-mismatch (Hello, délai de mort, aire et masque : ils doivent être identiques) note 0.85 ; dessine : ospf-mismatch
- VERDICT : ok : le texte cite ospf-mismatch

## 4.3 `s-end` · CHECKLIST · 9.0 s
- AFFICHÉ : À retenir ⏎ • Chaque routeur connaît la carte du réseau ⏎ • Calcule lui-même le plus court chemin
- DIT : À retenir. Chaque routeur connaît la carte du réseau. Calcule lui-même le plus court chemin. Partage à quelqu’un qui en a besoin.
- sujet/rôle : Routage dynamique (OSPF) · à retenir ; indices du texte : act:PARTAGER, actor:ROUTEUR, thing:OBJET_3D, topic:gateway, topic:health, topic:logistics, topic:ospf-map, topic:ospf-spf, topic:talk
- PLAN : net3d-ospf/ospf-overview (Des routeurs qui partagent une carte du réseau) note 0.85 ; dessine : ospf-map
- VERDICT : ok : le texte cite ospf-map ; déjà montré en 1.3

