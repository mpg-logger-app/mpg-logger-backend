package com.chandan.mpgloggerbackend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chandan.mpgloggerbackend.TestUsers;
import com.chandan.mpgloggerbackend.fuellog.FuelLogRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Sign-up, sign-in, sign-out and account details, called the same way the React app does. */
@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FuelLogRepository logs;

    @Autowired
    private UserSessionRepository sessions;

    @Autowired
    private AppUserRepository users;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        TestUsers.deleteAll(logs, sessions, users);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private ResultActions signUp(String username, String name, String password) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","name":"%s","password":"%s"}
                """.formatted(username, name, password)));
    }

    private ResultActions signIn(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)));
    }

    @Test
    void signUpCreatesAccountAndSignsIn() throws Exception {
        MockHttpServletResponse response = signUp("  Chandan ", " Chandan J ", "password123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("chandan"))
                .andExpect(jsonPath("$.name").value("Chandan J"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse();

        // A lasting, script-proof cookie: kept after the browser closes, until sign-out.
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.startsWith("mpg_session="), setCookie);
        assertTrue(setCookie.contains("Max-Age=" + AccountService.SESSION_LENGTH.toSeconds()), setCookie);
        assertTrue(setCookie.contains("HttpOnly"), setCookie);
        assertTrue(setCookie.contains("SameSite=Lax"), setCookie);
        assertTrue(setCookie.contains("Path=/api"), setCookie);

        mvc.perform(get("/api/account").cookie(response.getCookie("mpg_session")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("chandan"));
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        signUp("chandan", "Chandan", "password123").andExpect(status().isCreated());

        AppUser stored = users.findByUsername("chandan").orElseThrow();
        assertNotEquals("password123", stored.getPasswordHash());
        assertTrue(stored.getPasswordHash().startsWith("$2"), stored.getPasswordHash());
    }

    @Test
    void sessionTokenIsStoredOnlyAsHash() throws Exception {
        Cookie cookie = TestUsers.signUp(mvc, "chandan");

        UserSession session = sessions.findAll().getFirst();
        assertNotEquals(cookie.getValue(), session.getTokenHash());
        assertEquals(AccountService.hash(cookie.getValue()), session.getTokenHash());
    }

    @Test
    void usernamesAreUniqueIgnoringCase() throws Exception {
        signUp("chandan", "Chandan", "password123").andExpect(status().isCreated());
        signUp("CHANDAN", "Someone Else", "password456")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("That username is already taken."));
        assertEquals(1, users.count());
    }

    @Test
    void signUpRulesAreChecked() throws Exception {
        signUp("ab", "Name", "password123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("3 to 30")));
        signUp("chandan", " ", "password123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Name is required."));
        signUp("chandan", "Chandan", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("at least 8")));
        assertEquals(0, users.count());
    }

    @Test
    void signInWithRightPasswordStartsSession() throws Exception {
        signUp("chandan", "Chandan", "password123");

        Cookie cookie = signIn("Chandan", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chandan"))
                .andReturn().getResponse().getCookie("mpg_session");

        mvc.perform(get("/api/account").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void wrongUsernameOrPasswordGetTheSameAnswer() throws Exception {
        signUp("chandan", "Chandan", "password123");

        signIn("chandan", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong username or password."))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        signIn("nobody", "password123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong username or password."));
        mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signOutEndsTheSession() throws Exception {
        Cookie cookie = TestUsers.signUp(mvc, "chandan");

        String cleared = mvc.perform(post("/api/auth/signout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cleared.contains("Max-Age=0"), cleared);

        mvc.perform(get("/api/account").cookie(cookie)).andExpect(status().isUnauthorized());
        assertEquals(0, sessions.count());
    }

    @Test
    void signOutWithoutSessionStillSucceeds() throws Exception {
        mvc.perform(post("/api/auth/signout")).andExpect(status().isNoContent());
    }

    @Test
    void expiredSessionIsRejected() throws Exception {
        Cookie cookie = TestUsers.signUp(mvc, "chandan");
        UserSession session = sessions.findAll().getFirst();
        session.setExpiresAt(Instant.now().minusSeconds(1));
        sessions.save(session);

        mvc.perform(get("/api/account").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void usingTheAppPushesTheExpiryForward() throws Exception {
        Cookie cookie = TestUsers.signUp(mvc, "chandan");
        UserSession session = sessions.findAll().getFirst();

        // Fresh session: no need to resend the cookie yet.
        mvc.perform(get("/api/account").cookie(cookie))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        // Last renewed 10 days ago: it moves forward and the cookie is sent again.
        Instant oldExpiry = Instant.now().plus(AccountService.SESSION_LENGTH).minus(10, ChronoUnit.DAYS);
        session.setExpiresAt(oldExpiry);
        sessions.save(session);

        mvc.perform(get("/api/account").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.startsWith("mpg_session=" + cookie.getValue())));
        assertTrue(sessions.findAll().getFirst().getExpiresAt().isAfter(oldExpiry.plus(9, ChronoUnit.DAYS)));
    }

    @Test
    void accountNeedsSignIn() throws Exception {
        mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/account").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateChangesOnlyTheName() throws Exception {
        Cookie cookie = TestUsers.signUp(mvc, "chandan");

        mvc.perform(put("/api/account").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"  Chandan Jaladi ","username":"hacker"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chandan Jaladi"))
                .andExpect(jsonPath("$.username").value("chandan"));

        mvc.perform(get("/api/account").cookie(cookie))
                .andExpect(jsonPath("$.name").value("Chandan Jaladi"));

        mvc.perform(put("/api/account").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid account details"));
    }
}
