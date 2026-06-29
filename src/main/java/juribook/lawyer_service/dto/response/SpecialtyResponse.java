package juribook.lawyer_service.dto.response;

import juribook.lawyer_service.entity.Specialty;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse pour une spécialité.
 * Retourné dans LawyerProfileResponse et dans GET /api/specialties.
 */
@Data
@Builder
public class SpecialtyResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;

    public static SpecialtyResponse from(Specialty specialty) {
        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .slug(specialty.getSlug())
                .description(specialty.getDescription())
                .build();
    }
}