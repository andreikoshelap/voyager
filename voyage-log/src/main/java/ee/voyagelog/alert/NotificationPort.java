package ee.voyagelog.alert;

import ee.voyagelog.trip.Trip;

/**
 * Notification channel abstraction: Telegram for now, SMS can be added later
 * (for example, Messente) without touching scheduler logic.
 */
public interface NotificationPort {

    void pingSkipperOverdue(Trip trip);

    void alertEmergencyContacts(Trip trip);
}
