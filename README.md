# SmartBooking

> Plateforme de réservation intelligente conçue pour gérer les réservations de manière fiable, même en cas de concurrence ou de requêtes répétées.

## 📌 Présentation

SmartBooking est une API REST de réservation développée avec **Java et Spring Boot**.

Le projet met l'accent sur la fiabilité des réservations et la gestion des situations pouvant provoquer des doublons ou des conflits, notamment :

* deux utilisateurs qui réservent simultanément le même créneau ;
* une même requête envoyée plusieurs fois à cause d'un double-clic ou d'un retry réseau ;
* des changements de statut de réservation invalides ;
* la gestion sécurisée des utilisateurs et des accès.

L'objectif est de construire une application backend réaliste, testée et prête à être intégrée à un frontend Angular.

---

## 🎯 Objectifs techniques

SmartBooking a été conçu autour de plusieurs problématiques courantes dans les applications de réservation :

### 1. Gestion de la concurrence

Deux utilisateurs peuvent essayer de réserver simultanément la même ressource et le même créneau.

Le projet utilise un **verrou pessimiste JPA (`PESSIMISTIC_WRITE`) sur la ressource** afin de sérialiser les créations de réservations concurrentes.

Cela permet d'éviter qu'une vérification de disponibilité effectuée simultanément par plusieurs transactions aboutisse à deux réservations incompatibles.

### 2. Idempotence

Une requête peut être envoyée plusieurs fois à cause :

* d'un double-clic ;
* d'un problème réseau ;
* d'un retry automatique.

SmartBooking utilise une **`Idempotency-Key`** pour identifier une requête de réservation déjà traitée.

Une nouvelle requête utilisant la même clé peut ainsi retrouver la réservation existante au lieu d'en créer une seconde.

### 3. Machine à états

Une réservation possède plusieurs statuts :

```text
PENDING
   │
   ├──> CONFIRMED
   │       │
   │       ├──> CHECKED_IN
   │       │       │
   │       │       └──> COMPLETED
   │       │
   │       ├──> CANCELLED
   │       └──> NO_SHOW
   │
   └──> CANCELLED
```

Les transitions sont contrôlées afin d'empêcher les changements de statut invalides.

Les statuts terminaux sont :

* `COMPLETED`
* `CANCELLED`
* `NO_SHOW`

---

## 🛠️ Technologies

### Backend

* **Java 17**
* **Spring Boot 4**
* Spring Web
* Spring Data JPA
* Spring Security
* JWT
* BCrypt
* Bean Validation
* PostgreSQL
* Flyway

### Tests

* JUnit 5
* Spring Boot Test
* Tests d'intégration
* Tests de concurrence
* Tests d'idempotence
* Tests de transitions d'état

### Documentation API

* OpenAPI
* Swagger UI

### DevOps

* Docker
* Docker Compose
* Git
* GitHub
* GitHub Actions
* Maven

### Frontend prévu

* Angular

---

## 🏗️ Architecture

Le backend suit une organisation par responsabilités :

```text
src/
└── main/
    ├── java/
    │   └── com.smartbooking/
    │       ├── config/
    │       ├── controller/
    │       ├── domain/
    │       │   ├── model/
    │       │   └── repository/
    │       ├── dto/
    │       ├── exception/
    │       ├── security/
    │       └── service/
    │
    └── resources/
        ├── db/
        │   └── migration/
        └── application.yaml
```

Les responsabilités sont séparées entre :

* **Controllers** : exposition des endpoints REST ;
* **Services** : logique métier ;
* **Repositories** : accès aux données ;
* **Entities** : modèle de données ;
* **DTOs** : échanges entre API et clients ;
* **Security** : authentification et autorisation ;
* **Exceptions** : gestion centralisée des erreurs ;
* **Migrations** : évolution contrôlée de la base de données.

---

## 🔐 Authentification et sécurité

L'API utilise **Spring Security et JWT**.

Les utilisateurs peuvent :

* créer un compte ;
* se connecter ;
* obtenir un token JWT ;
* accéder aux endpoints protégés selon leurs droits.

Deux rôles sont actuellement utilisés :

```text
USER
ADMIN
```

Certaines opérations sont réservées aux administrateurs, notamment la gestion des ressources et certaines opérations de gestion des réservations.

Les mots de passe sont stockés sous forme **hachée avec BCrypt**.

Les secrets et informations sensibles sont fournis par des **variables d'environnement** et ne sont pas stockés dans le dépôt Git.

---

## 📚 API

### Authentification

```http
POST /api/auth/register
POST /api/auth/login
```

### Ressources

```http
GET  /api/resources
POST /api/resources
```

### Réservations

```http
POST  /api/bookings
GET   /api/bookings/{bookingId}
GET   /api/bookings/me
PATCH /api/bookings/{bookingId}/status
PATCH /api/bookings/{bookingId}/cancel
```

---

## 🔑 Idempotency-Key

Lors de la création d'une réservation, le client peut fournir :

```http
Idempotency-Key: unique-request-key
```

Exemple :

```http
POST /api/bookings
Idempotency-Key: booking-2027-001
```

Si la même requête est répétée avec la même clé, SmartBooking peut retrouver la réservation déjà créée plutôt que d'en générer une nouvelle.

---

## 🔒 Gestion des conflits

Lorsqu'un créneau est déjà réservé, l'API retourne une erreur HTTP :

```text
409 Conflict
```

Cela permet au frontend de distinguer un conflit de réservation d'une erreur serveur classique.

---

## 🗄️ Base de données

SmartBooking utilise **PostgreSQL**.

Les principales tables sont :

```text
users
resources
bookings
flyway_schema_history
```

Les relations principales sont :

```text
User
  │
  └──< Booking >── Resource
```

La base de données est versionnée avec **Flyway**.

Les migrations sont stockées dans :

```text
src/main/resources/db/migration
```

La première migration est :

```text
V1__init.sql
```

Hibernate utilise :

```yaml
ddl-auto: validate
```

La structure de la base est donc contrôlée par Flyway plutôt que générée automatiquement par Hibernate.

---

## 🧪 Tests

Le projet contient actuellement des tests couvrant notamment :

* démarrage de l'application ;
* création de réservations ;
* concurrence entre réservations ;
* idempotence ;
* transitions de statut ;
* contrôle des accès ;
* endpoints REST.

La suite actuelle comprend **8 tests automatisés**.

Exécution locale :

```bash
./mvnw clean test
```

Résultat attendu :

```text
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

---

## ⚙️ Continuous Integration

Le projet utilise **GitHub Actions** pour automatiser la validation du code.

À chaque `push` sur `main` ou `pull request`, la CI :

1. démarre PostgreSQL ;
2. configure Java 17 ;
3. exécute les migrations Flyway ;
4. compile le projet ;
5. exécute les tests.

Pipeline :

```text
Git Push
   ↓
GitHub Actions
   ↓
PostgreSQL
   ↓
Flyway
   ↓
Maven
   ↓
Tests
   ↓
✅ SUCCESS
```

### Statut CI

La branche `main` est actuellement validée par GitHub Actions.

---

## 📖 Swagger / OpenAPI

La documentation interactive de l'API est disponible avec Swagger UI.

Après avoir lancé l'application :

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger permet notamment de :

* consulter les endpoints ;
* voir les paramètres ;
* tester les requêtes ;
* observer les réponses HTTP.

---

## 🐳 Lancer le projet localement

### Prérequis

Installer :

* Java 17
* Docker Desktop
* Git

### 1. Cloner le projet

```bash
git clone https://github.com/bodio237/smartbooking.git
cd smartbooking
```

### 2. Démarrer PostgreSQL

```bash
docker compose up -d
```

### 3. Configurer les variables d'environnement

Créer un fichier `.env` à partir de :

```text
.env.example
```

Les variables principales sont :

```text
DB_USER
DB_PASSWORD
DB_NAME
JWT_SECRET
```

Ne jamais publier le fichier `.env` contenant de vraies informations sensibles.

### 4. Lancer l'application

Sous Windows :

```powershell
.\mvnw.cmd spring-boot:run
```

Sous Linux/macOS :

```bash
./mvnw spring-boot:run
```

L'API est ensuite disponible sur :

```text
http://localhost:8080
```

Swagger :

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 🐘 PostgreSQL avec Docker

Le projet utilise PostgreSQL dans un conteneur Docker.

Pour démarrer les services :

```bash
docker compose up -d
```

Pour vérifier les conteneurs :

```bash
docker ps
```

Pour arrêter les services :

```bash
docker compose down
```

---

## 📂 Variables d'environnement

Les valeurs sensibles ne sont pas versionnées dans Git.

Un fichier d'exemple est fourni :

```text
.env.example
```

Exemple :

```text
DB_USER=your_db_user
DB_PASSWORD=your_db_password
DB_NAME=your_db_name
JWT_SECRET=your_jwt_secret_at_least_32_characters_long
```

---

## 🚧 Roadmap

### Backend

* [x] API REST
* [x] Authentification JWT
* [x] Gestion des rôles
* [x] Gestion des ressources
* [x] Création de réservations
* [x] Gestion des conflits
* [x] Verrou pessimiste pour la concurrence
* [x] Idempotency-Key
* [x] Machine à états
* [x] Gestion centralisée des exceptions
* [x] Validation des données
* [x] PostgreSQL
* [x] Flyway
* [x] Tests automatisés
* [x] GitHub Actions
* [x] Swagger / OpenAPI

### Frontend

* [ ] Application Angular
* [ ] Authentification utilisateur
* [ ] Consultation des ressources
* [ ] Création d'une réservation
* [ ] Affichage des disponibilités
* [ ] Gestion des réservations utilisateur
* [ ] Interface administrateur

### Déploiement

* [ ] Déploiement du backend
* [ ] Déploiement du frontend
* [ ] Base PostgreSQL distante
* [ ] Configuration CORS de production
* [ ] Variables d'environnement de production

### Améliorations possibles

* [ ] Pagination
* [ ] Recherche et filtres
* [ ] Notifications
* [ ] Audit des changements de statut
* [ ] Monitoring
* [ ] Tests de charge
* [ ] Kafka ou RabbitMQ selon les besoins fonctionnels

---

## 🤖 Utilisation de l'IA

L'IA a été utilisée comme **outil d'assistance au développement**, notamment pour :

* comprendre certaines erreurs techniques ;
* explorer différentes approches d'implémentation ;
* améliorer certains tests ;
* revoir la structure du code ;
* documenter certains choix techniques.

Les choix d'architecture, l'implémentation, les tests et la validation du comportement de l'application restent sous la responsabilité du développeur.

L'objectif est d'utiliser l'IA comme un outil d'apprentissage et d'assistance, et non comme un substitut à la compréhension du code.

---

## 👩‍💻 Auteur

**Pricilia Bodio**

Étudiante ingénieure en informatique — ISEN Nantes

Domaines d'intérêt :

* Développement backend
* Développement full-stack
* Java / Spring Boot
* Angular
* Bases de données
* API REST
* Tests et qualité logicielle
* CI/CD

---

## 📄 Licence

Ce projet est développé dans le cadre d'un projet personnel à vocation pédagogique et professionnelle.
