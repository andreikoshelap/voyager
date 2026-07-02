package ee.voyagelog.skipper;

import jakarta.persistence.Column;
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

    @Column(name = "skipper_id")
    private Long skipperId;
    private String name;
    private String type;
    @Column(name = "length_m")
    private BigDecimal lengthM;
    @Column(name = "sail_number")
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
