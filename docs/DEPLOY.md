# Deploying SafeHer Circle

Three services, all on permanent free tiers, no credit card:

| Piece | Host | What it costs |
|---|---|---|
| Database | Neon | Free, 0.5 GB, no expiry |
| Backend | Render | Free, sleeps after 15 min idle |
| Frontend | Vercel | Free |

Set them up in this order — the backend needs the database URL, and the
frontend needs the backend URL.

---

## Before you start

Add these files to your repo:

| File | Goes in |
|---|---|
| `Dockerfile` | `backend/` |
| `.dockerignore` | `backend/` |
| `application-prod.properties` | `backend/src/main/resources/` |

Commit and push them. The hosts read from GitHub, so anything not pushed
does not exist as far as they are concerned.

---

## 1. Database — Neon

1. Go to **neon.com** and sign up with GitHub.
2. Create a project. Name it `safeher`. Pick the region closest to you —
   Singapore or Mumbai if you are in India.
3. On the dashboard, find **Connection string** and copy it. It looks like:

   ```
   postgresql://neondb_owner:AbC123xyz@ep-cool-name-12345.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```

4. Pull it apart — you need the three pieces separately:

   | Piece | Where it is in that string |
   |---|---|
   | User | between `//` and `:` → `neondb_owner` |
   | Password | between `:` and `@` → `AbC123xyz` |
   | Host + database | after `@` → `ep-cool-name-12345.ap-southeast-1.aws.neon.tech/neondb` |

5. Build the JDBC URL your backend needs, by putting `jdbc:postgresql://`
   in front of the host and keeping the SSL parameter:

   ```
   jdbc:postgresql://ep-cool-name-12345.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```

   Keep all three values somewhere for the next step.

6. Load your schema. In the Neon dashboard open the **SQL Editor**, paste the
   whole contents of `docs/schema.sql`, and run it. You should see the tables
   appear under **Tables** in the sidebar.

---

## 2. Backend — Render

1. Go to **render.com**, sign up with GitHub.
2. **New** → **Web Service** → connect your `safeher-circle` repo.
3. Settings:

   | Field | Value |
   |---|---|
   | Name | `safeher-api` |
   | Language | **Docker** |
   | Branch | `main` |
   | Root Directory | `backend` |
   | Instance Type | **Free** |

4. Under **Environment Variables**, add these six:

   | Key | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DATABASE_URL` | the `jdbc:postgresql://...` string from step 1 |
   | `DATABASE_USER` | `neondb_owner` |
   | `DATABASE_PASSWORD` | your Neon password |
   | `JWT_SECRET` | a long random string — run `openssl rand -base64 48` |
   | `CORS_ORIGINS` | `http://localhost:5173` for now; corrected in step 4 |

5. **Create Web Service.** The first build takes 5–10 minutes because Maven
   downloads everything. Watch the log for `Started BackendApplication`.

6. You get a URL like `https://safeher-api.onrender.com`. Test it:

   ```
   https://safeher-api.onrender.com/api/categories
   ```

   Eight categories in JSON means the backend and database are talking.

---

## 3. Frontend — Vercel

1. Go to **vercel.com**, sign up with GitHub, **Add New** → **Project**,
   import `safeher-circle`.
2. Settings:

   | Field | Value |
   |---|---|
   | Framework Preset | **Vite** |
   | Root Directory | `frontend` |

3. Under **Environment Variables**, add one:

   | Key | Value |
   |---|---|
   | `VITE_API_URL` | `https://safeher-api.onrender.com` — no trailing slash |

4. **Deploy.** Takes about a minute. You get a URL like
   `https://safeher-circle.vercel.app`.

---

## 4. Point them at each other

The backend is still refusing requests from your Vercel domain, because
`CORS_ORIGINS` says localhost.

1. Back in Render → your service → **Environment**.
2. Change `CORS_ORIGINS` to your Vercel URL:

   ```
   https://safeher-circle.vercel.app
   ```

   No trailing slash. To allow local development too, comma-separate them:

   ```
   https://safeher-circle.vercel.app,http://localhost:5173
   ```

3. Save. Render redeploys automatically.

---

## 5. Seed it and make yourself a moderator

Register an account through the live site, then in Neon's SQL Editor:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
```

Sign out and back in — the role lives inside the token.

To seed posts, edit `scripts/seed-posts.sh` and change the `API` line to your
Render URL and `EMAIL`/`PASSWORD` to the account you just made, then run it.

---

## Known limitations, worth saying out loud

**The backend sleeps.** Render's free tier spins down after 15 minutes of no
traffic, and the next request takes 30–60 seconds to wake it. If you are
demonstrating this to someone, open the link a minute beforehand. For a real
safety application this would be disqualifying, and that is the honest reason
this is a student project rather than a service anyone should rely on.

**The database sleeps too**, after 5 minutes — but Neon wakes in well under a
second, so you will not notice.

**Free Postgres has no automated backups.** Do not put anything in it you
would mind losing.

**Nothing here sends real SMS.** `LoggingAlertSender` writes the message to
the server log. Alerts reach nobody until a real provider is wired in.

---

## When something goes wrong

**Build fails on Render** — open the log and read the first error, not the
last. Maven prints a wall of text after the real failure.

**`/api/categories` returns 500** — the database connection is wrong. Check
`DATABASE_URL` starts with `jdbc:postgresql://` and ends with `?sslmode=require`.

**Site loads but every request fails** — open the browser console. A CORS
error means `CORS_ORIGINS` does not exactly match your Vercel URL. It is
almost always a trailing slash or `http` where it should be `https`.

**"Could not reach the server"** — the backend is asleep. Wait a minute and
reload.

**Login works, then everything 401s** — `JWT_SECRET` changed between deploys,
which invalidates every existing token. Sign in again.
