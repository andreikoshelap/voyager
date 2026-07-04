package ee.voyagelog.trip;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** UPDATED: startTrip now takes a StartTripCommand — see that record for field docs. */
@Service
public class TripService {

    static final List<TripStatus> ACTIVE_STATUSES =
            List.of(TripStatus.AT_SEA, TripStatus.OVERDUE, TripStatus.ALERTED);

    private final TripRepository trips;

    public TripService(TripRepository trips) {
        this.trips = trips;
    }

    @Transactional
    public Trip startTrip(StartTripCommand cmd) {
        trips.findFirstBySkipperIdAndStatusIn(cmd.skipperId(), ACTIVE_STATUSES).ifPresent(active -> {
            throw new IllegalStateException(
                    "Уже есть активный рейс: " + active.getDestination() + ". Сначала /back.");
        });
        Trip trip = new Trip(cmd.skipperId(), cmd.departureHarbourId(), cmd.destinationHarbourId(),
                cmd.destinationLabel(), cmd.markerLat(), cmd.markerLon(), cmd.locationConfidence(),
                cmd.crewCount(), cmd.etaReturn());
        return trips.save(trip);
    }

    @Transactional
    public Optional<Trip> checkIn(Long skipperId) {
        return trips.findFirstBySkipperIdAndStatusIn(skipperId, ACTIVE_STATUSES)
                .map(trip -> {
                    trip.complete();
                    return trip;
                });
    }

    @Transactional(readOnly = true)
    public Optional<Trip> activeTrip(Long skipperId) {
        return trips.findFirstBySkipperIdAndStatusIn(skipperId, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public List<Trip> activeTrips() {
        return trips.findByStatusIn(ACTIVE_STATUSES);
    }
}
