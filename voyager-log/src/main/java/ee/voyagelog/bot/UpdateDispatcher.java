package ee.voyagelog.bot;

import ee.voyagelog.harbour.Harbour;
import ee.voyagelog.harbour.HarbourRepository;
import ee.voyagelog.skipper.Skipper;
import ee.voyagelog.skipper.SkipperRepository;
import ee.voyagelog.telegram.TelegramClient;
import ee.voyagelog.telegram.dto.Message;
import ee.voyagelog.telegram.dto.Update;
import ee.voyagelog.trip.Trip;
import ee.voyagelog.trip.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Command router. The MVP /sail command accepts everything in one line:
 *   /sail Kelnase 6      -> sailing to Kelnase, returning in 6 hours
 *   /sail Aegna 4 3      -> Aegna, 4 hours, crew of 3
 * TODO phase 2: step-by-step ChatState wizard (harbour buttons, ETA, crew).
 */
@Service
public class UpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.of("Europe/Tallinn"));

    private final TelegramClient telegram;
    private final SkipperRepository skippers;
    private final HarbourRepository harbours;
    private final TripService tripService;

    public UpdateDispatcher(TelegramClient telegram,
                            SkipperRepository skippers,
                            HarbourRepository harbours,
                            TripService tripService) {
        this.telegram = telegram;
        this.skippers = skippers;
        this.harbours = harbours;
        this.tripService = tripService;
    }

    public void dispatch(Update update) {
        Message msg = update.message();
        if (msg == null || msg.text() == null) {
            return;
        }
        long chatId = msg.chat().id();
        String text = msg.text().trim();
        try {
            if (text.startsWith("/start")) {
                handleStart(chatId, msg);
            } else if (text.startsWith("/sail")) {
                handleSail(chatId, text);
            } else if (text.startsWith("/back")) {
                handleBack(chatId);
            } else if (text.startsWith("/status")) {
                handleStatus(chatId);
            } else if (text.startsWith("/harbour")) {
                handleHarbour(chatId, text);
            } else {
                telegram.sendMessage(chatId,
                        "Commands: /sail <destination> <hours> [crew], /back, /status, /harbour <name>");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            telegram.sendMessage(chatId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to handle update for chat {}", chatId, e);
            telegram.sendMessage(chatId, "Something went wrong, please try again.");
        }
    }

    private void handleStart(long chatId, Message msg) {
        Skipper skipper = skippers.findByTelegramChatId(chatId)
                .orElseGet(() -> {
                    String name = msg.from() != null && msg.from().firstName() != null
                            ? msg.from().firstName() : "Skipper";
                    return skippers.save(new Skipper(chatId, name));
                });
        telegram.sendMessage(chatId, """
                Hello, %s! This is a voyage log for sea trips.

                /sail Kelnase 6 - register a trip to Kelnase, returning in 6 hours
                /back - I am ashore, close the trip
                /status - active trip
                /harbour Aegna - harbour details

                If you do not return on time and do not answer the ping, your emergency contact will be alerted.
                """.formatted(skipper.getName()));
    }

    private void handleSail(long chatId, String text) {
        Skipper skipper = requireSkipper(chatId);
        String[] parts = text.split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Format: /sail <destination> <hours> [crew], for example: /sail Kelnase 6 2");
        }
        int crew = 1;
        int hoursIdx = parts.length - 1;
        if (parts.length >= 4 && isInt(parts[parts.length - 1]) && isInt(parts[parts.length - 2])) {
            crew = Integer.parseInt(parts[parts.length - 1]);
            hoursIdx = parts.length - 2;
        }
        if (!isInt(parts[hoursIdx])) {
            throw new IllegalArgumentException("Could not parse return time in hours. Example: /sail Kelnase 6");
        }
        int hours = Integer.parseInt(parts[hoursIdx]);
        String destination = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, hoursIdx));

        Instant eta = Instant.now().plus(Duration.ofHours(hours));
        Trip trip = tripService.startTrip(skipper.getId(), destination, crew, eta);
        telegram.sendMessage(chatId, """
                Trip #%d registered.
                Destination: %s
                Crew: %d
                Return ETA: %s
                Fair winds. Remember to send /back when ashore.
                """.formatted(trip.getId(), trip.getDestination(), trip.getCrewCount(),
                TIME.format(trip.getEtaReturn())));
    }

    private void handleBack(long chatId) {
        Skipper skipper = requireSkipper(chatId);
        Optional<Trip> closed = tripService.checkIn(skipper.getId());
        telegram.sendMessage(chatId, closed
                .map(t -> "Trip #" + t.getId() + " closed. Welcome back!")
                .orElse("No active trip."));
    }

    private void handleStatus(long chatId) {
        Skipper skipper = requireSkipper(chatId);
        telegram.sendMessage(chatId, tripService.activeTrip(skipper.getId())
                .map(t -> "Trip #%d: %s, status %s, ETA %s".formatted(
                        t.getId(), t.getDestination(), t.getStatus(), TIME.format(t.getEtaReturn())))
                .orElse("No active trip."));
    }

    private void handleHarbour(long chatId, String text) {
        String query = text.replaceFirst("/harbour", "").trim();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Format: /harbour <name>, for example: /harbour Kelnase");
        }
        Optional<Harbour> found = harbours.findFirstByNameIgnoreCaseContaining(query);
        telegram.sendMessage(chatId, found
                .map(h -> """
                        %s
                        Depth: %s
                        VHF: %s
                        Phone: %s
                        Price: %s
                        """.formatted(h.getName(),
                        orDash(h.getDepthM()), orDash(h.getVhfChannel()),
                        orDash(h.getPhone()), orDash(h.getPriceNote())))
                .orElse("Harbour not found. Try /harbour Pirita"));
    }

    private Skipper requireSkipper(long chatId) {
        return skippers.findByTelegramChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Send /start first"));
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String orDash(Object value) {
        return value == null ? "-" : value.toString();
    }
}
