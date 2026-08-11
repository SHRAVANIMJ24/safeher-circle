package com.safeher.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Talks to the Python scoring service.
 *
 * Every failure path returns empty rather than throwing. If the scoring
 * service is down, slow, or was never started, posting must still work — a
 * board that refuses posts because an advisory classifier is unavailable has
 * its priorities backwards.
 */
@Service
@RequiredArgsConstructor
public class ScoringClient {

    private static final Logger log = LoggerFactory.getLogger(ScoringClient.class);

    @Value("${safeher.scoring.url:http://localhost:8000}")
    private String scoringUrl;

    @Value("${safeher.scoring.enabled:true}")
    private boolean enabled;

    private RestClient client;

    private RestClient client() {
        if (client == null) {
            client = RestClient.builder()
                    .baseUrl(scoringUrl)
                    .requestFactory(timeouts())
                    .build();
        }
        return client;
    }

    private org.springframework.http.client.ClientHttpRequestFactory timeouts() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        // Generous, because the model runs on CPU — but bounded, so a hung
        // service cannot hold a post submission open indefinitely.
        factory.setReadTimeout(Duration.ofSeconds(8));
        return factory;
    }

    public Optional<ScoreResult> score(String title, String body) {
        if (!enabled) {
            return Optional.empty();
        }

        try {
            ScoreResult result = client()
                    .post()
                    .uri("/score")
                    .body(new ScoreRequest(title, body))
                    .retrieve()
                    .body(ScoreResult.class);

            return Optional.ofNullable(result);

        } catch (Exception ex) {
            log.warn("Scoring service unavailable, publishing without a score: {}",
                    ex.getMessage());
            return Optional.empty();
        }
    }

    public record ScoreRequest(String title, String body) {}

    public record ScoreResult(
            Float toxicity,
            Float urgency,
            @JsonProperty("predicted_category") String predictedCategory,
            @JsonProperty("model_version") String modelVersion,
            @JsonProperty("suggested_action") String suggestedAction,
            List<String> reasons
    ) {}
}
