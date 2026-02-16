package eventplanner.common.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;

/**
 * Utility class for PostGIS / JTS geometry operations.
 * Uses SRID 4326 (WGS 84) for all geographic points.
 */
public final class GeoUtils {

    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private GeoUtils() {
    }

    /**
     * Build a JTS Point from latitude / longitude BigDecimals.
     * Returns null when either coordinate is null.
     */
    public static Point createPoint(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        return createPoint(latitude.doubleValue(), longitude.doubleValue());
    }

    /**
     * Build a JTS Point from latitude / longitude doubles.
     * Note: JTS Coordinate order is (x=longitude, y=latitude).
     */
    public static Point createPoint(double latitude, double longitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);
        return point;
    }

    /**
     * Extract latitude (Y) from a JTS Point.
     */
    public static BigDecimal getLatitude(Point point) {
        return point != null ? BigDecimal.valueOf(point.getY()) : null;
    }

    /**
     * Extract longitude (X) from a JTS Point.
     */
    public static BigDecimal getLongitude(Point point) {
        return point != null ? BigDecimal.valueOf(point.getX()) : null;
    }

    /**
     * Convert kilometres to metres.
     */
    public static double kmToMeters(BigDecimal km) {
        return km != null ? km.doubleValue() * 1_000.0 : 10_000.0;
    }

    /**
     * Convert kilometres to metres.
     */
    public static double kmToMeters(double km) {
        return km * 1_000.0;
    }
}
