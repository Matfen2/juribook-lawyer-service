package juribook.lawyer_service.service;

import juribook.lawyer_service.dto.response.LawyerSearchResponse;
import juribook.lawyer_service.event.LawyerEventPublisher;
import juribook.lawyer_service.event.SearchEventPublisher;
import juribook.lawyer_service.repository.LawyerRepository;
import juribook.lawyer_service.repository.ReviewRepository;
import juribook.lawyer_service.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Tests de LawyerService.search() côté publication search-events
 * (Sprint 7.5).
 *
 * ⚠️ Fichier volontairement scindé de LawyerServiceTest.java (jamais
 * vu en entier) : évite de deviner à l'aveugle sa structure existante.
 * Si LawyerServiceTest.java construit LawyerService manuellement (ex:
 * `new LawyerService(...)` à 4 arguments, comme
 * LawyerServiceRecalculateRatingTest le faisait plus tôt), il faudra
 * lui ajouter un 5e argument (SearchEventPublisher mocké) partout où le
 * constructeur est appelé explicitement — @InjectMocks n'a besoin que
 * d'un @Mock supplémentaire dans la classe de test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LawyerService.search - publication search-events (Sprint 7.5)")
class LawyerServiceSearchEventTest {

    @Mock private LawyerRepository lawyerRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private LawyerEventPublisher lawyerEventPublisher;
    @Mock private SearchEventPublisher searchEventPublisher;

    @InjectMocks
    private LawyerService lawyerService;

    @BeforeEach
    void setUp() {
        lenient().when(lawyerRepository.search(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));
        lenient().when(lawyerRepository.searchWithQuery(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));
    }

    @Test
    @DisplayName("recherche avec specialty+city - publie les valeurs normalisées")
    void search_withSpecialtyAndCity_publishesNormalizedValues() {
        lawyerService.search("Droit-Du-Travail", "paris", null, null, 0, 20);

        ArgumentCaptor<String> specialtyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cityCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchEventPublisher).publishSearchPerformed(
                specialtyCaptor.capture(), cityCaptor.capture(), any());

        // normalisé : slug en minuscules, ville avec 1re lettre capitalisée
        org.assertj.core.api.Assertions.assertThat(specialtyCaptor.getValue()).isEqualTo("droit-du-travail");
        org.assertj.core.api.Assertions.assertThat(cityCaptor.getValue()).isEqualTo("Paris");
    }

    @Test
    @DisplayName("recherche sans aucun filtre - publie quand même, avec null partout")
    void search_noFilters_publishesWithNulls() {
        lawyerService.search(null, null, null, null, 0, 20);

        verify(searchEventPublisher).publishSearchPerformed(isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("recherche avec query textuelle - transmet la query normalisée (trim)")
    void search_withQuery_publishesTrimmedQuery() {
        lawyerService.search(null, null, "  licenciement  ", null, 0, 20);

        verify(searchEventPublisher).publishSearchPerformed(isNull(), isNull(), eq("licenciement"));
    }

    @Test
    @DisplayName("recherche avec query textuelle utilise searchWithQuery, publie quand même l'event")
    void search_withQuery_stillPublishesEvent() {
        Page<LawyerSearchResponse> ignored = lawyerService.search("droit-penal", "Lyon", "urgent", 300, 0, 20);

        verify(searchEventPublisher).publishSearchPerformed("droit-penal", "Lyon", "urgent");
    }
}