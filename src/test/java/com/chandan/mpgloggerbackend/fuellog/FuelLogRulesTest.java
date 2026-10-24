package com.chandan.mpgloggerbackend.fuellog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link FuelLogRules}: the validation rules and calculations ported from the Swift app. */
class FuelLogRulesTest {

    /** With no logs yet, the previous odometer the user typed is used. */
    @Test
    void firstLogUsesTypedPreviousOdometer() {
        assertEquals(12000, FuelLogRules.resolvePreviousOdometer(null, 12000.0));
    }

    /** The first log can't be saved without a previous odometer. */
    @Test
    void firstLogWithoutPreviousOdometerIsRejected() {
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.resolvePreviousOdometer(null, null));
    }

    /** Once there are logs, a new one starts at the latest odometer, whatever was typed. */
    @Test
    void laterLogsIgnoreTypedValueAndUseLatestOdometer() {
        assertEquals(12350, FuelLogRules.resolvePreviousOdometer(12350.0, 1.0));
    }

    /** A normal fill-up passes every rule. */
    @Test
    void validEntryPasses() {
        FuelLogRules.validate(12000, 12300, 10, 3.49);
    }

    /** The odometer must be strictly above the previous reading; equal or lower is rejected. */
    @Test
    void odometerMustIncrease() {
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(12000, 12000, 10, 3.49));
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(12000, 11999, 10, 3.49));
    }

    /** A previous odometer, gallons or price of 0 is rejected. */
    @Test
    void zeroValuesAreRejected() {
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(0, 100, 10, 3.49));
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(100, 200, 0, 3.49));
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(100, 200, 10, 0));
    }

    /** An edited odometer must stay below the next log's; the latest log (no next log) has no upper limit. */
    @Test
    void editedOdometerMustStayBelowNextLog() {
        FuelLogRules.validateBeforeNext(12400, 12500.0);
        FuelLogRules.validateBeforeNext(99999, null);
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validateBeforeNext(12500, 12500.0));
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validateBeforeNext(12600, 12500.0));
    }

    /** Miles driven, MPG (0 with no gallons) and cost give the same results as the Swift app. */
    @Test
    void calculationsMatchSwiftApp() {
        assertEquals(300, FuelLogRules.milesDriven(12000, 12300));
        assertEquals(30, FuelLogRules.mpg(12000, 12300, 10), 1e-9);
        assertEquals(0, FuelLogRules.mpg(12000, 12300, 0));
        assertEquals(34.9, FuelLogRules.totalCost(10, 3.49), 1e-9);
    }
}
