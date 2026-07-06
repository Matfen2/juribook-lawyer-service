package juribook.lawyer_service.service;

import juribook.lawyer_service.dto.request.CreateLawyerProfileRequest;
import juribook.lawyer_service.dto.request.UpdateLawyerProfileRequest;
import juribook.lawyer_service.dto.response.LawyerProfileResponse;
import juribook.lawyer_service.dto.response.LawyerSearchResponse;
import juribook.lawyer_service.dto.response.SpecialtyResponse;
import juribook.lawyer_service.entity.Address;
import juribook.lawyer_service.entity.Lawyer;
import juribook.lawyer_service.entity.Specialty;
import juribook.lawyer_service.event.LawyerEventPublisher;
import juribook.lawyer_service.event.SearchEventPublisher;
import juribook.lawyer_service.exception.LawyerProfileAlreadyExistsException;
import juribook.lawyer_service.exception.LawyerProfileNotFoundException;
import juribook.lawyer_service.repository.LawyerRepository;
import juribook.lawyer_service.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de LawyerService - couverture CRUD, recherche, filtres, pagination.
 *
 * Convention de nommage : methode_scenario_resultatAttendu
 * Mocking : LawyerRepository, SpecialtyRepository, LawyerEventPublisher et
 * SearchEventPublisher sont mockés, aucune base de données
 * réelle ni Kafka réel n'est sollicité. LawyerEventPublisher.
 * publishStatusChanged et SearchEventPublisher.publishSearchPerformed
 * sont des méthodes void, Mockito ne nécessite aucun stubbing pour elles
 * (no-op par défaut), mais sans les mocks eux-mêmes @InjectMocks
 * laisserait les champs à null et ferait planter tout appel à
 * updateProfile (lawyerEventPublisher) ou search (searchEventPublisher).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LawyerService")
class LawyerServiceTest {

    @Mock
    private LawyerRepository lawyerRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private LawyerEventPublisher lawyerEventPublisher;

    @Mock
    private SearchEventPublisher searchEventPublisher;

    @InjectMocks
    private LawyerService lawyerService;

    private Specialty specialtyDroitTravail;
    private Specialty specialtyDroitPenal;
    private Lawyer sophieLawyer;

    @BeforeEach
    void setUp() {
        specialtyDroitTravail = new Specialty("Droit du travail", "droit-du-travail", "Litiges employeur/salarié");
        specialtyDroitTravail.setId(1L);

        specialtyDroitPenal = new Specialty("Droit pénal", "droit-penal", "Défense pénale");
        specialtyDroitPenal.setId(3L);

        sophieLawyer = new Lawyer();
        sophieLawyer.setId(10L);
        sophieLawyer.setAuthUserId(100L);
        sophieLawyer.setName("Maître Sophie Martin");
        sophieLawyer.setBarNumber("75001");
        sophieLawyer.setBio("Avocate en droit du travail depuis 12 ans.");
        sophieLawyer.setHourlyRate(220);
        sophieLawyer.setYearsExperience(12);
        sophieLawyer.setLanguages("Français, Anglais");
        sophieLawyer.setAvailable(true);
        sophieLawyer.setAverageRating(4.5);
        sophieLawyer.setReviewCount(8);
        sophieLawyer.setSpecialties(List.of(specialtyDroitTravail));
        sophieLawyer.setAddress(new Address("12 avenue de l'Opéra", "Paris", "75001", "Paris", "Île-de-France"));
    }

    // ══════════════════════════════════════════════════════════
    //  CREATE - createProfile
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createProfile")
    class CreateProfile {

        @Test
        @DisplayName("crée le profil quand aucun profil n'existe déjà pour cet authUserId")
        void createProfile_noExistingProfile_savesAndReturnsResponse() {
            CreateLawyerProfileRequest request = new CreateLawyerProfileRequest();
            request.setName("Maître Sophie Martin");
            request.setBio("Avocate en droit du travail depuis 12 ans.");
            request.setHourlyRate(220);
            request.setYearsExperience(12);
            request.setLanguages("Français, Anglais");
            request.setSpecialtyIds(List.of(1L));
            request.setCity("Paris");
            request.setStreet("12 avenue de l'Opéra");
            request.setPostalCode("75001");
            request.setDepartment("Paris");
            request.setRegion("Île-de-France");

            when(lawyerRepository.existsByAuthUserId(100L)).thenReturn(false);
            when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialtyDroitTravail));
            when(lawyerRepository.save(any(Lawyer.class))).thenReturn(sophieLawyer);

            LawyerProfileResponse response = lawyerService.createProfile(100L, "75001", "Maître Sophie Martin", request);

            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getBarNumber()).isEqualTo("75001");
            assertThat(response.getSpecialties()).hasSize(1);
            verify(lawyerRepository).save(any(Lawyer.class));
        }

        @Test
        @DisplayName("ne publie aucun événement Kafka — available démarre toujours à true")
        void createProfile_neverPublishesStatusChangedEvent() {
            CreateLawyerProfileRequest request = new CreateLawyerProfileRequest();
            request.setName("Maître Sophie Martin");
            request.setSpecialtyIds(List.of(1L));
            request.setCity("Paris");

            when(lawyerRepository.existsByAuthUserId(100L)).thenReturn(false);
            when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialtyDroitTravail));
            when(lawyerRepository.save(any(Lawyer.class))).thenReturn(sophieLawyer);

            lawyerService.createProfile(100L, "75001", "Maître Sophie Martin", request);

            verifyNoInteractions(lawyerEventPublisher);
        }

        @Test
        @DisplayName("lève LawyerProfileAlreadyExistsException si un profil existe déjà")
        void createProfile_existingProfile_throwsAlreadyExists() {
            CreateLawyerProfileRequest request = new CreateLawyerProfileRequest();
            request.setName("Maître Sophie Martin");
            request.setSpecialtyIds(List.of(1L));

            when(lawyerRepository.existsByAuthUserId(100L)).thenReturn(true);

            assertThatThrownBy(() -> lawyerService.createProfile(100L, "75001", "Maître Sophie Martin", request))
                    .isInstanceOf(LawyerProfileAlreadyExistsException.class)
                    .hasMessageContaining("existe déjà");

            verify(lawyerRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève IllegalArgumentException si une spécialité n'existe pas")
        void createProfile_unknownSpecialtyId_throwsIllegalArgument() {
            CreateLawyerProfileRequest request = new CreateLawyerProfileRequest();
            request.setName("Maître Sophie Martin");
            request.setSpecialtyIds(List.of(999L));

            when(lawyerRepository.existsByAuthUserId(100L)).thenReturn(false);
            when(specialtyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lawyerService.createProfile(100L, "75001", "Maître Sophie Martin", request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Spécialité introuvable");

            verify(lawyerRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  READ - getMyProfile / getProfileById
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getMyProfile / getProfileById")
    class ReadProfile {

        @Test
        @DisplayName("getMyProfile retourne le profil quand il existe")
        void getMyProfile_existingAuthUserId_returnsProfile() {
            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));

            LawyerProfileResponse response = lawyerService.getMyProfile(100L);

            assertThat(response.getAuthUserId()).isEqualTo(100L);
            assertThat(response.getBarNumber()).isEqualTo("75001");
        }

        @Test
        @DisplayName("getMyProfile lève LawyerProfileNotFoundException si aucun profil")
        void getMyProfile_noProfile_throwsNotFound() {
            when(lawyerRepository.findByAuthUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lawyerService.getMyProfile(999L))
                    .isInstanceOf(LawyerProfileNotFoundException.class)
                    .hasMessageContaining("Profil introuvable");
        }

        @Test
        @DisplayName("getProfileById retourne le profil public quand il existe")
        void getProfileById_existingId_returnsProfile() {
            when(lawyerRepository.findById(10L)).thenReturn(Optional.of(sophieLawyer));

            LawyerProfileResponse response = lawyerService.getProfileById(10L);

            assertThat(response.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getProfileById lève LawyerProfileNotFoundException si id inconnu")
        void getProfileById_unknownId_throwsNotFound() {
            when(lawyerRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lawyerService.getProfileById(404L))
                    .isInstanceOf(LawyerProfileNotFoundException.class)
                    .hasMessageContaining("Avocat introuvable");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UPDATE - updateProfile (patch partiel)
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("met à jour uniquement les champs non-null (patch partiel)")
        void updateProfile_partialFields_updatesOnlyProvidedFields() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setHourlyRate(250); // seul ce champ change

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            LawyerProfileResponse response = lawyerService.updateProfile(100L, request);

            assertThat(response.getHourlyRate()).isEqualTo(250);
            // La bio originale doit être préservée car non fournie dans le patch
            assertThat(response.getBio()).isEqualTo("Avocate en droit du travail depuis 12 ans.");
        }

        @Test
        @DisplayName("ne publie aucun événement Kafka si available n'est pas dans la requête")
        void updateProfile_availableNotInRequest_doesNotPublishEvent() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setHourlyRate(250); // available reste null dans la requête

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            lawyerService.updateProfile(100L, request);

            verifyNoInteractions(lawyerEventPublisher);
        }

        @Test
        @DisplayName("met à jour les spécialités quand specialtyIds est fourni et non vide")
        void updateProfile_newSpecialtyIds_replacesSpecialties() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setSpecialtyIds(List.of(3L));

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(specialtyRepository.findById(3L)).thenReturn(Optional.of(specialtyDroitPenal));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            LawyerProfileResponse response = lawyerService.updateProfile(100L, request);

            assertThat(response.getSpecialties()).hasSize(1);
            assertThat(response.getSpecialties().get(0).getSlug()).isEqualTo("droit-penal");
        }

        @Test
        @DisplayName("ne touche pas aux spécialités quand specialtyIds est null")
        void updateProfile_nullSpecialtyIds_keepsExistingSpecialties() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setBio("Nouvelle bio");
            // specialtyIds reste null

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            LawyerProfileResponse response = lawyerService.updateProfile(100L, request);

            assertThat(response.getSpecialties()).hasSize(1);
            assertThat(response.getSpecialties().get(0).getSlug()).isEqualTo("droit-du-travail");
            verify(specialtyRepository, never()).findById(any());
        }

        @Test
        @DisplayName("met à jour partiellement l'adresse en préservant les champs non fournis")
        void updateProfile_partialAddress_mergesWithExisting() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setCity("Lyon"); // seule la ville change

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            LawyerProfileResponse response = lawyerService.updateProfile(100L, request);

            assertThat(response.getAddress().getCity()).isEqualTo("Lyon");
            // Le code postal d'origine doit être préservé
            assertThat(response.getAddress().getPostalCode()).isEqualTo("75001");
        }

        @Test
        @DisplayName("met à jour la disponibilité quand available est fourni")
        void updateProfile_availableFalse_updatesAvailability() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setAvailable(false);

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            LawyerProfileResponse response = lawyerService.updateProfile(100L, request);

            assertThat(response.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("publie lawyer.status-changed quand available passe de true à false")
        void updateProfile_availableChangesFromTrueToFalse_publishesStatusChangedEvent() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setAvailable(false);

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            lawyerService.updateProfile(100L, request);

            verify(lawyerEventPublisher).publishStatusChanged(argThat(l -> !l.isAvailable()));
        }

        @Test
        @DisplayName("publie lawyer.status-changed quand available repasse de false à true")
        void updateProfile_availableChangesFromFalseToTrue_publishesStatusChangedEvent() {
            sophieLawyer.setAvailable(false);
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setAvailable(true);

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            lawyerService.updateProfile(100L, request);

            verify(lawyerEventPublisher).publishStatusChanged(argThat(Lawyer::isAvailable));
        }

        @Test
        @DisplayName("ne publie rien quand available est fourni mais identique à la valeur actuelle")
        void updateProfile_availableUnchanged_doesNotPublishEvent() {
            // sophieLawyer.available == true, requête redemande explicitement true
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setAvailable(true);

            when(lawyerRepository.findByAuthUserId(100L)).thenReturn(Optional.of(sophieLawyer));
            when(lawyerRepository.save(any(Lawyer.class))).thenAnswer(inv -> inv.getArgument(0));

            lawyerService.updateProfile(100L, request);

            verifyNoInteractions(lawyerEventPublisher);
        }

        @Test
        @DisplayName("lève LawyerProfileNotFoundException si le profil n'existe pas")
        void updateProfile_noProfile_throwsNotFound() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();

            when(lawyerRepository.findByAuthUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lawyerService.updateProfile(999L, request))
                    .isInstanceOf(LawyerProfileNotFoundException.class);

            verify(lawyerRepository, never()).save(any());
            verifyNoInteractions(lawyerEventPublisher);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SEARCH - search (filtres + pagination)
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("sans filtre, appelle search() avec des paramètres null et retourne tous les résultats")
        void search_noFilters_callsSearchWithNulls() {
            Page<Lawyer> page = new PageImpl<>(List.of(sophieLawyer), PageRequest.of(0, 20), 1);
            when(lawyerRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            Page<LawyerSearchResponse> result = lawyerService.search(null, null, null, null, 0, 20);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getBarNumber()).isEqualTo("75001");
            verify(lawyerRepository).search(isNull(), isNull(), isNull(), any(Pageable.class));
            verify(lawyerRepository, never()).searchWithQuery(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("avec une query non vide, appelle searchWithQuery() plutôt que search()")
        void search_withQuery_callsSearchWithQuery() {
            Page<Lawyer> page = new PageImpl<>(List.of(sophieLawyer));
            when(lawyerRepository.searchWithQuery(any(), any(), any(), eq("droit social"), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search(null, null, "droit social", null, 0, 20);

            verify(lawyerRepository).searchWithQuery(isNull(), isNull(), isNull(), eq("droit social"), any(Pageable.class));
            verify(lawyerRepository, never()).search(any(), any(), any(), any());
        }

        @Test
        @DisplayName("normalise le slug de spécialité en minuscules et trim")
        void search_specialtyWithWhitespaceAndCase_normalizesSlug() {
            Page<Lawyer> page = new PageImpl<>(List.of(sophieLawyer));
            when(lawyerRepository.search(eq("droit-du-travail"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search("  DROIT-DU-TRAVAIL  ", null, null, null, 0, 20);

            verify(lawyerRepository).search(eq("droit-du-travail"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("capitalise la ville avant la requête (paris → Paris)")
        void search_cityLowercase_capitalizesBeforeQuery() {
            Page<Lawyer> page = new PageImpl<>(List.of(sophieLawyer));
            when(lawyerRepository.search(isNull(), eq("Paris"), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search(null, "paris", null, null, 0, 20);

            verify(lawyerRepository).search(isNull(), eq("Paris"), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("capitalise la ville même en MAJUSCULES (PARIS → Paris)")
        void search_cityUppercase_capitalizesBeforeQuery() {
            Page<Lawyer> page = new PageImpl<>(List.of(sophieLawyer));
            when(lawyerRepository.search(isNull(), eq("Paris"), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search(null, "PARIS", null, null, 0, 20);

            verify(lawyerRepository).search(isNull(), eq("Paris"), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("traite une chaîne blanche comme null pour city, specialty et query")
        void search_blankStrings_treatedAsNull() {
            Page<Lawyer> page = new PageImpl<>(List.of());
            when(lawyerRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search("   ", "   ", "   ", null, 0, 20);

            verify(lawyerRepository).search(isNull(), isNull(), isNull(), any(Pageable.class));
            verify(lawyerRepository, never()).searchWithQuery(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("transmet maxRate inchangé au repository")
        void search_withMaxRate_passesThroughUnchanged() {
            Page<Lawyer> page = new PageImpl<>(List.of(sophieLawyer));
            when(lawyerRepository.search(isNull(), isNull(), eq(300), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search(null, null, null, 300, 0, 20);

            verify(lawyerRepository).search(isNull(), isNull(), eq(300), any(Pageable.class));
        }

        @Test
        @DisplayName("limite la taille de page au maximum (50) même si size demandé est plus grand")
        void search_sizeAboveMax_isCappedAt50() {
            Page<Lawyer> page = new PageImpl<>(List.of());
            when(lawyerRepository.search(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search(null, null, null, null, 0, 200);

            verify(lawyerRepository).search(any(), any(), any(),
                    argThat((Pageable p) -> p.getPageSize() == 50));
        }

        @Test
        @DisplayName("respecte le numéro de page demandé")
        void search_pageNumber_isPassedToPageable() {
            Page<Lawyer> page = new PageImpl<>(List.of());
            when(lawyerRepository.search(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            lawyerService.search(null, null, null, null, 2, 20);

            verify(lawyerRepository).search(any(), any(), any(),
                    argThat((Pageable p) -> p.getPageNumber() == 2));
        }

        @Test
        @DisplayName("retourne une page vide si aucun avocat ne correspond")
        void search_noMatches_returnsEmptyPage() {
            Page<Lawyer> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(lawyerRepository.search(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<LawyerSearchResponse> result = lawyerService.search("droit-fiscal", "Marseille", null, null, 0, 20);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("mappe correctement chaque Lawyer en LawyerSearchResponse avec bio tronquée si > 200 caractères")
        void search_longBio_truncatesExcerpt() {
            String longBio = "A".repeat(250);
            Lawyer lawyerWithLongBio = new Lawyer();
            lawyerWithLongBio.setId(20L);
            lawyerWithLongBio.setName("Maître Thomas Leblanc");
            lawyerWithLongBio.setBarNumber("69001");
            lawyerWithLongBio.setBio(longBio);
            lawyerWithLongBio.setSpecialties(List.of());
            lawyerWithLongBio.setAddress(new Address());
            lawyerWithLongBio.setAvailable(true);

            Page<Lawyer> page = new PageImpl<>(List.of(lawyerWithLongBio));
            when(lawyerRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            Page<LawyerSearchResponse> result = lawyerService.search(null, null, null, null, 0, 20);

            String excerpt = result.getContent().get(0).getBioExcerpt();
            assertThat(excerpt).hasSize(203); // 200 caractères + "..."
            assertThat(excerpt).endsWith("...");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SPECIALTIES - getAllSpecialties
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getAllSpecialties")
    class GetAllSpecialties {

        @Test
        @DisplayName("retourne toutes les spécialités mappées en DTO")
        void getAllSpecialties_returnsAllMapped() {
            when(specialtyRepository.findAll())
                    .thenReturn(List.of(specialtyDroitTravail, specialtyDroitPenal));

            List<SpecialtyResponse> result = lawyerService.getAllSpecialties();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SpecialtyResponse::getSlug)
                    .containsExactlyInAnyOrder("droit-du-travail", "droit-penal");
        }

        @Test
        @DisplayName("retourne une liste vide si aucune spécialité en base")
        void getAllSpecialties_emptyRepository_returnsEmptyList() {
            when(specialtyRepository.findAll()).thenReturn(List.of());

            List<SpecialtyResponse> result = lawyerService.getAllSpecialties();

            assertThat(result).isEmpty();
        }
    }
}