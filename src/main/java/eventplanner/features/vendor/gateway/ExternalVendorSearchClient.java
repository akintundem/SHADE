package eventplanner.features.vendor.gateway;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.features.vendor.gateway.model.ExternalVendorSearchResult;

import java.util.List;

/**
 * Contract for integrating with external vendor discovery providers (e.g., Google Places).
 */
public interface ExternalVendorSearchClient {

    /**
     * Search for vendors externally by query/type/location.
     *
     * @param query     free-form search query (business name, keywords)
     * @param type      organization type/category to filter
     * @param location  optional location (city, region) hint
     * @param limit     maximum number of results to return
     * @return list of external vendor search results
     */
    List<ExternalVendorSearchResult> search(String query, OrganizationType type, String location, int limit);
}


