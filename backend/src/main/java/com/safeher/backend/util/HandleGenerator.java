package com.safeher.backend.util;

import com.safeher.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * Builds the pseudonyms shown next to posts, e.g. "quiet-lark-4471".
 *
 * Word choice matters here. The lists deliberately avoid anything that reads as
 * a judgement about the person, and avoid gendered or appearance-based words —
 * a handle is assigned, not chosen, so it must be neutral for everyone.
 */
@Component
@RequiredArgsConstructor
public class HandleGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "quiet", "steady", "bright", "calm", "swift", "clever", "gentle",
            "bold", "warm", "keen", "still", "sunlit", "amber", "wild",
            "patient", "open", "rooted", "clear", "brave", "kind"
    );

    private static final List<String> NOUNS = List.of(
            "lark", "cedar", "river", "ember", "willow", "harbour", "meadow",
            "sparrow", "lantern", "compass", "orchard", "beacon", "thistle",
            "falcon", "pebble", "maple", "anchor", "heron", "juniper", "wren"
    );

    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final UserRepository userRepository;

    /**
     * Returns a handle not currently in use. Retries on collision; with
     * 400 word pairs and a 4-digit suffix the odds of exhausting the attempts
     * are negligible until the user count is very large.
     */
    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = build();
            if (!userRepository.existsByAnonHandle(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate an unused handle after " + MAX_ATTEMPTS + " attempts");
    }

    private String build() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        int suffix = 1000 + random.nextInt(9000);
        return adjective + "-" + noun + "-" + suffix;
    }
}
