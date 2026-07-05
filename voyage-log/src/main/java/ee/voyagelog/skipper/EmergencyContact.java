package ee.voyagelog.skipper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "emergency_contact")
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skipper_id")
    private Long skipperId;
    private String name;
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;
    private String phone;

    protected EmergencyContact() {
    }

    public EmergencyContact(Long skipperId, String name, Long telegramChatId, String phone) {
        this.skipperId = skipperId;
        this.name = name;
        this.telegramChatId = telegramChatId;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public String getPhone() {
        return phone;
    }
}
