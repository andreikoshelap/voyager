package ee.voyagelog.trip;

import ee.voyagelog.harbour.HarbourRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final HarbourRepository harbours;

    public TripController(TripService tripService, HarbourRepository harbours) {
        this.tripService = tripService;
        this.harbours = harbours;
    }

    @GetMapping("/active")
    public List<ActiveTripResponse> active() {
        return tripService.activeTrips().stream()
                .map(trip -> harbours.findFirstByNameIgnoreCaseContaining(trip.getDestination())
                        .map(harbour -> ActiveTripResponse.from(trip, harbour.getLat(), harbour.getLon()))
                        .orElseGet(() -> ActiveTripResponse.from(trip)))
                .toList();
    }
}
