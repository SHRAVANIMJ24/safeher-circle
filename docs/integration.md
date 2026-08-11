# Wiring the scoring service into Spring

## Files

| File | Goes in |
|---|---|
| `ModerationScore.java` | `entity/` |
| `ModerationScoreRepository.java` | `repository/` |
| `ScoringClient.java` | `service/` |

## application.properties

```properties
safeher.scoring.url=http://localhost:8000
safeher.scoring.enabled=true
```

Set `enabled=false` to run the backend without the Python service.

## PostService — score on create

Add the two dependencies to the constructor block:

```java
    private final ScoringClient scoringClient;
    private final ModerationScoreRepository moderationScoreRepository;
```

Then, in `create()`, replace the final line:

```java
        return PostResponse.from(postRepository.save(post));
```

with:

```java
        Post saved = postRepository.save(post);
        recordScore(saved);
        return PostResponse.from(saved);
```

And add this method to the class:

```java
    /**
     * Asks the scoring service what it thinks, and stores the answer.
     *
     * The post is already saved and published before this runs. A score can
     * raise a post's position in the moderation queue; it can never stop the
     * post appearing. If the scoring service is down, the post is simply
     * unscored.
     */
    private void recordScore(Post post) {
        scoringClient.score(post.getTitle(), post.getBody()).ifPresent(result -> {
            ModerationScore score = ModerationScore.builder()
                    .targetType(TargetType.POST)
                    .targetId(post.getId())
                    .toxicity(result.toxicity())
                    .urgency(result.urgency())
                    .predictedCategory(result.predictedCategory())
                    .modelVersion(result.modelVersion())
                    .autoAction(result.suggestedAction())
                    .build();

            moderationScoreRepository.save(score);

            // urgency_score orders the "urgent" sort on the board and the
            // moderation queue. It changes position, never visibility.
            if (result.urgency() != null) {
                post.setUrgencyScore((short) Math.round(result.urgency() * 100));
                postRepository.save(post);
            }
        });
    }
```

You will need these imports in PostService:

```java
import com.safeher.backend.entity.ModerationScore;
import com.safeher.backend.entity.TargetType;
import com.safeher.backend.repository.ModerationScoreRepository;
```

## What deliberately does not happen

There is no branch anywhere that sets `status` to `PENDING` or `REMOVED` based
on a score. That is the point of the design, not an oversight. If you add one
later, understand what you are choosing: the model will hide posts from women
describing what happened to them, because it cannot tell those apart from
posts committing the same abuse.
