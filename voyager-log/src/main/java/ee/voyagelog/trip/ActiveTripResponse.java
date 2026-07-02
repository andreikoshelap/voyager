package ee.voyagelog.trip;

import java.time.Instant;


public record ActiveTripResponse(
        Long id,
        String destination,
        TripStatus status,
        int crewCount,
        Instant departedAt,
        Instant etaReturn) {

    public static ActiveTripResponse from(Trip t) {
        return new ActiveTripResponse(
                t.getId(), t.getDestination(), t.getStatus(),
                t.getCrewCount(), t.getDepartedAt(), t.getEtaReturn());
    }
}
