package juribook.lawyer_service.service;

import juribook.lawyer_service.client.BookingDetailsDto;
import juribook.lawyer_service.client.BookingServiceClient;
import juribook.lawyer_service.dto.request.CreateReviewRequest;
import juribook.lawyer_service.dto.response.ReviewResponse;
import juribook.lawyer_service.entity.Review;
import juribook.lawyer_service.event.ReviewEventPublisher;
import juribook.lawyer_service.exception.InvalidReviewException;
import juribook.lawyer_service.exception.NotEligibleToReviewException;
import juribook.lawyer_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Création d'avis, seul un client ayant eu un
 * rendez-vous COMPLETED peut laisser un avis, une seule fois par
 * réservation, l'avocat concerné est déduit de la réservation (jamais
 * déclaré par le client).
 *
 * Après sauvegarde de l'avis, délègue à LawyerService le
 * recalcul de la note moyenne du profil, dans la MÊME transaction
 * (@Transactional couvre les deux appels), pour garantir que le profil
 * n'affiche jamais une note désynchronisée des avis réellement
 * enregistrés.
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

        // Recalcul immédiat, même transaction, jamais de
        // fenêtre où le profil afficherait une note obsolète.
        lawyerService.recalculateRating(saved.getLawyerId());

        reviewEventPublisher.publishReviewCreated(saved);

        return ReviewResponse.from(saved);
    }
}