package juribook.lawyer_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@Slf4j
public class RestClientBookingServiceClient implements BookingServiceClient {

    private final RestClient restClient;

    public RestClientBookingServiceClient(@Value("${booking-service.base-url}") String bookingServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceBaseUrl)
                .build();
    }

    @Override
    public Optional<BookingDetailsDto> getBookingDetails(Long bookingId) {
        try {
            BookingDetailsDto details = restClient.get()
                    .uri("/api/bookings/{bookingId}", bookingId)
                    .retrieve()
                    .body(BookingDetailsDto.class);

            return Optional.ofNullable(details);
        } catch (RestClientException e) {
            log.error("Échec de l'appel au booking-service pour récupérer le détail de la réservation (bookingId={})",
                    bookingId, e);
            return Optional.empty();
        }
    }
}