package ee.voyagelog.telegram;

import ee.voyagelog.bot.UpdateDispatcher;
import ee.voyagelog.config.TelegramProperties;
import ee.voyagelog.telegram.dto.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final UpdateDispatcher dispatcher;
    private final TelegramProperties props;

    public WebhookController(UpdateDispatcher dispatcher, TelegramProperties props) {
        this.dispatcher = dispatcher;
        this.props = props;
    }

    @PostMapping("/telegram/webhook")
    public ResponseEntity<Void> onUpdate(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody Update update) {
        if (!props.webhookSecret().equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        dispatcher.dispatch(update);
        return ResponseEntity.ok().build();
    }
}
