package ee.voyagelog.harbour;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * UPDATED: added harbourCode, address, website, email, amenities, sourceUrl
 * — populated from an Estonia-wide guest-harbor registry export (V4
 * migration). All nullable: the original 7 Tallinn-bay harbors only have
 * them where a matching registry row was found, and harbors added by hand
 * later won't have them at all.
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
    private String harbourCode;
    private String address;
    private String website;
    private String email;
    private String amenities;
    private String sourceUrl;

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

    public String getHarbourCode() {
        return harbourCode;
    }

    public String getAddress() {
        return address;
    }

    public String getWebsite() {
        return website;
    }

    public String getEmail() {
        return email;
    }

    public String getAmenities() {
        return amenities;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }
}
