package juribook.lawyer_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Profil public d'un avocat inscrit sur JuriBook.
 *
 * Cette entité est distincte de l'entité User de l'auth-service.
 * Elle contient uniquement les données professionnelles nécessaires
 * à la recherche et la prise de rendez-vous :
 *   - Identification : authUserId (clé étrangère logique vers auth-service)
 *   - Cabinet : adresse embarquée (@Embedded)
 *   - Spécialités : liste @ManyToMany avec la table de référence
 *   - Disponibilité : booléen pour filtrer la recherche
 *
 * Principe microservices : chaque service possède ses propres données.
 * On ne fait PAS de jointure inter-services, on utilise authUserId
 * pour récupérer le nom/email depuis l'auth-service si nécessaire.
 *
 * Relations :
 *   Lawyer ──< lawyer_specialties >── Specialty   (ManyToMany)
 *   Address est embarquée dans la table lawyers    (@Embedded)
 */
@Entity
@Table(name = "lawyers")
@Data
public class Lawyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Lien vers l'auth-service ─────────────────────────────
    // ID de l'utilisateur correspondant dans l'auth-service.
    // Unique : un utilisateur ne peut avoir qu'un seul profil avocat.
    @Column(name = "auth_user_id", nullable = false, unique = true)
    private Long authUserId;

    // ── Informations professionnelles ────────────────────────

    // Numéro de barreau (5 chiffres) - identifiant officiel de l'avocat
    @Column(name = "bar_number", nullable = false, unique = true, length = 5)
    private String barNumber;

    // Présentation libre affichée sur le profil public
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    // Tarif horaire indicatif en euros (peut être null si non communiqué)
    @Column(name = "hourly_rate")
    private Integer hourlyRate;

    // Nombre d'années d'expérience
    @Column(name = "years_experience")
    private Integer yearsExperience;

    // Langues parlées - ex : "Français, Anglais, Arabe"
    @Column(name = "languages", length = 200)
    private String languages;

    // ── Adresse du cabinet ───────────────────────────────────
    // @Embedded : stockée dans la même table (pas de jointure)
    @Embedded
    private Address address;

    // ── Spécialités ──────────────────────────────────────────
    // ManyToMany → table de jointure lawyer_specialties
    // LAZY : on ne charge les spécialités que si nécessaire
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lawyer_specialties",
        joinColumns = @JoinColumn(name = "lawyer_id"),
        inverseJoinColumns = @JoinColumn(name = "specialty_id")
    )
    private List<Specialty> specialties = new ArrayList<>();

    // ── Statut et disponibilité ──────────────────────────────

    // true si l'avocat accepte de nouveaux clients
    @Column(name = "available", nullable = false)
    private boolean available = true;

    // Note moyenne calculée à partir des avis (mis à jour par booking-service)
    @Column(name = "average_rating")
    private Double averageRating;

    // Nombre total d'avis reçus
    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    // ── Audit ────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}