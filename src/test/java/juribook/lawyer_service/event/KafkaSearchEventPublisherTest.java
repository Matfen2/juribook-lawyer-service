package juribook.lawyer_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaSearchEventPublisher")
class KafkaSearchEventPublisherTest {

    @Mock
    private ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private KafkaSearchEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaSearchEventPublisher(kafkaTemplateProvider, objectMapper);
    }

    @Test
    @DisplayName("recherche avec spécialité et ville - publie les deux dans le payload")
    void publishSearchPerformed_withSpecialtyAndCity_includesBothInPayload() {
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);

        publisher.publishSearchPerformed("Droit du travail", "Paris", "licenciement");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("search-events"), eq(null), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue())
                .contains("\"eventType\":\"search.performed\"")
                .contains("\"specialty\":\"Droit du travail\"")
                .contains("\"city\":\"Paris\"")
                .contains("\"query\":\"licenciement\"");
    }

    @Test
    @DisplayName("recherche sans filtre - publie quand même, avec specialty/city null")
    void publishSearchPerformed_noFilters_publishesWithNulls() {
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);

        publisher.publishSearchPerformed(null, null, null);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("search-events"), eq(null), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue())
                .contains("\"eventType\":\"search.performed\"")
                .contains("\"specialty\":null")
                .contains("\"city\":null");
    }

    @Test
    @DisplayName("Kafka indisponible - ne plante pas")
    void publishSearchPerformed_kafkaUnavailable_doesNotThrow() {
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(null);

        assertThatCode(() -> publisher.publishSearchPerformed("Droit pénal", "Lyon", null))
                .doesNotThrowAnyException();

        verifyNoInteractions(kafkaTemplate);
    }
}