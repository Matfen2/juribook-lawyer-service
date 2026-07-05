package juribook.lawyer_service.service;

import juribook.lawyer_service.entity.Lawyer;
import juribook.lawyer_service.exception.LawyerProfileNotFoundException;
import juribook.lawyer_service.repository.LawyerRepository;
import juribook.lawyer_service.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests dédiés à LawyerService.recalculateRating - 
 * fichier séparé de LawyerServiceTest.java (CRUD/recherche) pour rester
 * lisible, même classe testée.
 *
 * LawyerEventPublisher et ReviewRepository mockés séparément (pas
 * @InjectMocks global) : ce fichier ne teste QUE recalculateRating, pas
 * besoin de reconstruire tout le contexte CRUD/recherche pour ça.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LawyerService.recalculateRating")
class LawyerServiceRecalculateRatingTest {

    @Mock private LawyerRepository lawyerRepository;
    @Mock private juribook.lawyer_service.repository.SpecialtyRepository specialtyRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private juribook.lawyer_service.event.LawyerEventPublisher lawyerEventPublisher;

    private LawyerService lawyerService;

    private static final Long LAWYER_ID = 4L;
    private Lawyer lawyer;

    @BeforeEach
    void setUp() {
        lawyerService = new LawyerService(lawyerRepository, specialtyRepository, reviewRepository, lawyerEventPublisher);

        lawyer = new Lawyer();
        lawyer.setId(LAWYER_ID);
        lawyer.setName("Sophie Martin");
    }

    @Test
    @DisplayName("plusieurs avis - moyenne arrondie à une décimale")
    void recalculateRating_multipleReviews_roundsToOneDecimal() {
        when(lawyerRepository.findById(LAWYER_ID)).thenReturn(Optional.of(lawyer));
        when(reviewRepository.findAverageRatingByLawyerId(LAWYER_ID)).thenReturn(4.42857142857);
        when(reviewRepository.countByLawyerIdAndVisibleTrue(LAWYER_ID)).thenReturn(7L);
        when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

        lawyerService.recalculateRating(LAWYER_ID);

        ArgumentCaptor<Lawyer> captor = ArgumentCaptor.forClass(Lawyer.class);
        verify(lawyerRepository).save(captor.capture());
        assertThat(captor.getValue().getAverageRating()).isEqualTo(4.4);
        assertThat(captor.getValue().getReviewCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("aucun avis visible restant - averageRating repasse à null, pas 0.0")
    void recalculateRating_noVisibleReviews_setsNullNotZero() {
        when(lawyerRepository.findById(LAWYER_ID)).thenReturn(Optional.of(lawyer));
        when(reviewRepository.findAverageRatingByLawyerId(LAWYER_ID)).thenReturn(null); // AVG() sur vide = NULL en SQL
        when(reviewRepository.countByLawyerIdAndVisibleTrue(LAWYER_ID)).thenReturn(0L);
        when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

        lawyerService.recalculateRating(LAWYER_ID);

        ArgumentCaptor<Lawyer> captor = ArgumentCaptor.forClass(Lawyer.class);
        verify(lawyerRepository).save(captor.capture());
        assertThat(captor.getValue().getAverageRating()).isNull();
        assertThat(captor.getValue().getReviewCount()).isZero();
    }

    @Test
    @DisplayName("avocat introuvable - lève LawyerProfileNotFoundException")
    void recalculateRating_lawyerNotFound_throws() {
        when(lawyerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lawyerService.recalculateRating(999L))
                .isInstanceOf(LawyerProfileNotFoundException.class);
    }

    @Test
    @DisplayName("un seul avis à 5 - moyenne exacte, pas d'arrondi parasite")
    void recalculateRating_singleFiveStarReview_exactAverage() {
        when(lawyerRepository.findById(LAWYER_ID)).thenReturn(Optional.of(lawyer));
        when(reviewRepository.findAverageRatingByLawyerId(LAWYER_ID)).thenReturn(5.0);
        when(reviewRepository.countByLawyerIdAndVisibleTrue(LAWYER_ID)).thenReturn(1L);
        when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

        lawyerService.recalculateRating(LAWYER_ID);

        ArgumentCaptor<Lawyer> captor = ArgumentCaptor.forClass(Lawyer.class);
        verify(lawyerRepository).save(captor.capture());
        assertThat(captor.getValue().getAverageRating()).isEqualTo(5.0);
    }
}