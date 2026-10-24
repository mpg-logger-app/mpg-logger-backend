package com.chandan.mpgloggerbackend.fuellog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private long create(String json) throws Exception {
        String location = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

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

    @Test
    void ruleViolationReturnsProblemDetail() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","previousOdometer":12000,"odometer":12000,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid fuel log"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("greater than")));
    }

    @Test
    void missingFieldIsRejected() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","previousOdometer":12000,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isBadRequest());
    }

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

    @Test
    void updateUnknownLogIsNotFound() throws Exception {
        mvc.perform(put(BASE + "/999999").contentType(MediaType.APPLICATION_JSON).content("""
                        {"date":"2026-10-01","odometer":12600,"gallons":10,"pricePerGallon":3}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesLog() throws Exception {
        long id = create("""
                {"date":"2026-10-01","previousOdometer":12000,"odometer":12300,"gallons":10,"pricePerGallon":3}
                """);

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteUnknownLogIsNotFound() throws Exception {
        mvc.perform(delete(BASE + "/999999")).andExpect(status().isNotFound());
    }
}
