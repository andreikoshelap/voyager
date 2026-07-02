package ee.voyagelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class VoyageLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoyageLogApplication.class, args);
    }
}
