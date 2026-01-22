package eventplanner.features.currency.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for currency data
 */
@Data
public class CurrencyResponse {

    private String code;
    private String name;
    private String symbol;
    private Integer decimalPlaces;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
