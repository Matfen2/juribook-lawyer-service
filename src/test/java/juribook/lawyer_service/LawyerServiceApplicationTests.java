package juribook.lawyer_service;

import org.junit.jupiter.api.Test;

/**
 * Smoke test - vérifie que la classe principale existe et compile.
 *
 * On n'utilise PAS @SpringBootTest car le démarrage complet du contexte
 * nécessite PostgreSQL + Kafka qui ne sont pas disponibles en CI.
 *
 * Les tests unitaires sont couverts par LawyerServiceTest, etc.
 * (à venir Sprint 2.4)
 */
class LawyerServiceApplicationTests {

    @Test
    void applicationClassExists() {
        Class<?> appClass = LawyerServiceApplication.class;
        assert appClass != null;
    }
}