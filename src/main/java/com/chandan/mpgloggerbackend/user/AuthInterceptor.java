package com.chandan.mpgloggerbackend.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs before every /api request except sign-up, sign-in and sign-out (see WebConfig):
 * finds the signed-in user from the session cookie, or answers 401.
 * The user is left on the request for {@link CurrentUserResolver}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    static final String USER_ATTRIBUTE = AuthInterceptor.class.getName() + ".user";

    private final AccountService accounts;

    public AuthInterceptor(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = SessionCookie.read(request)
                .orElseThrow(AuthInterceptor::notSignedIn);
        UserSession session = accounts.findActiveSession(token)
                .orElseThrow(AuthInterceptor::notSignedIn);

        // Keeps a regularly used session from ever running out.
        if (accounts.renewSession(session)) {
            SessionCookie.write(request, response, token);
        }
        request.setAttribute(USER_ATTRIBUTE, session.getUser());
        return true;
    }

    private static ResponseStatusException notSignedIn() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
    }
}
