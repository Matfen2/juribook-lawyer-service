package juribook.lawyer_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import juribook.lawyer_service.dto.request.CreateReviewRequest;
import juribook.lawyer_service.dto.response.ReviewResponse;
import juribook.lawyer_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST des avis.
 *
 * Route plate /api/reviews (pas /api/lawyers/{id}/reviews), le
 * lawyerId n'est jamais fourni par le client, il est résolu côté
 * serveur à partir de bookingId (cf. ReviewService), ce qui élimine
 * tout risque d'incohérence entre un lawyerId déclaré et la réalité de
 * la réservation.
 *
 * ⚠️ hideReview/unhideReview existaient déjà dans
 * ReviewService, mais n'étaient reliés à AUCUNE
 * route HTTP jusqu'ici, le README documentait PATCH .../hide|unhide
 * comme s'ils existaient déjà, ce n'était vrai que côté service.
 * Corrigé ici, avec les règles ADMIN correspondantes ajoutées à
 * SecurityConfig.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Avis", description = "Avis client sur un avocat, après rendez-vous honoré")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(
        summary = "Laisser un avis",
        description = """
            Réservé au CLIENT ayant eu un rendez-vous COMPLETED. bookingId
            désigne précisément la réservation évaluée, l'avocat concerné
            est déduit de cette réservation, pas fourni par le client.
            Un seul avis possible par réservation. Publie review.created
            sur Kafka (topic review-events) une fois l'avis enregistré.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Avis créé"),
        @ApiResponse(responseCode = "400", description = "Note hors de 1-5, réservation introuvable, ou déjà évaluée"),
        @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant, réservation n'appartenant pas au client, ou statut différent de COMPLETED")
    })
    public ResponseEntity<ReviewResponse> createReview(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request) {

        Long clientId = (Long) authentication.getPrincipal();
        ReviewResponse response = reviewService.createReview(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Modération admin ────────────────────────
    @GetMapping("/moderation")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Panneau de modération, tous les avis, paginé",
        description = """
            Réservé à l'ADMIN. Trié par note croissante puis date
            décroissante (les avis les moins bien notés en premier,
            proxy objectif en l'absence d'un vrai mécanisme de
            signalement côté client, hors scope de ce sprint). Le
            filtre `visible` est optionnel : omis, retourne les avis
            visibles ET masqués confondus.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page de résultats"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant (ADMIN requis)")
    })
    public ResponseEntity<Page<ReviewResponse>> getReviewsForModeration(
            @Parameter(description = "true = visibles uniquement, false = masqués uniquement, omis = tous")
            @RequestParam(required = false) Boolean visible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(reviewService.getReviewsForModeration(visible, page, size));
    }

    @PatchMapping("/{id}/hide")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Masquer un avis", description = "Réversible (cf. /unhide). Recalcule immédiatement la note moyenne de l'avocat, sans cet avis.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avis masqué"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant (ADMIN requis)"),
        @ApiResponse(responseCode = "404", description = "Avis introuvable")
    })
    public ResponseEntity<ReviewResponse> hideReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.hideReview(id));
    }

    @PatchMapping("/{id}/unhide")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Démasquer un avis", description = "Annule un masquage précédent. Recalcule immédiatement la note moyenne, avis réintégré.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avis démasqué"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant (ADMIN requis)"),
        @ApiResponse(responseCode = "404", description = "Avis introuvable")
    })
    public ResponseEntity<ReviewResponse> unhideReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.unhideReview(id));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Supprimer définitivement un avis",
        description = """
            ⚠️ IRRÉVERSIBLE, contrairement à /hide (réversible via
            /unhide), cette opération retire l'avis de la base
            définitivement. Recalcule immédiatement la note moyenne de
            l'avocat concerné.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Avis supprimé"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant (ADMIN requis)"),
        @ApiResponse(responseCode = "404", description = "Avis introuvable")
    })
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}