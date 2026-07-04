package juribook.lawyer_service.exception;

/** 403 - réservation n'appartenant pas au client, ou statut différent de COMPLETED. */
public class NotEligibleToReviewException extends RuntimeException {
    public NotEligibleToReviewException(String message) {
        super(message);
    }
}