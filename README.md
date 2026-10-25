# MPG Logger – Backend

Spring Boot REST API for MPG Logger, migrated from the original SwiftUI/SwiftData iPad app.
Data is stored in a PostgreSQL database hosted on [Neon](https://neon.tech).
Tests use a throwaway in-memory H2 database and never touch Neon.

## Database setup

The app reads its connection from three environment variables:

| Variable | Value |
|---|---|
| `DB_URL` | `jdbc:postgresql://ep-falling-hat-b52u8qyv-pooler.c-7.us-east-2.aws.neon.tech/mpgloggerdb?sslmode=require` |
| `DB_USERNAME` | `mpgloggerdb_owner` |
| `DB_PASSWORD` | your Neon password (never commit it) |

Set them for the current PowerShell window:

```powershell
$env:DB_URL = "jdbc:postgresql://ep-falling-hat-b52u8qyv-pooler.c-7.us-east-2.aws.neon.tech/mpgloggerdb?sslmode=require"
$env:DB_USERNAME = "mpgloggerdb_owner"
$env:DB_PASSWORD = "<your password>"
```

In IntelliJ, add the same three under **Run > Edit Configurations > Environment variables**.

Then create the tables once (safe to re-run):

```powershell
.\scripts\db-setup.ps1
```

This runs [`db/schema.sql`](db/schema.sql) with `psql`, which creates the `mpglogger` schema and its
tables: `app_user`, `user_session` and `fuel_log`. Without `psql`, run that file in DBeaver
(**Alt+X**, Execute SQL Script) or the Neon console's **SQL Editor** instead. On startup Hibernate
checks that the tables match the entities (`ddl-auto=validate`) and fails fast if they don't.

### Upgrading a database from before accounts existed

`schema.sql` is for a new, empty database. A database that already has fuel logs but no users is
upgraded in two steps, so the existing logs end up on your account:

1. Before deploying this version, add the new tables (safe to re-run):

   ```powershell
   .\scripts\db-setup.ps1 -SqlFile db\migrations\001-add-users.sql
   ```

   Existing logs get an empty `user_id` for now, so they're hidden until step 2.

2. Deploy, sign up, then right away (before logging any fuel) give the existing logs to your account:

   ```powershell
   .\scripts\db-setup.ps1 -SqlFile db\migrations\002-claim-existing-logs.sql
   ```

   This only claims logs while exactly one account exists, then makes `user_id` required.

## Accounts and sessions

- Usernames are unique and case-insensitive (stored lowercase): 3–30 letters, numbers, `.` or `_`.
- Passwords are hashed with BCrypt (`spring-security-crypto`); they are never stored or returned.
  8 characters minimum, 72 bytes maximum (BCrypt's limit).
- Signing in creates a row in `user_session` and sets an `mpg_session` cookie: random 256-bit token,
  `HttpOnly`, `SameSite=Lax`, `Path=/api`, `Secure` over HTTPS, `Max-Age` 400 days (the most browsers allow).
  Only the token's SHA-256 is stored. The expiry moves forward (at most daily) while the app is used,
  so in practice you stay signed in, across browser restarts and server redeploys, until you sign out.
- Every `/api` endpoint except sign-up/in/out needs a session (`AuthInterceptor`), and each user
  only ever sees their own fuel logs. Another user's log answers `404`, the same as a missing one.
- `server.forward-headers-strategy=framework` lets the app see HTTPS behind a hosting proxy.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens on http://localhost:8080 (or on `$PORT` if that is set). Run the tests with
`.\mvnw.cmd test`. `.\mvnw.cmd verify` also writes a JaCoCo coverage report to
`target/site/jacoco/index.html`.

## Hosting

The app runs on Render ($7 a month): this API as an always-on web service, and the React app as a
free static site that forwards `/api` to it. [`render.yaml`](render.yaml) describes both, and Render
redeploys them on every push to `develop`. [`HOSTING.md`](HOSTING.md) walks through the setup step by
step. The [`Dockerfile`](Dockerfile) builds the API's container; to try it locally (with Docker Desktop running):

```powershell
docker build -t mpg-logger-backend .
docker run -p 8080:8080 -e DB_URL=$env:DB_URL -e DB_USERNAME=$env:DB_USERNAME -e DB_PASSWORD=$env:DB_PASSWORD mpg-logger-backend
```

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/signup` | `{username, name, password}` → `201`, signs in. `409` if the username is taken |
| POST | `/api/auth/signin` | `{username, password}` → `200`, signs in. `401` "Wrong username or password." |
| POST | `/api/auth/signout` | Ends this browser's session → `204` |
| GET | `/api/account` | The signed-in user `{id, username, name}`, or `401` |
| PUT | `/api/account` | `{name}` → updated user (only the name can change for now) |
| GET | `/api/fuel-logs` | All of your logs, highest odometer first |
| POST | `/api/fuel-logs` | Add a log |
| PUT | `/api/fuel-logs/{id}` | Edit a log |
| DELETE | `/api/fuel-logs/{id}` | Delete a log |

POST body:

```json
{
  "date": "2026-10-23",
  "previousOdometer": 12000,
  "odometer": 12300,
  "gallons": 10.2,
  "pricePerGallon": 3.49
}
```

`previousOdometer` is only used for the very first log. After that the server uses the
odometer of the log with the highest reading, the same as the iPad app did.

Rules (from the Swift app): previous odometer > 0, odometer > previous odometer,
gallons > 0, price > 0. Breaking one returns `400` with a `detail` message.

PUT takes the same body. `previousOdometer` is only used when editing the oldest log; every
other log keeps starting where the log before it ended. If the odometer changes, the next log's
previous odometer is moved to the new reading, so its miles driven and MPG update too. The new
odometer must stay below the next log's odometer, or the edit is rejected with `400`.

Each log in a response also includes the calculated `milesDriven`, `mpg` and `totalCost`.
