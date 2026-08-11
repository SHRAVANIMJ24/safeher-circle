package com.safeher.backend.service;

import com.safeher.backend.dto.SosAlertResponse;
import com.safeher.backend.dto.TriggerSosRequest;
import com.safeher.backend.entity.*;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.SosAlertRepository;
import com.safeher.backend.repository.SosNotificationRepository;
import com.safeher.backend.repository.TrustedContactRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SosService {

    private static final Logger log = LoggerFactory.getLogger(SosService.class);

    private final SosAlertRepository alertRepository;
    private final SosNotificationRepository notificationRepository;
    private final TrustedContactRepository contactRepository;
    private final AlertSender alertSender;

    @Value("${safeher.sos.map-link-base:https://www.google.com/maps?q=}")
    private String mapLinkBase;

    /**
     * Raises an alert and tells the person's contacts.
     *
     * If an alert is already live, that one is returned rather than a second
     * being created. Someone frightened will press the button repeatedly, and
     * five alerts means five rounds of messages to the same people.
     */
    @Transactional
    public SosAlertResponse trigger(User user, TriggerSosRequest request) {
        var existing = alertRepository
                .findFirstByUserAndStatusOrderByTriggeredAtDesc(user, SosStatus.ACTIVE);

        if (existing.isPresent()) {
            SosAlert live = existing.get();
            updateLocationIfNewer(live, request);
            return SosAlertResponse.from(live, countNotified(live));
        }

        SosAlert alert = SosAlert.builder()
                .user(user)
                .status(SosStatus.ACTIVE)
                .triggerMethod(defaultTo(request.triggerMethod(), "BUTTON"))
                .alarmType(defaultTo(request.alarmType(), "SIREN"))
                .exactLat(request.latitude())
                .exactLng(request.longitude())
                .accuracyMeters(request.accuracyMeters())
                .note(request.note())
                .build();

        alert = alertRepository.save(alert);

        int notified = notifyContacts(user, alert);
        return SosAlertResponse.from(alert, notified);
    }

    /** The person confirming they are all right. */
    @Transactional
    public SosAlertResponse resolve(User user, UUID alertId, SosStatus outcome) {
        SosAlert alert = alertRepository.findByIdAndUser(alertId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That alert does not exist."));

        if (alert.getStatus() != SosStatus.ACTIVE) {
            return SosAlertResponse.from(alert, countNotified(alert));
        }

        alert.setStatus(outcome);
        alert.setResolvedAt(Instant.now());
        alertRepository.save(alert);

        return SosAlertResponse.from(alert, countNotified(alert));
    }

    @Transactional(readOnly = true)
    public SosAlertResponse active(User user) {
        SosAlert alert = alertRepository
                .findFirstByUserAndStatusOrderByTriggeredAtDesc(user, SosStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No alert is currently active."));

        return SosAlertResponse.from(alert, countNotified(alert));
    }

    @Transactional(readOnly = true)
    public List<SosAlertResponse> history(User user) {
        return alertRepository.findByUserOrderByTriggeredAtDesc(user)
                .stream()
                .map(alert -> SosAlertResponse.from(alert, countNotified(alert)))
                .toList();
    }

    private int notifyContacts(User user, SosAlert alert) {
        List<TrustedContact> contacts =
                contactRepository.findByUserOrderByPriorityAsc(user);

        if (contacts.isEmpty()) {
            log.warn("Alert {} raised but the account has no contacts set up",
                    alert.getId());
            return 0;
        }

        String message = composeMessage(user, alert);
        int sent = 0;

        for (TrustedContact contact : contacts) {
            if (!contact.isNotifyBySms()) {
                continue;
            }

            SosNotification record = SosNotification.builder()
                    .alert(alert)
                    .contact(contact)
                    .channel(alertSender.channel())
                    .build();

            try {
                String reference = alertSender.send(alert, contact, message);
                record.setDeliveryStatus("SENT");
                record.setProviderRef(reference);
                record.setSentAt(Instant.now());
                sent++;
            } catch (Exception ex) {
                // One contact failing must not stop the rest being told.
                record.setDeliveryStatus("FAILED");
                record.setErrorMessage(ex.getMessage());
                log.error("Could not reach contact {} for alert {}",
                        contact.getId(), alert.getId(), ex);
            }

            notificationRepository.save(record);
        }

        return sent;
    }

    /**
     * Kept short and unambiguous. Someone reading this on a lock screen at 2am
     * needs the name, the fact it is an alert, and a location link — in that
     * order, with nothing before it.
     */
    private String composeMessage(User user, SosAlert alert) {
        StringBuilder message = new StringBuilder();
        message.append(user.getAnonHandle())
               .append(" has raised an alert on SafeHer Circle and asked you to be told.");

        if (alert.getExactLat() != null && alert.getExactLng() != null) {
            message.append(" Location: ")
                   .append(mapLinkBase)
                   .append(alert.getExactLat())
                   .append(",")
                   .append(alert.getExactLng());
        } else {
            message.append(" No location was available.");
        }

        if (alert.getNote() != null && !alert.getNote().isBlank()) {
            message.append(" Note: ").append(alert.getNote());
        }

        return message.toString();
    }

    /** A later fix on the same alert is better than the first one. */
    private void updateLocationIfNewer(SosAlert alert, TriggerSosRequest request) {
        if (request.latitude() != null && request.longitude() != null) {
            alert.setExactLat(request.latitude());
            alert.setExactLng(request.longitude());
            alert.setAccuracyMeters(request.accuracyMeters());
            alertRepository.save(alert);
        }
    }

    private int countNotified(SosAlert alert) {
        return (int) notificationRepository.findByAlert(alert)
                .stream()
                .filter(n -> "SENT".equals(n.getDeliveryStatus()))
                .count();
    }

    private String defaultTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

}
