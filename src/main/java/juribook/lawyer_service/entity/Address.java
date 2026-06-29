package juribook.lawyer_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adresse du cabinet : entité embarquée (@Embeddable).
 *
 * Stockée dans la table `lawyers` (pas de table séparée).
 * Ce choix évite une jointure inutile car l'adresse n'existe
 * que dans le contexte d'un cabinet — elle n'a pas de sens seule.
 *
 * Colonnes générées : street, city, postal_code, department, region
 */
@Embeddable
@Data
@NoArgsConstructor
public class Address {

    // Rue et numéro - ex : "12 rue de la Paix"
    @Column(name = "street", length = 200)
    private String street;

    // Ville - indexée pour la recherche géographique
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    // Code postal - 5 chiffres, ex : "75001"
    @Column(name = "postal_code", length = 5)
    private String postalCode;

    // Département - ex : "Paris", "Rhône", "Bouches-du-Rhône"
    @Column(name = "department", length = 100)
    private String department;

    // Région administrative - ex : "Île-de-France", "PACA"
    @Column(name = "region", length = 100)
    private String region;

    public Address(String street, String city, String postalCode, String department, String region) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.department = department;
        this.region = region;
    }
}