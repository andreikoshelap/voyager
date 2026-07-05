package ee.voyagelog.telegram;

import ee.voyagelog.config.TelegramProperties;
import ee.voyagelog.telegram.dto.Update;
import ee.voyagelog.telegram.dto.UpdatesResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin Bot API wrapper without third-party libraries; RestClient is enough.
 */
@Component
public class TelegramClient {

    private final RestClient http;

    public TelegramClient(TelegramProperties props) {
        this.http = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + props.token())
                .build();
    }

    public void sendMessage(long chatId, String text) {
        http.post().uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                .retrieve()
                .toBodilessEntity();
    }

    public List<Update> getUpdates(long offset) {
        UpdatesResponse resp = http.get()
                .uri(uri -> uri.path("/getUpdates")
                        .queryParam("offset", offset)
                        .queryParam("timeout", 0)
                        .build())
                .retrieve()
                .body(UpdatesResponse.class);
        return resp == null || resp.result() == null ? List.of() : resp.result();
    }

    public void setWebhook(String url, String secretToken) {
        http.post().uri("/setWebhook")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("url", url, "secret_token", secretToken))
                .retrieve()
                .toBodilessEntity();
    }
}
