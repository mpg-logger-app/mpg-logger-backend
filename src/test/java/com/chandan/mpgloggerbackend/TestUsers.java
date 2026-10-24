package com.chandan.mpgloggerbackend;

import com.chandan.mpgloggerbackend.fuellog.FuelLogRepository;
import com.chandan.mpgloggerbackend.user.AppUserRepository;
import com.chandan.mpgloggerbackend.user.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Shared test helpers for users and sign-in. */
public final class TestUsers {

    private TestUsers() {
    }

    /** Empties every table, children first so foreign keys don't get in the way. */
    public static void deleteAll(FuelLogRepository logs, UserSessionRepository sessions, AppUserRepository users) {
        logs.deleteAll();
        sessions.deleteAll();
        users.deleteAll();
    }

    /** Signs up through the API and returns the session cookie it set. */
    public static Cookie signUp(MockMvc mvc, String username) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","name":"Test User","password":"password123"}
                                """.formatted(username)))
                .andReturn().getResponse().getCookie("mpg_session");
    }
}
