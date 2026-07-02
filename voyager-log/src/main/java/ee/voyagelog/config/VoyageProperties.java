package ee.voyagelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * grace-period — сколько ждём после ETA, прежде чем считать рейс просроченным;
 * alert-delay — сколько ждём ответа шкипера, прежде чем поднимать тревогу контакту.
 */
@ConfigurationProperties(prefix = "voyage")
public record VoyageProperties(Duration gracePeriod, Duration alertDelay, String publicBaseUrl) {
}
