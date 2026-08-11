package com.safeher.backend.service;

import com.safeher.backend.entity.SosAlert;
import com.safeher.backend.entity.TrustedContact;

/**
 * How an alert actually reaches a person.
 *
 * This is an interface so the app can be built and demonstrated without a paid
 * SMS account. The logging implementation is the default; a Twilio one can be
 * dropped in later without touching SosService.
 */
public interface AlertSender {

    /** @return the provider's message reference, or null if there isn't one. */
    String send(SosAlert alert, TrustedContact contact, String message);

    /** Named in logs and stored on the notification record. */
    String channel();
}
