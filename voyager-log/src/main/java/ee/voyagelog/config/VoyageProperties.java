package ee.voyagelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * grace-period: how long to wait after ETA before marking a trip overdue;
 * alert-delay: how long to wait for the skipper before alerting a contact.
 */
@ConfigurationProperties(prefix = "voyage")
public record VoyageProperties(Duration gracePeriod, Duration alertDelay, String publicBaseUrl) {
}
