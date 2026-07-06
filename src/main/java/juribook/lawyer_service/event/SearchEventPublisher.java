package juribook.lawyer_service.event;

/**
 * Publication d'un événement sur search-events à chaque recherche
 * d'avocat, pour analyser les tendances côté audit-service
 * (spécialités et villes les plus recherchées, distinct de
 * specialty_popularity, qui compte les RÉSERVATIONS, pas les
 * recherches : une spécialité très recherchée mais jamais réservée peut
 * révéler un manque d'avocats disponibles).
 *
 * Publié à CHAQUE recherche, y compris sans filtre (specialty/city
 * peuvent être null), le consumer ignore simplement les dimensions
 * nulles plutôt que de ne rien publier du tout.
 */
public interface SearchEventPublisher {

    void publishSearchPerformed(String specialty, String city, String query);
}