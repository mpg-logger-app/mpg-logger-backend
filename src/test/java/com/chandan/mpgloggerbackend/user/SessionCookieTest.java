package com.chandan.mpgloggerbackend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests for {@link SessionCookie}: reading the token and the cookie's security attributes. */
class SessionCookieTest {

    /** A request without any cookies has no session. */
    @Test
    void noCookiesMeansNoSession() {
        assertEquals(Optional.empty(), SessionCookie.read(new MockHttpServletRequest()));
    }

    /** Other cookies, and a blank session cookie, are ignored; the session cookie's value is found among them. */
    @Test
    void readsOnlyANonBlankSessionCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("theme", "dark"), new Cookie(SessionCookie.NAME, " "));
        assertEquals(Optional.empty(), SessionCookie.read(request));

        request.setCookies(new Cookie("theme", "dark"), new Cookie(SessionCookie.NAME, "token-123"));
        assertEquals(Optional.of("token-123"), SessionCookie.read(request));
    }

    /** Over HTTPS the cookie is marked Secure; over plain HTTP (local development) it isn't. */
    @Test
    void secureOnlyOverHttps() {
        MockHttpServletRequest https = new MockHttpServletRequest();
        https.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        SessionCookie.write(https, response, "token-123");
        assertTrue(response.getHeader(HttpHeaders.SET_COOKIE).contains("Secure"));

        MockHttpServletResponse plain = new MockHttpServletResponse();
        SessionCookie.write(new MockHttpServletRequest(), plain, "token-123");
        assertFalse(plain.getHeader(HttpHeaders.SET_COOKIE).contains("Secure"));
    }

    /** Clearing sends an empty, already-expired cookie with the same path, so the browser deletes it. */
    @Test
    void clearExpiresTheCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        SessionCookie.clear(new MockHttpServletRequest(), response);
        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(header.startsWith(SessionCookie.NAME + "=;"), header);
        assertTrue(header.contains("Max-Age=0"), header);
        assertTrue(header.contains("Path=/api"), header);
    }
}
