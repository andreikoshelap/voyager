package ee.voyagelog.telegram;

import ee.voyagelog.bot.UpdateDispatcher;
import ee.voyagelog.telegram.dto.Update;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Local development without HTTPS: long polling instead of webhook.
 * Remember to remove the webhook (deleteWebhook), otherwise getUpdates returns 409.
 */
@Component
@Profile("local")
public class LongPollingRunner {

    private final TelegramClient telegram;
    private final UpdateDispatcher dispatcher;
    private final AtomicLong offset = new AtomicLong(0);

    public LongPollingRunner(TelegramClient telegram, UpdateDispatcher dispatcher) {
        this.telegram = telegram;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelay = 2000)
    public void poll() {
        for (Update update : telegram.getUpdates(offset.get())) {
            offset.set(update.updateId() + 1);
            dispatcher.dispatch(update);
        }
    }
}
