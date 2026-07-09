package juribook.lawyer_service.service;

import juribook.lawyer_service.client.BookingServiceClient;
import juribook.lawyer_service.dto.response.ReviewResponse;
import juribook.lawyer_service.entity.Review;
import juribook.lawyer_service.event.ReviewEventPublisher;
import juribook.lawyer_service.exception.ReviewNotFoundException;
import juribook.lawyer_service.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Tests de ReviewService pour la modération admin,
 * fichier séparé du ReviewServiceTest existant (non fourni en entier),
 * pour ne pas risquer de dupliquer/écraser ses mocks à l'aveugle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService - Modération")
class ReviewServiceModerationTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private ReviewEventPublisher reviewEventPublisher;
    @Mock private LawyerService lawyerService;

    private ReviewService reviewService;

    private static final Long LAWYER_ID = 4L;
    private static final Long REVIEW_ID = 1L;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, bookingServiceClient, reviewEventPublisher, lawyerService);
    }

    private Review buildReview(int rating, boolean visible) {
        Review r = new Review();
        r.setId(REVIEW_ID);
        r.setLawyerId(LAWYER_ID);
        r.setClientId(42L);
        r.setBookingId(7L);
        r.setRating(rating);
        r.setVisible(visible);
        return r;
    }

    @Nested
    @DisplayName("getReviewsForModeration")
    class GetReviewsForModeration {

        @Test
        @DisplayName("délègue au repository et mappe vers ReviewResponse")
        void getReviewsForModeration_delegatesAndMaps() {
            Review review = buildReview(1, true);
            Page<Review> page = new PageImpl<>(java.util.List.of(review));
            when(reviewRepository.findForModeration(isNull(), any(Pageable.class))).thenReturn(page);

            Page<ReviewResponse> result = reviewService.getReviewsForModeration(null, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).rating()).isEqualTo(1);
            verify(reviewRepository).findForModeration(isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("filtre visible transmis tel quel")
        void getReviewsForModeration_passesVisibleFilter() {
            when(reviewRepository.findForModeration(eq(false), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(java.util.List.of()));

            reviewService.getReviewsForModeration(false, 0, 20);

            verify(reviewRepository).findForModeration(eq(false), any(Pageable.class));
        }

        @Test
        @DisplayName("clampe la taille de page à 50")
        void getReviewsForModeration_clampsPageSize() {
            when(reviewRepository.findForModeration(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(java.util.List.of()));

            reviewService.getReviewsForModeration(null, 0, 500);

            verify(reviewRepository).findForModeration(any(), argThat(p -> p.getPageSize() == 50));
        }
    }

    @Nested
    @DisplayName("deleteReview")
    class DeleteReview {

        @Test
        @DisplayName("cas nominal - supprime définitivement et recalcule la note moyenne")
        void deleteReview_deletesAndRecalculatesRating() {
            Review review = buildReview(1, true);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            reviewService.deleteReview(REVIEW_ID);

            verify(reviewRepository).delete(review);
            verify(lawyerService).recalculateRating(LAWYER_ID);
        }

        @Test
        @DisplayName("ne publie aucun événement Kafka (cohérent avec hide/unhide)")
        void deleteReview_neverPublishesEvent() {
            Review review = buildReview(3, true);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            reviewService.deleteReview(REVIEW_ID);

            verifyNoInteractions(reviewEventPublisher);
        }

        @Test
        @DisplayName("avis introuvable - lève ReviewNotFoundException, ne supprime rien")
        void deleteReview_notFound_throwsWithoutDeleting() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.deleteReview(999L))
                    .isInstanceOf(ReviewNotFoundException.class);

            verify(reviewRepository, never()).delete(any());
            verifyNoInteractions(lawyerService);
        }
    }
}