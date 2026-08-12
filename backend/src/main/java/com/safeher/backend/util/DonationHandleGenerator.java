package com.safeher.backend.util;

import com.safeher.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * Builds the second pseudonym, used only on the donation board.
 *
 * The word lists are deliberately different from HandleGenerator's, so the two
 * handles are visibly distinct — "calm-maple-3867" and "give-cotton-9021"
 * cannot be mistaken for each other or for a naming pattern that pairs them.
 *
 * The point: a request reading "I cannot afford pads this month" is a
 * disclosure of poverty. If it shared a handle with someone's posts about a
 * domestic situation, anyone could assemble a profile of a woman at her most
 * vulnerable. Two handles, one person, no link visible from outside.
 */
@Component
@RequiredArgsConstructor
public class DonationHandleGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "cotton", "linen", "paper", "copper", "walnut", "olive", "hazel",
            "indigo", "saffron", "ivory", "sandal", "khadi", "jute", "brass",
            "marigold", "cardamom", "clove", "mango", "neem", "tamarind"
    );

    private static final List<String> NOUNS = List.of(
            "basket", "parcel", "bundle", "satchel", "crate", "pouch", "tin",
            "carton", "trunk", "hamper", "sack", "case", "box", "bag",
            "jar", "pot", "tray", "chest", "roll", "wrap"
    );

    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final UserRepository userRepository;

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = build();
            if (!userRepository.existsByDonationHandle(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate an unused donation handle after "
                + MAX_ATTEMPTS + " attempts");
    }

    private String build() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        int suffix = 1000 + random.nextInt(9000);
        return adjective + "-" + noun + "-" + suffix;
    }
}
