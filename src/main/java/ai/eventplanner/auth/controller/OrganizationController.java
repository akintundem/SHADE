package ai.eventplanner.auth.controller;

import ai.eventplanner.auth.dto.req.OrganizationRegisterRequest;
import ai.eventplanner.auth.dto.res.OrganizationResponse;
import ai.eventplanner.auth.dto.req.OrganizationUpdateRequest;
import ai.eventplanner.auth.service.AuthService;
import ai.eventplanner.auth.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/organizations")
public class OrganizationController {

    private final AuthService authService;

    public OrganizationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public org.springframework.http.ResponseEntity<OrganizationResponse> registerOrganization(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody OrganizationRegisterRequest request) {
        OrganizationResponse response = authService.registerOrganization(principal.getUser(), request);
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse getOrganization(@PathVariable UUID organizationId) {
        return authService.getOrganization(organizationId);
    }

    @PutMapping("/{organizationId}")
    public OrganizationResponse updateOrganization(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable UUID organizationId,
                                                    @Valid @RequestBody OrganizationUpdateRequest request) {
        return authService.updateOrganization(organizationId, principal.getUser(), request);
    }

    @GetMapping("/search")
    public Page<OrganizationResponse> searchOrganizations(@RequestParam(defaultValue = "") String searchTerm,
                                                          @PageableDefault(size = 10) Pageable pageable) {
        return authService.searchOrganizations(searchTerm, pageable);
    }
}
