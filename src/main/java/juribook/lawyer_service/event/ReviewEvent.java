package juribook.lawyer_service.event;

import java.time.LocalDateTime;

/**
 * Payload sérialisé en JSON et publié sur le topic Kafka review-events
 * à chaque création d'avis.
 *
 * Contient le rating et le commentaire directement (pas juste
 * reviewId), les consommateurs déjà identifiés dans le cahier des
 * charges (notification, audit, analytics, abuse-detection) ont tous
 * besoin du contenu pour agir (ex: détection d'abus sur le texte,
 * agrégation de note moyenne), pas seulement d'un identifiant à
 * re-résoudre.
 */
public record ReviewEvent(
    String eventType,
    Long reviewId,
    Long lawyerId,
    Long clientId,
    Long bookingId,
    int rating,
    String comment,
    LocalDateTime occurredAt
) {
}