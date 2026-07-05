package ee.voyagelog.trip;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/active")
    public List<ActiveTripResponse> active() {
        return tripService.activeTrips().stream()
                .map(ActiveTripResponse::from)
                .toList();
    }
}
