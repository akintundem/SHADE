package eventplanner.features.currency.service;

import eventplanner.features.currency.entity.Currency;
import eventplanner.features.currency.repository.CurrencyRepository;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing currencies.
 * Handles currency validation, lookup, and formatting.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    /**
     * Get all active currencies
     * Cached for performance
     */
    @Cacheable(value = "currencies", key = "'active'")
    public List<Currency> getAllActiveCurrencies() {
        log.debug("Fetching all active currencies");
        return currencyRepository.findAllActive();
    }

    /**
     * Get all currencies (including inactive)
     */
    public List<Currency> getAllCurrencies() {
        log.debug("Fetching all currencies");
        return currencyRepository.findAll();
    }

    /**
     * Get currency by code (case-insensitive)
     */
    @Cacheable(value = "currencies", key = "#code.toUpperCase()")
    public Optional<Currency> getCurrencyByCode(String code) {
        log.debug("Fetching currency: {}", code);
        return Optional.ofNullable(currencyRepository.findByCodeIgnoreCase(code));
    }

    /**
     * Get currency by code or throw exception
     */
    public Currency getCurrencyByCodeOrThrow(String code) {
        return getCurrencyByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + code));
    }

    /**
     * Validate currency code
     * Returns true if currency exists and is active
     */
    public boolean isValidCurrency(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        return getCurrencyByCode(code)
            .map(Currency::getIsActive)
            .orElse(false);
    }

    /**
     * Validate currency code and throw exception if invalid
     */
    public void validateCurrency(String code) {
        if (!isValidCurrency(code)) {
            throw new BadRequestException("Invalid or inactive currency code: " + code);
        }
    }

    /**
     * Format price with currency symbol
     * Example: formatPrice(99.99, "USD") -> "$99.99"
     */
    public String formatPrice(BigDecimal amount, String currencyCode) {
        Currency currency = getCurrencyByCodeOrThrow(currencyCode);

        BigDecimal rounded = amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        String symbol = currency.getSymbol() != null ? currency.getSymbol() : currencyCode;

        return String.format("%s%s", symbol, rounded.toPlainString());
    }

    /**
     * Format price without symbol (just properly rounded)
     * Example: formatAmount(99.999, "USD") -> "99.99"
     */
    public String formatAmount(BigDecimal amount, String currencyCode) {
        Currency currency = getCurrencyByCodeOrThrow(currencyCode);
        BigDecimal rounded = amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        return rounded.toPlainString();
    }

    /**
     * Round amount according to currency decimal places
     */
    public BigDecimal roundAmount(BigDecimal amount, String currencyCode) {
        Currency currency = getCurrencyByCodeOrThrow(currencyCode);
        return amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    /**
     * Get currency symbol
     */
    public String getCurrencySymbol(String currencyCode) {
        return getCurrencyByCodeOrThrow(currencyCode).getSymbol();
    }

    /**
     * Get decimal places for currency
     */
    public int getDecimalPlaces(String currencyCode) {
        return getCurrencyByCodeOrThrow(currencyCode).getDecimalPlaces();
    }

    /**
     * Check if currency requires decimal places
     * (e.g., JPY doesn't use decimal places)
     */
    public boolean hasDecimalPlaces(String currencyCode) {
        return getDecimalPlaces(currencyCode) > 0;
    }

    /**
     * Create or update currency (admin only)
     */
    @Transactional
    public Currency saveCurrency(Currency currency) {
        validateCurrencyData(currency);

        log.info("Saving currency: {}", currency.getCode());
        return currencyRepository.save(currency);
    }

    /**
     * Activate currency
     */
    @Transactional
    public Currency activateCurrency(String code) {
        log.info("Activating currency: {}", code);

        Currency currency = getCurrencyByCodeOrThrow(code);
        currency.setIsActive(true);
        return currencyRepository.save(currency);
    }

    /**
     * Deactivate currency
     */
    @Transactional
    public Currency deactivateCurrency(String code) {
        log.info("Deactivating currency: {}", code);

        Currency currency = getCurrencyByCodeOrThrow(code);
        currency.setIsActive(false);
        return currencyRepository.save(currency);
    }

    /**
     * Validate currency data before saving
     */
    private void validateCurrencyData(Currency currency) {
        if (currency.getCode() == null || currency.getCode().isBlank()) {
            throw new BadRequestException("Currency code is required");
        }

        if (currency.getCode().length() != 3) {
            throw new BadRequestException("Currency code must be 3 characters (ISO 4217 standard)");
        }

        if (currency.getName() == null || currency.getName().isBlank()) {
            throw new BadRequestException("Currency name is required");
        }

        if (currency.getDecimalPlaces() == null || currency.getDecimalPlaces() < 0) {
            throw new BadRequestException("Decimal places must be >= 0");
        }

        // Normalize code to uppercase
        currency.setCode(currency.getCode().toUpperCase());
    }
}
