package ee.voyagelog.alert;

import ee.voyagelog.config.VoyageProperties;
import ee.voyagelog.trip.Trip;
import ee.voyagelog.trip.TripRepository;
import ee.voyagelog.trip.TripStatus;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Ядро safety-логики:
 *   AT_SEA  + (eta_return + grace) < now  -> OVERDUE, пингуем шкипера
 *   OVERDUE + (overdue_at + delay) < now  -> ALERTED, тревога контактам
 */
@Component
public class OverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueScheduler.class);

    private final TripRepository trips;
    private final NotificationPort notifier;
    private final VoyageProperties props;

    public OverdueScheduler(TripRepository trips, NotificationPort notifier, VoyageProperties props) {
        this.trips = trips;
        this.notifier = notifier;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "PT1M")
    @SchedulerLock(name = "overdueCheck", lockAtMostFor = "PT50S")
    @Transactional
    public void check() {
        Instant now = Instant.now();

        for (Trip trip : trips.findByStatusAndEtaReturnBefore(TripStatus.AT_SEA, now.minus(props.gracePeriod()))) {
            trip.markOverdue(now);
            notifier.pingSkipperOverdue(trip);
            log.info("Trip {} -> OVERDUE", trip.getId());
        }

        for (Trip trip : trips.findByStatusAndOverdueAtBefore(TripStatus.OVERDUE, now.minus(props.alertDelay()))) {
            trip.markAlerted(now);
            notifier.alertEmergencyContacts(trip);
            log.info("Trip {} -> ALERTED", trip.getId());
        }
    }
}
