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
     * GeoJSON FeatureCollection consumed directly by MapLibre/Leaflet.
     * UPDATED: properties now include the V4 registry fields (harbourCode,
     * address, website, amenities) so the frontend can surface them without
     * a second request per harbor.
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
                    props.put("harbourCode", h.getHarbourCode());
                    props.put("nickname", h.getNickname());
                    props.put("address", h.getAddress());
                    props.put("website", h.getWebsite());
                    props.put("email", h.getEmail());
                    props.put("amenities", h.getAmenities());
                    props.put("sourceUrl", h.getSourceUrl());
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
