package ee.voyagelog.harbour;

import ee.voyagelog.api.GeoJson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/harbours")
public class HarbourController {

    private final HarbourRepository harbours;

    public HarbourController(HarbourRepository harbours) {
        this.harbours = harbours;
    }

    /**
     * GeoJSON FeatureCollection that MapLibre/Leaflet can consume directly.
     * hasHost means the harbour has a bot host, so a berth can be requested.
     */
    @GetMapping
    public GeoJson.FeatureCollection all() {
        var features = harbours.findAll().stream()
                .filter(h -> h.getLat() != null && h.getLon() != null)
                .map(h -> {
                    Map<String, Object> props = new LinkedHashMap<>();
                    props.put("id", h.getId());
                    props.put("name", h.getName());
                    props.put("vhfChannel", h.getVhfChannel());
                    props.put("phone", h.getPhone());
                    props.put("depthM", h.getDepthM());
                    props.put("priceNote", h.getPriceNote());
                    props.put("hasHost", h.getTelegramChatId() != null);
                    return GeoJson.Feature.point(h.getLon(), h.getLat(), props);
                })
                .toList();
        return GeoJson.FeatureCollection.of(features);
    }

    @GetMapping("/{id}")
    public HarbourResponse byId(@PathVariable Long id) {
        return harbours.findById(id)
                .map(HarbourResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Harbour not found: " + id));
    }
}
