package juribook.lawyer_service.repository;

import juribook.lawyer_service.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingId(Long bookingId);

    // Tous les avis, y compris masqués, pas encore utilisé (futur
    // panneau de modération admin).
    List<Review> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);

    // Avis publics uniquement, c'est celle-ci que la page
    // détail avocat consulte, jamais les avis masqués par l'admin.
    List<Review> findByLawyerIdAndVisibleTrueOrderByCreatedAtDesc(Long lawyerId);

    // ── Agrégats pour le recalcul de note moyenne ──
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.lawyerId = :lawyerId AND r.visible = true")
    Double findAverageRatingByLawyerId(@Param("lawyerId") Long lawyerId);

    long countByLawyerIdAndVisibleTrue(Long lawyerId);

    /**
     * Panneau de modération admin. Pas de vrai mécanisme
     * de "signalement" côté client (hors scope), le tri par note
     * croissante (pire d'abord) sert de proxy objectif pour repérer les
     * avis à examiner en priorité, plutôt qu'un flag "reported" qui
     * n'existe pas.
     *
     * `visible` optionnel (IS NULL OR ...) : null retourne TOUT
     * (visibles et masqués confondus), l'admin doit voir les deux pour
     * pouvoir démasquer aussi bien que masquer.
     */
    @Query("""
        SELECT r FROM Review r
        WHERE (:visible IS NULL OR r.visible = :visible)
        ORDER BY r.rating ASC, r.createdAt DESC
    """)
    Page<Review> findForModeration(@Param("visible") Boolean visible, Pageable pageable);
}