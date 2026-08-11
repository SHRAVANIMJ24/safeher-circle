# SafeHer Circle

A women's support platform combining three things that usually live in separate apps: an anonymous community board, an emergency alarm, and a directory of places to get help.

## Why

Existing tools solve one piece each. Safety apps send SOS alerts but have no community. Forums offer support but no way to actually reach someone. Helpline lists exist but are hard to find at the moment you need one. This project puts them in one place, with location awareness so help comes from nearby — and without ever attaching a real name to anything published.

## Features

### Anonymous community board
Everyone writes under a generated handle like `calm-maple-3867`. Posts are filed under eight categories and tagged with an area, and can be filtered by category, city or free-text search. Replies thread one level deep. The original poster is badged on her own replies, so when someone returns to say what worked, it is visible.

Reading the board does not require an account.

### Emergency alarm
Three alarm sounds, generated in the browser with the Web Audio API rather than loaded as files, so they work with no signal. A siren that carries outdoors, a sharp pulse like a smoke alarm, and a low warble that travels through walls.

The sound and the alert are separate buttons. Someone in a stairwell may want noise and nobody told; someone being followed quietly may want her sister to know without a siren announcing it.

### Trusted contacts
Up to five people, contacted in priority order when an alert is raised, with a map link to the exact location. One tap marks the person safe and closes the alert. Pressing the button repeatedly does not raise five alerts — the live one is returned and its location updated.

### Fake incoming call
Schedules the phone to ring after a chosen delay, with a full-screen call display and a generated ringtone. The purpose is an excuse to leave a conversation without having to announce that you feel unsafe, which is often the thing that escalates a situation.

Nothing is sent anywhere and nothing is recorded.

### Directory of help
Helplines, NGOs, shelters and legal aid, filtered by city. National numbers are pinned to the top and never filtered out, because 112 works everywhere.

Every entry carries a verification status. Numbers taken from the National Commission for Women's own published list are marked verified; organisation entries we have not independently confirmed say so plainly on the card. A wrong number here is worse than an empty table — someone dials it, gets nothing, and loses time she may not have.

### Reporting and moderation
Any post or comment can be reported. Three separate reports mark content as flagged, which raises it in the moderation queue and changes nothing a reader sees. Content is only hidden when a moderator decides.

Moderators never see who reported something. On a board where women write about abusive partners, "who reported me" is a question that gets people hurt.

### Automated content scoring
A separate FastAPI service scores comments with `unitary/toxic-bert`. What it does and, more importantly, what it turned out not to do is in its own section below.

## What we measured about the model

We tested the classifier against content representative of this platform before deciding where to use it.

| Content | Toxicity | Model's suggestion |
|---|---|---|
| First-person account of workplace harassment | 0.0007 | NONE |
| First-person account of a sexual assault | 0.0021 | NONE |
| Notice about a free sanitary pad distribution | 0.0006 | NONE |
| A reply reading "you are a stupid liar... shut up" | 0.9851 | FLAG |

The model detects **abusive language directed at a person**. It has no notion of whether a *situation* is distressing.

An account of a sexual assault and a notice about free pads differ by 0.0015 — indistinguishable. Either differs from an insult by three orders of magnitude.

This is not a flaw in the model; it is doing exactly what it was trained to do. But it means the obvious design — routing posts through a toxicity classifier to surface people who need help — would surface nobody, while confidently policing tone.

**What we did about it:**

- **Scoring runs on comments, not posts.** Posts here are women describing what happened to them, in measured prose the model cannot read. Comments are where people attack each other, which it reads accurately.
- **Nothing is hidden automatically.** A high score sets a comment to FLAGGED, which raises it in the moderation queue and changes nothing a reader sees.
- **The moderator sees the number, not a verdict.** The queue shows `toxicity 0.94` rather than "flagged by AI", so she judges the model rather than deferring to it.

## Privacy design

Three decisions worth knowing about:

**Location is stored coarsely on posts and precisely on alerts.** A post's coordinates are rounded to two decimal places, roughly a 1.1km square. An alert's are exact. Same app, opposite defaults: for a post, precision endangers the author; for an alert, precision is the entire point.

**Login errors are deliberately identical.** "No such account" and "wrong password" return the same message, so the login form cannot be used to discover which women have accounts here.

**Handles are permanent and generated, never chosen.** A user's email lives only in the `users` table. Everything public references the handle, which is copied onto each post so a thread stays readable after someone deletes their account.

## Tech stack

| Layer | Choice |
|---|---|
| Frontend | React 19 (Vite), React Router, plain CSS |
| Backend | Java 21, Spring Boot 4.1, Spring Security |
| Database | PostgreSQL 18 |
| Auth | JWT, bcrypt |
| Audio | Web Audio API — no audio files |
| Scoring service | Python, FastAPI, Hugging Face Transformers |

## Project structure

```
├── frontend/      React app
├── backend/       Spring Boot REST API
├── ml-service/    FastAPI content scoring
└── docs/          Schema, deployment notes, seed data
```

## Running it locally

**Requirements:** Node 18+, Java 21, Maven, PostgreSQL 14+, Python 3.10+

```bash
git clone https://github.com/SHRAVANIMJ24/safeher-circle.git
cd safeher-circle
```

**Database**
```bash
createdb -U postgres safeher
psql -U postgres -d safeher -f docs/schema.sql
psql -U postgres -d safeher -f docs/directory-seed.sql
```

**Backend**
```bash
cd backend
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# fill in your database password and a JWT secret of 32+ characters
./mvnw spring-boot:run
```

**Frontend**
```bash
cd frontend
npm install
echo "VITE_API_URL=http://localhost:8081" > .env
npm run dev
```

**Scoring service** — optional; set `safeher.scoring.enabled=false` to skip it
```bash
cd ml-service
python -m venv venv
source venv/Scripts/activate     # or: source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 8000
```

The first run downloads about 400MB of model weights.

| Service | Port |
|---|---|
| Frontend | 5173 |
| Backend | 8081 |
| Scoring service | 8000 |

**Seed some posts**
```bash
bash scripts/seed-posts.sh
```

**Make yourself a moderator** — there is no UI for this on purpose
```bash
psql -U postgres -d safeher -c "UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';"
```
Sign out and back in afterwards; the role is baked into the token.

## Limitations, stated rather than hidden

**No SMS is actually sent.** `LoggingAlertSender` writes the message to the server log. Alerts reach nobody until a real provider is wired in. The interface exists so that swap is a single class.

**The model is trained on English.** Hinglish, Marathi and Devanagari script score unpredictably. On a platform aimed at Indian users this is a serious gap, not a footnote.

**Category prediction is keyword matching, not machine learning.** A trained classifier needs labelled posts from this platform, which do not exist yet.

**Most directory entries are unverified.** Only the national helplines have been checked against a primary source. The UI says so on each card.

**This is a student project, not an emergency service.** In an emergency in India, dial **112**.

## License

MIT
