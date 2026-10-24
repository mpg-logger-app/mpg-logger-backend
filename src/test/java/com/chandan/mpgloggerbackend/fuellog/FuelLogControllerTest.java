package com.chandan.mpgloggerbackend.fuellog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chandan.mpgloggerbackend.TestUsers;
import com.chandan.mpgloggerbackend.user.AppUserRepository;
import com.chandan.mpgloggerbackend.user.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Calls the REST API the same way the React app does. */
@SpringBootTest
class FuelLogControllerTest {

    private static final String BASE = "/api/fuel-logs";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FuelLogRepository repository;

    @Autowired
    private UserSessionRepository sessions;

    @Autowired
    private AppUserRepository users;

    /** Signed in as "alice": every request sends her session cookie. */
    private MockMvc mvc;

    private MockMvc anonymous;

    /** Empties the tables and signs up "alice"; requests through {@link #mvc} are made as her. */
    @BeforeEach
    void setUp() throws Exception {
        TestUsers.deleteAll(repository, sessions, users);
        anonymous = MockMvcBuilders.webAppContextSetup(context).build();
        mvc = signedInAs("alice");
    }

    /** Signs up a user and returns a MockMvc whose requests all carry that user's session cookie. */
    private MockMvc signedInAs(String username) throws Exception {
        Cookie session = TestUsers.signUp(anonymous, username);
        return MockMvcBuilders.webAppContextSetup(context).defaultRequest(get("/").cookie(session)).build();
    }

    /** POSTs a log as alice and returns the new log's id, read from the {@code Location} header. */
    private long create(String json) throws Exception {
        String location = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    /** POST answers 201 with a Location header and the log including miles driven, MPG and cost. */
    @Test
    void createReturnsCreatedLogWithCalculations() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3.5}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(BASE + "/")))
                .andExpect(jsonPath("$.date").value("2026-10-01"))
                .andExpect(jsonPath("$.previousOdometer").value(12000.0))
                .andExpect(jsonPath("$.milesDriven").value(300.0))
                .andExpect(jsonPath("$.mpg").value(30.0))
                .andExpect(jsonPath("$.totalCost").value(35.0));
    }

    /** GET lists the logs newest (highest odometer) first, each starting where the previous ended. */
    @Test
    void listReturnsHighestOdometerFirst() throws Exception {
        create("""
                {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3}
                """);
        create("""
                {"date":"2026-10-08","odometer":12600,"gallons":10,"pricePerGallon":3}
                """);

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].odometer").value(12600.0))
                .andExpect(jsonPath("$[0].previousOdometer").value(12300.0))
                .andExpect(jsonPath("$[1].odometer").value(12300.0));
    }

    /** A broken rule answers 400 problem+json with a readable detail. */
    @Test
    void ruleViolationReturnsProblemDetail() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","previousOdometer":12000,"odometer":12000,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid fuel log"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("greater than")));
    }

    /** A body missing a required field (the odometer) answers 400. */
    @Test
    void missingFieldIsRejected() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","previousOdometer":12000,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isBadRequest());
    }

    /** PUT edits a log, and the next log now starts at the edited odometer. */
    @Test
    void updateEditsLogAndMovesNextLog() throws Exception {
        long first = create("""
                {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3}
                """);
        create("""
                {"date":"2026-10-08","odometer":12600,"gallons":10,"pricePerGallon":3}
                """);

        mvc.perform(put(BASE + "/" + first).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-02","odometer":12400,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-02"))
                .andExpect(jsonPath("$.milesDriven").value(400.0));

        mvc.perform(get(BASE))
                .andExpect(jsonPath("$[0].previousOdometer").value(12400.0))
                .andExpect(jsonPath("$[0].milesDriven").value(200.0));
    }

    /** PUT can't move a log's odometer up to or past the next log's. */
    @Test
    void updatePastNextLogIsRejected() throws Exception {
        long first = create("""
                {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3}
                """);
        create("""
                {"date":"2026-10-08","odometer":12600,"gallons":10,"pricePerGallon":3}
                """);

        mvc.perform(put(BASE + "/" + first).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","odometer":12600,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("next log")));
    }

    /** PUT on a log that doesn't exist answers 404. */
    @Test
    void updateUnknownLogIsNotFound() throws Exception {
        mvc.perform(put(BASE + "/999999").contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","odometer":12600,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isNotFound());
    }

    /** DELETE answers 204 and the log is gone from the list. */
    @Test
    void deleteRemovesLog() throws Exception {
        long id = create("""
                {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3}
                """);

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE)).andExpect(jsonPath("$.length()").value(0));
    }

    /** DELETE on a log that doesn't exist answers 404. */
    @Test
    void deleteUnknownLogIsNotFound() throws Exception {
        mvc.perform(delete(BASE + "/999999")).andExpect(status().isNotFound());
    }

    /** Without a session (no cookie, or a made-up one) the fuel-log endpoints answer 401. */
    @Test
    void signedOutRequestsAreRejected() throws Exception {
        anonymous.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Please sign in."));
        anonymous.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isUnauthorized());
        anonymous.perform(get(BASE).cookie(new Cookie("mpg_session", "made-up")))
                .andExpect(status().isUnauthorized());
    }

    /** Another user can't see, edit or delete alice's logs, and has their own odometer history. */
    @Test
    void usersOnlySeeAndChangeTheirOwnLogs() throws Exception {
        long alicesLog = create("""
                {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3}
                """);
        MockMvc bob = signedInAs("bob");

        bob.perform(get(BASE)).andExpect(jsonPath("$.length()").value(0));
        bob.perform(put(BASE + "/" + alicesLog).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","odometer":12400,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isNotFound());
        bob.perform(delete(BASE + "/" + alicesLog)).andExpect(status().isNotFound());

        // Bob's first log needs his own previous odometer: Alice's 12,300 doesn't count.
        bob.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-02","odometer":500,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isBadRequest());
        bob.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-02","previousOdometer":100,"odometer":500,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isCreated());

        mvc.perform(get(BASE))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].odometer").value(12300.0));
    }
}
