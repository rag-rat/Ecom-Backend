package com.ecom.notification.service;

public interface NotificationService {

    /*
     * send()
     * ────────
     * Deliberately generic — recipient could be an email address or a
     * phone number depending on the implementation; subject is ignored
     * by SMS-style implementations (SMS has no subject line concept)
     * but kept in the shared interface for the email case. This is a
     * reasonable simplification for a learning build; a more rigorous
     * design might split into separate EmailNotificationService and
     * SmsNotificationService interfaces with different method shapes
     */

    void send(String recipient, String subject, String body);
}
