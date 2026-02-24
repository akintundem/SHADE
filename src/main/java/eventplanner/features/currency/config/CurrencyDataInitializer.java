package eventplanner.features.currency.config;

import eventplanner.features.currency.entity.Currency;
import eventplanner.features.currency.repository.CurrencyRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the currencies reference table on startup if it is empty.
 * Mirrors the pattern used by LocationDataInitializer.
 */
@Component
public class CurrencyDataInitializer {

    private final CurrencyRepository currencyRepository;

    public CurrencyDataInitializer(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @PostConstruct
    @Transactional
    public void initialize() {
        try {
            if (currencyRepository.count() > 0) {
                return; // Already seeded — nothing to do
            }

            List<Currency> currencies = List.of(
                currency("USD", "US Dollar",             "$",   2),
                currency("EUR", "Euro",                  "€",   2),
                currency("GBP", "British Pound",         "£",   2),
                currency("JPY", "Japanese Yen",          "¥",   0),
                currency("CAD", "Canadian Dollar",       "CA$", 2),
                currency("AUD", "Australian Dollar",     "A$",  2),
                currency("NGN", "Nigerian Naira",        "₦",   2),
                currency("ZAR", "South African Rand",    "R",   2),
                currency("INR", "Indian Rupee",          "₹",   2),
                currency("CNY", "Chinese Yuan",          "¥",   2),
                currency("BRL", "Brazilian Real",        "R$",  2),
                currency("MXN", "Mexican Peso",          "$",   2),
                currency("CHF", "Swiss Franc",           "CHF", 2),
                currency("SEK", "Swedish Krona",         "kr",  2),
                currency("NZD", "New Zealand Dollar",    "NZ$", 2),
                currency("GHS", "Ghanaian Cedi",         "₵",   2),
                currency("KES", "Kenyan Shilling",       "KSh", 2),
                currency("EGP", "Egyptian Pound",        "E£",  2),
                currency("AED", "UAE Dirham",            "د.إ", 2),
                currency("SGD", "Singapore Dollar",      "S$",  2)
            );

            currencyRepository.saveAll(currencies);
        } catch (Exception e) {
            // Non-fatal — currencies can be added manually if this fails
            org.slf4j.LoggerFactory.getLogger(CurrencyDataInitializer.class)
                .warn("Currency data initialization failed: {}", e.getMessage());
        }
    }

    private Currency currency(String code, String name, String symbol, int decimalPlaces) {
        Currency c = new Currency();
        c.setCode(code);
        c.setName(name);
        c.setSymbol(symbol);
        c.setDecimalPlaces(decimalPlaces);
        c.setIsActive(true);
        return c;
    }
}
