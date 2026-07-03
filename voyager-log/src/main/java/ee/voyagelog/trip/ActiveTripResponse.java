package ee.voyagelog.trip;

import java.time.Instant;


public record ActiveTripResponse(
        Long id,
        String destination,
        Double destinationLat,
        Double destinationLon,
        TripStatus status,
        int crewCount,
        Instant departedAt,
        Instant etaReturn) {

    public static ActiveTripResponse from(Trip t) {
        return from(t, null, null);
    }

    public static ActiveTripResponse from(Trip t, Double destinationLat, Double destinationLon) {
        return new ActiveTripResponse(
                t.getId(), t.getDestination(), destinationLat, destinationLon, t.getStatus(),
                t.getCrewCount(), t.getDepartedAt(), t.getEtaReturn());
    }
}
