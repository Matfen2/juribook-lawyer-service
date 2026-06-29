package juribook.lawyer_service.exception;

/**
 * Levée quand un avocat tente de créer un second profil.
 * Transformée en 409 par GlobalExceptionHandler.
 */
public class LawyerProfileAlreadyExistsException extends RuntimeException {
    public LawyerProfileAlreadyExistsException(String message) {
        super(message);
    }
}