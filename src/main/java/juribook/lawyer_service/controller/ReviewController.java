package juribook.lawyer_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import juribook.lawyer_service.dto.request.CreateReviewRequest;
import juribook.lawyer_service.dto.response.ReviewResponse;
import juribook.lawyer_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST des avis.
 *
 * Route plate /api/reviews (pas /api/lawyers/{id}/reviews), le
 * lawyerId n'est jamais fourni par le client, il est résolu côté
 * serveur à partir de bookingId (cf. ReviewService), ce qui élimine
 * tout risque d'incohérence entre un lawyerId déclaré et la réalité de
 * la réservation.
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
            désigne précisément la réservation évaluée — l'avocat concerné
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
}