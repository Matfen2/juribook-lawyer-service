package juribook.lawyer_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import juribook.lawyer_service.entity.Lawyer;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de réponse pour la liste de recherche.
 *
 * Version allégée de LawyerProfileResponse ne contient pas
 * les champs lourds (bio complète) pour optimiser les performances
 * quand on retourne une page de 20 résultats.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LawyerSearchResponse {

    private Long id;
    private String barNumber;
    // Bio tronquée à 200 caractères dans la liste - le détail complet
    // est disponible via GET /api/lawyers/{id}
    private String bioExcerpt;
    private Integer hourlyRate;
    private Integer yearsExperience;
    private String languages;
    private boolean available;
    private Double averageRating;
    private int reviewCount;
    private AddressResponse address;
    private List<SpecialtyResponse> specialties;

    public static LawyerSearchResponse from(Lawyer lawyer) {
        String bio = lawyer.getBio();
        String excerpt = (bio != null && bio.length() > 200)
                ? bio.substring(0, 200) + "..."
                : bio;

        return LawyerSearchResponse.builder()
                .id(lawyer.getId())
                .barNumber(lawyer.getBarNumber())
                .bioExcerpt(excerpt)
                .hourlyRate(lawyer.getHourlyRate())
                .yearsExperience(lawyer.getYearsExperience())
                .languages(lawyer.getLanguages())
                .available(lawyer.isAvailable())
                .averageRating(lawyer.getAverageRating())
                .reviewCount(lawyer.getReviewCount())
                .address(AddressResponse.from(lawyer.getAddress()))
                .specialties(
                    lawyer.getSpecialties().stream()
                        .map(SpecialtyResponse::from)
                        .toList()
                )
                .build();
    }
}