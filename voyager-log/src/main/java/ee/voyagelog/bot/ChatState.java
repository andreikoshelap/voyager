package ee.voyagelog.bot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Состояние многошагового диалога. Telegram stateless, поэтому текущий шаг
 * визарда и накопленные ответы храним здесь (payload -> jsonb).
 * TODO: задействовать в FSM-визарде /sail (фаза 2).
 */
@Entity
@Table(name = "chat_state")
public class ChatState {

    @Id
    private Long chatId;

    private String state;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload = new HashMap<>();

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
