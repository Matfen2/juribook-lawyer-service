package juribook.lawyer_service.dto.response;

import juribook.lawyer_service.entity.Address;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse pour l'adresse du cabinet.
 */
@Data
@Builder
public class AddressResponse {

    private String street;
    private String city;
    private String postalCode;
    private String department;
    private String region;

    public static AddressResponse from(Address address) {
        if (address == null) return null;
        return AddressResponse.builder()
                .street(address.getStreet())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .department(address.getDepartment())
                .region(address.getRegion())
                .build();
    }
}