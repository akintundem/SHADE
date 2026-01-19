package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ticket_type_templates",
    indexes = {
        @Index(name = "idx_ticket_type_templates_created_by", columnList = "created_by")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketTypeTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Template name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private TicketTypeCategory category;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Min(value = 0, message = "Price must be greater than or equal to 0")
    @Column(name = "price_minor")
    private Long priceMinor;

    @Size(max = 3, message = "Currency code must be 3 characters")
    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Min(value = 0, message = "Quantity available must be greater than or equal to 0")
    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;

    @Column(name = "sale_start_date")
    private LocalDateTime saleStartDate;

    @Column(name = "sale_end_date")
    private LocalDateTime saleEndDate;

    @Min(value = 1, message = "Max tickets per person must be at least 1")
    @Column(name = "max_tickets_per_person")
    private Integer maxTicketsPerPerson;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    @Min(value = 0, message = "Early bird price must be greater than or equal to 0")
    @Column(name = "early_bird_price_minor")
    private Long earlyBirdPriceMinor;

    @Column(name = "early_bird_end_date")
    private LocalDateTime earlyBirdEndDate;

    @Min(value = 1, message = "Group discount minimum quantity must be at least 1")
    @Column(name = "group_discount_min_qty")
    private Integer groupDiscountMinQuantity;

    @Min(value = 1, message = "Group discount percent must be at least 1")
    @Column(name = "group_discount_percent_bps")
    private Integer groupDiscountPercentBps;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
