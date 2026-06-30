package juribook.lawyer_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * DTO de création du profil avocat.
 *
 * Appelé par POST /api/lawyers/profile lors de la première connexion
 * de l'avocat après validation de son compte par l'admin.
 *
 * authUserId est extrait du JWT côté serveur (pas envoyé par le client)
 * pour éviter qu'un avocat crée un profil pour quelqu'un d'autre.
 */
@Data
public class CreateLawyerProfileRequest {

    // ── Identité ───────────────────────────────────────────
    // Nom complet de l'avocat, dénormalisé depuis l'auth-service.
    // ⚠️ Idéalement extrait du claim "name" du JWT côté serveur
    // (comme barNumber) plutôt que fourni dans le body — à vérifier
    // selon le contenu réel du JwtService de l'auth-service.
    // En attendant, fourni explicitement par le client à la création.
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String name;

    // ── Informations professionnelles ────────────────────────

    @NotBlank(message = "La biographie est obligatoire")
    @Size(max = 2000, message = "La biographie ne peut pas dépasser 2000 caractères")
    private String bio;

    @Min(value = 0, message = "Le tarif ne peut pas être négatif")
    @Max(value = 9999, message = "Le tarif ne peut pas dépasser 9999 €/h")
    private Integer hourlyRate;

    @Min(value = 0, message = "L'expérience ne peut pas être négative")
    @Max(value = 60, message = "L'expérience ne peut pas dépasser 60 ans")
    private Integer yearsExperience;

    @Size(max = 200, message = "Les langues ne peuvent pas dépasser 200 caractères")
    private String languages;

    // ── Spécialités ──────────────────────────────────────────
    // Liste des IDs de spécialités parmi les 15 prédéfinies en BDD
    @NotEmpty(message = "Au moins une spécialité est obligatoire")
    @Size(max = 5, message = "Un avocat ne peut pas avoir plus de 5 spécialités")
    private List<Long> specialtyIds;

    // ── Adresse du cabinet ───────────────────────────────────

    @Size(max = 200, message = "La rue ne peut pas dépasser 200 caractères")
    private String street;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
    private String city;

    @Pattern(regexp = "^[0-9]{5}$", message = "Le code postal doit contenir exactement 5 chiffres")
    private String postalCode;

    @Size(max = 100, message = "Le département ne peut pas dépasser 100 caractères")
    private String department;

    @Size(max = 100, message = "La région ne peut pas dépasser 100 caractères")
    private String region;
}
