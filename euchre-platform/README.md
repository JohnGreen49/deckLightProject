# EuchreFlow

A multi-tenant, web-based platform for running **progressive euchre tournaments**. Players get a
personal live card telling them who they're partnered with and at which table each round; when one
player enters a game's score their partner's screen and the shared leaderboard update instantly.

> Status: **MVP foundation.** The full vertical slice works end-to-end — create a group, build a
> roster, generate a schedule with a pluggable algorithm, enter scores, and watch partners and the
> leaderboard update live over Server-Sent Events.

## Requirements coverage

| Goal | How it's met |
| --- | --- |
| Web-based, Java, Postgres, responsive UI | Spring Boot 3 (Java 21) REST/SSE API + React/TypeScript SPA + Postgres via Flyway/JPA |
| Per-player "who/where each round" + score entry | `PlayerPage` shows a card per round; score entry per table |
| Partner sees score immediately (event stream) | `EventBroker` fans out SSE events; a submitted score updates the partner's card and the leaderboard live |
| Realtime leaderboard | `LeaderboardPage` subscribes to the same stream |
| Pluggable pairing algorithm | `TournamentScheduler` strategy interface; drop in a `@Component` to add one |
| Default = standard hardcoded tables | `StandardCardScheduler` ships curated 4- and 8-player charts; `GeneratedScheduler` covers other sizes |
| Skinnable per group | `groups.theme` JSON → CSS variables via `applyTheme()` |
| Multi-group / multi-tenant | Every row is scoped by `group_id` |
| Evite integration | Deferred — Evite has no public guest API. Roster is CSV/manual import today, behind a seam a future `RosterProvider` can extend |

## Architecture

```
frontend/ (React + Vite + TS)          backend/ (Spring Boot)
  ┌──────────────┐   REST /api/**        ┌────────────────────────────┐
  │ Organizer    │ ───────────────────▶  │ Controllers                │
  │ Player       │                       │  → Services                │
  │ Leaderboard  │ ◀───────SSE────────── │  → Repositories (JPA)      │
  └──────────────┘  /tournaments/{id}    │  → Scheduling (pluggable)  │
                       /events           │  → EventBroker (SSE)       │
                                         └────────────┬───────────────┘
                                                      │
                                                 Postgres (Flyway)
```

- **Scheduling** (`com.euchreflow.platform.scheduling`) is the extensibility point. Algorithms work
  on abstract seat indices (0..N-1) so they're pure and unit-testable; the service maps indices to
  players. `SchedulerRegistry` auto-discovers every `TournamentScheduler` bean and falls back to the
  generated rotation when a chosen algorithm doesn't cover the player count.
- **Realtime** uses Server-Sent Events (one stream per tournament). Scores are published **after**
  the DB transaction commits, so subscribers never see a result that later rolls back. The broker is
  single-node today; to scale out, back `publish` with Postgres `LISTEN/NOTIFY` or Redis pub/sub.
- **Multi-tenancy** is row-scoped by `group_id`. Skinning is per-group theme JSON applied as CSS
  custom properties.

## Running locally

### 1. Postgres

```bash
docker compose up -d db     # or use your own Postgres
```

Defaults (override with env vars): DB `euchreflow`, user/password `euchreflow`, port 5432.

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run       # or: mvn spring-boot:run
# API on http://localhost:8080 ; Flyway migrates the schema on startup
```

Config lives in `backend/src/main/resources/application.yml` and reads `DB_URL`, `DB_USER`,
`DB_PASSWORD`, `CORS_ORIGINS`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173 , proxies /api to :8080
```

### Try it

1. Open the **Organizer** console → create a group → add ≥4 players (note their join codes) →
   create a tournament → select players → **Generate schedule**.
2. Open **Play** in another tab, enter a player's join code → see their per-round card.
3. Enter a score as one player; their partner's card and the **leaderboard** update instantly.

## Tests

```bash
cd backend && mvn test       # scheduler correctness: every player partners every other once,
                             # no double-seating, requested-round truncation, input validation
```

## API sketch

| Method & path | Purpose |
| --- | --- |
| `POST /api/groups` | Create a tenant |
| `POST /api/groups/{id}/players` · `.../players/import` | Add / bulk-import roster |
| `POST /api/groups/{id}/tournaments` | Create a tournament |
| `POST /api/tournaments/{id}/schedule` | Enroll players + generate rounds |
| `GET /api/tournaments/{id}/schedule` | Organizer full view |
| `GET /api/tournaments/{id}/players/{pid}/schedule` | A player's cards |
| `POST /api/seatings/{id}/score` | Submit / correct a result |
| `GET /api/tournaments/{id}/leaderboard` | Standings |
| `GET /api/tournaments/{id}/events` | SSE live stream |
| `GET /api/join/{code}` | Resolve a player from a join code |
| `GET /api/schedulers` | List available pairing algorithms |

## Adding a scheduling algorithm

```java
@Component
public class MyScheduler implements TournamentScheduler {
    public String id() { return "my-scheduler"; }
    public String displayName() { return "My rotation"; }
    public boolean supports(int playerCount) { return playerCount % 4 == 0; }
    public Schedule build(ScheduleRequest req) { /* return rounds of TableAssignment (seat indices) */ }
}
```

It appears automatically in the organizer's algorithm dropdown and is selectable per tournament.

## Roadmap

- Organizer authentication (magic-link email) and per-group access control
- `RosterProvider` seam with a Google/Apple/CSV importer (and Evite if their data becomes accessible)
- Configurable scoring rules per group (points vs. hands-won vs. margin)
- Horizontal-scale event bus (Postgres `LISTEN/NOTIFY` / Redis)
- Theme editor UI (logo upload, color pickers) instead of raw JSON
