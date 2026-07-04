package ee.voyagelog.trip;

/**
 * Minimal great-circle math for placing an "approximate area" marker near a
 * harbour when no destination could be resolved. Spherical-earth
 * approximation — accurate enough at the ~1 nautical mile scale we use it
 * for, not meant for actual navigation.
 */
public final class GeoUtil {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private GeoUtil() {
    }

    public record LatLon(double lat, double lon) {
    }

    public static LatLon destinationPoint(double lat, double lon, double bearingDeg, double distanceMeters) {
        double phi1 = Math.toRadians(lat);
        double lambda1 = Math.toRadians(lon);
        double theta = Math.toRadians(bearingDeg);
        double delta = distanceMeters / EARTH_RADIUS_M;

        double phi2 = Math.asin(Math.sin(phi1) * Math.cos(delta) + Math.cos(phi1) * Math.sin(delta) * Math.cos(theta));
        double lambda2 = lambda1 + Math.atan2(
                Math.sin(theta) * Math.sin(delta) * Math.cos(phi1),
                Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2));

        return new LatLon(Math.toDegrees(phi2), Math.toDegrees(lambda2));
    }
}
