package juribook.lawyer_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Avis laissé par un client sur un avocat, après un rendez-vous honoré.
 *
 * visible (Sprint 6.4) : true par défaut, passé à false par l'admin
 * pour masquer un avis inapproprié (PATCH /api/reviews/{id}/hide). Un
 * avis masqué reste en base (pas de suppression — traçabilité, cf.
 * ReviewService), mais n'entre plus dans le calcul de la note moyenne
 * (ReviewRepository.findAverageRatingByLawyerId filtre dessus).
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

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}