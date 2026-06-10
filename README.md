# Template Progetto Skillshare
Progetto di Ingegneria del Software \
Corso di Laurea in Informatica per il management \
Anno accademico 2025/2026

# Skillshare

A peer-to-peer platform for **exchanging skills** between users, built as a Software Engineering project for the Corso di Laurea in Informatica per il Management, University of Bologna (A.Y. 2025/2026).

Skillshare is structured as a barter economy rather than a traditional e-learning store: every member is both a student and a teacher. Users publish announcements offering a skill they can teach in exchange for one they want to learn, request exchanges from one another, coordinate through an internal chat, and review each other afterwards.

## Features

- **Identity & Profile** — registration, login, editable profile (bio, skill tags, profile photo).
- **Skill Marketplace** — publish, edit and remove announcements; browse and search active ones.
- **Interaction & Coordination** — send exchange requests, accept or reject received ones, and chat privately once a request is accepted.
- **Reputation & Feedback** — leave a 1–5 review after an accepted exchange and view each user's public rating.

## Technology stack

GWT 2.13 · Maven · MapDB · JUnit 5 + Mockito · Jetty 12 (EE10) · Docker · Java 17

## Project structure

```
app/
├── skillshare-shared/   Shared data objects and RPC interfaces
├── skillshare-server/   Backend (services, repositories, managers, database)
└── skillshare-client/   Frontend (GWT entry point and screens)
artefatti/               Project artifacts (manuals, project diary, etc.)
```

## Quick start

> All commands run from the `app/` directory.

### With Docker

```sh
cd app
docker compose up -d --build
```

Then open http://localhost:8080/

### Locally (development mode)

Requires **JDK 17+** and **Maven 3.8+**. Run the two processes in two separate terminals:

```sh
# Terminal 1 — GWT Code Server (frontend)
mvn gwt:codeserver -pl *-client -am

# Terminal 2 — Jetty server (backend)
mvn jetty:run -pl *-server -am -Denv=dev
```

Then open http://localhost:8080/

### Tests

```sh
mvn test
```

## Demo account

| Email | Password |
|-------|----------|
| `test@unibo.it` | `1234` |

New accounts can also be created from the registration screen.

## Documentation

Detailed setup instructions are in [`app/README.md`](app/README.md). The user manual, developer manual and project diary are in the [`artefatti/`](artefatti/) folder.