package com.chandan.mpgloggerbackend.fuellog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Body of POST /api/fuel-logs and PUT /api/fuel-logs/{id}.
 * POST: previousOdometer is only read for the very first log; after that the server
 * uses the highest existing odometer reading, just like the Swift app.
 * PUT: previousOdometer is only read when editing the oldest log; every other log
 * keeps starting where the log before it ended.
 *
 * @param date             the day of the fill-up
 * @param previousOdometer where the tank started; optional (see above)
 * @param odometer         the odometer reading at this fill-up
 * @param gallons          gallons pumped
 * @param pricePerGallon   price paid per gallon, in dollars
 */
public record FuelLogRequest(
        @NotNull LocalDate date,
        @Positive Double previousOdometer,
        @NotNull @Positive Double odometer,
        @NotNull @Positive Double gallons,
        @NotNull @Positive Double pricePerGallon) {
}
