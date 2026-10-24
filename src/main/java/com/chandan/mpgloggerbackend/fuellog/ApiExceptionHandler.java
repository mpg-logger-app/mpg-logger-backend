package com.chandan.mpgloggerbackend.fuellog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns rule violations into a 400 response with a readable message. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Answers a broken fuel-log rule with {@code 400 Bad Request} as problem+json.
     *
     * @param ex the rule that was broken; its message becomes the {@code detail} the app shows
     * @return a problem with title "Invalid fuel log"
     */
    @ExceptionHandler(InvalidFuelLogException.class)
    public ProblemDetail handleInvalidFuelLog(InvalidFuelLogException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid fuel log");
        return problem;
    }
}
