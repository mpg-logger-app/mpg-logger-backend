package com.chandan.mpgloggerbackend.fuellog;

/**
 * Business rules ported from the Swift app (ContentView.swift and FuelLog.swift).
 * Kept free of Spring/JPA so it can be unit tested on its own.
 */
public final class FuelLogRules {

    /** Only static methods; never instantiated. */
    private FuelLogRules() {
    }

    /**
     * Swift: previousOdometerValue.
     * The very first log uses the odometer the user typed in; every later log
     * starts where the log with the highest odometer reading ended.
     *
     * @param latestOdometer    odometer of the existing log with the highest reading, or null if there are no logs
     * @param requestedPrevious the "Previous Odometer" the user typed (only used for the first log)
     * @return the odometer reading the new log starts at
     * @throws InvalidFuelLogException for the first log when no previous odometer was given
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

    /**
     * Swift: formIsValid. Checks a log's values, in the order the form shows them.
     *
     * @param previousOdometer where the tank started; must be above 0
     * @param odometer         the reading at this fill-up; must be above previousOdometer
     * @param gallons          gallons pumped; must be above 0
     * @param pricePerGallon   price per gallon; must be above 0
     * @throws InvalidFuelLogException naming the first rule that is broken
     */
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

    /**
     * Swift: FuelLog.milesDriven.
     *
     * @param previousOdometer where the tank started
     * @param odometer         the reading at this fill-up
     * @return the miles driven on this tank
     */
    public static double milesDriven(double previousOdometer, double odometer) {
        return odometer - previousOdometer;
    }

    /**
     * An edited log must still end before the next log does, because the next log
     * will start at the edited odometer.
     *
     * @param odometer     the edited log's new odometer reading
     * @param nextOdometer odometer of the next (newer) log, or null if the edited log is the latest
     * @throws InvalidFuelLogException if the edited reading reaches or passes the next log's
     */
    public static void validateBeforeNext(double odometer, Double nextOdometer) {
        if (nextOdometer != null && !(odometer < nextOdometer)) {
            throw new InvalidFuelLogException(
                    "Current odometer must be less than the next log's odometer (" + nextOdometer + ").");
        }
    }

    /**
     * Swift: FuelLog.mpg (returns 0 when gallons is 0, same as the Swift guard).
     *
     * @param previousOdometer where the tank started
     * @param odometer         the reading at this fill-up
     * @param gallons          gallons pumped
     * @return miles per gallon, or 0 when gallons is not above 0
     */
    public static double mpg(double previousOdometer, double odometer, double gallons) {
        if (!(gallons > 0)) {
            return 0;
        }
        return milesDriven(previousOdometer, odometer) / gallons;
    }

    /**
     * Swift: FuelLog.totalCost.
     *
     * @param gallons        gallons pumped
     * @param pricePerGallon price per gallon
     * @return what the fill-up cost
     */
    public static double totalCost(double gallons, double pricePerGallon) {
        return gallons * pricePerGallon;
    }
}
