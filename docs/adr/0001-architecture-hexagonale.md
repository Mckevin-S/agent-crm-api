# ADR 0001 – Architecture hexagonale

- Statut : accepté
- Date : 2026-11-22

## Contexte

L'application dépend d'un système externe, le CRM, qu'elle ne maîtrise pas : son format, sa disponibilité et ses évolutions lui échappent. Les règles de recommandation sont le cœur de la valeur et doivent rester lisibles et testables sans lancer Spring, une base de données ou le CRM.

## Décision

Le code est découpé en ports et adaptateurs :
- `domain` contient le modèle et les règles, en Java pur ;
- `application` contient les cas d'usage et déclare des ports (`CrmGateway`, `TaskRepository`...) ;
- `adapter` implémente ces ports (HTTP, JPA) et expose l'API REST ;
- `config` assemble le tout.

Les dépendances vont toujours vers le domaine. ArchUnit (`HexagonalArchitectureTest`) fait échouer le build si une règle est enfreinte.

## Conséquences

- Les scénarios métier s'exécutent sur des adaptateurs en mémoire, en quelques millisecondes.
- Changer de CRM ou de base ne touche que l'adaptateur concerné.
- Plus de classes qu'une organisation en couches classique : chaque port a son adaptateur et ses DTO. Ce coût est accepté parce que l'intégration avec le CRM est la partie la plus risquée du projet.
