package ee.voyagelog.harbour;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * UPDATED: added seawardBearingDeg — a rough, manually-eyeballed bearing
 * (degrees) toward open water, used only as the fallback direction for an
 * APPROXIMATE trip marker when no destination was given or resolved. Not
 * surveyed data, see V3 migration comment.
 */
@Entity
@Table(name = "harbour")
public class Harbour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double lat;
    private Double lon;
    @Column(name = "vhf_channel")
    private String vhfChannel;
    private String phone;
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;
    @Column(name = "depth_m")
    private BigDecimal depthM;
    @Column(name = "price_note")
    private String priceNote;
    @Column(name = "seaward_bearing_deg")
    private Integer seawardBearingDeg;

    protected Harbour() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
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

    public Integer getSeawardBearingDeg() {
        return seawardBearingDeg;
    }
}
