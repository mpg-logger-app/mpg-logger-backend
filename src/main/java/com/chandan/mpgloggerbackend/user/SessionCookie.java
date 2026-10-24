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

    /** The cookie's name. */
    public static final String NAME = "mpg_session";

    /** Only static methods; never instantiated. */
    private SessionCookie() {
    }

    /**
     * Finds the session token in the request.
     *
     * @param request the incoming request
     * @return the cookie's value, or empty if there is no session cookie or it is blank
     */
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

    /**
     * Sets the session cookie, lasting {@link AccountService#SESSION_LENGTH}.
     *
     * @param request  the incoming request (decides whether the cookie is Secure)
     * @param response where the cookie is set
     * @param token    the session token
     */
    public static void write(HttpServletRequest request, HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(request, token, AccountService.SESSION_LENGTH).toString());
    }

    /**
     * Tells the browser to delete the session cookie.
     *
     * @param request  the incoming request (decides whether the cookie is Secure)
     * @param response where the cookie is cleared
     */
    public static void clear(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(request, "", Duration.ZERO).toString());
    }

    /**
     * The cookie with all its security attributes.
     *
     * @param request the incoming request; HTTPS (directly or via a proxy) makes the cookie Secure
     * @param value   the token, or "" to clear it
     * @param maxAge  how long the browser keeps it; zero deletes it
     * @return the cookie, ready for a {@code Set-Cookie} header
     */
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
