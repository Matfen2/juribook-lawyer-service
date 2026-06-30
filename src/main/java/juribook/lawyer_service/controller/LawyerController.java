package juribook.lawyer_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import juribook.lawyer_service.dto.request.CreateLawyerProfileRequest;
import juribook.lawyer_service.dto.request.UpdateLawyerProfileRequest;
import juribook.lawyer_service.dto.response.LawyerProfileResponse;
import juribook.lawyer_service.dto.response.LawyerSearchResponse;
import juribook.lawyer_service.dto.response.SpecialtyResponse;
import juribook.lawyer_service.service.LawyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour les profils et la recherche d'avocats.
 *
 * Routes publiques (pas de JWT requis) :
 *   GET /api/lawyers          → recherche paginée avec filtres
 *   GET /api/lawyers/{id}     → profil public d'un avocat
 *   GET /api/specialties      → liste des spécialités
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

    // ── GET /api/lawyers — Recherche paginée ──────────────────
    @GetMapping("/api/lawyers")
    @Operation(
        summary = "Rechercher des avocats",
        description = """
            Recherche paginée avec filtres optionnels cumulables.
            Tous les paramètres sont optionnels — sans filtre, retourne tous les avocats disponibles.
            Résultats triés par note décroissante.
            """
    )
    @ApiResponse(responseCode = "200", description = "Page de résultats")
    public ResponseEntity<Page<LawyerSearchResponse>> search(

        @Parameter(description = "Slug de la spécialité (ex: droit-du-travail)")
        @RequestParam(required = false) String specialty,

        @Parameter(description = "Ville du cabinet (ex: Paris)")
        @RequestParam(required = false) String city,

        @Parameter(description = "Recherche textuelle libre dans la bio")
        @RequestParam(required = false) String query,

        @Parameter(description = "Tarif horaire maximum en €")
        @RequestParam(required = false) Integer maxRate,

        @Parameter(description = "Numéro de page (0-based, défaut : 0)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Résultats par page (défaut : 20, max : 50)")
        @RequestParam(defaultValue = "20") int size

    ) {
        return ResponseEntity.ok(
            lawyerService.search(specialty, city, query, maxRate, page, size)
        );
    }

    // ── POST /api/lawyers/profile — Créer son profil ─────────
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

        // barNumber : lu depuis les credentials du JWT (claim "barNumber"),
        // injecté par JwtAuthenticationFilter. Pas de repli sur le body —
        // ce champ n'existe plus dans CreateLawyerProfileRequest depuis le
        // Sprint 2.2, le numéro de barreau provient uniquement du token.
        String barNumber = authentication.getCredentials() != null
                ? (String) authentication.getCredentials()
                : null;

        // name : idéalement extrait d'un futur claim "name" du JWT (à l'image
        // de barNumber) — pour l'instant fourni dans le body par le client,
        // en attendant la confirmation du contenu réel du JwtService de
        // l'auth-service (claim "name" à vérifier côté génération du token).
        String name = request.getName();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lawyerService.createProfile(authUserId, barNumber, name, request));
    }

    // ── GET /api/lawyers/profile — Mon profil ────────────────
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

    // ── PUT /api/lawyers/profile — Modifier son profil ───────
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

    // ── GET /api/lawyers/{id} — Profil public ────────────────
    @GetMapping("/api/lawyers/{id}")
    @Operation(
        summary = "Consulter le profil public d'un avocat",
        description = "Accessible à tous — pas de JWT requis."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil retourné"),
        @ApiResponse(responseCode = "404", description = "Avocat introuvable")
    })
    public ResponseEntity<LawyerProfileResponse> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(lawyerService.getProfileById(id));
    }

    // ── GET /api/specialties — Liste des spécialités ─────────
    @GetMapping("/api/specialties")
    @Operation(
        summary = "Lister toutes les spécialités juridiques",
        description = "Retourne les 15 spécialités prédéfinies avec leur slug. Accessible à tous."
    )
    @ApiResponse(responseCode = "200", description = "Liste des spécialités")
    public ResponseEntity<List<SpecialtyResponse>> getAllSpecialties() {
        return ResponseEntity.ok(lawyerService.getAllSpecialties());
    }
}
