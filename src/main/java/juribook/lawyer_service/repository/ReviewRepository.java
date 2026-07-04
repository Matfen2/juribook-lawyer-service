package juribook.lawyer_service.repository;

import juribook.lawyer_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingId(Long bookingId);

    // Pas encore utilisé (affichage des avis sur un profil = sprint à
    // venir), mais naturel de le poser dès maintenant vu le modèle.
    List<Review> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);

    // ── Agrégats pour le recalcul de note moyenne ──

    // Null tant qu'aucun avis n'existe pour cet avocat, AVG() sur un
    // ensemble vide retourne NULL en SQL, jamais 0. Cohérent avec le
    // comportement déjà établi de Lawyer.averageRating (null par défaut,
    // pas 0.0, tant qu'aucun avis n'a été laissé).
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.lawyerId = :lawyerId")
    Double findAverageRatingByLawyerId(@Param("lawyerId") Long lawyerId);

    long countByLawyerId(Long lawyerId);
}