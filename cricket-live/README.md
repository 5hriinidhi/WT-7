# CricketLive

Real-time cricket score management system. Spring Boot REST API + MySQL (XAMPP) + a responsive
dashboard that updates live without refreshing.

## What it does

- Matches: create, start, list (live / upcoming / finished)
- Ball-by-ball scoring engine: runs, wides, no-balls, byes, leg byes, wickets, strike rotation,
  over changes, bowler rotation, innings change, target and chase, result (won by runs / wickets / tie)
- Live push to every open browser using Server-Sent Events (no polling)
- Player statistics: per match and career, plus top run scorers / wicket takers
- Match summary: result, both innings, top batters and bowlers
- A built-in simulated feed that plays matches ball by ball (the "fetch" step)

## Tech

Java 17+, Spring Boot 3.3, Spring Data JPA (Hibernate), MariaDB driver (XAMPP's MySQL),
plain HTML / CSS / JavaScript dashboard (no build step).

## Run it (XAMPP)

1. Install JDK 17 or newer. Check with `java -version`.
2. Open the XAMPP Control Panel and click **Start** next to **MySQL**. Apache is not needed.
3. Open this folder in IntelliJ IDEA (or Eclipse / VS Code with the Java extensions) and wait for
   Maven to download the dependencies.
4. Run `CricketLiveApplication` (the class with `main`).
   Or, if Maven is installed, from this folder: `mvn spring-boot:run`
5. Open <http://localhost:8080>. A T20 is already being played by the simulator.

The database `cricket_db` and all tables are created automatically on first start, and demo data
(4 teams, 44 players, 3 matches) is inserted when the tables are empty.

### No XAMPP handy?

`mvn spring-boot:run -Dspring-boot.run.profiles=h2` runs everything on an in-memory database.

### Settings you may need to change (`src/main/resources/application.properties`)

| Problem | Change |
|---|---|
| You set a password for `root` in XAMPP | `spring.datasource.password=yourpassword` |
| MySQL runs on another port (shown in XAMPP) | change `3306` in `spring.datasource.url` |
| Port 8080 is busy | `server.port=8081` |
| Simulator too fast / slow | `cricket.simulator.interval-ms=1500` (milliseconds per ball) |
| Tables were not created | create an empty database `cricket_db` in phpMyAdmin (start Apache for it) and run again |

## REST API

| Method | URL | Purpose |
|---|---|---|
| GET | `/api/matches?status=LIVE&page=0&size=20` | list matches (status optional) |
| GET | `/api/matches/{id}` | full live scoreboard |
| GET | `/api/matches/{id}/summary` | match summary (cached once finished) |
| GET | `/api/matches/{id}/balls?limit=30` | ball-by-ball commentary, newest first |
| POST | `/api/matches` | create a match |
| POST | `/api/matches/{id}/start` | start a match |
| POST | `/api/matches/{id}/balls` | score one delivery |
| PUT | `/api/matches/{id}/simulation?enabled=true` | switch auto-play on/off |
| GET | `/api/teams` | teams |
| GET | `/api/players?teamId=1&q=aar` | players (both filters optional) |
| GET | `/api/players/{id}` | career statistics |
| GET | `/api/stats/top-scorers?limit=5` | run leaderboard |
| GET | `/api/stats/top-wicket-takers?limit=5` | wicket leaderboard |
| GET | `/api/stream?matchId={id}` | Server-Sent Events: `card` (list updates) and `score` (that match) |
| GET | `/actuator/health` | health check |

Examples:

```
POST /api/matches
{ "team1Id": 1, "team2Id": 2, "venue": "Test Ground", "totalOvers": 5, "simulated": false }

POST /api/matches/4/balls
{ "runs": 4 }                              four off the bat
{ "runs": 0, "wicket": true }              wicket
{ "runs": 0, "extraType": "WIDE" }         wide (1 run added automatically)
{ "runs": 1, "extraType": "LEG_BYE" }      leg bye
```

Errors come back as JSON `{status, error, message}` with 400 (bad input), 404 (not found) or
409 (e.g. scoring a match that is not live).

## How it works

```
 ScoreFeed (simulator) ──► ScoringService ──► MySQL
 Dashboard "Scorer" ─────►       │
                                 └─ MatchUpdatedEvent (after commit)
                                          │
                                 LiveUpdateBroadcaster ──SSE──► every open browser
```

- `ScoringService.recordBall` is one transaction per delivery and takes a row lock on the match, so
  simultaneous scorers can never corrupt a score.
- The event is sent only after the commit, so a browser never sees half-written data.
- Finished-match summaries are cached; list endpoints are paged and fetch innings in one query.
- To use a real data provider, add another `ScoreFeed` implementation that calls its API and maps
  each delivery to a `BallRequest`.

### Scaling further

- Run several instances behind a load balancer (the app is stateless). SSE clients are held per
  instance, so add Redis pub/sub (or a message broker) to fan updates out across instances.
- Replace the in-memory cache with Redis, and move `ddl-auto=update` to Flyway migrations.

## Project layout

```
src/main/java/com/cricketlive
  model/        JPA entities (Team, Player, CricketMatch, Innings, BallEvent, PlayerMatchStats)
  repository/   Spring Data repositories
  service/      ScoringService (engine), ScoreboardService (read side), MatchService, PlayerService
  controller/   REST controllers + SSE endpoint
  live/         LiveUpdateBroadcaster (Server-Sent Events)
  feed/         ScoreFeed, SimulatedScoreFeed, MatchSimulator
  seed/         DataSeeder (demo data)
  exception/    error handling
src/main/resources/static/index.html   dashboard
src/test/java/...                      tests (run on an in-memory database)
```

## Simplifications

- No login: anyone who can reach the API can score. Add Spring Security before exposing it.
- No toss (team 1 always bats first), no free hits, run-outs are not modelled, bowlers rotate
  automatically, and the batting order is the order players were added to the team.
