package juribook.lawyer_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publie sur search-events. Même pattern ObjectProvider<KafkaTemplate>
 * que les autres publishers du projet (KafkaLawyerEventPublisher
 * auth-service, BookingEventPublisherImpl booking-service), tolérant à
 * l'absence de KafkaTemplate quand Kafka est désactivé en dev.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaSearchEventPublisher implements SearchEventPublisher {

    private static final String TOPIC = "search-events";

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void publishSearchPerformed(String specialty, String city, String query) {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        if (kafkaTemplate == null) {
            log.debug("Kafka désactivé - événement search.performed non publié (specialty={}, city={})",
                    specialty, city);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "search.performed");
        payload.put("specialty", specialty);
        payload.put("city", city);
        payload.put("query", query);
        payload.put("occurredAt", LocalDateTime.now().toString());

        try {
            String json = objectMapper.writeValueAsString(payload);
            // Pas de clé métier pertinente (pas d'entité identifiée par la
            // recherche elle-même), null laisse Kafka répartir en round-robin.
            kafkaTemplate.send(TOPIC, null, json);
            log.debug("Événement publié sur {} : search.performed (specialty={}, city={})",
                    TOPIC, specialty, city);
        } catch (Exception e) {
            log.error("Échec de la publication sur {} pour specialty={}, city={}", TOPIC, specialty, city, e);
        }
    }
}