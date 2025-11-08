package eventplanner.security.auth.dto.req;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.common.domain.enums.VendorProgramStatus;
import eventplanner.common.domain.enums.VendorTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationRegisterRequest {

    @NotBlank(message = "Organization name is required")
    @Size(max = 160)
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Organization type is required")
    private OrganizationType type;

    @Size(max = 200)
    private String website;

    @NotBlank(message = "Phone number is required")
    @Size(max = 40)
    private String phoneNumber;

    @Valid
    @NotNull(message = "Address is required")
    private OrganizationAddressRequest address;

    @Email(message = "Contact email must be valid")
    @NotBlank(message = "Contact email is required")
    private String contactEmail;

    @Size(max = 40)
    private String taxId;

    @Size(max = 60)
    private String registrationNumber;

    @Size(max = 120)
    private String googlePlaceId;

    @Size(max = 4000)
    private String googlePlaceData;

    private Boolean platformVendor;

    private VendorTier vendorTier;

    private VendorProgramStatus vendorStatus;

    private Double rating;

    private Integer reviewCount;
}
