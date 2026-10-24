package com.chandan.mpgloggerbackend.fuellog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FuelLogRulesTest {

    @Test
    void firstLogUsesTypedPreviousOdometer() {
        assertEquals(12000, FuelLogRules.resolvePreviousOdometer(null, 12000.0));
    }

    @Test
    void firstLogWithoutPreviousOdometerIsRejected() {
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.resolvePreviousOdometer(null, null));
    }

    @Test
    void laterLogsIgnoreTypedValueAndUseLatestOdometer() {
        assertEquals(12350, FuelLogRules.resolvePreviousOdometer(12350.0, 1.0));
    }

    @Test
    void validEntryPasses() {
        FuelLogRules.validate(12000, 12300, 10, 3.49);
    }

    @Test
    void odometerMustIncrease() {
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(12000, 12000, 10, 3.49));
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(12000, 11999, 10, 3.49));
    }

    @Test
    void zeroValuesAreRejected() {
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(0, 100, 10, 3.49));
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(100, 200, 0, 3.49));
        assertThrows(InvalidFuelLogException.class, () -> FuelLogRules.validate(100, 200, 10, 0));
    }

    @Test
    void calculationsMatchSwiftApp() {
        assertEquals(300, FuelLogRules.milesDriven(12000, 12300));
        assertEquals(30, FuelLogRules.mpg(12000, 12300, 10), 1e-9);
        assertEquals(0, FuelLogRules.mpg(12000, 12300, 0));
        assertEquals(34.9, FuelLogRules.totalCost(10, 3.49), 1e-9);
    }
}
