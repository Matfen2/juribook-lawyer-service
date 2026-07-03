package juribook.lawyer_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.lawyer_service.entity.Lawyer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publication de lawyer.status-changed sur Kafka.
 *
 * Décision à l'exécution via ObjectProvider<KafkaTemplate>, pas via
 * @ConditionalOnBean - piège d'ordre déjà rencontré et corrigé côté
 * booking-service au Sprint 5.2 : un @Component classique est scanné
 * avant la plupart des autoconfigurations Spring Boot (dont
 * KafkaAutoConfiguration), donc @ConditionalOnBean(KafkaTemplate.class)
 * échouerait systématiquement même avec Kafka pleinement actif.
 * ObjectProvider est résolu à chaque appel, une fois le contexte
 * Spring entièrement chargé.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LawyerEventPublisherImpl implements LawyerEventPublisher {

    private static final String TOPIC = "lawyer-events";

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void publishStatusChanged(Lawyer lawyer) {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        if (kafkaTemplate == null) {
            log.debug("Kafka désactivé - événement lawyer.status-changed non publié pour lawyerId={}",
                    lawyer.getId());
            return;
        }

        LawyerEvent event = new LawyerEvent(
                "lawyer.status-changed",
                lawyer.getId(),
                lawyer.isAvailable(),
                LocalDateTime.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            // Clé = lawyerId : garantit l'ordre des événements successifs
            // concernant un même avocat au sein d'une partition.
            kafkaTemplate.send(TOPIC, lawyer.getId().toString(), payload);
            log.info("Événement Kafka publié : type=lawyer.status-changed, lawyerId={}, available={}, topic={}",
                    lawyer.getId(), lawyer.isAvailable(), TOPIC);
        } catch (JsonProcessingException e) {
            log.error("Échec de sérialisation de l'événement lawyer.status-changed pour lawyerId={}",
                    lawyer.getId(), e);
        }
    }
}