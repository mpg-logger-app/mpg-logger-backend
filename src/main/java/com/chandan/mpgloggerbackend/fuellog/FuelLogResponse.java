package com.chandan.mpgloggerbackend.fuellog;

import java.time.LocalDate;

/** What the API returns for each log, including the calculated values. */
public record FuelLogResponse(
        Long id,
        LocalDate date,
        double previousOdometer,
        double odometer,
        double gallons,
        double pricePerGallon,
        double milesDriven,
        double mpg,
        double totalCost) {

    public static FuelLogResponse from(FuelLog log) {
        return new FuelLogResponse(
                log.getId(),
                log.getDate(),
                log.getPreviousOdometer(),
                log.getOdometer(),
                log.getGallons(),
                log.getPricePerGallon(),
                log.getMilesDriven(),
                log.getMpg(),
                log.getTotalCost());
    }
}
