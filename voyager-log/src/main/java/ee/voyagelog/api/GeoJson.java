package ee.voyagelog.api;

import java.util.List;
import java.util.Map;

/**
 * Minimal GeoJSON (RFC 7946), just enough for MapLibre:
 * map.addSource('harbours', { type: 'geojson', data: '/api/harbours' })
 */
public final class GeoJson {

    private GeoJson() {
    }

    public record FeatureCollection(String type, List<Feature> features) {

        public static FeatureCollection of(List<Feature> features) {
            return new FeatureCollection("FeatureCollection", features);
        }
    }

    public record Feature(String type, Point geometry, Map<String, Object> properties) {

        public static Feature point(double lon, double lat, Map<String, Object> properties) {
            return new Feature("Feature", new Point("Point", List.of(lon, lat)), properties);
        }
    }

    public record Point(String type, List<Double> coordinates) {
    }
}
