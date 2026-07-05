package juribook.lawyer_service.dto.response;

import juribook.lawyer_service.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long lawyerId,
    Long clientId,
    Long bookingId,
    int rating,
    String comment,
    boolean visible,
    LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(), review.getLawyerId(), review.getClientId(),
                review.getBookingId(), review.getRating(), review.getComment(),
                review.isVisible(), review.getCreatedAt()
        );
    }
}