package com.chandan.mpgloggerbackend.fuellog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chandan.mpgloggerbackend.TestUsers;
import com.chandan.mpgloggerbackend.user.AppUser;
import com.chandan.mpgloggerbackend.user.AppUserRepository;
import com.chandan.mpgloggerbackend.user.UserSessionRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** {@link FuelLogService} against an in-memory database: how adding, editing and deleting a log affects the logs around it. */
@SpringBootTest
class FuelLogServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    @Autowired
    private FuelLogService service;

    @Autowired
    private FuelLogRepository repository;

    @Autowired
    private UserSessionRepository sessions;

    @Autowired
    private AppUserRepository users;

    private AppUser user;
    private FuelLogResponse first;
    private FuelLogResponse second;
    private FuelLogResponse third;

    /** Empties the tables and gives alice three logs: 12,000 to 12,300 to 12,600 to 12,900. */
    @BeforeEach
    void setUp() {
        TestUsers.deleteAll(repository, sessions, users);
        user = users.save(new AppUser("alice", "Alice", "not-a-real-hash"));
        first = service.create(user, new FuelLogRequest(DAY, 12000.0, 12300.0, 10.0, 3.0));
        second = service.create(user, new FuelLogRequest(DAY.plusDays(7), null, 12600.0, 10.0, 3.0));
        third = service.create(user, new FuelLogRequest(DAY.plusDays(14), null, 12900.0, 10.0, 3.0));
    }

    /** Changing a log's odometer moves the next log's start with it, and only that log. */
    @Test
    void odometerChangeMovesNextLogsPreviousOdometer() {
        FuelLogResponse edited = service.update(user, first.id(), new FuelLogRequest(DAY, null, 12400.0, 10.0, 3.0));

        assertEquals(400, edited.milesDriven());
        FuelLog next = repository.findById(second.id()).orElseThrow();
        assertEquals(12400, next.getPreviousOdometer());
        assertEquals(200, next.getMilesDriven());
        // Only the very next log is affected.
        assertEquals(12600, repository.findById(third.id()).orElseThrow().getPreviousOdometer());
    }

    /** Editing the date, gallons or price recalculates the log but doesn't touch the next one. */
    @Test
    void editingOtherFieldsLeavesNextLogAlone() {
        FuelLogResponse edited = service.update(user, second.id(),
                new FuelLogRequest(DAY.plusDays(8), null, 12600.0, 12.0, 3.5));

        assertEquals(DAY.plusDays(8), edited.date());
        assertEquals(25, edited.mpg(), 1e-9);
        assertEquals(42, edited.totalCost(), 1e-9);
        assertEquals(12600, repository.findById(third.id()).orElseThrow().getPreviousOdometer());
    }

    /** An edit reaching the next log's odometer is rejected and nothing is saved. */
    @Test
    void odometerCannotReachNextLogsOdometer() {
        assertThrows(InvalidFuelLogException.class,
                () -> service.update(user, first.id(), new FuelLogRequest(DAY, null, 12600.0, 10.0, 3.0)));
        assertEquals(12300, repository.findById(first.id()).orElseThrow().getOdometer());
    }

    /** An edit can't drop the odometer to or below where the log started. */
    @Test
    void odometerMustStayAbovePreviousOdometer() {
        assertThrows(InvalidFuelLogException.class,
                () -> service.update(user, second.id(), new FuelLogRequest(DAY, null, 12300.0, 10.0, 3.0)));
    }

    /** Only the oldest log takes a new previous odometer; later logs ignore it. */
    @Test
    void onlyOldestLogCanChangePreviousOdometer() {
        FuelLogResponse oldest = service.update(user, first.id(), new FuelLogRequest(DAY, 11900.0, 12300.0, 10.0, 3.0));
        assertEquals(11900, oldest.previousOdometer());

        FuelLogResponse middle = service.update(user, second.id(),
                new FuelLogRequest(DAY.plusDays(7), 1.0, 12600.0, 10.0, 3.0));
        assertEquals(12300, middle.previousOdometer());
    }

    /** The latest log has no next log, so its odometer can go up as far as needed. */
    @Test
    void latestLogCanMoveUpFreely() {
        FuelLogResponse edited = service.update(user, third.id(),
                new FuelLogRequest(DAY.plusDays(14), null, 13000.0, 10.0, 3.0));
        assertEquals(400, edited.milesDriven());
    }

    /** A new log starts at the highest existing odometer, ignoring a typed previous odometer. */
    @Test
    void newLogStartsAtHighestOdometerIgnoringTypedPrevious() {
        FuelLogResponse created = service.create(user, new FuelLogRequest(DAY.plusDays(21), 1.0, 13200.0, 10.0, 3.0));
        assertEquals(12900, created.previousOdometer());
    }

    /** With no logs, a new log without a previous odometer is rejected. */
    @Test
    void firstLogNeedsPreviousOdometer() {
        repository.deleteAll();
        assertThrows(InvalidFuelLogException.class,
                () -> service.create(user, new FuelLogRequest(DAY, null, 12300.0, 10.0, 3.0)));
    }

    /** The list comes back newest first. */
    @Test
    void findAllIsNewestFirst() {
        assertEquals(java.util.List.of(third.id(), second.id(), first.id()),
                service.findAll(user).stream().map(FuelLogResponse::id).toList());
    }

    /** Deleting one log leaves the others. */
    @Test
    void deleteRemovesOnlyThatLog() {
        service.delete(user, second.id());
        assertEquals(java.util.List.of(third.id(), first.id()),
                service.findAll(user).stream().map(FuelLogResponse::id).toList());
    }

    /** Deleting a log that doesn't exist is a 404. */
    @Test
    void deletingUnknownLogIsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(user, -1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /** Editing a log that doesn't exist is a 404. */
    @Test
    void unknownLogIsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(user, -1L, new FuelLogRequest(DAY, null, 12400.0, 10.0, 3.0)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /** A second user's logs don't affect alice's previous or next log, and they can't delete hers. */
    @Test
    void eachUserHasTheirOwnOdometerHistory() {
        AppUser bob = users.save(new AppUser("bob", "Bob", "not-a-real-hash"));

        // Bob's first log starts where he says, not at Alice's 12,900.
        FuelLogResponse bobsFirst = service.create(bob, new FuelLogRequest(DAY, 100.0, 400.0, 10.0, 3.0));
        assertEquals(100, bobsFirst.previousOdometer());
        assertEquals(java.util.List.of(bobsFirst.id()),
                service.findAll(bob).stream().map(FuelLogResponse::id).toList());

        // Alice's "next log" for her latest entry is still none, even though Bob exists.
        assertEquals(400, service.update(user, third.id(),
                new FuelLogRequest(DAY.plusDays(14), null, 13000.0, 10.0, 3.0)).milesDriven());

        assertThrows(ResponseStatusException.class, () -> service.delete(bob, first.id()));
        assertEquals(3, service.findAll(user).size());
    }
}
