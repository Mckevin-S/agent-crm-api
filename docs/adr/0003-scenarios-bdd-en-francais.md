# ADR 0003 – Scénarios métier en Gherkin français

- Statut : accepté
- Date : 2026-11-22

## Contexte

Les règles de recommandation viennent du métier (conseillers, managers d'agence). Elles doivent pouvoir être relues et discutées par des personnes qui ne lisent pas le Java.

## Décision

Les comportements attendus sont décrits avec Cucumber, en Gherkin français (`# language: fr`), dans `backend/src/test/resources/features` :
- `recommandations.feature` : quand une action est proposée et avec quelle priorité ;
- `suivi.feature` : accepter, écarter, tracer un échange ;
- `mode-degrade.feature` : panne du CRM et cloisonnement des portefeuilles.

Les étapes (`AgencySteps`) appellent directement les cas d'usage avec des adaptateurs en mémoire et une horloge fixe.

## Conséquences

- Les scénarios servent de documentation vivante : le rapport HTML est publié par la CI.
- Les tests restent rapides, car ils n'utilisent ni Spring ni base de données. Les tests d'intégration (`AgencyApiIT`) couvrent séparément l'API complète sur PostgreSQL.
- Les étapes doivent rester génériques pour éviter qu'un nouveau scénario exige du nouveau code à chaque fois.
