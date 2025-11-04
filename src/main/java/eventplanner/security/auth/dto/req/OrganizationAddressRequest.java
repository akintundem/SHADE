package eventplanner.security.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationAddressRequest {

    @NotBlank(message = "Street is required")
    @Size(max = 120)
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 80)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 80)
    private String state;

    @NotBlank(message = "Zip code is required")
    @Size(max = 20)
    private String zipCode;

    @NotBlank(message = "Country is required")
    @Size(max = 80)
    private String country;
}
