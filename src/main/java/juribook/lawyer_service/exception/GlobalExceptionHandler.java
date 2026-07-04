package juribook.lawyer_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions HTTP pour le lawyer-service.
 *
 * Codes HTTP retournés :
 *   400 → validation des champs (@Valid), avis invalide (déjà existant, réservation introuvable)
 *   403 → accès interdit (mauvais rôle, réservation n'appartenant pas au client, statut ≠ COMPLETED)
 *   404 → profil introuvable
 *   409 → profil déjà existant
 *   500 → erreur inattendue
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 - Validation des champs ───────────────────────────
    // Retourne un objet avec le nom du champ + le message d'erreur
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        return ResponseEntity.badRequest()
                .body(Map.of("errors", errors, "message", "Données invalides"));
    }

    // ── 404 - Profil introuvable ──────────────────────────────
    @ExceptionHandler(LawyerProfileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            LawyerProfileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    // ── 409 - Profil déjà existant ────────────────────────────
    @ExceptionHandler(LawyerProfileAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            LawyerProfileAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    // ── 400 - Règle métier invalide ──────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    // ── 400 - Avis invalide (déjà existant, réservation introuvable)
    @ExceptionHandler(InvalidReviewException.class)
    public ResponseEntity<Map<String, String>> handleInvalidReview(
            InvalidReviewException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    // ── 403 - Client non éligible à laisser cet avis
    @ExceptionHandler(NotEligibleToReviewException.class)
    public ResponseEntity<Map<String, String>> handleNotEligibleToReview(
            NotEligibleToReviewException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }

    // ── 500 - Erreur inattendue ───────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erreur interne du serveur"));
    }
}