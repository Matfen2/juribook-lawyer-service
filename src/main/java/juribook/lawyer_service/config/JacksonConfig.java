package juribook.lawyer_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fournit explicitement un bean ObjectMapper (Jackson 2 classique).
 *
 * ⚠️ Ne crée PAS ce fichier si un JacksonConfig (ou équivalent)
 * existe déjà dans lawyer-service, vérifie d'abord. S'il n'existe
 * pas encore, LawyerEventPublisherImpl va en avoir
 * besoin pour sérialiser LawyerEvent en JSON, même fix que
 * notification-service et audit-service.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}