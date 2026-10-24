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

This runs [`db/schema.sql`](db/schema.sql) with `psql`, which creates the `mpglogger` schema and the
`fuel_log` table inside it. Without `psql`, run that file in DBeaver (**Alt+X**, Execute SQL Script)
or the Neon console's **SQL Editor** instead. On startup Hibernate checks that the tables match the
`FuelLog` entity (`ddl-auto=validate`) and fails fast if they don't.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens on http://localhost:8080. Run the tests with `.\mvnw.cmd test`.

## API

| Method | Path | Description |
|---|---|---|
| GET | `/api/fuel-logs` | All logs, highest odometer first |
| POST | `/api/fuel-logs` | Add a log |
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

Each log in a response also includes the calculated `milesDriven`, `mpg` and `totalCost`.
