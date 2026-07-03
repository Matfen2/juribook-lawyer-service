package juribook.lawyer_service.event;

import java.time.LocalDateTime;

/**
 * Payload sérialisé en JSON et publié sur le topic Kafka lawyer-events
 * quand la disponibilité d'un avocat change.
 *
 * available reprend directement Lawyer.available, le signal "cet
 * avocat accepte-t-il de nouvelles réservations" déjà utilisé pour
 * filtrer la recherche (GET /api/lawyers?...). Publié uniquement quand
 * ce champ change réellement de valeur (cf. LawyerService.updateProfile),
 * pas à chaque mise à jour de profil.
 */
public record LawyerEvent(
    String eventType,
    Long lawyerId,
    Boolean available,
    LocalDateTime occurredAt
) {
}