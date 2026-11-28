# Audit de pertinence

## 1.1 `s-hero` · HERO · 6.2 s
- AFFICHÉ : ASTUCE CYBERSÉCURITÉ ⏎ Le pare-feu : le contrôleur qui filtre chaque paquet ⏎ À l'entrée d'une entreprise, un agent vérifie chaque visiteur : qui es-tu, où vas-tu, as-tu le droit d'entrer ?
- DIT : Le pare-feu : le contrôleur qui filtre chaque paquet. À l'entrée d'une entreprise, un agent vérifie chaque visiteur : qui es-tu, où vas-tu, as-tu le droit d'entrer ?.
- sujet/rôle : Pare-feu · ouverture ; indices du texte : act:FILTRER, act:VERIFIER, actor:PARE_FEU, thing:OBJET_3D, thing:PAQUET, topic:cabling, topic:flow, topic:fw, topic:fw-packet, topic:office, topic:troubleshoot
- PLAN : firewall/filter (Tri des paquets) note 1.00 ; dessine : AUTORISER, BLOQUER, FILTRER, PAQUET, PARE_FEU
- VERDICT : ok : le texte cite PAQUET, FILTRER, PARE_FEU

## 1.2 `s-def` · DEFINITION · 8.5 s
- AFFICHÉ : Dans un réseau, le pare-feu joue ce rôle pour chaque paquet de données.
- DIT : Dans un réseau, le pare-feu joue ce rôle pour chaque paquet de données.
- sujet/rôle : Pare-feu · mécanisme ; indices du texte : actor:PARE_FEU, thing:DONNEES, thing:OBJET_3D, thing:PAQUET, topic:flow, topic:fw, topic:fw-packet, topic:measure
- PLAN : net3d-fw/fw-flows (Le pare-feu laisse passer ou bloque chaque paquet) note 1.00 ; dessine : fw-packet
- VERDICT : ok : le texte cite fw-packet

## 1.3 `s-def0-1` · DEFINITION · 13.3 s
- AFFICHÉ : Un pare-feu est un équipement ou un logiciel qui filtre le trafic entre deux réseaux selon des règles : il autorise certains paquets et bloque les autres.
- DIT : Un pare-feu est un équipement ou un logiciel qui filtre le trafic entre deux réseaux selon des règles : il autorise certains paquets et bloque les autres.
- sujet/rôle : Pare-feu · mécanisme ; indices du texte : act:ATTIRER, act:AUTORISER, act:BLOQUER, act:FILTRER, actor:PARE_FEU, thing:OBJET_3D, thing:PAQUET, thing:REGLE, topic:flow, topic:fw, topic:volunteer
- PLAN : firewall/rules (Table de règles) note 1.00 ; dessine : AUTORISER, BLOQUER, REGLE
- VERDICT : ok : le texte cite REGLE, AUTORISER, BLOQUER

## 1.4 `s-steps0` · STEPS · 12.2 s
- AFFICHÉ : À quoi ça sert ⏎ • Il protège le réseau interne contre les connexions venues d'Internet que personne n'a sollicitées ⏎ • Il limite ce que chaque zone peut atteindre : les invités ne voient pas les serveurs ⏎ • Il garde une trace des connexions autorisées et refusées
- DIT : À quoi ça sert. Il protège le réseau interne contre les connexions venues d'Internet que personne n'a sollicitées. Il limite ce que chaque zone peut atteindre : les invités ne voient pas les serveurs. Il garde une trace des connexions autorisées et refusées.
- sujet/rôle : Pare-feu · conseil, parade ; indices du texte : act:AUTORISER, act:BLOQUER, act:ENVOYER, act:LIMITER, actor:SERVEUR, thing:OBJET_3D, topic:fw-zones, topic:internet, topic:login, topic:purpose, topic:sender, topic:serverroom, topic:urgency
- PLAN : net3d-fw/fw-purpose (À quoi sert un pare-feu : bloquer l'entrant, séparer les zones, garder une trace) note 0.93 ; dessine : purpose
- VERDICT : ok : le texte cite purpose

## 2.1 `ep2-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 2/3 ⏎ Le pare-feu : le contrôleur qui filtre chaque paquet
- DIT : Le pare-feu : le contrôleur qui filtre chaque paquet.
- sujet/rôle : Pare-feu · ouverture ; indices du texte : act:FILTRER, act:VERIFIER, actor:PARE_FEU, thing:OBJET_3D, thing:PAQUET, topic:flow, topic:fw, topic:fw-packet
- PLAN : net3d-fw/poster (Affiche : un mur qui filtre les paquets) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Pare-feu »

## 2.2 `s-steps2-0` · DEFINITION · 11.8 s
- AFFICHÉ : Comment ça fonctionne (1/3) ⏎ Le pare-feu relie des zones : le réseau interne, Internet et souvent une zone intermédiaire pour les serveurs publics, la DMZ
- DIT : Le pare-feu relie des zones : le réseau interne, Internet et souvent une zone intermédiaire pour les serveurs publics, la DMZ.
- sujet/rôle : Pare-feu · mécanisme ; indices du texte : act:RELIER, actor:PARE_FEU, actor:SERVEUR, thing:OBJET_3D, topic:fw, topic:fw-zones, topic:internet, topic:serverroom
- PLAN : net3d-fw/fw-zones (Le pare-feu relie des zones : Internet, réseau interne, DMZ) note 1.00 ; dessine : fw-zones
- VERDICT : ok : le texte cite fw-zones

## 2.3 `s-steps2-1` · DEFINITION · 8.5 s
- AFFICHÉ : Comment ça fonctionne (2/3) ⏎ Il lit chaque paquet : adresses source et destination, protocole et port
- DIT : Il lit chaque paquet : adresses source et destination, protocole et port.
- sujet/rôle : Pare-feu · mécanisme ; indices du texte : thing:PAQUET, topic:cabling, topic:flow, topic:fw-packet, topic:sender
- PLAN : net3d-fw/fw-flows (Le pare-feu laisse passer ou bloque chaque paquet) note 0.90 ; dessine : fw-packet
- VERDICT : ok : le texte cite fw-packet ; déjà montré en 1.2

## 2.4 `s-steps2-2` · STEPS · 12.2 s
- AFFICHÉ : Comment ça fonctionne (3/3) ⏎ • Il compare le paquet aux règles : dans l'ordre, de haut en bas ⏎ • La première règle qui correspond s'applique : autoriser ou bloquer ⏎ • Si aucune règle ne correspond : la règle par défaut s'applique, et elle devrait refuser
- DIT : Comment ça fonctionne (3/3). Il compare le paquet aux règles : dans l'ordre, de haut en bas. La première règle qui correspond s'applique : autoriser ou bloquer. Si aucune règle ne correspond : la règle par défaut s'applique, et elle devrait refuser.
- sujet/rôle : Pare-feu · procédure ; indices du texte : act:AUTORISER, act:BLOQUER, act:VERIFIER, thing:PAQUET, thing:REGLE, topic:flow, topic:fw, topic:fw-rule
- PLAN : net3d-fw/fw-rules (Les règles se lisent de haut en bas : la première qui correspond s'applique) note 1.00 ; dessine : fw-rule
- VERDICT : ok : le texte cite fw-rule

## 3.1 `ep3-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 3/3 ⏎ Le pare-feu : le contrôleur qui filtre chaque paquet
- DIT : Le pare-feu : le contrôleur qui filtre chaque paquet.
- sujet/rôle : Pare-feu · ouverture ; indices du texte : act:FILTRER, act:VERIFIER, actor:PARE_FEU, thing:OBJET_3D, thing:PAQUET, topic:flow, topic:fw, topic:fw-packet
- PLAN : net3d-fw/poster (Affiche : un mur qui filtre les paquets) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Pare-feu » ; déjà montré en 2.1

## 3.2 `s-note2-0` · DEFINITION · 12.6 s
- AFFICHÉ : Un pare-feu à états suit les connexions : la réponse à une requête sortante est autorisée automatiquement, pas une connexion entrante que personne n'a demandée
- DIT : Un pare-feu à états suit les connexions : la réponse à une requête sortante est autorisée automatiquement, pas une connexion entrante que personne n'a demandée.
- sujet/rôle : Pare-feu · mécanisme ; indices du texte : act:AUTORISER, act:DEMANDER, act:REPONDRE, actor:PARE_FEU, thing:OBJET_3D, thing:PAQUET, thing:REPONSE, thing:REQUETE, topic:fw, topic:fw-state, topic:login
- PLAN : net3d-fw/fw-state (Pare-feu à états : la réponse à une requête sortante est autorisée) note 1.00 ; dessine : fw-state
- VERDICT : ok : le texte cite fw-state

## 3.3 `s-steps3-0` · STEPS · 9.0 s
- AFFICHÉ : Erreurs fréquentes ⏎ • Une règle trop large : comme tout autoriser, rend les autres inutiles ⏎ • L'ordre des règles : une règle générale placée trop haut masque les règles précises
- DIT : Erreurs fréquentes. Une règle trop large : comme tout autoriser, rend les autres inutiles. L'ordre des règles : une règle générale placée trop haut masque les règles précises.
- sujet/rôle : Pare-feu · procédure ; indices du texte : act:AUTORISER, thing:REGLE, topic:fw, topic:fw-mistake
- PLAN : net3d-fw/fw-mistake (Une règle trop large ou mal placée masque les règles précises) note 1.00 ; dessine : fw-mistake
- VERDICT : ok : le texte cite fw-mistake

## 3.4 `s-end` · CHECKLIST · 9.0 s
- AFFICHÉ : À retenir ⏎ • Un pare-feu applique des règles claires : on autorise le nécessaire ⏎ • On bloque le reste
- DIT : À retenir. Un pare-feu applique des règles claires : on autorise le nécessaire. On bloque le reste. Partage cette astuce.
- sujet/rôle : Pare-feu · à retenir ; indices du texte : act:AUTORISER, act:BLOQUER, act:PARTAGER, actor:PARE_FEU, thing:OBJET_3D, thing:REGLE, topic:fw, topic:talk
- PLAN : firewall/recap (À retenir : un filtre à l'entrée) note 0.90 ; dessine : AUTORISER, BLOQUER, FILTRER
- VERDICT : ok : le texte cite BLOQUER, AUTORISER

