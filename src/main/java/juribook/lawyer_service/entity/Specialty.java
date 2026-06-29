package juribook.lawyer_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spécialité juridique : table de référence.
 *
 * Peuplée par Flyway (V2__insert_specialties.sql) avec les 15 spécialités
 * prédéfinies du barreau français. Les avocats peuvent avoir plusieurs
 * spécialités (relation ManyToMany avec Lawyer).
 *
 * Exemples : Droit du travail, Droit pénal, Droit de la famille, ...
 */
@Entity
@Table(name = "specialties")
@Data
@NoArgsConstructor
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom de la spécialité - unique, immuable après insertion
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Slug URL-friendly - ex: "droit-du-travail" (utilisé dans les filtres de recherche)
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    // Description courte affichée sur le profil avocat
    @Column(length = 255)
    private String description;

    // Avocats ayant cette spécialité (côté inverse de la relation)
    @ManyToMany(mappedBy = "specialties")
    private List<Lawyer> lawyers;

    public Specialty(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description;
    }
}