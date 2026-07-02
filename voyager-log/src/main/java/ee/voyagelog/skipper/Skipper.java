package ee.voyagelog.skipper;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "skipper")
public class Skipper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long telegramChatId;
    private String name;
    private String phone;
    private Instant createdAt = Instant.now();

    protected Skipper() {
    }

    public Skipper(Long telegramChatId, String name) {
        this.telegramChatId = telegramChatId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
