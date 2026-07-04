package juribook.lawyer_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Avis laissé par un client sur un avocat, après un rendez-vous honoré.
 *
 * Rattaché à un bookingId précis (pas juste un couple lawyerId/clientId) :
 * le client désigne quelle réservation il évalue, ce qui permet une
 * contrainte d'unicité sans ambiguïté (un avis par réservation, cf.
 * migration V4) et une vérification d'éligibilité stricte, cf.
 * ReviewService.createReview, qui interroge le booking-service pour
 * confirmer que ce bookingId appartient bien à ce client, concerne bien
 * cet avocat, et est bien au statut COMPLETED avant d'accepter l'avis.
 *
 * lawyerId et clientId sont dénormalisés depuis la réservation au moment
 * de la création (pas de FK JPA, principe déjà établi dans les autres
 * services : database per service, simples colonnes de corrélation).
 */
@Entity
@Table(name = "reviews")
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lawyer_id", nullable = false)
    private Long lawyerId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    // Référence à la réservation COMPLETED qui a rendu cet avis
    // possible. UNIQUE en base (migration V4) : un avis par réservation.
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}