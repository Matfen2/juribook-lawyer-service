package juribook.lawyer_service.repository;

import juribook.lawyer_service.entity.Lawyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour les profils avocats.
 *
 * Note sur la recherche par ville :
 * On utilise LIKE avec CONCAT plutôt que LOWER() sur city pour éviter
 * l'erreur PostgreSQL "function lower(bytea) does not exist" causée
 * par le mapping Hibernate sur les colonnes @Embeddable.
 * La comparaison insensible à la casse est gérée via ilike (PostgreSQL)
 * ou en normalisant la ville côté Java avant la requête.
 */
@Repository
public interface LawyerRepository extends JpaRepository<Lawyer, Long> {

    // ── Lookups simples ──────────────────────────────────────

    Optional<Lawyer> findByAuthUserId(Long authUserId);
    boolean existsByAuthUserId(Long authUserId);
    boolean existsByBarNumber(String barNumber);

    // ── Recherche paginée - sans query textuelle ──────────────
    @Query("""
        SELECT DISTINCT l FROM Lawyer l
        LEFT JOIN l.specialties s
        WHERE l.available = true
          AND (:specialtySlug IS NULL OR s.slug = :specialtySlug)
          AND (:city IS NULL OR l.address.city = :city)
          AND (:maxRate IS NULL OR l.hourlyRate <= :maxRate)
        ORDER BY l.averageRating DESC NULLS LAST,
                 l.reviewCount DESC
    """)
    Page<Lawyer> search(
        @Param("specialtySlug") String specialtySlug,
        @Param("city")          String city,
        @Param("maxRate")       Integer maxRate,
        Pageable pageable
    );

    // ── Recherche paginée - avec query textuelle ──────────────
    @Query("""
        SELECT DISTINCT l FROM Lawyer l
        LEFT JOIN l.specialties s
        WHERE l.available = true
          AND (:specialtySlug IS NULL OR s.slug = :specialtySlug)
          AND (:city IS NULL OR l.address.city = :city)
          AND (:maxRate IS NULL OR l.hourlyRate <= :maxRate)
          AND (:query IS NULL OR l.bio LIKE CONCAT('%', :query, '%'))
        ORDER BY l.averageRating DESC NULLS LAST,
                 l.reviewCount DESC
    """)
    Page<Lawyer> searchWithQuery(
        @Param("specialtySlug") String specialtySlug,
        @Param("city")          String city,
        @Param("maxRate")       Integer maxRate,
        @Param("query")         String query,
        Pageable pageable
    );
}