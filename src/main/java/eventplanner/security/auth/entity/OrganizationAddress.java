package eventplanner.security.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class OrganizationAddress {

    @Column(name = "address_street", length = 120)
    private String street;

    @Column(name = "address_city", length = 80)
    private String city;

    @Column(name = "address_state", length = 80)
    private String state;

    @Column(name = "address_zip", length = 20)
    private String zipCode;

    @Column(name = "address_country", length = 80)
    private String country;
}
