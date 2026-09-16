# VOLAKO — Backend

API REST Spring Boot pour VOLAKO, l'application de gestion financière personnelle. Consommée par un frontend Next.js déployé séparément (Vercel).

**Périmètre actuel : V1** — authentification, comptes, catégories, transactions, dashboard basique. Le schéma de base de données couvre déjà l'intégralité du MCD (transferts, budgets, dettes, crédits, objectifs, récurrence, notifications), mais seule la logique V1 est implémentée.

## Stack

- Java 21, Spring Boot 3.5, Spring Security (JWT + BCrypt), Spring Data JPA
- Flyway pour les migrations (jamais `ddl-auto`)
- PostgreSQL (Neon en production, Docker en local)
- Maven

## Lancer en local

```bash
docker compose up -d              # Postgres local
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

L'API écoute sur `http://localhost:8080`, toutes les routes sont préfixées par `/api`.

## Tests

```bash
./mvnw test
```

Les tests d'intégration tournent contre H2 en mode compatibilité PostgreSQL (aucune dépendance à Docker).

## Déploiement (Render + Neon)

Le service lit exclusivement des variables d'environnement, jamais de valeurs en dur :

| Variable | Description |
|---|---|
| `DB_URL` | URL JDBC PostgreSQL (Neon) |
| `DB_USERNAME` / `DB_PASSWORD` | Identifiants séparés de l'URL |
| `JWT_SECRET` | Clé de signature des access tokens |
| `JWT_ACCESS_EXPIRATION_MS` | Durée de vie de l'access token (défaut 15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Durée de vie du refresh token (défaut 7 jours) |
| `FRONTEND_URL` | Origine autorisée en CORS (ex. `https://volako.vercel.app`) |
| `PORT` | Injecté automatiquement par Render |

`render.yaml` décrit le service ; `./mvnw clean package -DskipTests` produit `target/backend-0.1.0.jar`.

## Format d'erreur

```json
{ "error": "CODE_ERREUR", "message": "Description lisible" }
```
