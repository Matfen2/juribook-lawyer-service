package juribook.lawyer_service.exception;

/**
 * Levée quand un profil avocat n'existe pas en BDD.
 * Transformée en 404 par GlobalExceptionHandler.
 */
public class LawyerProfileNotFoundException extends RuntimeException {
    public LawyerProfileNotFoundException(String message) {
        super(message);
    }
}