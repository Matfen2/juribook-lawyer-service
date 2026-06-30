package juribook.lawyer_service.controller;

import juribook.lawyer_service.dto.request.CreateLawyerProfileRequest;
import juribook.lawyer_service.dto.request.UpdateLawyerProfileRequest;
import juribook.lawyer_service.dto.response.LawyerProfileResponse;
import juribook.lawyer_service.dto.response.LawyerSearchResponse;
import juribook.lawyer_service.dto.response.SpecialtyResponse;
import juribook.lawyer_service.service.LawyerService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de LawyerController.
 *
 * LawyerService est mocké — on teste uniquement le câblage HTTP :
 * codes de statut, délégation au service, extraction du principal/credentials
 * depuis l'objet Authentication (userId et barNumber issus du JWT).
 *
 * Pas de contexte Spring (pas de @WebMvcTest) pour rester cohérent avec
 * le smoke test sans @SpringBootTest et éviter toute dépendance PostgreSQL/Kafka en CI.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LawyerController")
class LawyerControllerTest {

    @Mock
    private LawyerService lawyerService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LawyerController lawyerController;

    private SpecialtyResponse specialtyResponse;
    private LawyerProfileResponse profileResponse;

    private LawyerProfileResponse buildProfileResponse() {
        return LawyerProfileResponse.builder()
                .id(10L)
                .authUserId(100L)
                .name("Maître Sophie Martin")
                .barNumber("75001")
                .bio("Avocate en droit du travail.")
                .hourlyRate(220)
                .available(true)
                .specialties(List.of())
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  GET /api/lawyers — recherche
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/lawyers — search")
    class SearchEndpoint {

        @Test
        @DisplayName("retourne 200 avec la page de résultats du service")
        void search_delegatesToServiceAndReturns200() {
            LawyerSearchResponse result = LawyerSearchResponse.builder()
                    .id(10L).barNumber("75001").available(true).specialties(List.of()).build();
            Page<LawyerSearchResponse> page = new PageImpl<>(List.of(result), PageRequest.of(0, 20), 1);

            when(lawyerService.search("droit-du-travail", "Paris", "litige", 300, 0, 20))
                    .thenReturn(page);

            ResponseEntity<Page<LawyerSearchResponse>> response =
                    lawyerController.search("droit-du-travail", "Paris", "litige", 300, 0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            verify(lawyerService).search("droit-du-travail", "Paris", "litige", 300, 0, 20);
        }

        @Test
        @DisplayName("transmet les valeurs par défaut page=0 et size=20 quand non fournies explicitement")
        void search_defaultPagination_passedThrough() {
            Page<LawyerSearchResponse> emptyPage = new PageImpl<>(List.of());
            when(lawyerService.search(null, null, null, null, 0, 20)).thenReturn(emptyPage);

            ResponseEntity<Page<LawyerSearchResponse>> response =
                    lawyerController.search(null, null, null, null, 0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(lawyerService).search(null, null, null, null, 0, 20);
        }

        @Test
        @DisplayName("retourne 200 avec une page vide quand aucun résultat")
        void search_noResults_returns200WithEmptyPage() {
            when(lawyerService.search(any(), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of()));

            ResponseEntity<Page<LawyerSearchResponse>> response =
                    lawyerController.search("droit-fiscal", "Nulle-Part", null, null, 0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  POST /api/lawyers/profile — créer
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/lawyers/profile — createProfile")
    class CreateProfileEndpoint {

        @Test
        @DisplayName("retourne 201 — barNumber vient exclusivement des credentials du JWT")
        void createProfile_barNumberAlwaysFromCredentials() {
            CreateLawyerProfileRequest request = new CreateLawyerProfileRequest();
            request.setName("Maître Sophie Martin");
            // barNumber n'existe plus dans le body depuis le Sprint 2.2 —
            // il provient exclusivement du claim JWT "barNumber"

            when(authentication.getPrincipal()).thenReturn(100L);
            when(authentication.getCredentials()).thenReturn("75001"); // valeur du JWT
            when(lawyerService.createProfile(100L, "75001", "Maître Sophie Martin", request))
                    .thenReturn(buildProfileResponse());

            ResponseEntity<LawyerProfileResponse> response =
                    lawyerController.createProfile(request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getBarNumber()).isEqualTo("75001");
            assertThat(response.getBody().getName()).isEqualTo("Maître Sophie Martin");
            verify(lawyerService).createProfile(100L, "75001", "Maître Sophie Martin", request);
        }

        @Test
        @DisplayName("barNumber est null si les credentials JWT sont absents — pas de repli sur le body")
        void createProfile_noCredentials_barNumberIsNull() {
            CreateLawyerProfileRequest request = new CreateLawyerProfileRequest();
            request.setName("Maître Thomas Leblanc");

            when(authentication.getPrincipal()).thenReturn(100L);
            when(authentication.getCredentials()).thenReturn(null);
            when(lawyerService.createProfile(100L, null, "Maître Thomas Leblanc", request))
                    .thenReturn(buildProfileResponse());

            ResponseEntity<LawyerProfileResponse> response =
                    lawyerController.createProfile(request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(lawyerService).createProfile(100L, null, "Maître Thomas Leblanc", request);
        }

        @Test
        @DisplayName("name provient toujours du body de la requête")
        void createProfile_nameAlwaysFromRequestBody() {
            CreateLawyerProfileRequest request = new CreateLawyerProfileRequest();
            request.setName("Maître Jean Dupont");

            when(authentication.getPrincipal()).thenReturn(100L);
            when(authentication.getCredentials()).thenReturn("75001");
            when(lawyerService.createProfile(100L, "75001", "Maître Jean Dupont", request))
                    .thenReturn(buildProfileResponse());

            lawyerController.createProfile(request, authentication);

            verify(lawyerService).createProfile(100L, "75001", "Maître Jean Dupont", request);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET /api/lawyers/profile — mon profil
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/lawyers/profile — getMyProfile")
    class GetMyProfileEndpoint {

        @Test
        @DisplayName("retourne 200 avec le profil de l'utilisateur authentifié")
        void getMyProfile_authenticatedUser_returns200() {
            when(authentication.getPrincipal()).thenReturn(100L);
            when(lawyerService.getMyProfile(100L)).thenReturn(buildProfileResponse());

            ResponseEntity<LawyerProfileResponse> response =
                    lawyerController.getMyProfile(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getAuthUserId()).isEqualTo(100L);
            verify(lawyerService).getMyProfile(100L);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PUT /api/lawyers/profile — modifier
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PUT /api/lawyers/profile — updateProfile")
    class UpdateProfileEndpoint {

        @Test
        @DisplayName("retourne 200 et délègue la mise à jour au service avec l'authUserId du token")
        void updateProfile_validRequest_returns200() {
            UpdateLawyerProfileRequest request = new UpdateLawyerProfileRequest();
            request.setHourlyRate(250);

            when(authentication.getPrincipal()).thenReturn(100L);
            when(lawyerService.updateProfile(100L, request)).thenReturn(buildProfileResponse());

            ResponseEntity<LawyerProfileResponse> response =
                    lawyerController.updateProfile(request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(lawyerService).updateProfile(100L, request);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET /api/lawyers/{id} — profil public
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/lawyers/{id} — getPublicProfile")
    class GetPublicProfileEndpoint {

        @Test
        @DisplayName("retourne 200 avec le profil public, pas besoin d'authentification")
        void getPublicProfile_existingId_returns200() {
            when(lawyerService.getProfileById(10L)).thenReturn(buildProfileResponse());

            ResponseEntity<LawyerProfileResponse> response = lawyerController.getPublicProfile(10L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getId()).isEqualTo(10L);
            verify(lawyerService).getProfileById(10L);
            verifyNoInteractions(authentication); // aucune authentification requise
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GET /api/specialties — liste des spécialités
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/specialties — getAllSpecialties")
    class GetAllSpecialtiesEndpoint {

        @Test
        @DisplayName("retourne 200 avec la liste complète des spécialités")
        void getAllSpecialties_returns200WithFullList() {
            List<SpecialtyResponse> specialties = List.of(
                    SpecialtyResponse.builder().id(1L).name("Droit du travail").slug("droit-du-travail").build(),
                    SpecialtyResponse.builder().id(2L).name("Droit pénal").slug("droit-penal").build()
            );
            when(lawyerService.getAllSpecialties()).thenReturn(specialties);

            ResponseEntity<List<SpecialtyResponse>> response = lawyerController.getAllSpecialties();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("retourne 200 avec une liste vide si aucune spécialité")
        void getAllSpecialties_empty_returns200WithEmptyList() {
            when(lawyerService.getAllSpecialties()).thenReturn(List.of());

            ResponseEntity<List<SpecialtyResponse>> response = lawyerController.getAllSpecialties();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }
}