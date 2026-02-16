package eventplanner.security.auth.config;

import com.opencsv.CSVReader;
import eventplanner.common.util.GeoUtils;
import eventplanner.security.auth.entity.Location;
import eventplanner.security.auth.repository.LocationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Initializes location data from CSV on startup.
 */
@Component
public class LocationDataInitializer {

    private final LocationRepository locationRepository;

    public LocationDataInitializer(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @PostConstruct
    @Transactional
    public void initialize() {
        try {
            ClassPathResource resource = new ClassPathResource("data/available_cities.csv");
            if (!resource.exists()) {
                return;
            }

            try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String[] header = reader.readNext(); // Skip header
                if (header == null) return;

                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < 6) continue;

                    UUID uuid = UUID.fromString(line[0].trim());
                    String city = line[1].trim();
                    String state = line[2].trim();
                    String country = line[3].trim();
                    BigDecimal latitude = new BigDecimal(line[4].trim());
                    BigDecimal longitude = new BigDecimal(line[5].trim());
                    Integer gstBps = line.length > 6 && !line[6].isBlank() ? Integer.parseInt(line[6].trim()) : 0;
                    Integer pstBps = line.length > 7 && !line[7].isBlank() ? Integer.parseInt(line[7].trim()) : 0;
                    Integer hstBps = line.length > 8 && !line[8].isBlank() ? Integer.parseInt(line[8].trim()) : 0;
                    Integer taxYear = line.length > 9 && !line[9].isBlank() ? Integer.parseInt(line[9].trim()) : null;
                    Integer salesTaxBps = line.length > 10 && !line[10].isBlank() ? Integer.parseInt(line[10].trim()) : 0;
                    Integer vatBps = line.length > 11 && !line[11].isBlank() ? Integer.parseInt(line[11].trim()) : 0;

                    Location location = locationRepository.findById(uuid).orElse(new Location());
                    location.setId(uuid);
                    location.setCity(city);
                    location.setState(state);
                    location.setCountry(country);
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    location.setLocation(GeoUtils.createPoint(latitude, longitude));
                    location.setGstRateBps(gstBps);
                    location.setPstRateBps(pstBps);
                    location.setHstRateBps(hstBps);
                    location.setTaxEffectiveYear(taxYear);
                    location.setSalesTaxRateBps(salesTaxBps);
                    location.setVatRateBps(vatBps);
                    locationRepository.save(location);
                }
            }
        } catch (Exception e) {
            // Silently handle errors
        }
    }
}
