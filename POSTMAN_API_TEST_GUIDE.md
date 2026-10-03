# Tester l'API Gym Management avec Postman

Ce guide permet de vérifier les endpoints réellement exposés par le backend, leurs contrats, les règles d'accès et les principaux cas d'erreur. Il sert aussi de checklist pour relever d'éventuels défauts.

> Faire ces essais sur une base locale ou de test, jamais sur une production. Les opérations de création, annulation, désactivation, remboursement et changement de mot de passe modifient les données. Les demandes de remboursement ne sont pas annulables via l'API documentée.

## 1. Préparer le backend

Prérequis : Java 21, PostgreSQL accessible et variables de connexion valides. Depuis la racine du dépôt, définir les variables dans PowerShell et démarrer le serveur :

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/gym_management"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<mot-de-passe-local>"
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
.\mvnw.cmd spring-boot:run
```

Flyway doit finir ses migrations sans erreur. Le profil local par défaut est `dev`. Le backend écoute normalement sur `http://localhost:8080`.

Contrôles de disponibilité :

```text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/v3/api-docs
```

Health et OpenAPI sont publics. Swagger UI : `http://localhost:8080/swagger-ui/index.html`.

### Attention au profil `dev`

La migration Flyway V2 rattache les données déjà présentes à l'organisation `legacy-gym`. Une organisation inscrite ensuite est séparée de ce tenant et commence avec ses propres données. Pour inspecter les lignes historiques, se connecter avec le compte qui appartient à `legacy-gym` (si disponible) ; ne pas conclure à un défaut si l'organisation nouvellement créée voit une liste vide.

Ne désactivez pas Flyway pour un test normal. Ne changez pas `ddl-auto` pour cacher une erreur de migration.

## 2. Créer un environnement Postman

Créer un environnement nommé `Gym API local`, puis ajouter :

| Variable | Initial value | Current value / usage |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | URL du serveur |
| `apiUrl` | `{{baseUrl}}/api` | Base de l'API |
| `organizationName` | `Gym Postman` | Nom d'organisation jetable |
| `organizationSlug` | `gym-postman-001` | Slug unique ; changer si déjà inscrit |
| `adminUsername` | `postman-admin` | Compte admin de test |
| `adminEmail` | `postman-admin@example.test` | Email de test |
| `testPassword` | valeur locale de test | Au moins 12 caractères |
| `adminToken` | vide | Rempli par le test du login admin |
| `userToken` | vide | Rempli par le test du login utilisateur |
| `customerId` | vide | Rempli après création client |
| `packId` | vide | Rempli après création pack |
| `subscriptionId` | vide | Rempli après création abonnement |
| `paymentId` | vide | Rempli après création paiement |
| `refundId` | vide | Rempli après demande de remboursement |
| `testUserId` | vide | Rempli après création utilisateur |
| `webhookSecret` | vide | Seulement si webhook configuré côté serveur |
| `webhookSignature` | vide | HMAC calculé pour le webhook |

Utiliser des valeurs de test, pas un vrai mot de passe. Les variables d'environnement Postman peuvent être exportées : ne pas mettre de secrets de production dans une collection partagée.

Pour isoler les tests, choisir un nouveau slug à chaque suite, par exemple `gym-pm-20261001-01`, et un username/email uniques. `POST /auth/register` crée un ADMIN et cette opération n'est pas réversible par l'API.

## 3. Conventions communes

- Pour les requêtes protégées : onglet **Authorization → Bearer Token**, valeur `{{adminToken}}` ou `{{userToken}}`. Ou en-tête `Authorization: Bearer {{adminToken}}`.
- Ajouter `Content-Type: application/json` aux requêtes JSON.
- Ne pas envoyer `organizationId` dans les appels métier : le backend déduit le tenant du JWT.
- Les listes paginées renvoient un objet `PageResponse` : les éléments sont dans `content`, pas dans un tableau racine.
- `page` commence à 0 ; `size` doit être de 1 à 100. Le tri accepte un seul `champ,asc|desc`, avec champ autorisé selon la route.
- Les dates sont au format ISO `YYYY-MM-DD`; timestamp en ISO-8601.
- Les erreurs JSON suivent généralement :

```json
{
  "timestamp": "2026-10-01T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Les données fournies sont invalides.",
  "path": "/api/customers",
  "details": {
    "email": "doit être une adresse e-mail bien formée"
  }
}
```

Enregistrer `X-Request-ID` de la réponse lors d'un problème ; il est utile pour retrouver les traces serveur.

### Tests Postman communs

À ajouter dans l'onglet **Scripts → Post-response** de requêtes JSON réussies :

```javascript
pm.test("HTTP status is successful", () => {
  pm.expect(pm.response.code).to.be.within(200, 299);
});

pm.test("Response is JSON when a body is expected", () => {
  pm.expect(pm.response.headers.get("Content-Type") || "").to.include("application/json");
  pm.expect(pm.response.text()).not.to.be.empty;
});
```

Pour les requêtes qui doivent répondre `204`, vérifier au contraire :

```javascript
pm.test("HTTP 204 with no response body", () => {
  pm.response.to.have.status(204);
  pm.expect(pm.response.text()).to.eql("");
});
```

## 4. Parcours de test recommandé

Exécuter dans cet ordre afin de récupérer les IDs automatiquement :

1. Vérifier l'API et OpenAPI.
2. Inscrire une organisation de test, puis se connecter comme ADMIN.
3. Vérifier organisation courante et profil.
4. Créer un client et une offre.
5. Créer et lister un abonnement.
6. Créer un paiement en espèces et le confirmer comme ADMIN.
7. Demander puis compléter un remboursement de montant partiel.
8. Créer un utilisateur `USER`, se connecter comme USER et vérifier les refus `403`.
9. Tester statistiques, export CSV, audit, outbox et pagination.
10. Effectuer les cas invalides et tenant croisé sur des données jetables.
11. Tester le logout en dernier : le token correspondant sera révoqué.

## 5. API publique et authentification

### `POST /api/auth/register` — créer organisation et premier ADMIN

**Authorization :** aucune  
**Body JSON :**

```json
{
  "organizationName": "{{organizationName}}",
  "organizationSlug": "{{organizationSlug}}",
  "username": "{{adminUsername}}",
  "email": "{{adminEmail}}",
  "password": "{{testPassword}}"
}
```

**Attendu :** `201 Created`, `UserResponse` avec `role: "ADMIN"`. La réponse ne doit contenir ni `password`, ni hash.

Tests post-response :

```javascript
pm.response.to.have.status(201);
const body = pm.response.json();
pm.expect(body.role).to.eql("ADMIN");
pm.expect(body).not.to.have.property("password");
pm.environment.set("registeredUserId", body.id);
```

**Cas négatifs / sécurité :**

- omettre `organizationSlug`, email invalide, username trop court ou password trop court → `400`;
- réutiliser le slug → `409 CONFLICT`;
- ajouter `"role": "USER"` ou `"role": "ADMIN"` au JSON : cela ne doit pas permettre de choisir un rôle. L'inscription produit le premier ADMIN selon la règle métier actuelle.
- utiliser des caractères invalides dans le slug (majuscules, espaces, underscore) → `400`.

### `POST /api/auth/login` — connexion tenant-aware

**Authorization :** aucune  
**Body JSON :**

```json
{
  "organizationSlug": "{{organizationSlug}}",
  "username": "{{adminUsername}}",
  "password": "{{testPassword}}"
}
```

**Attendu :** `200 OK`, réponse :

```json
{
  "token": "<JWT>",
  "type": "Bearer",
  "role": "ADMIN"
}
```

Script post-response :

```javascript
pm.response.to.have.status(200);
const body = pm.response.json();
pm.expect(body.type).to.eql("Bearer");
pm.expect(body.token).to.be.a("string").and.not.empty;
pm.environment.set("adminToken", body.token);
pm.environment.set("authRole", body.role);
```

**Cas négatifs :** mauvais password, username inexistant ou mauvais slug → `401 INVALID_CREDENTIALS`, sans révéler lequel est incorrect. N'effectuer pas cinq essais sur un compte que vous voulez conserver : cinq échecs verrouillent le compte temporairement.

### `POST /api/auth/logout` — révoquer les tokens du compte

**Authorization :** `Bearer {{adminToken}}`  
**Body :** aucun.

**Attendu :** `204 No Content`. Réessayer immédiatement `GET /api/users/me` avec le même token : attendu `401`. Cette révocation s'applique à tous les tokens du compte, pas seulement au token courant. Garder ce test pour la fin, ou se reconnecter ensuite.

## 6. Organisation et profil

### `GET /api/organizations/me`

**Authorization :** `Bearer {{adminToken}}`  
**Attendu :** `200`, `{ id, name, slug, createdAt }`. Vérifier que le slug est celui envoyé à l'inscription.

### `GET /api/users/me`

**Authorization :** Bearer token  
**Attendu :** `200`, `UserResponse` avec `id`, `username`, `email`, `role`, `enabled`, `createdAt`, `lastLogin`. Aucun champ password/hash.

### `PUT /api/users/me` — modifier l'email courant

**Authorization :** Bearer token  
**Body JSON :**

```json
{ "email": "admin-updated@example.test" }
```

**Attendu :** `200`, `UserResponse` mis à jour. Tester une adresse invalide → `400`; essayer un email déjà pris dans la même organisation → conflit `409`.

### `POST /api/users/change-password`

**Authorization :** Bearer token  
**Body JSON :**

```json
{
  "oldPassword": "{{testPassword}}",
  "newPassword": "Autre-MotDePasse-Test-2026"
}
```

**Attendu :** `204`. L'ancien token est révoqué par incrément de version. Se reconnecter avec le nouveau mot de passe pour continuer. Ancien mot de passe → login `401`, nouveau mot de passe → login `200`.

## 7. Clients — `/api/customers`

### `GET /api/customers` — liste, filtres, pagination et tri

**Authorization :** Bearer token.  
**Paramètres optionnels :** `q`, `lastName`, `phone`, `page`, `size`, `sort`.

Exemple :

```text
GET {{apiUrl}}/customers?q=marie&lastName=dupont&phone=061&page=0&size=20&sort=registrationDate,desc
```

Sans filtres, vérifier également :

```text
GET {{apiUrl}}/customers?page=0&size=20&sort=registrationDate,desc
```

**Attendu :** `200`, enveloppe `PageResponse<CustomerResponse>` :

```json
{
  "content": [
    {
      "id": 10,
      "firstName": "Marie",
      "lastName": "Dupont",
      "registrationDate": "2026-10-01",
      "phoneNumber": "+33123456789",
      "email": "marie@example.test",
      "activeSubscription": false,
      "enabled": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

La recherche `q` correspond au prénom, nom, téléphone ou email. Vérifier séparément `q`, `lastName`, `phone`, les trois combinés, paramètres absents, vides et valeurs sans correspondance. Les résultats sont limités au tenant courant et aux clients activés.

Cas pagination/tri invalides : `size=0`, `size=101`, `page=-1`, `sort=unknown,desc`, `sort=registrationDate,sideways` → attendu `400 BAD_REQUEST`.

### `GET /api/customers/search?lastName=...`

Route de recherche de compatibilité, filtrée par nom :

```text
GET {{apiUrl}}/customers/search?lastName=Dupont&page=0&size=20&sort=registrationDate,desc
```

**Attendu :** `200`, `PageResponse`. Paramètre `lastName` omis → `400` (il est obligatoire sur cette route).

### `GET /api/customers/{id}`

Exemple : `GET {{apiUrl}}/customers/{{customerId}}`.  
**Attendu :** `200 CustomerResponse`; ID absent, désactivé ou provenant d'un autre tenant → `404`.

### `POST /api/customers` — créer

**Body JSON :**

```json
{
  "firstName": "Marie",
  "lastName": "Dupont",
  "phoneNumber": "+33123456789",
  "email": "marie@example.test"
}
```

**Attendu :** `201`, entête `Location: /api/customers/{id}`, réponse client. Sauvegarder l'ID :

```javascript
pm.response.to.have.status(201);
const body = pm.response.json();
pm.environment.set("customerId", body.id);
```

**Validation à tester :** prénom/nom vides ou >100 caractères, email incorrect, téléphone ne satisfaisant pas `^[+0-9() .-]{7,25}$` → `400`. Email et téléphone peuvent être omis ou `null`.

### `PUT /api/customers/{id}` — modifier

Même corps que POST, URL avec `{{customerId}}`. **Attendu :** `200 CustomerResponse`; ID inconnu/cross-tenant → `404`; corps invalide → `400`.

### `DELETE /api/customers/{id}` — désactiver logiquement

**Attendu :** `204`. Vérifier ensuite :

- GET par ID → `404`;
- liste sans filtre ne contient plus le client;
- abonnements historiques/export et statistiques ne sont pas supprimés par cette action.

## 8. Offres / packs — `/api/packs`

Tout utilisateur authentifié peut lire les offres. Les créations, modifications, changements de statut et désactivations exigent `ADMIN`.

### `GET /api/packs`

Exemple :

```text
GET {{apiUrl}}/packs?page=0&size=20&sort=createdAt,desc
```

**Attendu :** `200 PageResponse<PackResponse>` avec `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. Tester un tri autorisé (`offerName`, `durationMonths`, `monthlyPrice`) puis un tri interdit.

### `GET /api/packs/{id}`

**Attendu :** `200 PackResponse`; ID inconnu/cross-tenant → `404`.

### `POST /api/packs`

**Admin requis. Body :**

```json
{
  "offerName": "Pack Postman",
  "description": "Offre créée pour le test API",
  "durationMonths": 1,
  "monthlyPrice": 3000.00
}
```

**Attendu :** `201`, `Location: /api/packs/{id}`, réponse pack (`active`, `createdAt`, `updatedAt` inclus). Sauvegarder `id` dans `packId`.

**Validation :** nom obligatoire, durée 1–120 mois, prix obligatoire et >=0, précision max 10 chiffres entiers/2 décimales, description max 1000 → invalide `400`.

### `PUT /api/packs/{id}`

Même contrat que POST, chemin `{{packId}}`. **Attendu :** `200 PackResponse`. Confirmer qu'un abonnement déjà créé conserve son instantané d'offre même si le pack change.

### `PATCH /api/packs/{id}/status`

```json
{ "active": false }
```

**Attendu :** `200`, pack désactivé. Retester avec `{ "active": true }`. Champ absent ou null → `400`.

### `DELETE /api/packs/{id}`

**Attendu :** `204`, mais désactivation logique : le pack est conservé pour l'historique. Tester les références depuis abonnements.

### Cas accès

Avec `userToken`, GET pack doit réussir (`200`), POST/PUT/PATCH/DELETE doivent renvoyer `403 FORBIDDEN`. Sans token, GET doit renvoyer `401 UNAUTHORIZED`.

## 9. Abonnements — `/api/subscriptions`

### `GET /api/subscriptions`

```text
GET {{apiUrl}}/subscriptions?page=0&size=20&sort=startDate,desc
```

**Attendu :** `200 PageResponse<SubscriptionResponse>`. Tri autorisé : `id`, `startDate`, `endDate`, `status`. Vérifier que les données sont du tenant du token.

### `GET /api/subscriptions/{id}`

**Attendu :** `200 SubscriptionResponse` avec `customerId`, `customerName`, `packId`, `packName`, `startDate`, `endDate`, `monthlyPrice`, `status`; ID inconnu ou cross-tenant → `404`.

### `POST /api/subscriptions`

Prérequis : un client activé et un pack actif dans le même tenant.  
**Body :**

```json
{
  "customerId": {{customerId}},
  "packId": {{packId}},
  "startDate": "2026-10-01"
}
```

**Attendu :** `201`, `Location`, abonnement avec `endDate` calculé et status cohérent (`ACTIVE` ou `SCHEDULED`). Enregistrer ID dans `subscriptionId`.

Tester :

- ID client/pack absent ou d'un autre tenant → `404`;
- pack désactivé → conflit `409`;
- chevauchement de dates avec abonnement actif/planifié → `409`;
- date au format invalide ou ID <=0 → `400`;
- mêmes période/client/pack et duplication concurrente, si possible, pour détecter double souscription.

### `GET /api/subscriptions/customer/{customerId}`

```text
GET {{apiUrl}}/subscriptions/customer/{{customerId}}?page=0&size=20&sort=startDate,desc
```

**Attendu :** `200 PageResponse`. Client inexistant/non accessible → `404`.

### `PUT /api/subscriptions/{id}`

Même body que POST. **Attendu :** `200` si l'abonnement est planifié. Essayer de modifier un abonnement actif/expiré/annulé → conflit `409`. Vérifier le contrôle de chevauchement après modification.

### `POST /api/subscriptions/{id}/renew`

Body vide. **Attendu :** `201`, nouvel abonnement, nouvel ID, historique précédent conservé. Réessayer et inspecter les règles d'overlap/doublon ; relever si une période inattendue est créée.

### `DELETE /api/subscriptions/{id}` — annuler

**Attendu :** `204`; l'abonnement passe en `CANCELLED` et une archive est créée. Rappel : cela n'efface pas l'abonnement de la base. Une deuxième annulation devrait renvoyer `409`.

## 10. Statistiques et export — `/api/statistics`

### `GET /api/statistics`

**Attendu :** `200` :

```json
{ "totalActiveCustomers": 12, "monthlyRevenue": 36000.00 }
```

### `GET /api/statistics/dashboard`

**Attendu :** `200 DashboardStatisticsResponse` :

```json
{
  "totalCustomers": 12,
  "activeCustomers": 8,
  "newCustomersThisMonth": 2,
  "activeSubscriptions": 8,
  "expiredSubscriptions": 3,
  "expiringSubscriptionsNext30Days": 1,
  "subscriptionsSoldThisMonth": 2,
  "estimatedMonthlyRevenue": 24000.00,
  "subscriptionsByPack": [
    { "packName": "Pack Postman", "subscriptions": 1 }
  ]
}
```

Comparer les KPI avec les données créées dans le même tenant. Le revenu est une estimation mensuelle d'abonnements actifs, non un total des paiements encaissés.

### `GET /api/statistics/revenue?startDate=...&endDate=...`

Exemple :

```text
GET {{apiUrl}}/statistics/revenue?startDate=2026-10-01&endDate=2026-10-31
```

**Attendu :** `200`, champs `startDate`, `endDate`, `estimatedMonthlyValue`. Dates absentes/invalides ou start après end → `400`.

### `GET /api/statistics/revenue/monthly?startDate=...&endDate=...`

**Attendu :** `200` tableau de `{ "month": "YYYY-MM-DD", "monthlyValue": 3000.00 }`. Sans résultat, vérifier que le tableau vide est cohérent.

### `GET /api/statistics/export?startDate=...&endDate=...`

**Attendu :** `200`, `Content-Type: text/csv`, téléchargement `subscriptions.csv`, en-tête CSV et abonnements/archives attendus pour la période. Vérifier les caractères accentués et l'échappement des valeurs CSV. Période invalide → `400`.

## 11. Utilisateurs et autorisations — `/api/users`

Toutes les routes sauf `/me` et `/change-password` sont ADMIN seulement.

### `GET /api/users`

```text
GET {{apiUrl}}/users?page=0&size=20&sort=createdAt,desc
```

**Attendu :** `200 PageResponse<UserResponse>`, limité à l'organisation. Aucun mot de passe/hash dans aucune ligne.

### `GET /api/users/{id}`

**Attendu :** `200 UserResponse`; utilisateur autre tenant/non existant → `404`.

### `POST /api/users` — créer un compte de l'organisation

**Admin requis.**

```json
{
  "username": "postman-staff",
  "email": "postman-staff@example.test",
  "password": "Staff-Test-Password-2026",
  "role": "USER"
}
```

**Attendu :** `201 UserResponse`; sauvegarder ID en `testUserId`. Essayer `role: "ROOT"`, email invalide, password trop court ou username dupliqué → `400`/`409`.

### `PUT /api/users/{id}`

**Admin requis.** Tous les champs sont optionnels :

```json
{
  "username": "postman-staff-updated",
  "email": "postman-staff-updated@example.test",
  "role": "USER"
}
```

**Attendu :** `200`. Vérifier qu'un changement de rôle incrémente la version de jeton et invalide les tokens existants de ce compte.

### `DELETE /api/users/{id}` — désactiver

**Admin requis. Attendu :** `204`. Token de cet utilisateur → `401`; utilisateur conservé dans l'historique et désactivé. Le compte ADMIN utilisé pour la suite ne doit pas être désactivé par erreur.

### Tester le rôle `USER`

1. Créer l'utilisateur avec POST `/users`.
2. Se connecter avec `POST /auth/login`, `organizationSlug={{organizationSlug}}`, username `postman-staff`, password.
3. Enregistrer le token retourné dans `userToken`.
4. `GET /users/me` avec `userToken` → `200`.
5. `GET /users` avec `userToken` → `403`.
6. `GET /packs` avec `userToken` → `200`.
7. `POST /packs` avec `userToken` → `403`.
8. `GET /audit` et `/notifications/outbox` avec `userToken` → `403`.

## 12. Paiements et remboursements — `/api/payments`

Les transactions sont liées à un abonnement et idempotentes. Aucun numéro de carte ne doit être envoyé ou stocké. Les méthodes électroniques restent en attente : aucun fournisseur de paiement réel n'est connecté.

### `GET /api/payments`

```text
GET {{apiUrl}}/payments?page=0&size=20&sort=createdAt,desc
```

**Attendu :** `200 PageResponse<PaymentResponse>`, tenant-scoped. Tri autorisé : `id`, `amount`, `status`, `createdAt`.

### `POST /api/payments` — créer paiement

**Body :**

```json
{
  "subscriptionId": {{subscriptionId}},
  "amount": 3000.00,
  "currency": "XOF",
  "method": "CASH",
  "idempotencyKey": "postman-payment-20261001-0001"
}
```

`idempotencyKey` : 8–100 caractères parmi lettres/chiffres/`.` `_` `:` `-`; il doit être unique par intention de paiement et organisation.

**Attendu :** `201 PaymentResponse`; enregistrer `paymentId`. Selon l'implémentation actuelle, même un paiement CASH nouvellement créé est `PENDING` jusqu'à confirmation explicite.

Répéter exactement la même requête et même clé : l'API doit renvoyer le même paiement (idempotence), pas en créer un second. Réutiliser cette clé avec amount/devise/méthode/abonnement différent → `409`. Utiliser une nouvelle clé crée un autre paiement.

### `POST /api/payments/{id}/cash-confirmation`

**Admin requis**, body vide. `POST {{apiUrl}}/payments/{{paymentId}}/cash-confirmation`.  
**Attendu :** `200 PaymentResponse` avec `status: COMPLETED` et `settledAt`. Répéter la confirmation déjà effectuée devrait rester stable. Pour un paiement non-CASH → `409`.

### `POST /api/payments/{id}/refunds`

Route protégée ADMIN selon la configuration Security.  
**Body :**

```json
{
  "amount": 500.00,
  "reason": "Test Postman, remboursement partiel"
}
```

**Attendu :** `201 RefundResponse` avec `status: REQUESTED`; enregistrer `refundId`. Paiement non encaissé, montant <=0 ou montant total remboursé + demandes en cours supérieur au paiement → erreur `409` (validation de corps invalide → `400`).

### `POST /api/payments/refunds/{refundId}/complete`

**Admin requis**, body vide. `POST {{apiUrl}}/payments/refunds/{{refundId}}/complete`.  
**Attendu :** `200 RefundResponse`, statut `COMPLETED`, paiement `PARTIALLY_REFUNDED` ou `REFUNDED`. Deuxième traitement de la même demande → `409`.

Tester plusieurs demandes concurrentes si possible, en restant dans le solde encaissé. Vérifier que la somme remboursée ne dépasse jamais le paiement.

## 13. Webhook fournisseur (optionnel) — `POST /api/payments/webhooks/provider`

Cette route est publique côté Spring Security mais exige une signature HMAC correcte. Elle ne peut réussir que si `PAYMENT_WEBHOOK_SECRET` est défini côté backend et qu'un paiement `PENDING` existe.

Message exact à signer en UTF-8 :

```text
<organizationId>:<paymentId>:<providerReference>
```

Signature : HMAC-SHA256 en hex minuscule, envoyée dans `X-Payment-Signature`.

Body :

```json
{
  "organizationId": 7,
  "paymentId": 42,
  "providerReference": "provider-ref-postman-42"
}
```

Exemple PowerShell (secret ne doit pas être committé) :

```powershell
$secret = "<même secret que PAYMENT_WEBHOOK_SECRET>"
$payload = "7:42:provider-ref-postman-42"
$key = [Text.Encoding]::UTF8.GetBytes($secret)
$hmac = [System.Security.Cryptography.HMACSHA256]::new($key)
$signature = [Convert]::ToHexString(
    $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($payload))
).ToLowerInvariant()
$signature
```

Requête : `POST {{apiUrl}}/payments/webhooks/provider`, header `X-Payment-Signature: {{webhookSignature}}`.  
**Attendu :** `204`, paiement lié passe à `COMPLETED`, référence fournisseur définie.

Cas négatifs : signature fausse/hex invalide → `401`; corps invalide ou header absent → `400`; paiement/organisation inconnus → `404`; référence déjà attachée à un autre paiement ou paiement déjà lié à une autre référence → `409`. Le webhook générique n'intègre pas de timestamp/nonce anti-rejeu spécifique au fournisseur : ne l'exposer à aucun fournisseur réel sans adapter et auditer ce contrat.

## 14. Audit et notifications

### `GET /api/audit`

**Admin requis.**

```text
GET {{apiUrl}}/audit?page=0&size=20&sort=occurredAt,desc
```

**Attendu :** `200 PageResponse` avec `actor`, `action`, `resourceType`, `resourceId`, `occurredAt`; ne doit contenir aucun password/token. Tester pagination et isolation du tenant.

### `GET /api/notifications/outbox`

**Admin requis.**

```text
GET {{apiUrl}}/notifications/outbox?page=0&size=20&sort=createdAt,desc
```

**Attendu :** `200 PageResponse<NotificationOutboxResponse>` avec `eventType`, `status`, `attempts`, dates et `lastError`. Examiner les notifications créées par inscription, abonnement et paiement. Les emails sont désactivés par défaut; l'élément peut rester `QUEUED`.

### `POST /api/notifications/outbox/{id}/retry`

**Admin requis**, body vide.  
**Attendu :** `204` si la relance est possible; notification absente/autre tenant → `404`; état non relançable (par exemple déjà `SENT` ou `PROCESSING`) → `409`.

## 15. Actuator et documentation

| Requête | Accès | Attendu |
|---|---|---|
| `GET /actuator/health` | public | `200`, statut `UP` si prêt |
| `GET /actuator/info` | public | `200` |
| `GET /actuator/metrics` | ADMIN | `200` |
| `GET /actuator/prometheus` | ADMIN | `200`, texte Prometheus |
| `GET /swagger-ui/index.html` | public | page Swagger |
| `GET /v3/api-docs` | public | document OpenAPI JSON |

Sans authentification, `/actuator/metrics` et `/actuator/prometheus` doivent refuser l'accès (`401`). Avec un USER authentifié, accès interdit (`403`).

## 16. Cas transversaux de sécurité et tenant

Créer une seconde organisation par inscription avec slug distinct et s'authentifier avec son token. Vérifier :

- client/pack/abonnement/utilisateur/paiement/audit créés dans org A n'apparaissent pas dans org B;
- GET par ID de ressource de l'autre tenant renvoie `404`, pas les données;
- update, delete, refund, completion et changement de statut cross-tenant échouent également sans modifier les données;
- ne jamais pouvoir fournir un `organizationId` dans un body pour contourner le tenant;
- la lecture `/users/me` et organisation `/organizations/me` correspondent au token courant;
- un JWT modifié, tronqué, expiré, révoqué ou sans claims tenant renvoie `401`;
- `GET /api/customers` sans Authorization → `401 UNAUTHORIZED`;
- les entêtes `X-Request-ID` sont renvoyés et ne changent pas le statut métier.

L'isolation est actuellement applicative (repositories/services), sans PostgreSQL RLS. Les tests Postman complètent mais ne remplacent pas des tests d'intégration et une revue de sécurité.

## 17. Validation, pagination et erreurs à éprouver partout

Pour toute collection paginée (`customers`, `packs`, `subscriptions`, `users`, `payments`, `audit`, `notifications/outbox`), tester :

| Entrée | Résultat attendu |
|---|---|
| `page=0&size=20` | `200`, métadonnées cohérentes |
| `page` au-delà du nombre de pages | `200`, `content: []`, pas d'exception |
| `size=1` | au plus un élément |
| `size=100` | accepté |
| `size=0`, `size=101` | `400` |
| `page=-1` | `400` |
| `sort` champ supporté asc/desc | `200`, ordre correct |
| champ de tri inconnu ou direction incorrecte | `400` |

Vérifier les codes communs :

- `200`: lecture/mise à jour;
- `201`: création, avec corps et Location lorsque prévu;
- `204`: logout, suppression logique, annulation, relance, changement de mot de passe;
- `400`: validation, pagination, enum/date/JSON invalides;
- `401`: token absent, mauvais, expiré, verrouillage/authentification invalide;
- `403`: utilisateur authentifié sans rôle ADMIN;
- `404`: ID inexistant ou hors tenant;
- `409`: doublon, règle métier/état/conflit d'idempotence;
- `500`: défaut inattendu ; corps public sans stacktrace.

Noter pour chaque anomalie : méthode + URL (sans secrets), statut, headers pertinents, corps de requête anonymisé, réponse, `X-Request-ID`, heure et étapes de reproduction.

## 18. Checklist de fin de campagne

- [ ] Inscription crée une organisation et uniquement le compte initial prévu comme ADMIN.
- [ ] Connexion exige le bon slug, username et password; le JWT fonctionne sur les routes protégées.
- [ ] Logout, changement de mot de passe, changement de rôle et désactivation invalident le token concerné.
- [ ] DTOs ne divulguent pas les mots de passe hachés, secrets, tokens ou données d'autres tenants.
- [ ] Les validations rejetées sont `400` structurés et les conflits métier sont explicites.
- [ ] Toutes les listes paginent/filtrent/ordonnent et restent bornées.
- [ ] Suppression client/pack/abonnement conserve les références historiques.
- [ ] Les dates, statuts actifs, expirés et futurs sont cohérents.
- [ ] Les KPI sont propres au tenant et les chiffres sont interprétés comme des estimations documentées.
- [ ] La clé d'idempotence empêche les doubles paiements et les remboursements ne dépassent pas le solde.
- [ ] Une erreur SMTP n'annule pas la transaction métier ; retries outbox sont traçables.
- [ ] Le CSV est valide, tenant-scoped et protège les cellules contre les formules.
- [ ] Tous les tests cross-tenant échouent de façon sûre.
- [ ] Le comportement réel PostgreSQL est également couvert par `mvnw test` avec Docker/Testcontainers.

Consigner les échecs dans une collection Postman ou un rapport séparé avec l'environnement utilisé. Ne pas exporter une collection contenant `adminToken`, `testPassword` ou `webhookSecret`.
