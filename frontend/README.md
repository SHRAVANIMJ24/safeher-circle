# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.

## Automated content scoring

A FastAPI service (`ml-service/`) scores content with
[`unitary/toxic-bert`](https://huggingface.co/unitary/toxic-bert). It runs on
comments only, and it never removes anything.

### What we measured

We tested the model against content representative of this platform:

| Content | Toxicity | Model's suggestion |
|---|---|---|
| First-person account of workplace harassment | 0.0007 | NONE |
| First-person account of a sexual assault | 0.0021 | NONE |
| Notice about a free sanitary pad distribution | 0.0006 | NONE |
| A reply reading "you are a stupid liar... shut up" | 0.9851 | FLAG |

### What that means

The model detects **abusive language directed at a person**. It has no notion
of whether a *situation* is distressing.

An account of a sexual assault and a notice about free pads differ by 0.0015 —
indistinguishable. Either differs from an insult by three orders of magnitude.

This is not a flaw in the model. It is doing precisely what it was trained to
do. But it means a common design — routing posts through a toxicity classifier
to find people who need help — would find nobody, while confidently policing
tone.

### What we did about it

**Scoring runs on comments, not posts.** Posts here are women describing what
happened to them, usually in measured prose, which the model cannot read.
Comments are where people attack each other, which the model reads well.

**Nothing is hidden automatically.** A high score sets a comment's status to
FLAGGED, which raises it in the moderation queue and changes nothing a reader
sees. A person decides what happens to it.

**The moderator sees the number, not a verdict.** The queue shows "toxicity
0.98" rather than "flagged by AI", so a moderator judges the model rather than
deferring to it.

### Known limitations

- The model is trained on English. Hinglish, Marathi and Devanagari script
  score unpredictably. On a platform aimed at Indian users this is a serious
  gap, not a footnote.
- Category prediction is keyword matching, not a trained classifier. A real one
  needs labelled posts from this platform, which do not exist yet.
- Urgency scoring is rule-based, and deliberately so: transparent rules a
  moderator can read beat an unreliable model she cannot.
