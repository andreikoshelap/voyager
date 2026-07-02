package ee.voyagelog.telegram;

import ee.voyagelog.config.TelegramProperties;
import ee.voyagelog.config.VoyageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class WebhookRegistrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebhookRegistrar.class);

    private final TelegramClient telegram;
    private final TelegramProperties telegramProps;
    private final VoyageProperties voyageProps;

    public WebhookRegistrar(TelegramClient telegram,
                            TelegramProperties telegramProps,
                            VoyageProperties voyageProps) {
        this.telegram = telegram;
        this.telegramProps = telegramProps;
        this.voyageProps = voyageProps;
    }

    @Override
    public void run(ApplicationArguments args) {
        String url = voyageProps.publicBaseUrl() + "/telegram/webhook";
        telegram.setWebhook(url, telegramProps.webhookSecret());
        log.info("Telegram webhook registered: {}", url);
    }
}
