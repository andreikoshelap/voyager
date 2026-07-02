package ee.voyagelog.skipper;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "vessel")
public class Vessel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long skipperId;
    private String name;
    private String type;
    private BigDecimal lengthM;
    private String sailNumber;

    protected Vessel() {
    }

    public Vessel(Long skipperId, String name, String type, BigDecimal lengthM) {
        this.skipperId = skipperId;
        this.name = name;
        this.type = type;
        this.lengthM = lengthM;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
