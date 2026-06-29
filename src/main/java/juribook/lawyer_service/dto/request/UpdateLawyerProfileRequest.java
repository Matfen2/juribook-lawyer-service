package juribook.lawyer_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * DTO de mise à jour du profil avocat.
 *
 * Tous les champs sont optionnels, seuls les champs non-null
 * sont mis à jour (patch partiel).
 * Appelé par PUT /api/lawyers/profile.
 */
@Data
public class UpdateLawyerProfileRequest {

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

    // null = ne pas modifier les spécialités
    @Size(max = 5, message = "Un avocat ne peut pas avoir plus de 5 spécialités")
    private List<Long> specialtyIds;

    // ── Adresse ──────────────────────────────────────────────
    @Size(max = 200)
    private String street;

    @Size(max = 100)
    private String city;

    @Pattern(regexp = "^[0-9]{5}$", message = "Le code postal doit contenir exactement 5 chiffres")
    private String postalCode;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String region;

    // true/false pour activer ou désactiver la disponibilité
    private Boolean available;
}