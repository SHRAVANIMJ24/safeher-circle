package com.safeher.backend.service;

import com.safeher.backend.dto.TrustedContactRequest;
import com.safeher.backend.dto.TrustedContactResponse;
import com.safeher.backend.entity.TrustedContact;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.TrustedContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    /**
     * Five is a deliberate ceiling, not a technical one. An alert that goes to
     * thirty people is an announcement; one that goes to five is a call for
     * help. It also keeps the SMS bill survivable.
     */
    private static final int MAX_CONTACTS = 5;

    private final TrustedContactRepository contactRepository;

    @Transactional(readOnly = true)
    public List<TrustedContactResponse> list(User user) {
        return contactRepository.findByUserOrderByPriorityAsc(user)
                .stream()
                .map(TrustedContactResponse::from)
                .toList();
    }

    @Transactional
    public TrustedContactResponse add(User user, TrustedContactRequest request) {
        if (contactRepository.countByUser(user) >= MAX_CONTACTS) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You can have up to " + MAX_CONTACTS + " contacts. Remove one "
                    + "before adding another.");
        }

        String phone = normalisePhone(request.phone());

        if (contactRepository.existsByUserAndPhone(user, phone)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "That number is already on your list.");
        }

        TrustedContact contact = TrustedContact.builder()
                .user(user)
                .name(request.name().trim())
                .phone(phone)
                .relationship(blankToNull(request.relationship()))
                .priority(request.priority() == null ? (short) 1 : request.priority())
                .notifyBySms(request.notifyBySms() == null || request.notifyBySms())
                .build();

        return TrustedContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public void remove(User user, UUID contactId) {
        TrustedContact contact = contactRepository.findByIdAndUser(contactId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "That contact is not on your list."));

        contactRepository.delete(contact);
    }

    /** Strips spaces and dashes so the same number is not stored twice. */
    private String normalisePhone(String phone) {
        return phone.replaceAll("[\\s\\-]", "");
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
