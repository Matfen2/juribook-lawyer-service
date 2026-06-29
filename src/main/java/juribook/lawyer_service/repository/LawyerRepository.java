package juribook.lawyer_service.repository;

import juribook.lawyer_service.entity.Lawyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour les profils avocats.
 */
@Repository
public interface LawyerRepository extends JpaRepository<Lawyer, Long> {
    // ── Lookups simples ──────────────────────────────────────
    Optional<Lawyer> findByAuthUserId(Long authUserId);
    boolean existsByAuthUserId(Long authUserId);
    boolean existsByBarNumber(String barNumber);
}
