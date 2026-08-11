# Scoring service

A small FastAPI service that reads post text and returns three numbers:
a toxicity score, an urgency score, and a suggested category.

**It never decides anything.** The Java backend stores the scores and uses them
to order the moderation queue. A human moderator decides what happens to the
content.

## Why it is advisory only

A toxicity classifier cannot tell someone describing harassment from someone
committing it, because the words are the same. Test it yourself once it is
running — the seeded post about a manager scheduling late meetings scores
higher than most of the board, and it is a woman asking for advice.

On a platform where women write about what was done to them, an auto-hiding
classifier would remove survivors' posts for describing their own experience.
That is the failure mode this design exists to avoid.

## Running it

```bash
cd ml-service
python -m venv venv

# Windows
venv\Scripts\activate
# macOS or Linux
source venv/bin/activate

pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

The first start downloads about 400MB of model weights and takes a few
minutes. After that it loads from a local cache in seconds.

Check it is alive:

```bash
curl http://localhost:8000/health
```

Interactive docs, which are useful for trying inputs by hand:
http://localhost:8000/docs

## Trying it

```bash
curl -X POST http://localhost:8000/score -H "Content-Type: application/json" -d '{"title":"Need advice","body":"My manager keeps scheduling meetings after 8pm when the floor is empty. Nothing has happened but I am uncomfortable."}'
```

## What each number means

| Field | Source | Meaning |
|---|---|---|
| `toxicity` | `unitary/toxic-bert` | Highest score across the model's labels. High means a moderator should look, not that the post is bad. |
| `urgency` | Rules in `score_urgency` | Whether the writer sounds like she is in danger now rather than describing the past. |
| `predicted_category` | Keyword matching | A suggestion for the category dropdown. Not a trained classifier — see below. |
| `suggested_action` | Thresholds | FLAG, REVIEW or NONE. Affects queue order only. |

## Known limitations, stated rather than hidden

**The category prediction is keyword matching, not machine learning.** A
trained classifier needs labelled posts from this platform, which do not exist
yet. Once there are a few thousand categorised posts, that is the obvious
upgrade. Calling the current version a model would be dishonest.

**The model is trained on English.** Hinglish, Marathi and Devanagari script
will score unpredictably. On a platform aimed at Indian users this is a serious
gap, not a footnote.

**Urgency is rules, not a model.** There is no good pretrained classifier for
"is this person in danger right now", and an unreliable one would be worse than
transparent rules a moderator can read and reason about.

**It runs on CPU.** Each request takes a few hundred milliseconds. Fine for
this scale, not for thousands of posts a minute.
