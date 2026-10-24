package com.chandan.mpgloggerbackend.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * The session cookie. It has an expiry date, so the browser keeps it after the tab or browser
 * is closed, and you stay signed in until you sign out.
 * HttpOnly: page scripts can't read it. SameSite=Lax: other sites can't send it with their
 * POST/PUT/DELETE requests (CSRF). Secure when served over HTTPS.
 */
public final class SessionCookie {

    public static final String NAME = "mpg_session";

    private SessionCookie() {
    }

    public static Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> NAME.equals(c.getName()) && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst();
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(request, token, AccountService.SESSION_LENGTH).toString());
    }

    public static void clear(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(request, "", Duration.ZERO).toString());
    }

    private static ResponseCookie build(HttpServletRequest request, String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api")
                .maxAge(maxAge)
                .build();
    }
}
