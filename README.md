# URL Shortener & Link Analytics

> Confidential take-home submission. Keep this repository private.

A persistent URL-shortening service built with Java 17 and Spring Boot. It creates collision-free generated codes or caller-selected aliases, returns permanent redirects, and records server-observed redirect analytics.

## What is included

- `POST /shorten` for generated codes and custom aliases
- exact `301 Moved Permanently` redirects from `GET /{code}`
- `404` responses for unknown codes
- deterministic, collision-free Base62 codes backed by a database sequence
- deliberate idempotency for duplicate URLs and aliases
- URL and alias validation with Problem Details error responses
- redirect count and last-accessed analytics
- PostgreSQL 16 with Flyway migrations for deployment
- persistent H2 file storage for simple local development
- Java 17 multi-stage container, non-root runtime, health checks, and graceful shutdown
- Maven tests, a WSL/Linux smoke test, and GitHub Actions CI

## Fastest start: Rancher Desktop or Docker

Prerequisites:

- Rancher Desktop with the **Moby/dockerd** engine selected, or Docker Engine with Compose v2
- at least 2 GB of free memory for the build and database

From WSL or another Bash shell:

```bash
cp .env.example .env
# Change POSTGRES_PASSWORD in .env before using this outside local development.
docker compose up --detach --build --wait
./scripts/smoke-test.sh
```

The API is now available at `http://localhost:8080`. Check the stack or follow the application log with:

```bash
docker compose ps
docker compose logs --follow app
curl --fail http://localhost:8080/actuator/health/readiness
```

Rancher Desktop users should enable their chosen WSL distribution under Rancher Desktop's WSL integration settings. On a Windows-mounted checkout, the repository is commonly reached as `/mnt/c/path/to/url-shortener`.

Stop the services with:

```bash
docker compose down
```

`docker compose down` keeps the named PostgreSQL volume. To intentionally erase all link data, use `docker compose down --volumes`.

## Run directly with Java 17

The Maven wrapper downloads Maven automatically, but a Java 17 JDK must already be on `PATH`.

```bash
java -version
./mvnw clean verify
./mvnw spring-boot:run
```

On Windows PowerShell, use `./mvnw.cmd` instead of `./mvnw`. Direct execution uses a persistent H2 database at `./data/url-shortener`; Docker Compose uses PostgreSQL and Flyway. The two modes intentionally keep separate data.

## API

### Create a generated short link

```bash
curl --include \
  --header 'Content-Type: application/json' \
  --data '{"url":"https://example.com/articles/42?source=demo"}' \
  http://localhost:8080/shorten
```

New links return `201 Created`, a `Location` header, and a response like:

```json
{
  "code": "_15FTGg",
  "shortUrl": "http://localhost:8080/_15FTGg",
  "originalUrl": "https://example.com/articles/42?source=demo",
  "customAlias": false,
  "createdAt": "2026-07-15T06:30:00Z"
}
```

Submitting an equivalent generated URL again returns the existing mapping with `200 OK`.

### Create a custom alias

```bash
curl --include \
  --header 'Content-Type: application/json' \
  --data '{"url":"https://example.com/docs","customAlias":"team-docs"}' \
  http://localhost:8080/shorten
```

Aliases are case-sensitive, must contain 3-32 URL-safe characters, must start with a letter or digit, and may otherwise contain letters, digits, `-`, or `_`. Route names such as `api`, `shorten`, and `actuator` are reserved.

Reusing an alias for the same normalized URL is idempotent and returns `200`. Reusing it for a different URL returns `409 Conflict`. Different custom aliases may intentionally target the same URL.

### Redirect and inspect analytics

```bash
curl --head http://localhost:8080/team-docs
curl http://localhost:8080/api/links/team-docs
```

| Method and path | Success | Purpose |
| --- | --- | --- |
| `POST /shorten` | `201` new, `200` existing | Create or recover a mapping |
| `GET /{code}` | `301` | Redirect to the submitted original URL |
| `GET /api/links/{code}` | `200` | Read redirect count and last-accessed time |
| `GET /actuator/health/readiness` | `200` | Container/orchestrator readiness probe |

Invalid input returns `400`, an alias owned by another URL returns `409`, and an unknown code returns `404`. Error bodies use `application/problem+json` and include a stable `errorCode`.

## Deliberate behavior

### Collision avoidance

Every mapping first receives a unique ID from the datastore sequence. Generated codes are `_` plus that ID encoded in Base62. A database sequence cannot issue the same ID twice, Base62 is one-to-one for positive IDs, and the database also enforces a unique constraint on `code`. Custom aliases cannot start with `_`, so generated and custom namespaces cannot overlap.

The sequence begins at one billion to avoid very short, easily guessed initial values, but generated codes are still enumerable. This design prioritizes a simple collision proof over secrecy.

### Duplicate URLs

For generated codes, the service normalizes the scheme and host case, removes default ports, and resolves URI dot segments. Equivalent normalized URLs share one mapping. The submitted, trimmed URL is stored separately and is used unchanged in the redirect `Location` header.

The unique normalized-URL constraint and retry-after-conflict path make concurrent duplicate requests converge on the same mapping instead of leaking a database error.

### Validation and redirects

Only absolute `http` and `https` URLs with a valid host are accepted. User-info components are rejected and the maximum length is 2,048 characters. The service never fetches the destination URL.

Redirect analytics are server-observed. Because `301` responses may be cached by clients, later visits can bypass this service and therefore cannot be counted reliably without a different redirect/cache strategy.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `APP_PORT` | `8080` | Host port published by Compose |
| `SHORTENER_BASE_URL` | `http://localhost:8080` | Public base used in returned short URLs |
| `POSTGRES_DB` | `urlshortener` | Compose database name |
| `POSTGRES_USER` | `urlshortener` | Compose database user |
| `POSTGRES_PASSWORD` | `urlshortener-dev` | Compose password; change outside local use |
| `DATABASE_URL` | H2 locally | JDBC URL when running the application directly |
| `DATABASE_USERNAME` | `sa` locally | Direct-run database username |
| `DATABASE_PASSWORD` | empty locally | Direct-run database password |
| `H2_CONSOLE_ENABLED` | `false` | Enables the H2 console in direct-run mode |

When publishing behind a proxy or on another hostname, set `SHORTENER_BASE_URL` to the externally reachable origin, including `https://` when TLS terminates at the proxy.

## Persistence and migrations

The `postgres` Spring profile disables ad-hoc SQL initialization, enables Flyway, and asks Hibernate to validate rather than modify the schema. Migration files live under `src/main/resources/db/migration/postgresql`. Compose stores PostgreSQL data in the `postgres_data` named volume.

The container runtime uses Java 17, UID/GID `10001`, a read-only root filesystem, `/tmp` as `tmpfs`, and `no-new-privileges`. Both application and database have health checks; the application waits for a healthy database before starting.

## Tests and verification

Run the complete Java suite with:

```bash
./mvnw clean verify
```

The 20 automated tests cover Base62 boundaries, URL normalization and rejection, alias rules, generated-code uniqueness across repeated calls, custom-alias conflicts, eight-way concurrent duplicate creation, API response contracts, exact redirects, unknown links, and analytics updates.

For a running container stack:

```bash
./scripts/smoke-test.sh
BASE_URL=http://another-host:8080 ./scripts/smoke-test.sh
```

The smoke test creates a custom alias, verifies its `301` and `Location`, checks a `404`, and reads analytics. The Docker build itself executes `mvn clean verify`, and `.github/workflows/ci.yml` repeats the Java 17 verification and production-image build for pushes and pull requests.

## Project structure

```text
src/main/java/.../api       HTTP contracts and Problem Details errors
src/main/java/.../service   validation, code allocation, shortening, resolution
src/main/java/.../domain    JPA entity and repository
src/main/resources          local settings, PostgreSQL profile, Flyway migration
src/test                    unit and Spring integration tests
scripts/smoke-test.sh       live Linux/WSL API verification
Dockerfile                  tested Java 17 multi-stage image
compose.yaml                app + persistent PostgreSQL stack
WRITEUP.md                  required AI-use and trade-off write-up
```

## Known production gaps

This is a focused take-home service, not an internet-ready public shortener. Authentication, authorization, rate limiting, abuse/phishing controls, expiration, administrative deletion, custom domains/TLS, distributed caching, and production telemetry are intentionally outside the current scope. See [WRITEUP.md](WRITEUP.md) for the decisions and next steps.
