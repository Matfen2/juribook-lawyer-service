package juribook.lawyer_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * DTO de création du profil avocat.
 *
 * authUserId est extrait du JWT côté serveur.
 * barNumber est lu depuis le JWT (claim "barNumber") si présent,
 * sinon depuis ce champ — permet la compatibilité si l'auth-service
 * ne l'inclut pas dans ses claims.
 */
@Data
public class CreateLawyerProfileRequest {

    // ── Numéro de barreau ────────────────────────────────────
    // Optionnel dans le body : lu en priorité depuis le JWT
    // Requis uniquement si absent du token
    @Pattern(regexp = "^[0-9]{5}$", message = "Le numéro de barreau doit contenir exactement 5 chiffres")
    private String barNumber;

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
    @NotEmpty(message = "Au moins une spécialité est obligatoire")
    @Size(max = 5, message = "Un avocat ne peut pas avoir plus de 5 spécialités")
    private List<Long> specialtyIds;

    // ── Adresse du cabinet ───────────────────────────────────
    @Size(max = 200)
    private String street;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100)
    private String city;

    @Pattern(regexp = "^[0-9]{5}$", message = "Le code postal doit contenir exactement 5 chiffres")
    private String postalCode;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String region;
}