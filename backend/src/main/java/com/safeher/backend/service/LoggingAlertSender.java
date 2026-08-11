package com.safeher.backend.service;

import com.safeher.backend.entity.SosAlert;
import com.safeher.backend.entity.TrustedContact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Prints the message that would have been sent.
 *
 * This exists so the whole alert flow can be exercised end to end with no SMS
 * account and no per-message cost. Nothing leaves the machine. Swap in a real
 * sender before this is ever used by an actual person — the console is not a
 * delivery channel.
 */
@Service
@ConditionalOnMissingBean(name = "twilioAlertSender")
public class LoggingAlertSender implements AlertSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertSender.class);

    @Override
    public String send(SosAlert alert, TrustedContact contact, String message) {
        log.warn("""

                ================ SOS (not actually sent) ================
                To:      {} <{}>
                Alert:   {}
                Message: {}
                =========================================================
                """,
                contact.getName(), contact.getPhone(), alert.getId(), message);

        return "logged-" + alert.getId();
    }

    @Override
    public String channel() {
        return "SMS";
    }
}
