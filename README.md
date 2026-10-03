# Gym Management API

Backend REST pour la gestion de clients, offres d'abonnement, abonnements, utilisateurs et statistiques.

## Prérequis

- Java 21
- PostgreSQL 16 ou Docker Compose
- Maven (le wrapper `mvnw.cmd` est fourni sous Windows)

## Configuration locale

Les secrets ne sont pas conservés dans le dépôt. Définissez les variables d'environnement avant de lancer l'application :

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/gym_management"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<mot-de-passe-local>"
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
.\mvnw.cmd spring-boot:run
```

`JWT_SECRET` doit contenir au moins 32 octets UTF-8 ; utilisez une valeur aléatoire robuste et distincte par environnement. L'access token expire après 15 minutes par défaut (`JWT_EXPIRATION`, en millisecondes). La base locale utilise Flyway pour ses migrations et Hibernate met à jour le schéma en profil `dev`.

L'inscription publique crée une organisation et son premier compte `ADMIN` ; le rôle n'est jamais fourni par le client. Les comptes et rôles suivants sont gérés par un administrateur de cette organisation. En production, l'inscription publique peut être fermée au niveau du reverse proxy si la création libre d'organisations n'est pas souhaitée.

## Docker Compose

Définissez `DB_PASSWORD` et `JWT_SECRET` dans l'environnement du processus Docker Compose, puis lancez :

```powershell
docker compose up --build
```

Pour un premier déploiement uniquement, vous pouvez fournir `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_EMAIL` et `APP_BOOTSTRAP_ADMIN_PASSWORD`, ainsi que `APP_BOOTSTRAP_ORGANIZATION_NAME` et `APP_BOOTSTRAP_ORGANIZATION_SLUG`. Un compte `ADMIN` est créé seulement si la table utilisateur est vide ; retirez ensuite ces variables de l'environnement.

Le profil `prod` utilise Flyway et Hibernate en validation de schéma. L'application et PostgreSQL partagent un réseau Compose ; le port API publié est `8080`. En production, configurez également `CORS_ALLOWED_ORIGIN` avec l'origine exacte du frontend.

## API et authentification

Le guide de réalisation du client Angular (architecture, écrans, contrats API, authentification, sécurité et tests) est dans [`FRONTEND_ANGULAR_GUIDE.md`](FRONTEND_ANGULAR_GUIDE.md).

- Inscription : `POST /api/auth/register`
- Connexion : `POST /api/auth/login`
- Déconnexion avec révocation des access tokens du compte : `POST /api/auth/logout`
- Clients : `/api/customers`
- Offres : `/api/packs`
- Abonnements : `/api/subscriptions`
- Tableau de bord : `GET /api/statistics/dashboard`
- Audit (administrateur) : `/api/audit`

L'inscription reçoit `organizationName`, `organizationSlug`, `username`, `email` et `password`. La connexion reçoit `organizationSlug`, `username` et `password`, car les identifiants sont uniques au sein d'une organisation. Le champ `role` n'est pas accepté dans le contrat d'inscription.

Les collections paginées utilisent `page`, `size` (maximum 100) et `sort=field,asc|desc`. Les endpoints protégés attendent `Authorization: Bearer <token>`. Les routes d'administration exigent l'autorité `ADMIN`.
Les access tokens vivent 15 minutes par défaut ; logout, changement de mot de passe, changement de rôle et désactivation incrémentent la version de jeton du compte et révoquent ses tokens actuels. La déconnexion révoque toutes les sessions de ce compte, pas un appareil isolé. Le login verrouille le compte 15 minutes après cinq échecs ; une limitation par adresse IP doit également être appliquée au reverse proxy en production.

Le chiffre d'affaires du dashboard est une estimation du montant mensuel des abonnements en cours, pas un relevé de paiements encaissés. Les revenus par mois représentent la valeur mensuelle des abonnements commencés dans chaque mois.

## Organisations, paiements et notifications

- Toutes les données métier sont rattachées à une organisation. Les anciens enregistrements sont conservés et migrés vers `legacy-gym` par Flyway V2 ; le JWT porte l'organisation et les repositories filtrent les lectures et mutations par tenant. Cette isolation est appliquée au niveau applicatif ; PostgreSQL RLS n'est pas activé.
- Paiements : `GET/POST /api/payments`, confirmation d'espèces par un ADMIN via `POST /api/payments/{id}/cash-confirmation`, demande de remboursement via `POST /api/payments/{id}/refunds` et validation via `POST /api/payments/refunds/{id}/complete`. Une clé d'idempotence est obligatoire ; seuls les montants, devise et références sont conservés, jamais les données de carte. Aucun fournisseur n'est intégré.
- Le callback générique `POST /api/payments/webhooks/provider` accepte `X-Payment-Signature`, un HMAC-SHA256 hexadécimal du texte UTF-8 `organizationId:paymentId:providerReference`. Configurez `PAYMENT_WEBHOOK_SECRET` avec un secret aléatoire et partagez-le uniquement avec le système de paiement de confiance ; l'intégration d'un fournisseur devra également valider son format d'événement et sa politique de rejeu.
- Les emails de bienvenue, création/expiration d'abonnement et confirmation de paiement sont placés dans un outbox transactionnel avec déduplication, reprises et dead-letter. Le SMTP est désactivé par défaut ; configurez `EMAIL_NOTIFICATIONS_ENABLED=true`, `EMAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` et `SMTP_PASSWORD` pour activer l'envoi. L'administration peut consulter/rejouer l'outbox via `/api/notifications/outbox`.
- `X-Request-ID` est propagé (ou généré) par requête et ajouté aux logs. Les compteurs de livraison d'email sont exposés par Micrometer. Les métriques Actuator/Prometheus restent protégées par une autorité `ADMIN`.
- La structure actuelle conserve les packages techniques existants et introduit les contrats DTO communs ; les nouveaux modules peuvent être organisés par fonctionnalité. Les routes `/api` sont conservées pour compatibilité ; une version `/api/v1` pourra être ajoutée au prochain changement de contrat public.
- Il n'y a pas de refresh token persistant : les access tokens courts sont révoqués par version utilisateur. Une session par appareil et une rotation de refresh tokens nécessiteront un stockage de sessions dédié et des règles de révocation/rétention.
- L'isolation multi-tenant est appliquée dans les repositories/services ; PostgreSQL RLS n'est pas activé. Les migrations et tests d'intégration Testcontainers doivent être exécutés avec Docker avant mise en production.

La documentation OpenAPI interactive est disponible sur `/swagger-ui/index.html` et le document JSON sur `/v3/api-docs`. Les contrôles de santé et métriques Actuator sont sous `/actuator`; seuls health et info sont publics.

## Tests

Les tests unitaires se lancent avec :

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
.\mvnw.cmd test
```

Les tests d'intégration utilisent PostgreSQL via Testcontainers et sont automatiquement ignorés si Docker n'est pas disponible.
