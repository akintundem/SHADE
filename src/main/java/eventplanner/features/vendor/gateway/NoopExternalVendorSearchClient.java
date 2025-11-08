package eventplanner.features.vendor.gateway;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.features.vendor.gateway.model.ExternalVendorSearchResult;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Default no-op implementation that can be replaced with a Google Places-backed client.
 */
@Component
@Primary
public class NoopExternalVendorSearchClient implements ExternalVendorSearchClient {

    @Override
    public List<ExternalVendorSearchResult> search(String query, OrganizationType type, String location, int limit) {
        return Collections.emptyList();
    }
}

