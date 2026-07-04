package juribook.lawyer_service.exception;

/** 400 - requête invalide : avis déjà existant pour cette réservation, réservation introuvable, incohérence lawyerId. */
public class InvalidReviewException extends RuntimeException {
    public InvalidReviewException(String message) {
        super(message);
    }
}