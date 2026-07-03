package juribook.lawyer_service.event;

import juribook.lawyer_service.entity.Lawyer;

/**
 * Abstraction de publication des événements liés au statut d'un avocat,
 * sur le topic Kafka lawyer-events.
 */
public interface LawyerEventPublisher {

    void publishStatusChanged(Lawyer lawyer);
}