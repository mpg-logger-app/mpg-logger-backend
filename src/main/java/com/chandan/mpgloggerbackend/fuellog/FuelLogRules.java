package com.chandan.mpgloggerbackend.fuellog;

/**
 * Business rules ported from the Swift app (ContentView.swift and FuelLog.swift).
 * Kept free of Spring/JPA so it can be unit tested on its own.
 */
public final class FuelLogRules {

    private FuelLogRules() {
    }

    /**
     * Swift: previousOdometerValue.
     * The very first log uses the odometer the user typed in; every later log
     * starts where the log with the highest odometer reading ended.
     *
     * @param latestOdometer    odometer of the existing log with the highest reading, or null if there are no logs
     * @param requestedPrevious the "Previous Odometer" the user typed (only used for the first log)
     */
    public static double resolvePreviousOdometer(Double latestOdometer, Double requestedPrevious) {
        if (latestOdometer != null) {
            return latestOdometer;
        }
        if (requestedPrevious == null) {
            throw new InvalidFuelLogException("previousOdometer is required for the first fuel log.");
        }
        return requestedPrevious;
    }

    /** Swift: formIsValid. */
    public static void validate(double previousOdometer, double odometer, double gallons, double pricePerGallon) {
        if (!(previousOdometer > 0)) {
            throw new InvalidFuelLogException("Previous odometer must be greater than 0.");
        }
        if (!(odometer > previousOdometer)) {
            throw new InvalidFuelLogException(
                    "Current odometer must be greater than the previous odometer (" + previousOdometer + ").");
        }
        if (!(gallons > 0)) {
            throw new InvalidFuelLogException("Gallons must be greater than 0.");
        }
        if (!(pricePerGallon > 0)) {
            throw new InvalidFuelLogException("Price per gallon must be greater than 0.");
        }
    }

    /** Swift: FuelLog.milesDriven. */
    public static double milesDriven(double previousOdometer, double odometer) {
        return odometer - previousOdometer;
    }

    /** Swift: FuelLog.mpg (returns 0 when gallons is 0, same as the Swift guard). */
    public static double mpg(double previousOdometer, double odometer, double gallons) {
        if (!(gallons > 0)) {
            return 0;
        }
        return milesDriven(previousOdometer, odometer) / gallons;
    }

    /** Swift: FuelLog.totalCost. */
    public static double totalCost(double gallons, double pricePerGallon) {
        return gallons * pricePerGallon;
    }
}
