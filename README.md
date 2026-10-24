# MPG Logger – Backend

Spring Boot REST API for MPG Logger, migrated from the original SwiftUI/SwiftData iPad app.
Data is stored in a local H2 database file (`./data/mpglogger.mv.db`).

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
