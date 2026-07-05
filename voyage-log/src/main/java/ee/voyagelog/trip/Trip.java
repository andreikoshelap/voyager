package ee.voyagelog.trip;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * UPDATED: added destinationHarbourId (set only on a CONFIRMED match),
 * markerLat/markerLon (always populated for new trips — either the matched
 * harbour's coordinates or a GeoUtil-computed offshore point), and
 * locationConfidence. Nullable in the DB so pre-existing test rows from
 * before this migration don't need a backfill guess.
 */
@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skipper_id")
    private Long skipperId;
    @Column(name = "vessel_id")
    private Long vesselId;
    @Column(name = "departure_harbour_id")
    private Long departureHarbourId;
    @Column(name = "destination_harbour_id")
    private Long destinationHarbourId;
    private String destination;
    @Column(name = "marker_lat")
    private Double markerLat;
    @Column(name = "marker_lon")
    private Double markerLon;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_confidence")
    private LocationConfidence locationConfidence;

    @Column(name = "crew_count")
    private int crewCount;
    @Column(name = "departed_at")
    private Instant departedAt;
    @Column(name = "eta_return")
    private Instant etaReturn;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    @Column(name = "overdue_at")
    private Instant overdueAt;
    @Column(name = "alerted_at")
    private Instant alertedAt;

    @Version
    private long version;

    protected Trip() {
    }

    public Trip(Long skipperId, Long departureHarbourId, Long destinationHarbourId, String destinationLabel,
                double markerLat, double markerLon, LocationConfidence locationConfidence,
                int crewCount, Instant etaReturn) {
        this.skipperId = skipperId;
        this.departureHarbourId = departureHarbourId;
        this.destinationHarbourId = destinationHarbourId;
        this.destination = destinationLabel;
        this.markerLat = markerLat;
        this.markerLon = markerLon;
        this.locationConfidence = locationConfidence;
        this.crewCount = crewCount;
        this.departedAt = Instant.now();
        this.etaReturn = etaReturn;
        this.status = TripStatus.AT_SEA;
    }

    public void complete() {
        requireActive();
        this.status = TripStatus.COMPLETED;
    }

    public void markOverdue(Instant now) {
        if (status != TripStatus.AT_SEA) {
            throw new IllegalStateException("Only AT_SEA can become OVERDUE, currently: " + status);
        }
        this.status = TripStatus.OVERDUE;
        this.overdueAt = now;
    }

    public void markAlerted(Instant now) {
        if (status != TripStatus.OVERDUE) {
            throw new IllegalStateException("Only OVERDUE can become ALERTED, currently: " + status);
        }
        this.status = TripStatus.ALERTED;
        this.alertedAt = now;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new IllegalStateException("Trip is not active: " + status);
        }
    }

    public boolean isActive() {
        return status == TripStatus.AT_SEA || status == TripStatus.OVERDUE || status == TripStatus.ALERTED;
    }

    public Long getId() {
        return id;
    }

    public Long getSkipperId() {
        return skipperId;
    }

    public Long getDepartureHarbourId() {
        return departureHarbourId;
    }

    public Long getDestinationHarbourId() {
        return destinationHarbourId;
    }

    public String getDestination() {
        return destination;
    }

    public Double getMarkerLat() {
        return markerLat;
    }

    public Double getMarkerLon() {
        return markerLon;
    }

    public LocationConfidence getLocationConfidence() {
        return locationConfidence;
    }

    public int getCrewCount() {
        return crewCount;
    }

    public Instant getDepartedAt() {
        return departedAt;
    }

    public Instant getEtaReturn() {
        return etaReturn;
    }

    public TripStatus getStatus() {
        return status;
    }
}
