package com.chandan.mpgloggerbackend.fuellog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Body of POST /api/fuel-logs.
 * previousOdometer is only read for the very first log; after that the server
 * uses the highest existing odometer reading, just like the Swift app.
 */
public record FuelLogRequest(
        @NotNull LocalDate date,
        @Positive Double previousOdometer,
        @NotNull @Positive Double odometer,
        @NotNull @Positive Double gallons,
        @NotNull @Positive Double pricePerGallon) {
}
