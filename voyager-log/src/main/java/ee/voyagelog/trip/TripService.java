package ee.voyagelog.trip;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    static final List<TripStatus> ACTIVE_STATUSES =
            List.of(TripStatus.AT_SEA, TripStatus.OVERDUE, TripStatus.ALERTED);

    private final TripRepository trips;

    public TripService(TripRepository trips) {
        this.trips = trips;
    }

    @Transactional
    public Trip startTrip(Long skipperId, String destination, int crewCount, Instant etaReturn) {
        trips.findFirstBySkipperIdAndStatusIn(skipperId, ACTIVE_STATUSES).ifPresent(active -> {
            throw new IllegalStateException(
                    "Уже есть активный рейс на " + active.getDestination() + ". Сначала /back.");
        });
        return trips.save(new Trip(skipperId, destination, crewCount, etaReturn));
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
}
