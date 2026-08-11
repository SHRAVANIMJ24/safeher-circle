"""
SafeHer Circle — content scoring service.

Reads a piece of text and returns three numbers. It does not decide anything;
the Java backend stores the scores and a human moderator acts on them.

WHY THIS SERVICE CANNOT BE TRUSTED TO ACT ALONE
-----------------------------------------------
A toxicity classifier cannot distinguish someone describing harassment from
someone committing it. The vocabulary is the same. A post reading "he kept
following me and shouting" and a post shouting the same words at a person
score similarly, because the model sees tokens, not intent.

On a board where women write about what was done to them, an auto-hiding
classifier would silence survivors for describing their own experience. So
this service is advisory only: it orders the moderation queue and nothing else.
"""

import logging
import os
import re
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("safeher-ml")

MODEL_NAME = os.getenv("TOXICITY_MODEL", "unitary/toxic-bert")
MODEL_VERSION = f"{MODEL_NAME}@1"

# BERT truncates at 512 tokens. Long posts are scored on the opening, which is
# where someone usually says the thing that matters.
MAX_CHARS = 2000

models = {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load the model once at startup, not per request."""
    from transformers import pipeline

    log.info("Loading %s — first run downloads about 400MB", MODEL_NAME)
    models["toxicity"] = pipeline(
        "text-classification",
        model=MODEL_NAME,
        top_k=None,
    )
    log.info("Model ready")
    yield
    models.clear()


app = FastAPI(
    title="SafeHer Circle scoring service",
    description="Advisory content scores. Never auto-removes anything.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("ALLOWED_ORIGINS", "http://localhost:8081").split(","),
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)


class ScoreRequest(BaseModel):
    title: str = Field(default="", max_length=500)
    body: str = Field(..., max_length=20000)


class ScoreResponse(BaseModel):
    toxicity: float
    urgency: float
    predicted_category: str | None
    model_version: str
    suggested_action: str
    reasons: list[str]


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": "toxicity" in models}


@app.post("/score", response_model=ScoreResponse)
def score(request: ScoreRequest):
    text = f"{request.title}\n\n{request.body}".strip()[:MAX_CHARS]

    toxicity, labels = score_toxicity(text)
    urgency, urgency_reasons = score_urgency(text)
    category = predict_category(text)

    reasons = []
    if labels:
        reasons.append("toxicity signals: " + ", ".join(labels))
    reasons.extend(urgency_reasons)

    return ScoreResponse(
        toxicity=round(toxicity, 4),
        urgency=round(urgency, 4),
        predicted_category=category,
        model_version=MODEL_VERSION,
        suggested_action=suggest_action(toxicity, urgency),
        reasons=reasons,
    )


def score_toxicity(text: str) -> tuple[float, list[str]]:
    """
    Highest score across the model's labels.

    toxic-bert returns several labels (toxic, severe_toxic, obscene, threat,
    insult, identity_hate). The maximum is used rather than the average,
    because a post that is only a threat still needs a moderator's eyes.
    """
    if "toxicity" not in models:
        return 0.0, []

    results = models["toxicity"](text)
    scores = results[0] if isinstance(results[0], list) else results

    highest = 0.0
    fired = []
    for item in scores:
        if item["score"] > highest:
            highest = item["score"]
        if item["score"] > 0.5:
            fired.append(item["label"])

    return highest, fired


# Phrases suggesting someone is in danger now rather than describing the past.
# Deliberately conservative — this raises queue position, so a false positive
# costs a moderator thirty seconds, while a false negative could cost more.
URGENT_PATTERNS = [
    (r"\b(right now|happening now|as i write|currently)\b", 0.3, "present tense distress"),
    (r"\b(help me|need help|please help)\b", 0.25, "explicit request for help"),
    (r"\b(scared|terrified|afraid|frightened)\b", 0.2, "fear stated"),
    (r"\b(he is outside|following me|outside my|banging on)\b", 0.35, "immediate threat described"),
    (r"\b(tonight|today|this morning|an hour ago)\b", 0.15, "recent timeframe"),
    (r"\b(hit me|hurt me|threatened me|hit her|threatened her)\b", 0.3, "violence described"),
    (r"\b(nowhere to go|no money|no one to call|locked in)\b", 0.25, "no options stated"),
]


def score_urgency(text: str) -> tuple[float, list[str]]:
    """
    Rules, not a model.

    There is no good pretrained classifier for "is this person in danger right
    now", and an unreliable one here would be worse than transparent rules a
    moderator can reason about. Being able to say why a post was ranked high
    matters more than squeezing out extra accuracy.
    """
    lowered = text.lower()
    total = 0.0
    reasons = []

    for pattern, weight, label in URGENT_PATTERNS:
        if re.search(pattern, lowered):
            total += weight
            reasons.append(label)

    return min(total, 1.0), reasons


CATEGORY_HINTS = {
    "harassment": ["followed", "stalking", "catcall", "groped", "staring",
                   "harass", "touched me", "leering"],
    "domestic": ["husband", "in-laws", "marriage", "dowry", "at home",
                 "my family", "divorce", "sasural"],
    "workplace": ["manager", "office", "colleague", "hr ", "boss",
                  "workplace", "at work", "promotion"],
    "legal": ["fir", "police complaint", "lawyer", "court", "legal",
              "rights", "agreement", "case"],
    "health": ["period", "menstrual", "pad", "cup", "doctor", "gynae",
               "cramps", "pregnan"],
    "mental-health": ["anxious", "depress", "lonely", "panic", "therapy",
                      "counsell", "overwhelmed", "can't sleep"],
    "financial": ["money", "loan", "salary", "rent", "afford", "job",
                  "scheme", "savings"],
}


def predict_category(text: str) -> str | None:
    """
    Keyword scoring, offered only as a suggestion to pre-fill the form.

    A trained classifier would need labelled posts from this platform, which do
    not exist yet. Once there are a few thousand categorised posts, that is the
    obvious upgrade — and the honest version of this project says so rather
    than pretending the keyword list is a model.
    """
    lowered = text.lower()
    best, best_hits = None, 0

    for slug, keywords in CATEGORY_HINTS.items():
        hits = sum(1 for word in keywords if word in lowered)
        if hits > best_hits:
            best, best_hits = slug, hits

    return best if best_hits >= 2 else None


def suggest_action(toxicity: float, urgency: float) -> str:
    """
    A recommendation. The backend is free to ignore it, and does — nothing here
    hides content.

    FLAG   raise it in the queue for a moderator
    REVIEW worth a look when there is time
    NONE   no signal
    """
    if toxicity >= 0.85:
        return "FLAG"
    if urgency >= 0.6:
        return "FLAG"
    if toxicity >= 0.6 or urgency >= 0.4:
        return "REVIEW"
    return "NONE"
