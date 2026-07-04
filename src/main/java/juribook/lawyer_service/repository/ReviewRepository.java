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

    // Tous les avis, y compris masqués — pas encore utilisé (futur
    // panneau de modération admin), mais posé dès le 6.1.
    List<Review> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);

    // Avis publics uniquement, c'est celle-ci que la page
    // détail avocat consulte, jamais les avis masqués par l'admin.
    List<Review> findByLawyerIdAndVisibleTrueOrderByCreatedAtDesc(Long lawyerId);

    // ── Agrégats pour le recalcul de note moyenne ──
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.lawyerId = :lawyerId AND r.visible = true")
    Double findAverageRatingByLawyerId(@Param("lawyerId") Long lawyerId);

    long countByLawyerIdAndVisibleTrue(Long lawyerId);
}