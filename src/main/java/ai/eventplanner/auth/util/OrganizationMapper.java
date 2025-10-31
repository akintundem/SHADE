package ai.eventplanner.auth.util;

import ai.eventplanner.auth.dto.req.OrganizationAddressRequest;
import ai.eventplanner.auth.entity.OrganizationAddress;

/**
 * Utility functions for transforming organization-related request DTOs to entities.
 */
public final class OrganizationMapper {

    private OrganizationMapper() {
    }

    public static OrganizationAddress toAddress(OrganizationAddressRequest request) {
        if (request == null) {
            return null;
        }
        OrganizationAddress address = new OrganizationAddress();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        return address;
    }
}
