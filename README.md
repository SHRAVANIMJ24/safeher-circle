# SafeHer Circle

A women's support platform combining three things that usually live in separate apps: an anonymous community board, a local mutual-aid donation exchange, and a one-tap emergency alarm.

> **Status:** In development. MVP in progress.

## Why

Existing tools solve one piece each. Safety apps send SOS alerts but have no community. Forums offer support but no way to actually get someone help or supplies. Donation platforms handle money but not neighbour-to-neighbour need. This project puts all three in one place, with location awareness so help comes from nearby.

## Features

### MVP
- **Anonymous posting** — Share experiences and ask for help under an auto-generated handle. Posts are tagged by category and by area, never by exact address.
- **Category and location filtering** — Browse posts relevant to your city and situation.
- **Panic alarm** — One tap plays a loud siren, a scream, or a fake male voice, depending on which is safer in the moment. Works offline.
- **Trusted contacts** — Up to five people get an SMS with your live location when you trigger an alert. One tap marks you safe and stops it.

### Planned
- Fake incoming call, to give you a reason to leave a situation
- Donation board for pads, sanitary supplies, clothes, and baby items — matched by area
- Verified NGO and helpline directory by city
- Report and moderation queue, with automated flagging
- Community safety map built from anonymised reports
- Hindi and Marathi interface

## Tech stack

| Layer | Choice |
|---|---|
| Frontend | React (Vite), Tailwind CSS, React Router |
| Backend | Java 17, Spring Boot 3, Spring Security |
| Database | PostgreSQL with PostGIS |
| Auth | JWT |
| Maps | Leaflet + OpenStreetMap |
| Notifications | Twilio (SMS) |
| Moderation service | Python FastAPI (separate microservice) |

## Project structure

```
├── frontend/      React app
├── backend/       Spring Boot REST API
├── ml-service/    Content moderation and classification
└── docs/          Architecture notes and API spec
```

## Running it locally

**Requirements:** Node 18+, Java 17+, Maven, PostgreSQL 14+

```bash
git clone https://github.com/YOUR-USERNAME/safeher-circle.git
cd safeher-circle
```

Database:
```bash
createdb safeher
psql safeher < docs/schema.sql
```

Backend:
```bash
cd backend
cp src/main/resources/application-example.properties src/main/resources/application.properties
# fill in your database credentials
mvn spring-boot:run
```

Frontend:
```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

The app runs at `http://localhost:5173`, the API at `http://localhost:8080`.

## Configuration

Secrets go in `.env` files and are never committed. You'll need:

| Variable | Used for |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Database connection |
| `JWT_SECRET` | Signing auth tokens |
| `TWILIO_SID`, `TWILIO_TOKEN`, `TWILIO_PHONE` | Sending SOS messages |

## A note on safety

This is a student project, not an emergency service. It does not replace calling the police. In India, dial **112** for emergencies or **181** for the women's helpline.

## Contributing

Issues and pull requests are welcome. Branch naming: `feature/short-description`. Commits follow `feat:`, `fix:`, `docs:`, `refactor:`.

## License

MIT
