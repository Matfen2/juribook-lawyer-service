package juribook.lawyer_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import juribook.lawyer_service.entity.Lawyer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de réponse complet du profil avocat.
 *
 * Retourné par :
 *   POST /api/lawyers/profile  (création)
 *   GET  /api/lawyers/profile  (consultation du profil personnel)
 *   PUT  /api/lawyers/profile  (mise à jour)
 *   GET  /api/lawyers/{id}     (profil public — Sprint 2.3)
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LawyerProfileResponse {

    private Long id;
    private Long authUserId;
    private String name;
    private String barNumber;
    private String bio;
    private Integer hourlyRate;
    private Integer yearsExperience;
    private String languages;
    private boolean available;
    private Double averageRating;
    private int reviewCount;
    private AddressResponse address;
    private List<SpecialtyResponse> specialties;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method — convertit l'entité Lawyer en DTO de réponse.
     * Utilisé dans LawyerService pour éviter la duplication de mapping.
     */
    public static LawyerProfileResponse from(Lawyer lawyer) {
        return LawyerProfileResponse.builder()
                .id(lawyer.getId())
                .authUserId(lawyer.getAuthUserId())
                .name(lawyer.getName())
                .barNumber(lawyer.getBarNumber())
                .bio(lawyer.getBio())
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
                .createdAt(lawyer.getCreatedAt())
                .updatedAt(lawyer.getUpdatedAt())
                .build();
    }
}
