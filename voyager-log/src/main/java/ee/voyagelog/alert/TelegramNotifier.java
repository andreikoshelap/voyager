package ee.voyagelog.alert;

import ee.voyagelog.skipper.EmergencyContact;
import ee.voyagelog.skipper.EmergencyContactRepository;
import ee.voyagelog.skipper.Skipper;
import ee.voyagelog.skipper.SkipperRepository;
import ee.voyagelog.telegram.TelegramClient;
import ee.voyagelog.trip.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class TelegramNotifier implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.of("Europe/Tallinn"));

    private final TelegramClient telegram;
    private final SkipperRepository skippers;
    private final EmergencyContactRepository contacts;

    public TelegramNotifier(TelegramClient telegram,
                            SkipperRepository skippers,
                            EmergencyContactRepository contacts) {
        this.telegram = telegram;
        this.skippers = skippers;
        this.contacts = contacts;
    }

    @Override
    public void pingSkipperOverdue(Trip trip) {
        skippers.findById(trip.getSkipperId()).ifPresent(skipper ->
                telegram.sendMessage(skipper.getTelegramChatId(), """
                        Trip #%d is overdue (ETA was %s).
                        Are you ashore? Reply with /back - otherwise your emergency contact will be notified soon.
                        """.formatted(trip.getId(), TIME.format(trip.getEtaReturn()))));
    }

    @Override
    public void alertEmergencyContacts(Trip trip) {
        Skipper skipper = skippers.findById(trip.getSkipperId()).orElse(null);
        if (skipper == null) {
            return;
        }
        String alert = """
                ALERT: %s has not returned from the trip and is not responding.
                Destination: %s
                Departed: %s, return ETA: %s
                Crew: %d
                Skipper phone: %s
                If contact cannot be established, call the Maritime Rescue Coordination Centre (JRCC Tallinn): 619 1224.
                """.formatted(skipper.getName(), trip.getDestination(),
                TIME.format(trip.getDepartedAt()), TIME.format(trip.getEtaReturn()),
                trip.getCrewCount(), skipper.getPhone() == null ? "-" : skipper.getPhone());

        var reachable = contacts.findBySkipperId(skipper.getId()).stream()
                .filter(c -> c.getTelegramChatId() != null)
                .toList();
        if (reachable.isEmpty()) {
            log.warn("Trip {} ALERTED, but skipper {} has no reachable contacts", trip.getId(), skipper.getId());
            return;
        }
        for (EmergencyContact contact : reachable) {
            telegram.sendMessage(contact.getTelegramChatId(), alert);
        }
    }
}
