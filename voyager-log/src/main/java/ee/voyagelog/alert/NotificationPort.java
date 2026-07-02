package ee.voyagelog.alert;

import ee.voyagelog.trip.Trip;

/**
 * Абстракция канала уведомлений: сейчас Telegram, потом легко добавить SMS
 * (например, Messente) не трогая логику scheduler'а.
 */
public interface NotificationPort {

    void pingSkipperOverdue(Trip trip);

    void alertEmergencyContacts(Trip trip);
}
