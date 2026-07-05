package ee.voyagelog.bot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Multi-step dialog state. Telegram is stateless, so the current wizard step
 * and collected answers are stored here (payload -> jsonb).
 * TODO: use this in the /sail FSM wizard (phase 2).
 */
@Entity
@Table(name = "chat_state")
public class ChatState {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    private String state;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected ChatState() {
    }

    public ChatState(Long chatId, String state) {
        this.chatId = chatId;
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void transition(String newState) {
        this.state = newState;
        this.updatedAt = Instant.now();
    }
}
