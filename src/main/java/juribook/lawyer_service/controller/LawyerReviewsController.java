package juribook.lawyer_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import juribook.lawyer_service.dto.response.ReviewPublicResponse;
import juribook.lawyer_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST - consultation publique des avis d'un avocat.
 *
 * Dans un controller séparé de ReviewController (mappé sur /api/reviews)
 * plutôt qu'ajouté dedans : un controller Spring ne peut avoir qu'un
 * seul préfixe de route via @RequestMapping, même raison que
 * LawyerBookingsController côté booking-service.
 */
@RestController
@RequestMapping("/api/lawyers/{lawyerId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Avis", description = "Consultation publique des avis d'un avocat")
public class LawyerReviewsController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(
        summary = "Avis d'un avocat",
        description = """
            Public — pas de token requis. Retourne uniquement les avis
            visibles (jamais ceux masqués par l'admin, Sprint 6.4), triés
            du plus récent au plus ancien. Chaque entrée n'expose que
            note, commentaire et date, pas d'identité du client, pas de
            référence à la réservation.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des avis visibles (peut être vide)")
    })
    public ResponseEntity<List<ReviewPublicResponse>> getReviews(@PathVariable Long lawyerId) {
        return ResponseEntity.ok(reviewService.getVisibleReviews(lawyerId));
    }
}