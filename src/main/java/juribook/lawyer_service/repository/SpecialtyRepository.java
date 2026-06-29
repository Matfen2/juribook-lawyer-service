package juribook.lawyer_service.repository;

import juribook.lawyer_service.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour les spécialités juridiques.
 * Table en lecture seule après initialisation par Flyway.
 */
@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    Optional<Specialty> findBySlug(String slug);
    Optional<Specialty> findByName(String name);
    boolean existsBySlug(String slug);
}