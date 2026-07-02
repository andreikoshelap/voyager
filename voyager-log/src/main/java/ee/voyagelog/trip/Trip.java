package ee.voyagelog.trip;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long skipperId;
    private Long vesselId;
    private Long departureHarbourId;
    private String destination;
    private int crewCount;
    private Instant departedAt;
    private Instant etaReturn;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private Instant overdueAt;
    private Instant alertedAt;

    @Version
    private long version;

    protected Trip() {
    }

    public Trip(Long skipperId, String destination, int crewCount, Instant etaReturn) {
        this.skipperId = skipperId;
        this.destination = destination;
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
            throw new IllegalStateException("Только AT_SEA может стать OVERDUE, сейчас: " + status);
        }
        this.status = TripStatus.OVERDUE;
        this.overdueAt = now;
    }

    public void markAlerted(Instant now) {
        if (status != TripStatus.OVERDUE) {
            throw new IllegalStateException("Только OVERDUE может стать ALERTED, сейчас: " + status);
        }
        this.status = TripStatus.ALERTED;
        this.alertedAt = now;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new IllegalStateException("Рейс не активен: " + status);
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

    public String getDestination() {
        return destination;
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
