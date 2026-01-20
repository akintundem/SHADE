package eventplanner.features.ticket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.ticket.dto.request.ApplyTicketTypeTemplateRequest;
import eventplanner.features.ticket.dto.request.CreateTicketTypeTemplateRequest;
import eventplanner.features.ticket.dto.request.UpdateTicketTypeTemplateRequest;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.entity.TicketTypeTemplate;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.features.ticket.repository.TicketTypeTemplateRepository;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.money.Monetary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketTypeTemplateService {

    private final TicketTypeTemplateRepository templateRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;
    private final AuthorizationService authorizationService;

    public TicketTypeTemplate createTemplate(CreateTicketTypeTemplateRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        UserAccount creator = userAccountRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getId()));

        TicketTypeTemplate template = new TicketTypeTemplate();
        template.setCreatedBy(creator);
        applyTemplateFields(template, request);

        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<TicketTypeTemplate> listTemplates(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return templateRepository.findByCreatedById(principal.getId());
    }

    public TicketTypeTemplate updateTemplate(UUID id, UpdateTicketTypeTemplateRequest request, UserPrincipal principal) {
        if (id == null) {
            throw new IllegalArgumentException("Template ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        TicketTypeTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        ensureOwnership(template, principal);

        applyTemplateFields(template, request);
        return templateRepository.save(template);
    }

    public void deleteTemplate(UUID id, UserPrincipal principal) {
        TicketTypeTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        ensureOwnership(template, principal);
        templateRepository.delete(template);
    }

    public TicketType applyTemplate(UUID templateId, UUID eventId, ApplyTicketTypeTemplateRequest request, UserPrincipal principal) {
        if (templateId == null || eventId == null) {
            throw new IllegalArgumentException("Template ID and event ID are required");
        }
        TicketTypeTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        ensureOwnership(template, principal);

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        if (!authorizationService.canAccessEvent(principal, eventId)) {
            throw new IllegalArgumentException("Access denied to event: " + eventId);
        }

        TicketType ticketType = new TicketType();
        ticketType.setEvent(event);
        String name = request != null && request.getName() != null && !request.getName().isBlank()
            ? request.getName().trim()
            : template.getName();
        ticketType.setName(name);
        ticketType.setCategory(template.getCategory());
        ticketType.setDescription(template.getDescription());
        ticketType.setPriceMinor(template.getPriceMinor());
        ticketType.setCurrency(template.getCurrency());
        ticketType.setQuantityAvailable(template.getQuantityAvailable());
        ticketType.setQuantitySold(0);
        ticketType.setQuantityReserved(0);
        ticketType.setSaleStartDate(template.getSaleStartDate());
        ticketType.setSaleEndDate(template.getSaleEndDate());
        ticketType.setMaxTicketsPerPerson(template.getMaxTicketsPerPerson());
        ticketType.setRequiresApproval(template.getRequiresApproval());
        ticketType.setEarlyBirdPriceMinor(template.getEarlyBirdPriceMinor());
        ticketType.setEarlyBirdEndDate(template.getEarlyBirdEndDate());
        ticketType.setGroupDiscountMinQuantity(template.getGroupDiscountMinQuantity());
        ticketType.setGroupDiscountPercentBps(template.getGroupDiscountPercentBps());
        ticketType.setMetadata(template.getMetadata());
        boolean active = request != null && request.getIsActive() != null ? request.getIsActive() : false;
        ticketType.setIsActive(active);

        return ticketTypeRepository.save(ticketType);
    }

    private void applyTemplateFields(TicketTypeTemplate template, CreateTicketTypeTemplateRequest request) {
        template.setName(request.getName());
        template.setCategory(request.getCategory());
        template.setDescription(request.getDescription());
        template.setPriceMinor(request.getPriceMinor());
        template.setCurrency(validateCurrencyCode(request.getCurrency() != null ? request.getCurrency() : "USD"));
        template.setQuantityAvailable(request.getQuantityAvailable());
        template.setSaleStartDate(request.getSaleStartDate());
        template.setSaleEndDate(request.getSaleEndDate());
        template.setMaxTicketsPerPerson(request.getMaxTicketsPerPerson());
        template.setRequiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : false);
        template.setEarlyBirdPriceMinor(request.getEarlyBirdPriceMinor());
        template.setEarlyBirdEndDate(request.getEarlyBirdEndDate());
        template.setGroupDiscountMinQuantity(request.getGroupDiscountMinQuantity());
        template.setGroupDiscountPercentBps(request.getGroupDiscountPercentBps());
        validateTemplateDates(template.getSaleStartDate(), template.getSaleEndDate());
        validatePricingConfig(template.getEarlyBirdPriceMinor(), template.getEarlyBirdEndDate(),
            template.getGroupDiscountMinQuantity(), template.getGroupDiscountPercentBps());
        if (request.getMetadata() != null) {
            try {
                template.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid metadata format");
            }
        }
    }

    private void applyTemplateFields(TicketTypeTemplate template, UpdateTicketTypeTemplateRequest request) {
        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getPriceMinor() != null) {
            template.setPriceMinor(request.getPriceMinor());
        }
        if (request.getCurrency() != null) {
            template.setCurrency(validateCurrencyCode(request.getCurrency()));
        }
        if (request.getQuantityAvailable() != null) {
            template.setQuantityAvailable(request.getQuantityAvailable());
        }
        if (request.getSaleStartDate() != null) {
            template.setSaleStartDate(request.getSaleStartDate());
        }
        if (request.getSaleEndDate() != null) {
            template.setSaleEndDate(request.getSaleEndDate());
        }
        if (request.getMaxTicketsPerPerson() != null) {
            template.setMaxTicketsPerPerson(request.getMaxTicketsPerPerson());
        }
        if (request.getRequiresApproval() != null) {
            template.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getEarlyBirdPriceMinor() != null) {
            template.setEarlyBirdPriceMinor(request.getEarlyBirdPriceMinor());
        }
        if (request.getEarlyBirdEndDate() != null) {
            template.setEarlyBirdEndDate(request.getEarlyBirdEndDate());
        }
        if (request.getGroupDiscountMinQuantity() != null) {
            template.setGroupDiscountMinQuantity(request.getGroupDiscountMinQuantity());
        }
        if (request.getGroupDiscountPercentBps() != null) {
            template.setGroupDiscountPercentBps(request.getGroupDiscountPercentBps());
        }
        validateTemplateDates(template.getSaleStartDate(), template.getSaleEndDate());
        validatePricingConfig(template.getEarlyBirdPriceMinor(), template.getEarlyBirdEndDate(),
            template.getGroupDiscountMinQuantity(), template.getGroupDiscountPercentBps());
        if (request.getMetadata() != null) {
            try {
                template.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid metadata format");
            }
        }
    }

    private String validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        String normalized = currencyCode.trim().toUpperCase();
        try {
            Monetary.getCurrency(normalized);
            return normalized;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode, e);
        }
    }

    private void validatePricingConfig(Long earlyBirdPriceMinor, LocalDateTime earlyBirdEndDate,
                                       Integer groupDiscountMinQuantity, Integer groupDiscountPercentBps) {
        if (earlyBirdPriceMinor != null && earlyBirdEndDate == null) {
            throw new IllegalArgumentException("Early bird end date is required when early bird price is set");
        }
        if (groupDiscountMinQuantity != null && (groupDiscountPercentBps == null || groupDiscountPercentBps <= 0)) {
            throw new IllegalArgumentException("Group discount percent is required when minimum quantity is set");
        }
    }

    private void validateTemplateDates(LocalDateTime saleStartDate, LocalDateTime saleEndDate) {
        if (saleStartDate != null && saleEndDate != null && saleEndDate.isBefore(saleStartDate)) {
            throw new IllegalArgumentException("Sale end date must be after sale start date");
        }
    }

    private void ensureOwnership(TicketTypeTemplate template, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (template.getCreatedBy() != null && template.getCreatedBy().getId() != null &&
            template.getCreatedBy().getId().equals(principal.getId())) {
            return;
        }
        if (principal.isSystemAdmin()) {
            return;
        }
        throw new IllegalArgumentException("Access denied to template");
    }
}
