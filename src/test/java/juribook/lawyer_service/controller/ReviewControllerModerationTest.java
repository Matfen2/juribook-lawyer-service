package juribook.lawyer_service.controller;

import juribook.lawyer_service.config.SecurityConfig;
import juribook.lawyer_service.dto.response.ReviewResponse;
import juribook.lawyer_service.security.JwtService;
import juribook.lawyer_service.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests ReviewController pour la modération admin,
 * fichier séparé du ReviewControllerTest existant (non fourni en
 * entier). Même pattern de sécurité que le reste du projet :
 * @Import(SecurityConfig.class) + SecurityMockMvcRequestPostProcessors.
 *
 * ⚠️ JwtService supposé être la seule dépendance de JwtAuthenticationFilter
 * dans lawyer-service (pas de UserRepository), à ajuster si un autre
 * @MockitoBean est nécessaire au chargement du contexte.
 */
@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@DisplayName("ReviewController - Modération")
class ReviewControllerModerationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtService jwtService;

    private static RequestPostProcessor admin() {
        return SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN");
    }

    private static RequestPostProcessor client() {
        return SecurityMockMvcRequestPostProcessors.user("client@test.com").roles("CLIENT");
    }

    private ReviewResponse buildResponse(long id, int rating, boolean visible) {
        return new ReviewResponse(id, 4L, 42L, 7L, rating, "commentaire", visible, LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/reviews/moderation")
    class ModerationList {

        @Test
        @DisplayName("✅ 200 - ADMIN, retourne la page du service")
        void getReviewsForModeration_admin_returns200() throws Exception {
            when(reviewService.getReviewsForModeration(isNull(), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of(buildResponse(1L, 1, true))));

            mockMvc.perform(get("/api/reviews/moderation").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].rating").value(1));
        }

        @Test
        @DisplayName("❌ 403 - refusé pour un CLIENT")
        void getReviewsForModeration_client_returns403() throws Exception {
            mockMvc.perform(get("/api/reviews/moderation").with(client()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(reviewService);
        }

        @Test
        @DisplayName("❌ 403 - refusé sans authentification")
        void getReviewsForModeration_noAuth_returns403() throws Exception {
            mockMvc.perform(get("/api/reviews/moderation"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(reviewService);
        }

        @Test
        @DisplayName("transmet le filtre visible")
        void getReviewsForModeration_visibleFilter_passedThrough() throws Exception {
            when(reviewService.getReviewsForModeration(eq(false), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/reviews/moderation").with(admin()).param("visible", "false"))
                    .andExpect(status().isOk());

            verify(reviewService).getReviewsForModeration(false, 0, 20);
        }
    }

    @Nested
    @DisplayName("PATCH /api/reviews/{id}/hide et /unhide")
    class HideUnhide {

        @Test
        @DisplayName("✅ 200 - ADMIN peut masquer")
        void hideReview_admin_returns200() throws Exception {
            when(reviewService.hideReview(1L)).thenReturn(buildResponse(1L, 1, false));

            mockMvc.perform(patch("/api/reviews/1/hide").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visible").value(false));
        }

        @Test
        @DisplayName("❌ 403 - un CLIENT ne peut pas masquer")
        void hideReview_client_returns403() throws Exception {
            mockMvc.perform(patch("/api/reviews/1/hide").with(client()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(reviewService);
        }

        @Test
        @DisplayName("✅ 200 - ADMIN peut démasquer")
        void unhideReview_admin_returns200() throws Exception {
            when(reviewService.unhideReview(1L)).thenReturn(buildResponse(1L, 1, true));

            mockMvc.perform(patch("/api/reviews/1/unhide").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visible").value(true));
        }

        @Test
        @DisplayName("❌ 403 - un CLIENT ne peut pas démasquer")
        void unhideReview_client_returns403() throws Exception {
            mockMvc.perform(patch("/api/reviews/1/unhide").with(client()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(reviewService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/reviews/{id}")
    class Delete {

        @Test
        @DisplayName("✅ 204 - ADMIN peut supprimer définitivement")
        void deleteReview_admin_returns204() throws Exception {
            mockMvc.perform(delete("/api/reviews/1").with(admin()))
                    .andExpect(status().isNoContent());

            verify(reviewService).deleteReview(1L);
        }

        @Test
        @DisplayName("❌ 403 - un CLIENT ne peut pas supprimer")
        void deleteReview_client_returns403() throws Exception {
            mockMvc.perform(delete("/api/reviews/1").with(client()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(reviewService);
        }

        @Test
        @DisplayName("❌ 403 - refusé sans authentification")
        void deleteReview_noAuth_returns403() throws Exception {
            mockMvc.perform(delete("/api/reviews/1"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(reviewService);
        }
    }
}