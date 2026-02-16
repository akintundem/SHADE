package eventplanner.features.currency.controller;

import eventplanner.features.currency.dto.CurrencyResponse;
import eventplanner.features.currency.entity.Currency;
import eventplanner.features.currency.service.CurrencyService;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for currency operations
 */
@RestController
@RequestMapping("/api/v1/currencies")
@Tag(name = "Currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    @RequiresPermission(RbacPermissions.CURRENCY_READ)
    @Operation(summary = "Get all active currencies", description = "Get list of all active currencies")
    public ResponseEntity<List<CurrencyResponse>> getAllActiveCurrencies() {
        List<Currency> currencies = currencyService.getAllActiveCurrencies();
        List<CurrencyResponse> responses = currencies.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{code}")
    @RequiresPermission(RbacPermissions.CURRENCY_READ)
    @Operation(summary = "Get currency by code", description = "Get currency details by ISO 4217 code")
    public ResponseEntity<CurrencyResponse> getCurrency(@PathVariable @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z]{3}$") String code) {
        return currencyService.getCurrencyByCode(code)
            .map(currency -> ResponseEntity.ok(toResponse(currency)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{code}/validate")
    @RequiresPermission(RbacPermissions.CURRENCY_READ)
    @Operation(summary = "Validate currency code", description = "Check if currency code is valid and active")
    public ResponseEntity<Boolean> validateCurrency(@PathVariable @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z]{3}$") String code) {
        boolean isValid = currencyService.isValidCurrency(code);
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/{code}/symbol")
    @RequiresPermission(RbacPermissions.CURRENCY_READ)
    @Operation(summary = "Get currency symbol", description = "Get the currency symbol for a currency code")
    public ResponseEntity<String> getCurrencySymbol(@PathVariable @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z]{3}$") String code) {
        String symbol = currencyService.getCurrencySymbol(code);
        return ResponseEntity.ok(symbol);
    }

    private CurrencyResponse toResponse(Currency entity) {
        CurrencyResponse response = new CurrencyResponse();
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setSymbol(entity.getSymbol());
        response.setDecimalPlaces(entity.getDecimalPlaces());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
