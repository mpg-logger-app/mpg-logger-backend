package com.chandan.mpgloggerbackend.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns sign-up and account rule violations into a 400 response with a readable message. */
@RestControllerAdvice
public class AccountExceptionHandler {

    /**
     * Answers a broken account rule with {@code 400 Bad Request} as problem+json.
     *
     * @param ex the rule that was broken; its message becomes the {@code detail} the app shows
     * @return a problem with title "Invalid account details"
     */
    @ExceptionHandler(InvalidAccountException.class)
    public ProblemDetail handleInvalidAccount(InvalidAccountException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid account details");
        return problem;
    }
}
