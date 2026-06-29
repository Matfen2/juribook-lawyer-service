# juribook-lawyer-service

Microservice de gestion des profils avocats pour **JuriBook** : création et modification du profil, recherche par spécialité et ville, consultation publique.

## Stack

- Java 21 · Spring Boot 4.1.0 · Maven
- Spring Security · JWT (validation des tokens émis par l'auth-service)
- PostgreSQL 16 · Flyway (migrations + données de référence)
- Apache Kafka (producer, topic `lawyer-events`)
- Springdoc OpenAPI (Swagger UI)
- Port : **8082**

## Structure du projet

```
src/main/java/juribook/lawyer_service/
├── config/
│   ├── SecurityConfig.java             # Règles d'accès par rôle + filtre JWT
│   └── OpenApiConfig.java              # Configuration Swagger UI
├── controller/
│   └── LawyerController.java           # GET /api/lawyers, POST/GET/PUT /api/lawyers/profile, GET /api/specialties
├── dto/
│   ├── request/
│   │   ├── CreateLawyerProfileRequest.java
│   │   └── UpdateLawyerProfileRequest.java
│   └── response/
│       ├── LawyerProfileResponse.java  # Profil complet (détail)
│       ├── LawyerSearchResponse.java   # Profil allégé (liste de recherche)
│       ├── SpecialtyResponse.java
│       └── AddressResponse.java
├── entity/
│   ├── Lawyer.java                     # Entité JPA principale
│   ├── Specialty.java                  # Table de référence (15 spécialités)
│   └── Address.java                    # @Embeddable — adresse du cabinet
├── exception/
│   ├── GlobalExceptionHandler.java     # Handlers 400/401/403/404/409/500
│   ├── LawyerProfileNotFoundException.java
│   └── LawyerProfileAlreadyExistsException.java
├── filter/
│   └── JwtAuthenticationFilter.java    # Filtre Spring Security (OncePerRequestFilter)
├── repository/
│   ├── LawyerRepository.java           # Requêtes JPQL + recherche paginée
│   └── SpecialtyRepository.java
└── service/
    └── LawyerService.java              # Logique métier profil + recherche
    └── JwtService.java                 # Validation des tokens JWT (lecture seule)
src/main/resources/
├── application.yaml
└── db/migration/
    ├── V1__create_lawyers_tables.sql   # Tables lawyers, specialties, lawyer_specialties
    └── V2__insert_specialties.sql      # 15 spécialités prédéfinies du barreau français
```

## Lancer en local (hors Docker)

```bash
# Prérequis : PostgreSQL sur localhost:5433 avec la base lawyerdb
# L'auth-service doit tourner sur localhost:8081 pour valider les tokens
mvn spring-boot:run
```

## Lancer via Docker Compose

```bash
# Depuis juribook-docker/docker/
docker compose up -d postgres-lawyer lawyer-service
```

## Swagger UI

[http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)

## Health check

[http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)

---

## Endpoints

### Publics (pas de token requis)

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/api/lawyers` | Recherche paginée avec filtres optionnels |
| `GET` | `/api/lawyers/{id}` | Profil public d'un avocat |
| `GET` | `/api/specialties` | Liste des 15 spécialités prédéfinies |

### Protégés LAWYER (token JWT requis, rôle LAWYER)

| Méthode | URL | Description |
|---|---|---|
| `POST` | `/api/lawyers/profile` | Créer son profil (une seule fois) |
| `GET` | `/api/lawyers/profile` | Consulter son propre profil |
| `PUT` | `/api/lawyers/profile` | Modifier son profil (patch partiel) |

---

## Exemples Postman

### Lister les spécialités (public)
```
GET http://localhost:8082/api/specialties
```
Retourne les 15 spécialités avec leur `id` et `slug` — noter les IDs pour la création de profil.

---

### Rechercher des avocats (public)

Tous les paramètres sont optionnels et cumulables :

```
# Tous les avocats disponibles
GET http://localhost:8082/api/lawyers

# Par spécialité (slug)
GET http://localhost:8082/api/lawyers?specialty=droit-du-travail

# Par ville
GET http://localhost:8082/api/lawyers?city=Paris

# Combiné
GET http://localhost:8082/api/lawyers?specialty=droit-du-travail&city=Paris

# Avec tarif maximum et pagination
GET http://localhost:8082/api/lawyers?maxRate=300&page=0&size=10

# Recherche textuelle libre
GET http://localhost:8082/api/lawyers?query=barreau
```

Réponse - 200 :
```json
{
    "content": [
        {
            "id": 1,
            "barNumber": "75001",
            "bioExcerpt": "Avocat au barreau de Paris depuis 10 ans...",
            "hourlyRate": 200,
            "yearsExperience": 10,
            "available": true,
            "averageRating": null,
            "reviewCount": 0,
            "address": {
                "city": "Paris",
                "postalCode": "75001",
                "region": "Île-de-France"
            },
            "specialties": [
                { "id": 1, "name": "Droit du travail", "slug": "droit-du-travail" },
                { "id": 5, "name": "Droit des affaires", "slug": "droit-des-affaires" }
            ]
        }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
}
```

---

### Créer son profil avocat
```json
POST http://localhost:8082/api/lawyers/profile
Authorization: Bearer <token_jwt_avocat>
Content-Type: application/json

{
    "barNumber": "75001",
    "bio": "Avocat au barreau de Paris depuis 10 ans, spécialisé en droit du travail et droit des affaires.",
    "hourlyRate": 200,
    "yearsExperience": 10,
    "languages": "Français, Anglais",
    "specialtyIds": [1, 5],
    "street": "12 rue de la Paix",
    "city": "Paris",
    "postalCode": "75001",
    "department": "Paris",
    "region": "Île-de-France"
}
```
Réponse - 201

---

### Consulter son propre profil
```
GET http://localhost:8082/api/lawyers/profile
Authorization: Bearer <token_jwt_avocat>
```
Réponse — 200

---

### Modifier son profil (patch partiel)
```json
PUT http://localhost:8082/api/lawyers/profile
Authorization: Bearer <token_jwt_avocat>
Content-Type: application/json

{
    "hourlyRate": 250,
    "available": false,
    "bio": "Bio mise à jour."
}
```
Réponse : 200 - seuls les champs fournis sont modifiés.

---

### Profil public d'un avocat
```
GET http://localhost:8082/api/lawyers/1
```
Pas de token requis — Réponse — 200

---

### Cas d'erreur

```
POST /api/lawyers/profile une 2e fois      → 409 Conflict
POST /api/lawyers/profile sans token       → 401 Unauthorized
POST /api/lawyers/profile avec token CLIENT → 403 Forbidden
POST avec specialtyIds: [999]             → 400 "Spécialité introuvable : id=999"
POST sans city                            → 400 "La ville est obligatoire"
```

---

## Commandes SQL utiles

### Accéder à la base PostgreSQL

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb
```

### Lister tous les profils avocats

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb -c "SELECT id, auth_user_id, bar_number, city, available, average_rating FROM lawyers;"
```

### Lister les spécialités

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb -c "SELECT id, name, slug FROM specialties ORDER BY id;"
```

### Lister les spécialités d'un avocat

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb -c "SELECT l.bar_number, s.name FROM lawyers l JOIN lawyer_specialties ls ON l.id = ls.lawyer_id JOIN specialties s ON s.id = ls.specialty_id;"
```

### Vérifier les migrations Flyway

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

### Réinitialiser le schéma (dev uniquement)

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

---

## Variables d'environnement

| Variable | Description | Valeur par défaut |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL PostgreSQL | `jdbc:postgresql://localhost:5433/lawyerdb` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur PostgreSQL | `juribook` |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL | `juribook` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Adresse Kafka | `localhost:9092` |
| `JWT_SECRET` | Secret JWT partagé avec l'auth-service | valeur de dev |

---

## Modèle de données

### Relations JPA

```
Lawyer ──< lawyer_specialties >── Specialty   (ManyToMany)
Address est embarquée dans lawyers             (@Embeddable)
```

### Spécialités prédéfinies (V2__insert_specialties.sql)

| Slug | Nom |
|---|---|
| `droit-du-travail` | Droit du travail |
| `droit-de-la-famille` | Droit de la famille |
| `droit-penal` | Droit pénal |
| `droit-immobilier` | Droit immobilier |
| `droit-des-affaires` | Droit des affaires |
| `droit-fiscal` | Droit fiscal |
| `droit-de-la-consommation` | Droit de la consommation |
| `droit-administratif` | Droit administratif |
| `droit-des-etrangers` | Droit des étrangers |
| `droit-de-la-sante` | Droit de la santé |
| `droit-des-successions` | Droit des successions |
| `droit-des-contrats` | Droit des contrats |
| `droit-propriete-intellectuelle` | Droit de la propriété intellectuelle |
| `droit-bancaire` | Droit bancaire |
| `droit-environnement` | Droit de l'environnement |

---

## Sécurité JWT

Le lawyer-service **ne génère pas** de JWT, il valide uniquement les tokens émis par l'auth-service grâce au secret partagé (`jwt.secret`).

Les claims extraits du token :
- `id` → authUserId (Long) - stocké dans `SecurityContext.principal`
- `role` → LAWYER | CLIENT | ADMIN - utilisé pour les règles d'accès
- `barNumber` → numéro de barreau (optionnel - peut être absent selon la version de l'auth-service)