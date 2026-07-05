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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de ReviewService - création avec toutes ses
 * validations métier, modération admin, consultation publique.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private ReviewEventPublisher reviewEventPublisher;
    @Mock private LawyerService lawyerService;

    @InjectMocks
    private ReviewService reviewService;

    private static final Long CLIENT_ID = 42L;
    private static final Long LAWYER_ID = 4L;
    private static final Long BOOKING_ID = 7L;

    private CreateReviewRequest request;
    private BookingDetailsDto completedBooking;

    @BeforeEach
    void setUp() {
        request = new CreateReviewRequest();
        request.setBookingId(BOOKING_ID);
        request.setRating(5);
        request.setComment("Excellent avocat.");

        completedBooking = new BookingDetailsDto(
                BOOKING_ID, CLIENT_ID, LAWYER_ID, 15L, "COMPLETED",
                LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), LocalTime.of(9, 30)
        );
    }

    @Nested
    @DisplayName("createReview")
    class CreateReview {

        @Test
        @DisplayName("cas nominal - sauvegarde, recalcule la note, publie l'événement")
        void createReview_happyPath_savesRecalculatesAndPublishes() {
            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
            when(bookingServiceClient.getBookingDetails(BOOKING_ID)).thenReturn(Optional.of(completedBooking));
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
                Review r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            ReviewResponse response = reviewService.createReview(CLIENT_ID, request);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.lawyerId()).isEqualTo(LAWYER_ID); // dérivé de la réservation
            assertThat(response.rating()).isEqualTo(5);

            verify(lawyerService).recalculateRating(LAWYER_ID);
            verify(reviewEventPublisher).publishReviewCreated(any(Review.class));
        }

        @Test
        @DisplayName("bookingId déjà évalué - 400, aucun appel réseau ni publication")
        void createReview_bookingAlreadyReviewed_throwsInvalidReview() {
            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(CLIENT_ID, request))
                    .isInstanceOf(InvalidReviewException.class)
                    .hasMessageContaining("déjà été laissé");

            verifyNoInteractions(bookingServiceClient, reviewEventPublisher, lawyerService);
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("réservation introuvable - 400")
        void createReview_bookingNotFound_throwsInvalidReview() {
            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
            when(bookingServiceClient.getBookingDetails(BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(CLIENT_ID, request))
                    .isInstanceOf(InvalidReviewException.class)
                    .hasMessageContaining("introuvable");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("réservation n'appartenant pas au client - 403")
        void createReview_notOwner_throwsNotEligible() {
            BookingDetailsDto othersBooking = new BookingDetailsDto(
                    BOOKING_ID, 999L, LAWYER_ID, 15L, "COMPLETED",
                    LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), LocalTime.of(9, 30));

            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
            when(bookingServiceClient.getBookingDetails(BOOKING_ID)).thenReturn(Optional.of(othersBooking));

            assertThatThrownBy(() -> reviewService.createReview(CLIENT_ID, request))
                    .isInstanceOf(NotEligibleToReviewException.class)
                    .hasMessageContaining("ne vous appartient pas");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("réservation pas COMPLETED - 403")
        void createReview_notCompleted_throwsNotEligible() {
            BookingDetailsDto pendingBooking = new BookingDetailsDto(
                    BOOKING_ID, CLIENT_ID, LAWYER_ID, 15L, "PENDING",
                    LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), LocalTime.of(9, 30));

            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
            when(bookingServiceClient.getBookingDetails(BOOKING_ID)).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> reviewService.createReview(CLIENT_ID, request))
                    .isInstanceOf(NotEligibleToReviewException.class)
                    .hasMessageContaining("PENDING");

            verify(reviewRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getVisibleReviews")
    class GetVisibleReviews {

        @Test
        @DisplayName("mappe uniquement les avis visibles, triés du plus récent")
        void getVisibleReviews_mapsToPublicResponse() {
            Review review = new Review();
            review.setId(1L);
            review.setRating(5);
            review.setComment("Top.");
            when(reviewRepository.findByLawyerIdAndVisibleTrueOrderByCreatedAtDesc(LAWYER_ID))
                    .thenReturn(List.of(review));

            List<ReviewPublicResponse> result = reviewService.getVisibleReviews(LAWYER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rating()).isEqualTo(5);
        }

        @Test
        @DisplayName("aucun avis visible - liste vide, pas d'erreur")
        void getVisibleReviews_none_returnsEmptyList() {
            when(reviewRepository.findByLawyerIdAndVisibleTrueOrderByCreatedAtDesc(LAWYER_ID))
                    .thenReturn(List.of());

            assertThat(reviewService.getVisibleReviews(LAWYER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("hideReview / unhideReview")
    class Moderation {

        @Test
        @DisplayName("hideReview - passe visible à false, recalcule la note")
        void hideReview_setsInvisible_recalculates() {
            Review review = new Review();
            review.setId(1L);
            review.setLawyerId(LAWYER_ID);
            review.setVisible(true);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

            ReviewResponse response = reviewService.hideReview(1L);

            assertThat(response.visible()).isFalse();
            verify(lawyerService).recalculateRating(LAWYER_ID);
        }

        @Test
        @DisplayName("unhideReview — repasse visible à true, recalcule la note")
        void unhideReview_setsVisible_recalculates() {
            Review review = new Review();
            review.setId(1L);
            review.setLawyerId(LAWYER_ID);
            review.setVisible(false);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

            ReviewResponse response = reviewService.unhideReview(1L);

            assertThat(response.visible()).isTrue();
            verify(lawyerService).recalculateRating(LAWYER_ID);
        }

        @Test
        @DisplayName("avis introuvable - 404, aucun recalcul")
        void hideReview_notFound_throwsReviewNotFound() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.hideReview(999L))
                    .isInstanceOf(ReviewNotFoundException.class);

            verifyNoInteractions(lawyerService);
        }
    }
}