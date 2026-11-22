# ADR 0004 – Authentification par jeton JWT signé, API sans état

- Statut : accepté
- Date : 2026-11-22

## Contexte

Le front-end Angular et l'API sont deux applications distinctes. Il faut une authentification simple à déployer et testable, et le rôle (conseiller ou manager) doit être connu à chaque requête.

## Décision

L'API émet un jeton JWT signé HS256 avec le support natif de Spring Security (`oauth2-resource-server`, `NimbusJwtEncoder`/`NimbusJwtDecoder`). Le jeton porte l'identifiant du conseiller, son nom et son rôle. Le cloisonnement des portefeuilles est vérifié dans les cas d'usage, car il dépend du client consulté.

## Conséquences

- Aucune session côté serveur : l'API se met à l'échelle horizontalement.
- Pas de cookie, donc pas de protection CSRF à gérer.
- Révocation immédiate impossible avant expiration : la durée de validité est courte (8 h, une journée d'agence). Un fournisseur OIDC pourra remplacer l'émetteur interne sans changer le reste de l'API.
