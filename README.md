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
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd spring-boot:run
```

`JWT_SECRET` doit contenir au moins 32 octets UTF-8 ; utilisez une valeur aléatoire robuste et distincte par environnement. L'access token expire après 15 minutes par défaut (`JWT_EXPIRATION`, en millisecondes). La base locale utilise Flyway pour ses migrations et Hibernate met à jour le schéma en profil `dev`.

L'inscription publique crée toujours un compte `USER`. Le premier compte `ADMIN` doit être provisionné par une procédure d'exploitation contrôlée, puis les comptes et rôles suivants peuvent être gérés via les routes admin protégées. Ne rendez jamais publique une route de promotion en rôle.

## Docker Compose

Définissez `DB_PASSWORD` et `JWT_SECRET` dans l'environnement du processus Docker Compose, puis lancez :

```powershell
docker compose up --build
```

Pour un premier déploiement uniquement, vous pouvez également fournir `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_EMAIL` et `APP_BOOTSTRAP_ADMIN_PASSWORD`. Un compte `ADMIN` est créé seulement si la table utilisateur est vide ; retirez ensuite ces variables de l'environnement.

Le profil `prod` utilise Flyway et Hibernate en validation de schéma. L'application et PostgreSQL partagent un réseau Compose ; le port API publié est `8080`. En production, configurez également `CORS_ALLOWED_ORIGIN` avec l'origine exacte du frontend.

## API et authentification

- Inscription : `POST /api/auth/register`
- Connexion : `POST /api/auth/login`
- Déconnexion avec révocation des access tokens du compte : `POST /api/auth/logout`
- Clients : `/api/customers`
- Offres : `/api/packs`
- Abonnements : `/api/subscriptions`
- Tableau de bord : `GET /api/statistics/dashboard`
- Audit (administrateur) : `/api/audit`

Les collections paginées utilisent `page`, `size` (maximum 100) et `sort=field,asc|desc`. Les endpoints protégés attendent `Authorization: Bearer <token>`. Les routes d'administration exigent l'autorité `ADMIN`.
Les access tokens vivent 15 minutes par défaut ; logout, changement de mot de passe, changement de rôle et désactivation incrémentent la version de jeton du compte et révoquent ses tokens actuels. La déconnexion révoque toutes les sessions de ce compte, pas un appareil isolé. Le login verrouille le compte 15 minutes après cinq échecs ; une limitation par adresse IP doit également être appliquée au reverse proxy en production.

Le chiffre d'affaires du dashboard est une estimation du montant mensuel des abonnements en cours, pas un relevé de paiements encaissés. Les revenus par mois représentent la valeur mensuelle des abonnements commencés dans chaque mois.

## Évolutions SaaS envisagées

- Le modèle demeure mono-organisation. Le multi-tenant devra ajouter un `Organization` et une clé de tenant aux utilisateurs, clients, offres, abonnements, événements d'audit et futurs paiements ; chaque requête devra être limitée à ce tenant et couverte par des tests d'isolation PostgreSQL. Ce changement est différé pour ne pas introduire une isolation partielle.
- Les paiements devront être des transactions liées aux abonnements (montant `BigDecimal`, méthode, statut, référence idempotente, remboursement/reçu). Aucun numéro de carte ne devra être stocké ; l'intégration d'un fournisseur passera par une couche dédiée et des webhooks signés.
- Les notifications (bienvenue, renouvellement, échéance, paiement) devraient être déclenchées par des événements métier et traitées par un worker via un outbox transactionnel, pas envoyées depuis les services REST.
- La structure actuelle conserve les packages techniques existants et introduit les contrats DTO communs ; les nouveaux modules peuvent être organisés par fonctionnalité. Les routes `/api` sont conservées pour compatibilité ; une version `/api/v1` pourra être ajoutée au prochain changement de contrat public.
- Il n'y a pas de refresh token persistant : les access tokens courts sont révoqués par version utilisateur. Une session par appareil et une rotation de refresh tokens nécessiteront un stockage de sessions dédié et des règles de révocation/rétention.

La documentation OpenAPI interactive est disponible sur `/swagger-ui/index.html` et le document JSON sur `/v3/api-docs`. Les contrôles de santé et métriques Actuator sont sous `/actuator`; seuls health et info sont publics.

## Tests

Les tests unitaires se lancent avec :

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd test
```

Les tests d'intégration utilisent PostgreSQL via Testcontainers et sont automatiquement ignorés si Docker n'est pas disponible.
