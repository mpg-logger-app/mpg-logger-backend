package com.chandan.mpgloggerbackend.fuellog;

/** Thrown when a new or edited fuel log breaks one of the rules in {@link FuelLogRules}. Mapped to HTTP 400. */
public class InvalidFuelLogException extends RuntimeException {

    public InvalidFuelLogException(String message) {
        super(message);
    }
}
