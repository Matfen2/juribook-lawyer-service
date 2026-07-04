package juribook.lawyer_service.client;

import java.util.Optional;

/**
 * Abstraction de l'appel inter-services vers le booking-service, pour
 * vérifier qu'une réservation donnée est bien éligible à un avis (appartient
 * au client qui le poste et est au statut COMPLETED).
 * Premier appel de lawyer-service vers booking-service (jusqu'ici,
 * seul le sens inverse existait : booking-service → lawyer-service).
 */
public interface BookingServiceClient {

    Optional<BookingDetailsDto> getBookingDetails(Long bookingId);
}