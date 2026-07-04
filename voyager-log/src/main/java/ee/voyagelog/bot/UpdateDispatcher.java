package ee.voyagelog.bot;

import ee.voyagelog.harbour.Harbour;
import ee.voyagelog.harbour.HarbourRepository;
import ee.voyagelog.skipper.Skipper;
import ee.voyagelog.skipper.SkipperRepository;
import ee.voyagelog.telegram.TelegramClient;
import ee.voyagelog.telegram.dto.Message;
import ee.voyagelog.telegram.dto.Update;
import ee.voyagelog.trip.GeoUtil;
import ee.voyagelog.trip.LocationConfidence;
import ee.voyagelog.trip.StartTripCommand;
import ee.voyagelog.trip.Trip;
import ee.voyagelog.trip.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

/**
 * Command router.
 *
 * Two ways to log a trip:
 *  - typed command: /sail <departure harbour> <hours> [crew] — departure
 *    only; destination defaults to "по заливу" with an APPROXIMATE marker
 *    ~1 nm off the departure harbour (see GeoUtil).
 *  - deep-link wizard: tapping "Log a trip here" on a harbour in the web
 *    chart opens /start sail_<harbourId>, pre-filling the departure
 *    harbour so the person only has to reply with destination + hours
 *    [+ crew] as free text, e.g. "Aegna 4 2" or "по заливу 4". This is
 *    the flow voyage-web's harbour-panel.component.ts drives.
 *
 * Either way the destination text is resolved against the harbour
 * directory: a match -> CONFIRMED marker at that harbour; no match (or no
 * destination at all) -> APPROXIMATE marker offset ~1 nm from the
 * departure harbour along its seawardBearingDeg. We never fabricate a
 * precise position — an unresolved destination is always labeled as such.
 */
@Service
public class UpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.of("Europe/Tallinn"));
    private static final double APPROX_OFFSET_METERS = 1852; // 1 nautical mile
    private static final String WIZARD_AWAITING_SAIL_DETAILS = "AWAITING_SAIL_DETAILS";

    private final TelegramClient telegram;
    private final SkipperRepository skippers;
    private final HarbourRepository harbours;
    private final TripService tripService;
    private final ChatStateRepository chatStates;

    public UpdateDispatcher(TelegramClient telegram,
                            SkipperRepository skippers,
                            HarbourRepository harbours,
                            TripService tripService,
                            ChatStateRepository chatStates) {
        this.telegram = telegram;
        this.skippers = skippers;
        this.harbours = harbours;
        this.tripService = tripService;
        this.chatStates = chatStates;
    }

    public void dispatch(Update update) {
        Message msg = update.message();
        if (msg == null || msg.text() == null) {
            return;
        }
        long chatId = msg.chat().id();
        String text = msg.text().trim();

        Optional<ChatState> wizard = chatStates.findById(chatId);
        if (wizard.isPresent() && WIZARD_AWAITING_SAIL_DETAILS.equals(wizard.get().getState()) && !text.startsWith("/")) {
            handleSailWizardReply(chatId, text, wizard.get());
            return;
        }
        // A slash command cancels a stale wizard rather than silently ignoring it.
        wizard.ifPresent(w -> chatStates.deleteById(chatId));

        try {
            if (text.startsWith("/start")) {
                handleStart(chatId, msg, text);
            } else if (text.startsWith("/sail")) {
                handleSail(chatId, text);
            } else if (text.startsWith("/back")) {
                handleBack(chatId, text);
            } else if (text.startsWith("/status")) {
                handleStatus(chatId, text);
            } else if (text.startsWith("/harbour")) {
                handleHarbour(chatId, text);
            } else {
                telegram.sendMessage(chatId,
                        "Команды: /sail <гавань отправления> <часов> [экипаж], /back <номер>, /status [номер], /harbour <название>");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            telegram.sendMessage(chatId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to handle update for chat {}", chatId, e);
            telegram.sendMessage(chatId, "Что-то пошло не так, попробуй ещё раз.");
        }
    }

    private void handleStart(long chatId, Message msg, String text) {
        Skipper skipper = skippers.findByTelegramChatId(chatId)
                .orElseGet(() -> {
                    String name = msg.from() != null && msg.from().firstName() != null
                            ? msg.from().firstName() : "Шкипер";
                    return skippers.save(new Skipper(chatId, name));
                });

        String[] tokens = text.split("\\s+", 2);
        String payload = tokens.length > 1 ? tokens[1] : null;

        if (payload != null && payload.startsWith("sail_")) {
            if (startSailWizard(chatId, payload.substring("sail_".length()))) {
                return;
            }
        }
        // payload.startsWith("berth_") — berth-request relay isn't wired up yet
        // (TODO, tracked in voyage-web's README); falls through to the greeting.

        telegram.sendMessage(chatId, """
                Привет, %s! Это судовой журнал выходов в море.

                /sail Pirita 6 2 — регистрирую выход: вышел из Pirita, вернусь через 6 часов, экипаж 2
                /back <номер> — я на берегу, рейс закрыт
                /status — список активных рейсов
                /harbour Aegna — справка по гавани

                Совет: на карте voyage-log нажми «Log a trip here» на нужной гавани —
                бот сам подставит место отправления, останется написать только куда и на сколько.

                Если не вернёшься вовремя и не ответишь на пинг — твоему контактному лицу уйдёт тревога.
                """.formatted(skipper.getName()));
    }

    /** Returns true if the wizard was started (harbour id resolved to a real harbour). */
    private boolean startSailWizard(long chatId, String harbourIdRaw) {
        Long harbourId;
        try {
            harbourId = Long.parseLong(harbourIdRaw);
        } catch (NumberFormatException e) {
            return false;
        }
        Optional<Harbour> harbour = harbours.findById(harbourId);
        if (harbour.isEmpty()) {
            return false;
        }
        ChatState state = new ChatState(chatId, WIZARD_AWAITING_SAIL_DETAILS);
        state.getPayload().put("departureHarbourId", harbourId);
        chatStates.save(state);

        telegram.sendMessage(chatId, """
                Гавань отправления: %s.
                Куда идёшь и на сколько часов? Например: Aegna 4 2
                Если без определённого пункта — просто «по заливу 4» или «4» (часы, без экипажа).
                """.formatted(harbour.get().getName()));
        return true;
    }

    private void handleSailWizardReply(long chatId, String text, ChatState state) {
        try {
            Long departureHarbourId = ((Number) state.getPayload().get("departureHarbourId")).longValue();
            Harbour departure = harbours.findById(departureHarbourId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Гавань отправления пропала из справочника, начни заново: /sail"));

            SailInput input = parseSailReply(text);
            Skipper skipper = requireSkipper(chatId);
            TripPlan plan = resolveDestination(departure, input.destinationText());

            Instant eta = Instant.now().plus(Duration.ofHours(input.hours()));
            Trip trip = tripService.startTrip(new StartTripCommand(
                    skipper.getId(), departure.getId(), plan.destinationHarbourId(), plan.label(),
                    plan.markerLat(), plan.markerLon(), plan.confidence(), input.crew(), eta));

            telegram.sendMessage(chatId, confirmationMessage(trip, departure, plan));
            chatStates.deleteById(chatId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Keep the wizard open so the person can just retype the reply.
            telegram.sendMessage(chatId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to handle /sail wizard reply for chat {}", chatId, e);
            telegram.sendMessage(chatId, "Что-то пошло не так, начни заново: /sail");
            chatStates.deleteById(chatId);
        }
    }

    private void handleSail(long chatId, String text) {
        Skipper skipper = requireSkipper(chatId);
        String[] parts = text.split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Формат: /sail <гавань отправления> <часов> [экипаж], например: /sail Pirita 6 2");
        }
        int crew = 1;
        int hoursIdx = parts.length - 1;
        if (parts.length >= 4 && isInt(parts[parts.length - 1]) && isInt(parts[parts.length - 2])) {
            crew = Integer.parseInt(parts[parts.length - 1]);
            hoursIdx = parts.length - 2;
        }
        if (!isInt(parts[hoursIdx])) {
            throw new IllegalArgumentException("Не понял, через сколько часов вернёшься. Пример: /sail Pirita 6");
        }
        int hours = Integer.parseInt(parts[hoursIdx]);
        String typedHarbour = String.join(" ", Arrays.copyOfRange(parts, 1, hoursIdx));

        Harbour departure = resolveHarbour(typedHarbour)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Не нашёл гавань «" + typedHarbour + "» в справочнике. Проверь /harbour " + typedHarbour
                                + " или уточни название."));

        TripPlan plan = resolveDestination(departure, null);
        Instant eta = Instant.now().plus(Duration.ofHours(hours));
        Trip trip = tripService.startTrip(new StartTripCommand(
                skipper.getId(), departure.getId(), plan.destinationHarbourId(), plan.label(),
                plan.markerLat(), plan.markerLon(), plan.confidence(), crew, eta));

        telegram.sendMessage(chatId, confirmationMessage(trip, departure, plan));
    }

    private String confirmationMessage(Trip trip, Harbour departure, TripPlan plan) {
        String routeLine = plan.confidence() == LocationConfidence.CONFIRMED
                ? "Куда: %s → %s".formatted(departure.getName(), plan.label())
                : "Вышел из: %s (%s, точка на карте примерная)".formatted(departure.getName(), plan.label());
        return """
                Рейс №%d зарегистрирован.
                %s
                Экипаж: %d
                ETA возвращения: %s
                Семь футов под килем! Не забудь /back %d на берегу.
                """.formatted(trip.getId(), routeLine, trip.getCrewCount(), TIME.format(trip.getEtaReturn()),
                trip.getId());
    }

    /** Parses "Aegna 4 2" / "по заливу 4" / "4" into destination text + hours + crew. */
    private SailInput parseSailReply(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Напиши хотя бы через сколько часов вернёшься, например: 4");
        }
        int crew = 1;
        int hoursIdx = parts.length - 1;
        if (parts.length >= 2 && isInt(parts[parts.length - 1]) && isInt(parts[parts.length - 2])) {
            crew = Integer.parseInt(parts[parts.length - 1]);
            hoursIdx = parts.length - 2;
        }
        if (!isInt(parts[hoursIdx])) {
            throw new IllegalArgumentException("Не понял, через сколько часов вернёшься. Пример: Aegna 4 2");
        }
        int hours = Integer.parseInt(parts[hoursIdx]);
        String destination = String.join(" ", Arrays.copyOfRange(parts, 0, hoursIdx)).trim();
        return new SailInput(destination, hours, crew);
    }

    private record SailInput(String destinationText, int hours, int crew) {
    }

    /**
     * Resolves free-text destination against the harbour directory.
     * Empty/unmatched text -> APPROXIMATE marker ~1 nm off the departure
     * harbour — never left unmarked, never faked as a precise position.
     */
    private TripPlan resolveDestination(Harbour departure, String destinationText) {
        if (destinationText != null && !destinationText.isBlank()) {
            Optional<Harbour> match = resolveHarbour(destinationText.trim());
            if (match.isPresent()) {
                Harbour h = match.get();
                return new TripPlan(h.getId(), h.getName(), h.getLat(), h.getLon(), LocationConfidence.CONFIRMED);
            }
        }
        String label = (destinationText == null || destinationText.isBlank()) ? "по заливу" : destinationText.trim();
        int bearing = departure.getSeawardBearingDeg() != null ? departure.getSeawardBearingDeg() : 0;
        GeoUtil.LatLon point =
                GeoUtil.destinationPoint(departure.getLat(), departure.getLon(), bearing, APPROX_OFFSET_METERS);
        return new TripPlan(null, label, point.lat(), point.lon(), LocationConfidence.APPROXIMATE);
    }

    private record TripPlan(Long destinationHarbourId, String label, double markerLat, double markerLon,
                            LocationConfidence confidence) {
    }

    private void handleBack(long chatId, String text) {
        Skipper skipper = requireSkipper(chatId);
        Optional<Long> tripId = optionalTripId(text, "/back");
        Optional<Trip> closed = tripId
                .map(id -> tripService.checkIn(skipper.getId(), id))
                .orElseGet(() -> tripService.checkIn(skipper.getId()));
        telegram.sendMessage(chatId, closed
                .map(t -> "Рейс №" + t.getId() + " закрыт. С возвращением!")
                .orElse(tripId
                        .map(id -> "Активный рейс №" + id + " не найден.")
                        .orElse("Активного рейса нет.")));
    }

    private void handleStatus(long chatId, String text) {
        Skipper skipper = requireSkipper(chatId);
        Optional<Long> tripId = optionalTripId(text, "/status");
        if (tripId.isPresent()) {
            telegram.sendMessage(chatId, tripService.activeTrip(skipper.getId(), tripId.get())
                    .map(this::statusLine)
                    .orElse("Активный рейс №" + tripId.get() + " не найден."));
            return;
        }

        var active = tripService.activeTrips(skipper.getId());
        if (active.isEmpty()) {
            telegram.sendMessage(chatId, "Активного рейса нет.");
            return;
        }
        telegram.sendMessage(chatId, "Активные рейсы:\n" + active.stream()
                .map(this::statusLine)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(""));
    }

    private void handleHarbour(long chatId, String text) {
        String query = text.replaceFirst("/harbour", "").trim();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Формат: /harbour <название>, например: /harbour Kelnase");
        }
        Optional<Harbour> found = resolveHarbour(query);
        telegram.sendMessage(chatId, found
                .map(h -> """
                        %s
                        Глубина: %s
                        VHF: %s
                        Телефон: %s
                        Цена: %s
                        """.formatted(h.getName(),
                        orDash(h.getDepthM()), orDash(h.getVhfChannel()),
                        orDash(h.getPhone()), orDash(h.getPriceNote())))
                .orElse("Гавань не нашёл. Попробуй /harbour Pirita"));
    }

    private Optional<Harbour> resolveHarbour(String query) {
        String normalized = query.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        String folded = normalized.toLowerCase();
        var all = harbours.findAll();

        Optional<Harbour> byCode = all.stream()
                .filter(h -> h.getHarbourCode() != null && h.getHarbourCode().equalsIgnoreCase(normalized))
                .findFirst();
        if (byCode.isPresent()) {
            return byCode;
        }

        Optional<Harbour> byNickname = all.stream()
                .filter(h -> h.getNickname() != null && h.getNickname().equalsIgnoreCase(normalized))
                .findFirst();
        if (byNickname.isPresent()) {
            return byNickname;
        }

        Optional<Harbour> byExactName = all.stream()
                .filter(h -> h.getName().equalsIgnoreCase(normalized))
                .findFirst();
        if (byExactName.isPresent()) {
            return byExactName;
        }

        return all.stream()
                .filter(h -> h.getName().toLowerCase().contains(folded))
                .findFirst();
    }

    private String statusLine(Trip trip) {
        return "Рейс №%d: %s, статус %s, ETA %s".formatted(
                trip.getId(), trip.getDestination(), trip.getStatus(), TIME.format(trip.getEtaReturn()));
    }

    private Optional<Long> optionalTripId(String text, String command) {
        String args = text.replaceFirst(command + "(?:@\\w+)?", "").trim();
        if (args.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = args.split("\\s+");
        if (parts.length != 1) {
            throw new IllegalArgumentException("Формат: " + command + " <номер рейса>");
        }
        try {
            return Optional.of(Long.parseLong(parts[0]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Номер рейса должен быть числом. Формат: " + command + " <номер рейса>");
        }
    }

    private Skipper requireSkipper(long chatId) {
        return skippers.findByTelegramChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Сначала /start"));
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
        return value == null ? "—" : value.toString();
    }
}
