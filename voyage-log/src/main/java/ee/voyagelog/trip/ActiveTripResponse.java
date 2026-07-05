package ee.voyagelog.trip;

import java.time.Instant;

/**
 * UPDATED: exposes markerLat/markerLon/locationConfidence directly — the
 * frontend just plots the point, no client-side harbour lookup needed.
 * destinationHarbourId is non-null only for CONFIRMED trips. Still no
 * personal data (no skipper name, no vessel, no phone).
 */
public record ActiveTripResponse(
        Long id,
        Long departureHarbourId,
        Long destinationHarbourId,
        String destination,
        Double markerLat,
        Double markerLon,
        LocationConfidence locationConfidence,
        TripStatus status,
        int crewCount,
        Instant departedAt,
        Instant etaReturn) {

    public static ActiveTripResponse from(Trip t) {
        return new ActiveTripResponse(
                t.getId(), t.getDepartureHarbourId(), t.getDestinationHarbourId(), t.getDestination(),
                t.getMarkerLat(), t.getMarkerLon(), t.getLocationConfidence(), t.getStatus(),
                t.getCrewCount(), t.getDepartedAt(), t.getEtaReturn());
    }
}
