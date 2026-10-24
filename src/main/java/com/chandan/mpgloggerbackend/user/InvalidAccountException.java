package com.chandan.mpgloggerbackend.user;

/** Thrown when sign-up or account details break one of the rules in {@link AccountRules}. Mapped to HTTP 400. */
public class InvalidAccountException extends RuntimeException {

    /** @param message which rule was broken, shown to the user as is */
    public InvalidAccountException(String message) {
        super(message);
    }
}
