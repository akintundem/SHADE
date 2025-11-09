package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.OrganizationRegisterRequest;
import eventplanner.security.auth.dto.res.OrganizationResponse;
import eventplanner.security.auth.dto.req.OrganizationUpdateRequest;
import eventplanner.security.auth.service.OrganizationManagementService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/organizations")
public class OrganizationController {

    private final OrganizationManagementService organizationManagementService;

    public OrganizationController(OrganizationManagementService organizationManagementService) {
        this.organizationManagementService = organizationManagementService;
    }

    @PostMapping("/register")
    @RequiresPermission(RbacPermissions.ORGANIZATION_CREATE)
    public ResponseEntity<OrganizationResponse> registerOrganization(@Valid @RequestBody OrganizationRegisterRequest request) {
        OrganizationResponse response = organizationManagementService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{organizationId}")
    @RequiresPermission(value = RbacPermissions.ORGANIZATION_READ, resources = {"organization_id=#organizationId"})
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID organizationId) {
        OrganizationResponse response = organizationManagementService.getOrganization(organizationId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{organizationId}")
    @RequiresPermission(value = RbacPermissions.ORGANIZATION_UPDATE, resources = {"organization_id=#organizationId"})
    public ResponseEntity<OrganizationResponse> updateOrganization(@PathVariable UUID organizationId,
                                                    @Valid @RequestBody OrganizationUpdateRequest request) {
        OrganizationResponse response = organizationManagementService.updateOrganization(organizationId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @RequiresPermission(RbacPermissions.ORGANIZATION_SEARCH)
    public ResponseEntity<Page<OrganizationResponse>> searchOrganizations(@RequestParam(defaultValue = "") String searchTerm,
                                                          @PageableDefault(size = 10) Pageable pageable) {
        Page<OrganizationResponse> response = organizationManagementService.searchOrganizations(searchTerm, pageable);
        return ResponseEntity.ok(response);
    }
}
