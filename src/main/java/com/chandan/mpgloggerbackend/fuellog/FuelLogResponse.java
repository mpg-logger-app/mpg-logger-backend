package com.chandan.mpgloggerbackend.fuellog;

import java.time.LocalDate;

/**
 * What the API returns for each log, including the calculated values.
 *
 * @param id               the log's id
 * @param date             the day of the fill-up
 * @param previousOdometer where the tank started
 * @param odometer         the reading at this fill-up
 * @param gallons          gallons pumped
 * @param pricePerGallon   price paid per gallon
 * @param milesDriven      odometer minus previous odometer
 * @param mpg              miles driven per gallon
 * @param totalCost        gallons times price per gallon
 */
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

    /**
     * Copies a stored log into a response, working out the calculated values.
     *
     * @param log the stored log
     * @return the response for the API
     */
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
