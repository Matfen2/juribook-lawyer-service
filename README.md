# juribook-lawyer-service

Microservice de gestion des profils avocats pour **JuriBook** : création et modification du profil, recherche par spécialité et ville, consultation publique, publication d'événements Kafka sur les changements de disponibilité, avis clients avec note moyenne calculée et modération admin.

## Stack

- Java 21 · Spring Boot 4.1.0 · Maven
- Spring Security · JWT (validation des tokens émis par l'auth-service)
- PostgreSQL 16 · Flyway (migrations + données de référence)
- Apache Kafka (producer - topics `lawyer-events`, `review-events`)
- Spring Web (`RestClient`) - appel inter-services vers `booking-service` 
- Springdoc OpenAPI (Swagger UI)
- Port : **8082**

## Structure du projet

```
src/main/java/juribook/lawyer_service/
├── client/
│   ├── BookingDetailsDto.java          # Miroir PARTIEL de BookingHistoryResponse (booking-service)
│   ├── BookingServiceClient.java       # Interface - getBookingDetails
│   └── RestClientBookingServiceClient.java
├── config/
│   ├── SecurityConfig.java             # Règles d'accès par rôle + filtre JWT
│   ├── OpenApiConfig.java              # Configuration Swagger UI
│   └── JacksonConfig.java              # Bean ObjectMapper explicite (cf. Notes techniques — fix date juillet 2026)
├── controller/
│   ├── LawyerController.java           # GET /api/lawyers, POST/GET/PUT /api/lawyers/profile, GET /api/specialties
│   └── ReviewController.java           # POST /api/reviews, PATCH /api/reviews/{id}/hide|unhide (Sprint 6.1/6.4)
├── dto/
│   ├── request/
│   │   ├── CreateLawyerProfileRequest.java
│   │   ├── UpdateLawyerProfileRequest.java
│   │   └── CreateReviewRequest.java    # bookingId, rating, comment
│   └── response/
│       ├── LawyerProfileResponse.java  # Profil complet (détail), inclut authUserId, averageRating, reviewCount
│       ├── LawyerSearchResponse.java   # Profil allégé (liste de recherche)
│       ├── SpecialtyResponse.java
│       ├── AddressResponse.java
│       └── ReviewResponse.java         # Inclut visible
├── entity/
│   ├── Lawyer.java                     # Entité JPA principale, averageRating/reviewCount recalculés
│   ├── Specialty.java                  # Table de référence (15 spécialités)
│   ├── Address.java                    # @Embeddable, adresse du cabinet
│   └── Review.java                     # Avis client, rating, comment, visible
├── event/
│   ├── LawyerEvent.java                # Payload JSON lawyer-events
│   ├── LawyerEventPublisher.java       # Interface - publishStatusChanged
│   ├── LawyerEventPublisherImpl.java   # Impl unique, décide à l'exécution via ObjectProvider<KafkaTemplate>
│   ├── ReviewEvent.java                # Payload JSON review-events 
│   ├── ReviewEventPublisher.java       # Interface - publishReviewCreated
│   └── ReviewEventPublisherImpl.java   # Même pattern ObjectProvider<KafkaTemplate>
├── exception/
│   ├── GlobalExceptionHandler.java     # Handlers 400/401/403/404/409/500
│   ├── LawyerProfileNotFoundException.java
│   ├── LawyerProfileAlreadyExistsException.java
│   ├── InvalidReviewException.java     # 400 - avis déjà existant, réservation introuvable 
│   ├── NotEligibleToReviewException.java  # 403 - réservation n'appartenant pas au client, statut ≠ COMPLETED 
│   └── ReviewNotFoundException.java    # 404 - modération sur un avis inexistant
├── filter/
│   └── JwtAuthenticationFilter.java    # Filtre Spring Security (OncePerRequestFilter)
├── repository/
│   ├── LawyerRepository.java           # Requêtes JPQL + recherche paginée
│   ├── SpecialtyRepository.java
│   └── ReviewRepository.java           # existsByBookingId, agrégats note moyenne 
└── service/
    ├── LawyerService.java              # Logique métier profil + recherche + Kafka + recalculateRating
    ├── ReviewService.java              # Création + modération d'avis
    └── JwtService.java                 # Validation des tokens JWT
src/main/resources/
├── application.yaml
└── db/migration/
    ├── V1__create_lawyers_tables.sql   # Tables lawyers, specialties, lawyer_specialties
    ├── V2__insert_specialties.sql      # 15 spécialités prédéfinies du barreau français
    ├── V3__add_name_to_lawyers.sql
    ├── V4__create_reviews_table.sql   
    └── V5__add_visible_to_reviews.sql 
```

## Lancer en local (hors Docker)

```bash
# Prérequis : PostgreSQL sur localhost:5433 avec la base lawyerdb
# L'auth-service doit tourner sur localhost:8081 pour valider les tokens
# Kafka n'est pas requis pour démarrer (KafkaTemplate se connecte de
# façon paresseuse), sans broker, la publication est juste sautée
# (log DEBUG), cf. Kafka ci-dessous.
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
| `PUT` | `/api/lawyers/profile` | Modifier son profil (patch partiel), un changement de `available` publie `lawyer.status-changed` |

### Protégés CLIENT (token JWT requis, rôle CLIENT)

| Méthode | URL | Description |
|---|---|---|
| `POST` | `/api/reviews` | Laisser un avis sur une réservation `COMPLETED`, publie `review.created` |

### Protégés ADMIN (token JWT requis, rôle ADMIN)

| Méthode | URL | Description |
|---|---|---|
| `PATCH` | `/api/reviews/{id}/hide` | Masquer un avis inapproprié, recalcule la note moyenne immédiatement |
| `PATCH` | `/api/reviews/{id}/unhide` | Annuler un masquage |

---

## Exemples Postman

### Lister les spécialités (public)
```
GET http://localhost:8082/api/specialties
```
Retourne les 15 spécialités avec leur `id` et `slug`, noter les IDs pour la création de profil.

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
    "name": "Sophie Martin",
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
Réponse - 201. Ne publie **pas** d'événement Kafka : `available` démarre toujours à `true`, ce n'est pas un "changement" au sens de `lawyer.status-changed`.

---

### Consulter son propre profil
```
GET http://localhost:8082/api/lawyers/profile
Authorization: Bearer <token_jwt_avocat>
```
Réponse — 200

---

### Modifier son profil, sans toucher à `available`
```json
PUT http://localhost:8082/api/lawyers/profile
Authorization: Bearer <token_jwt_avocat>
Content-Type: application/json

{
    "hourlyRate": 250,
    "bio": "Bio mise à jour."
}
```
Réponse : 200. Aucun événement Kafka publié - `available` n'est pas dans la requête.

---

### Se désactiver — publie `lawyer.status-changed`
```json
PUT http://localhost:8082/api/lawyers/profile
Authorization: Bearer <token_jwt_avocat>
Content-Type: application/json

{
    "available": false
}
```
Réponse : 200. Publie `lawyer.status-changed` sur Kafka avec `available: false`, le `booking-service` en tient compte pour refuser toute nouvelle réservation et annuler automatiquement les demandes `PENDING` en cours. Repasser `available: true` republie l'événement avec `available: true`.

---

### Profil public d'un avocat
```
GET http://localhost:8082/api/lawyers/1
```
Pas de token requis - Réponse - 200

---

### Laisser un avis

```json
POST http://localhost:8082/api/reviews
Authorization: Bearer <token_jwt_client>
Content-Type: application/json

{
    "bookingId": 4,
    "rating": 5,
    "comment": "Avocate très à l'écoute, je recommande."
}
```
Réponse - 201 :
```json
{
    "id": 1,
    "lawyerId": 4,
    "clientId": 4,
    "bookingId": 4,
    "rating": 5,
    "comment": "Avocate très à l'écoute, je recommande.",
    "visible": true,
    "createdAt": "2026-07-04T02:06:58.496375"
}
```
`lawyerId` n'est **jamais** fourni par le client, il est résolu côté serveur à partir de `bookingId` (appel à `GET /api/bookings/{id}` sur le booking-service). Publie `review.created` sur `review-events`, et recalcule immédiatement `averageRating`/`reviewCount` sur le profil de l'avocat concerné (Sprint 6.3, même transaction).

⚠️ Prérequis pour que ça fonctionne : la réservation désignée par `bookingId` doit être au statut `COMPLETED`. **Aucun mécanisme automatique ne fait encore cette transition** (`CONFIRMED → COMPLETED`), à ce stade, il faut la forcer manuellement en base pour tester :
```bash
docker exec -it juribook-postgres-booking psql -U juribook -d bookingdb -c "UPDATE bookings SET status = 'COMPLETED' WHERE id = 4;"
```

---

### Cas d'erreur sur la création d'avis

```
POST /api/reviews avec rating hors 1-5              → 400 "La note doit être comprise entre 1 et 5"
POST /api/reviews sur un bookingId déjà évalué        → 400 "Un avis a déjà été laissé pour cette réservation"
POST /api/reviews sur un bookingId inexistant          → 400 "Réservation introuvable : id=..."
POST /api/reviews sur une réservation d'un autre client → 403 "Cette réservation ne vous appartient pas"
POST /api/reviews sur une réservation pas COMPLETED     → 403 "Seul un rendez-vous honoré (COMPLETED) peut faire l'objet d'un avis — statut actuel : ..."
```

---

### Masquer un avis inapproprié (ADMIN)

```
PATCH http://localhost:8082/api/reviews/1/hide
Authorization: Bearer <token_jwt_admin>
```
Réponse - 200, `visible: false`. La note moyenne de l'avocat est recalculée immédiatement, sans cet avis.

```
PATCH http://localhost:8082/api/reviews/1/unhide
Authorization: Bearer <token_jwt_admin>
```
Réponse - 200, `visible: true`, l'avis repèse dans la moyenne.

Refusé - **403** avec un token CLIENT ou LAWYER ; **404** si `id` n'existe pas.

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

### Lister les avis d'un avocat, y compris masqués

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb -c "SELECT id, client_id, booking_id, rating, comment, visible, created_at FROM reviews WHERE lawyer_id = 4 ORDER BY created_at DESC;"
```

### Vérifier la cohérence note moyenne / avis réellement visibles

```bash
docker exec -it juribook-postgres-lawyer psql -U juribook -d lawyerdb -c "SELECT l.id, l.average_rating, l.review_count, (SELECT ROUND(AVG(rating)::numeric, 1) FROM reviews WHERE lawyer_id = l.id AND visible = true) AS avg_recalcule, (SELECT COUNT(*) FROM reviews WHERE lawyer_id = l.id AND visible = true) AS count_recalcule FROM lawyers l WHERE l.id = 4;"
```
Les deux paires de colonnes doivent toujours être identiques : `LawyerService.recalculateRating` s'exécute dans la même transaction que toute création/masquage d'avis, aucune fenêtre de désynchronisation possible.

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
Review ──> Lawyer, Review ──> Booking (booking-service)   (colonnes de corrélation, pas de FK JPA)
```

`Review.lawyerId` et `Review.clientId` ne sont pas des FK JPA, même principe que partout ailleurs dans le projet (*database per service*). `Review.bookingId` porte une contrainte `UNIQUE` en base (migration V4) : un avis par réservation, jamais deux.

### Note moyenne — recalcul complet, pas incrémental (Sprint 6.3/6.4)

`Lawyer.averageRating` (nullable, `null` tant qu'aucun avis visible n'existe) et `Lawyer.reviewCount` sont recalculés **entièrement** à chaque création, masquage, ou démasquage d'avis, `LawyerService.recalculateRating` relit tous les avis `visible = true` de l'avocat via `AVG()`/`COUNT()` SQL, plutôt que de maintenir une somme courante à incrémenter/décrémenter. Plus simple, correct par construction, largement suffisant pour le volume d'avis attendu sur ce projet.

Arrondi à une décimale pour l'affichage (`4.4285714...` → `4.4`). Toujours exécuté dans la **même transaction** que l'opération déclenchante (`ReviewService.createReview`/`hideReview`/`unhideReview`) : si le recalcul échoue, l'opération sur l'avis est annulée aussi, le profil n'affiche jamais une moyenne désynchronisée des avis réellement visibles en base.

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

## Kafka

### Topics et événements publiés

| Topic | Événement | Déclencheur |
|---|---|---|
| `lawyer-events` | `lawyer.status-changed` | `PUT /api/lawyers/profile` : uniquement quand `available` change réellement de valeur |
| `review-events` | `review.created` | `POST /api/reviews` : à chaque avis créé (jamais sur masquage/démasquage, cf. limites connues) |

`LawyerService.updateProfile` capture la valeur de `available` **avant** modification, et ne publie que si la nouvelle valeur diffère de l'ancienne, pas à chaque sauvegarde de profil qui ne touche pas à ce champ.

### Format des payloads

`lawyer-events` (`LawyerEvent`) :
```json
{
    "eventType": "lawyer.status-changed",
    "lawyerId": 4,
    "available": false,
    "occurredAt": "2026-07-02T20:30:00.123"
}
```

`review-events` (`ReviewEvent`), contient le rating et le commentaire directement, pas juste `reviewId` : les consommateurs identifiés (notification, audit, analytics, abuse-detection) ont tous besoin du contenu pour agir (détection d'abus sur le texte, agrégation de note moyenne côté analytics), pas seulement d'un identifiant à re-résoudre :
```json
{
    "eventType": "review.created",
    "reviewId": 1,
    "lawyerId": 4,
    "clientId": 4,
    "bookingId": 4,
    "rating": 5,
    "comment": "Avocate très à l'écoute, je recommande.",
    "occurredAt": "2026-07-04T02:06:58.496375"
}
```

Clé de partition Kafka = `lawyerId` dans les deux cas : garantit l'ordre des événements successifs concernant un même avocat au sein d'une partition (utile pour un futur calcul incrémental de note moyenne côté analytics, si le recalcul synchrone actuel devait un jour être déchargé vers un consumer dédié).

### Consommateurs

| Service | Topic | Usage |
|---|---|---|
| `booking-service` | `lawyer-events` | Cache local `LawyerStatusCache`, refuse les nouvelles réservations et annule les `PENDING` en cours si l'avocat devient indisponible |
| `audit-service` | les deux | Trace automatiquement, aucune configuration spécifique nécessaire, écoute déjà les 8 topics de la plateforme |

`notification-service` ne consomme ni `lawyer-events` ni `review-events` à ce stade.

### Activation

`LawyerEventPublisherImpl` et `ReviewEventPublisherImpl` décident **à l'exécution** si Kafka est disponible, via `ObjectProvider<KafkaTemplate<String, String>>`, pas via `@ConditionalOnBean`, cf. [Notes techniques](#notes-techniques). Sans broker en local, la publication est simplement sautée (log `DEBUG`), le démarrage et les autres fonctionnalités du service ne sont jamais affectés.

### Vérifier manuellement (broker actif)

```bash
docker exec -it juribook-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic lawyer-events --from-beginning
docker exec -it juribook-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic review-events --from-beginning
```

---

## Sécurité JWT

Le lawyer-service **ne génère pas** de JWT, il valide uniquement les tokens émis par l'auth-service grâce au secret partagé (`jwt.secret`).

Les claims extraits du token :
- `id` → authUserId (Long) - stocké dans `SecurityContext.principal`
- `role` → LAWYER | CLIENT | ADMIN - utilisé pour les règles d'accès
- `barNumber` → numéro de barreau (optionnel - peut être absent selon la version de l'auth-service)

---

## Notes techniques

### `ObjectProvider<KafkaTemplate>` plutôt que `@ConditionalOnBean`

`booking-service` a rencontré un bug où `@ConditionalOnBean(KafkaTemplate.class)` sur un `@Component` classique échouait systématiquement à cause de l'ordre de scan vs autoconfiguration Spring Boot, le `@Component` est scanné avant que `KafkaAutoConfiguration` n'ait tourné, donc la condition ne voit jamais le `KafkaTemplate`, même quand Kafka est parfaitement actif. `LawyerEventPublisherImpl` applique directement le pattern corrigé dès sa première version (`ObjectProvider<KafkaTemplate<String, String>>`, résolu à chaque publication plutôt qu'à l'enregistrement du bean), jamais rencontré ce bug ici.

### `ObjectMapper` (Jackson 2) explicite

Même fix que `notification-service`/`audit-service`/`booking-service` : Spring Boot 4 utilise Jackson 3 (`JsonMapper`) en interne pour la sérialisation HTTP des `@RestController`, ce qui ne crée aucun bean `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2 classique) automatiquement. `LawyerEventPublisherImpl` et `ReviewEventPublisherImpl` en dépendent explicitement pour sérialiser leurs événements en JSON, `JacksonConfig.java` fournit ce bean.

### Sérialisation des dates

`new ObjectMapper().findAndRegisterModules()` enregistre le support de `java.time`, mais **ne désactive pas** le comportement par défaut de Jackson qui sérialise les dates en tableau de composants numériques (`[2026,7,4,2,6,58,496375400]`) plutôt qu'en chaîne ISO-8601. Spring Boot désactive ce comportement automatiquement dans son propre `ObjectMapper` autoconfiguré, mais un `new ObjectMapper()` construit à la main comme dans `JacksonConfig` contourne cette configuration. Repéré en observant un `occurredAt` de `review.created` mal formé dans `audit_entries`, tous les événements publiés par ce service (et par `booking-service`/`notification-service`/`audit-service`, même pattern partout) avant ce fix ont un `occurredAt` illisible pour quiconque tente de le parser en aval.

Fix appliqué :
```java
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
}
```

---

## Limites connues

- **`lawyer.status-changed` n'est publié que sur un changement de `available`** — pas sur les autres modifications de profil (bio, tarif, spécialités...). Cohérent avec le besoin actuel (`booking-service` ne s'intéresse qu'à la disponibilité), mais si un futur besoin (ex: `search-events` pour l'analytics, notification de changement de tarif) apparaît, il faudra étendre `updateProfile` en conséquence.
- **Aucun endpoint admin dédié pour désactiver un avocat** : le déclencheur de `lawyer.status-changed` est le champ `available`, que l'avocat contrôle **lui-même** via `PUT /api/lawyers/profile`. Le cahier des charges évoquait une désactivation côté admin ; ce choix d'implémentation réutilise l'infrastructure déjà existante plutôt que d'ajouter un nouveau workflow admin.
- **Pas de provisioning explicite des topics depuis ce service** : `lawyer-events`/`review-events` sont provisionnés centralement par `kafka-init` (dépôt `juribook-docker`).
- **Aucun mécanisme automatique ne fait passer une réservation en `COMPLETED`** : condition indispensable pour laisser un avis, mais rien côté `booking-service` ne déclenche jamais cette transition (ni job planifié, ni action manuelle exposée). À ce stade, seul un `UPDATE` SQL direct en base permet de tester le flux d'avis de bout en bout. Sujet ouvert, probablement un futur sprint côté `booking-service` (job similaire à `BookingReminderJob`, qui basculerait `CONFIRMED → COMPLETED` une fois la date/heure du rendez-vous passée).
- **`review.created` n'est publié qu'à la création, jamais sur masquage/démasquage** : un consommateur qui agrégerait les avis depuis Kafka (ex: futur service analytics) ne verrait jamais qu'un avis a été masqué, et afficherait une moyenne divergente de celle recalculée en base par `LawyerService.recalculateRating`. Pas un problème pour l'instant (aucun consommateur de ce type n'existe), mais à corriger si review-events devient la source de vérité pour un futur agrégateur externe, publier aussi `review.hidden`/`review.unhidden`.
- **Pas de suppression d'avis, seulement un masquage** : choix délibéré (traçabilité), mais signifie qu'un avis clairement erroné (ex: posté par erreur sur le mauvais avocat) reste en base indéfiniment, juste invisible.
- **Pas encore d'endpoint de consultation des avis d'un avocat** : `ReviewRepository.findByLawyerIdOrderByCreatedAtDesc` existe déjà mais n'est exposé par aucune route ; seul le résumé agrégé (`averageRating`/`reviewCount` sur `LawyerProfileResponse`) est consultable pour l'instant, pas le détail des avis eux-mêmes.
- **`occurred_at` reste `NULL` de façon permanente pour tous les événements enregistrés dans `audit_entries` avant le fix Jackson ci-dessus**, le payload brut original reste correct dans la colonne `payload`, mais l'extraction ne peut pas être refaite après coup sans un script de backfill (hors scope pour l'instant).