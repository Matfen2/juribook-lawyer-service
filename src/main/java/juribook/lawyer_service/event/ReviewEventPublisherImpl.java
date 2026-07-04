package juribook.lawyer_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.lawyer_service.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publication de review.created sur Kafka.
 *
 * Même pattern que LawyerEventPublisherImpl et les
 * publishers de booking-service : décision à l'exécution via
 * ObjectProvider<KafkaTemplate>, jamais via @ConditionalOnBean.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventPublisherImpl implements ReviewEventPublisher {

    private static final String TOPIC = "review-events";

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void publishReviewCreated(Review review) {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        if (kafkaTemplate == null) {
            log.debug("Kafka désactivé - événement review.created non publié pour reviewId={}", review.getId());
            return;
        }

        ReviewEvent event = new ReviewEvent(
                "review.created",
                review.getId(),
                review.getLawyerId(),
                review.getClientId(),
                review.getBookingId(),
                review.getRating(),
                review.getComment(),
                LocalDateTime.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            // Clé = lawyerId : garantit l'ordre des avis successifs sur
            // un même avocat au sein d'une partition (utile pour un futur
            // calcul incrémental de note moyenne côté analytics).
            kafkaTemplate.send(TOPIC, review.getLawyerId().toString(), payload);
            log.info("Événement Kafka publié : type=review.created, reviewId={}, lawyerId={}, rating={}, topic={}",
                    review.getId(), review.getLawyerId(), review.getRating(), TOPIC);
        } catch (JsonProcessingException e) {
            log.error("Échec de sérialisation de l'événement review.created pour reviewId={}",
                    review.getId(), e);
        }
    }
}