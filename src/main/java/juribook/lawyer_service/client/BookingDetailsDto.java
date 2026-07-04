package juribook.lawyer_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Miroir de BookingHistoryResponse (booking-service), désérialise la
 * réponse de GET /api/bookings/{id}. Utilisé pour vérifier, avant
 * d'accepter un avis, que la réservation appartient bien au client qui
 * le poste, concerne bien l'avocat visé, et est au statut COMPLETED.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingDetailsDto(
    Long id,
    Long clientId,
    Long lawyerId,
    Long timeSlotId,
    String status,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime
) {
}