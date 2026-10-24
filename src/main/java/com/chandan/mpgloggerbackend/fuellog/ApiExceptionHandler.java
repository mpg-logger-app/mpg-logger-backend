package com.chandan.mpgloggerbackend.fuellog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns rule violations into a 400 response with a readable message. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidFuelLogException.class)
    public ProblemDetail handleInvalidFuelLog(InvalidFuelLogException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid fuel log");
        return problem;
    }
}
