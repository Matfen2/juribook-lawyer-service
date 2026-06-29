package juribook.lawyer_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import juribook.lawyer_service.dto.request.CreateLawyerProfileRequest;
import juribook.lawyer_service.dto.request.UpdateLawyerProfileRequest;
import juribook.lawyer_service.dto.response.LawyerProfileResponse;
import juribook.lawyer_service.service.LawyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


/**
 * Controller REST pour les profils et la recherche d'avocats.
 *
 * Routes LAWYER uniquement :
 *   POST /api/lawyers/profile → créer son profil
 *   GET  /api/lawyers/profile → consulter son profil
 *   PUT  /api/lawyers/profile → modifier son profil
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Avocats", description = "Recherche et gestion des profils avocats")
public class LawyerController {

    private final LawyerService lawyerService;

    // ── POST /api/lawyers/profile - Créer son profil ─────────
    @PostMapping("/api/lawyers/profile")
    @Operation(
        summary = "Créer son profil avocat",
        description = "Accessible uniquement aux avocats (LAWYER). Un seul profil par avocat."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profil créé"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
        @ApiResponse(responseCode = "409", description = "Profil déjà existant")
    })
    public ResponseEntity<LawyerProfileResponse> createProfile(
            @Valid @RequestBody CreateLawyerProfileRequest request,
            Authentication authentication) {

        Long authUserId  = (Long) authentication.getPrincipal();
        String barNumber = authentication.getCredentials() != null
                ? (String) authentication.getCredentials()
                : request.getBarNumber();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lawyerService.createProfile(authUserId, barNumber, request));
    }

    // ── GET /api/lawyers/profile - Mon profil ────────────────
    @GetMapping("/api/lawyers/profile")
    @Operation(summary = "Consulter son propre profil avocat")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil retourné"),
        @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
        @ApiResponse(responseCode = "404", description = "Profil introuvable")
    })
    public ResponseEntity<LawyerProfileResponse> getMyProfile(Authentication authentication) {
        Long authUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lawyerService.getMyProfile(authUserId));
    }

    // ── PUT /api/lawyers/profile - Modifier son profil ───────
    @PutMapping("/api/lawyers/profile")
    @Operation(
        summary = "Modifier son profil avocat",
        description = "Patch partiel — seuls les champs non-null sont mis à jour."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
        @ApiResponse(responseCode = "404", description = "Profil introuvable")
    })
    public ResponseEntity<LawyerProfileResponse> updateProfile(
            @Valid @RequestBody UpdateLawyerProfileRequest request,
            Authentication authentication) {

        Long authUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lawyerService.updateProfile(authUserId, request));
    }
}
