package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.util.GeoUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

/**
 * Entity representing an available city location.
 * Uses PostGIS geometry(Point, 4326) for spatial queries.
 */
@Entity
@Table(
    name = "locations",
    indexes = {
        @Index(name = "idx_locations_city", columnList = "city"),
        @Index(name = "idx_locations_country", columnList = "country")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Location extends BaseEntity {

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    /**
     * PostGIS Point (SRID 4326) for spatial queries.
     * Automatically synced from latitude/longitude before persist/update.
     */
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @PrePersist
    @PreUpdate
    private void syncLocation() {
        this.location = GeoUtils.createPoint(latitude, longitude);
    }

    @Column(name = "gst_rate_bps")
    private Integer gstRateBps = 0;

    @Column(name = "pst_rate_bps")
    private Integer pstRateBps = 0;

    @Column(name = "hst_rate_bps")
    private Integer hstRateBps = 0;

    @Column(name = "sales_tax_rate_bps")
    private Integer salesTaxRateBps = 0;

    @Column(name = "vat_rate_bps")
    private Integer vatRateBps = 0;

    @Column(name = "tax_effective_year")
    private Integer taxEffectiveYear;

    public String getDisplayName() {
        return String.format("%s, %s, %s", city, state, country);
    }
}
