package eventplanner.features.vendor.controller;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.features.vendor.dto.VendorSearchResult;
import eventplanner.features.vendor.service.VendorSearchService;
import eventplanner.security.auth.dto.req.OrganizationRegisterRequest;
import eventplanner.security.auth.dto.res.OrganizationResponse;
import eventplanner.security.auth.service.OrganizationManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorSearchService vendorSearchService;
    private final OrganizationManagementService organizationManagementService;

    @GetMapping
    public ResponseEntity<List<VendorSearchResult>> searchVendors(@RequestParam(required = false) String query,
                                                                  @RequestParam(required = false) OrganizationType type,
                                                                  @RequestParam(required = false) String location,
                                                                  @RequestParam(defaultValue = "10")
                                                                  @Min(1) @Max(50) int limit) {
        List<VendorSearchResult> results = vendorSearchService.search(query, type, location, limit);
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> registerVendor(@RequestBody @Valid OrganizationRegisterRequest request) {
        OrganizationResponse response = organizationManagementService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

