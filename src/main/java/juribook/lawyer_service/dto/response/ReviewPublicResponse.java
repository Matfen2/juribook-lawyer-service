package juribook.lawyer_service.dto.response;

import juribook.lawyer_service.entity.Review;

import java.time.LocalDateTime;

/**
 * Représentation PUBLIQUE d'un avis, volontairement plus
 * restreinte que ReviewResponse : ni clientId, ni bookingId, ni
 * visible n'ont leur place dans un affichage public sur la fiche
 * avocat. Seuls note, commentaire et date sont montrés, cf. critère
 * d'acceptation littéral du sprint.
 */
public record ReviewPublicResponse(
    Long id,
    int rating,
    String comment,
    LocalDateTime createdAt
) {
    public static ReviewPublicResponse from(Review review) {
        return new ReviewPublicResponse(
                review.getId(), review.getRating(), review.getComment(), review.getCreatedAt()
        );
    }
}