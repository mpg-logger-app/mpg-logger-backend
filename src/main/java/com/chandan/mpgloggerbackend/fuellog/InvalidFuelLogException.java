package com.chandan.mpgloggerbackend.fuellog;

/** Thrown when a new or edited fuel log breaks one of the rules in {@link FuelLogRules}. Mapped to HTTP 400. */
public class InvalidFuelLogException extends RuntimeException {

    /** @param message which rule was broken, shown to the user as is */
    public InvalidFuelLogException(String message) {
        super(message);
    }
}
