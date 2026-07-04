package ee.voyagelog.trip;

import java.time.Instant;

/** Input to TripService.startTrip — keeps the growing parameter list readable. */
public record StartTripCommand(
        Long skipperId,
        Long departureHarbourId,
        Long destinationHarbourId,
        String destinationLabel,
        double markerLat,
        double markerLon,
        LocationConfidence locationConfidence,
        int crewCount,
        Instant etaReturn) {
}
