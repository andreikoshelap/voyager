package ee.voyagelog.harbour;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "harbour")
public class Harbour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double lat;
    private Double lon;
    private String vhfChannel;
    private String phone;
    private Long telegramChatId;
    private BigDecimal depthM;
    private String priceNote;

    protected Harbour() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVhfChannel() {
        return vhfChannel;
    }

    public String getPhone() {
        return phone;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public BigDecimal getDepthM() {
        return depthM;
    }

    public String getPriceNote() {
        return priceNote;
    }
}
