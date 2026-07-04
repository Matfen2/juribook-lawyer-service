package juribook.lawyer_service.service;

import juribook.lawyer_service.client.BookingDetailsDto;
import juribook.lawyer_service.client.BookingServiceClient;
import juribook.lawyer_service.dto.request.CreateReviewRequest;
import juribook.lawyer_service.dto.response.ReviewPublicResponse;
import juribook.lawyer_service.dto.response.ReviewResponse;
import juribook.lawyer_service.entity.Review;
import juribook.lawyer_service.event.ReviewEventPublisher;
import juribook.lawyer_service.exception.InvalidReviewException;
import juribook.lawyer_service.exception.NotEligibleToReviewException;
import juribook.lawyer_service.exception.ReviewNotFoundException;
import juribook.lawyer_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Création, modération et consultation d'avis (Sprint 6.1 → 6.5).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingServiceClient bookingServiceClient;
    private final ReviewEventPublisher reviewEventPublisher;
    private final LawyerService lawyerService;

    @Transactional
    public ReviewResponse createReview(Long clientId, CreateReviewRequest request) {
        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new InvalidReviewException(
                "Un avis a déjà été laissé pour cette réservation");
        }

        Optional<BookingDetailsDto> bookingOpt = bookingServiceClient.getBookingDetails(request.getBookingId());
        BookingDetailsDto booking = bookingOpt.orElseThrow(() -> new InvalidReviewException(
            "Réservation introuvable : id=" + request.getBookingId()));

        if (!clientId.equals(booking.clientId())) {
            throw new NotEligibleToReviewException(
                "Cette réservation ne vous appartient pas");
        }

        if (!"COMPLETED".equals(booking.status())) {
            throw new NotEligibleToReviewException(
                "Seul un rendez-vous honoré (COMPLETED) peut faire l'objet d'un avis — statut actuel : " + booking.status());
        }

        Review review = new Review();
        review.setLawyerId(booking.lawyerId());
        review.setClientId(clientId);
        review.setBookingId(request.getBookingId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        log.info("Avis créé : id={}, lawyerId={}, clientId={}, bookingId={}, rating={}",
                saved.getId(), saved.getLawyerId(), clientId, request.getBookingId(), request.getRating());

        lawyerService.recalculateRating(saved.getLawyerId());
        reviewEventPublisher.publishReviewCreated(saved);

        return ReviewResponse.from(saved);
    }

    // ══════════════════════════════════════════════════════════
    //  Consultation publique (Sprint 6.5)
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ReviewPublicResponse> getVisibleReviews(Long lawyerId) {
        return reviewRepository.findByLawyerIdAndVisibleTrueOrderByCreatedAtDesc(lawyerId)
                .stream()
                .map(ReviewPublicResponse::from)
                .toList();
    }

    // ══════════════════════════════════════════════════════════
    //  Modération admin (Sprint 6.4)
    // ══════════════════════════════════════════════════════════

    @Transactional
    public ReviewResponse hideReview(Long reviewId) {
        return setVisibility(reviewId, false);
    }

    @Transactional
    public ReviewResponse unhideReview(Long reviewId) {
        return setVisibility(reviewId, true);
    }

    private ReviewResponse setVisibility(Long reviewId, boolean visible) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(
                    "Avis introuvable : id=" + reviewId));

        review.setVisible(visible);
        Review saved = reviewRepository.save(review);

        log.info("Avis {} : id={}, lawyerId={}", visible ? "démasqué" : "masqué",
                saved.getId(), saved.getLawyerId());

        lawyerService.recalculateRating(saved.getLawyerId());

        return ReviewResponse.from(saved);
    }
}