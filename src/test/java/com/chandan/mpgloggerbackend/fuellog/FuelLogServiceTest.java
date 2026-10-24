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

    @BeforeEach
    void setUp() {
        TestUsers.deleteAll(repository, sessions, users);
        user = users.save(new AppUser("alice", "Alice", "not-a-real-hash"));
        first = service.create(user, new FuelLogRequest(DAY, 12000.0, 12300.0, 10.0, 3.0));
        second = service.create(user, new FuelLogRequest(DAY.plusDays(7), null, 12600.0, 10.0, 3.0));
        third = service.create(user, new FuelLogRequest(DAY.plusDays(14), null, 12900.0, 10.0, 3.0));
    }

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

    @Test
    void editingOtherFieldsLeavesNextLogAlone() {
        FuelLogResponse edited = service.update(user, second.id(),
                new FuelLogRequest(DAY.plusDays(8), null, 12600.0, 12.0, 3.5));

        assertEquals(DAY.plusDays(8), edited.date());
        assertEquals(25, edited.mpg(), 1e-9);
        assertEquals(42, edited.totalCost(), 1e-9);
        assertEquals(12600, repository.findById(third.id()).orElseThrow().getPreviousOdometer());
    }

    @Test
    void odometerCannotReachNextLogsOdometer() {
        assertThrows(InvalidFuelLogException.class,
                () -> service.update(user, first.id(), new FuelLogRequest(DAY, null, 12600.0, 10.0, 3.0)));
        assertEquals(12300, repository.findById(first.id()).orElseThrow().getOdometer());
    }

    @Test
    void odometerMustStayAbovePreviousOdometer() {
        assertThrows(InvalidFuelLogException.class,
                () -> service.update(user, second.id(), new FuelLogRequest(DAY, null, 12300.0, 10.0, 3.0)));
    }

    @Test
    void onlyOldestLogCanChangePreviousOdometer() {
        FuelLogResponse oldest = service.update(user, first.id(), new FuelLogRequest(DAY, 11900.0, 12300.0, 10.0, 3.0));
        assertEquals(11900, oldest.previousOdometer());

        FuelLogResponse middle = service.update(user, second.id(),
                new FuelLogRequest(DAY.plusDays(7), 1.0, 12600.0, 10.0, 3.0));
        assertEquals(12300, middle.previousOdometer());
    }

    @Test
    void latestLogCanMoveUpFreely() {
        FuelLogResponse edited = service.update(user, third.id(),
                new FuelLogRequest(DAY.plusDays(14), null, 13000.0, 10.0, 3.0));
        assertEquals(400, edited.milesDriven());
    }

    @Test
    void newLogStartsAtHighestOdometerIgnoringTypedPrevious() {
        FuelLogResponse created = service.create(user, new FuelLogRequest(DAY.plusDays(21), 1.0, 13200.0, 10.0, 3.0));
        assertEquals(12900, created.previousOdometer());
    }

    @Test
    void firstLogNeedsPreviousOdometer() {
        repository.deleteAll();
        assertThrows(InvalidFuelLogException.class,
                () -> service.create(user, new FuelLogRequest(DAY, null, 12300.0, 10.0, 3.0)));
    }

    @Test
    void findAllIsNewestFirst() {
        assertEquals(java.util.List.of(third.id(), second.id(), first.id()),
                service.findAll(user).stream().map(FuelLogResponse::id).toList());
    }

    @Test
    void deleteRemovesOnlyThatLog() {
        service.delete(user, second.id());
        assertEquals(java.util.List.of(third.id(), first.id()),
                service.findAll(user).stream().map(FuelLogResponse::id).toList());
    }

    @Test
    void deletingUnknownLogIsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(user, -1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void unknownLogIsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(user, -1L, new FuelLogRequest(DAY, null, 12400.0, 10.0, 3.0)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

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
