package com.chandan.mpgloggerbackend.fuellog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private FuelLogResponse first;
    private FuelLogResponse second;
    private FuelLogResponse third;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        first = service.create(new FuelLogRequest(DAY, 12000.0, 12300.0, 10.0, 3.0));
        second = service.create(new FuelLogRequest(DAY.plusDays(7), null, 12600.0, 10.0, 3.0));
        third = service.create(new FuelLogRequest(DAY.plusDays(14), null, 12900.0, 10.0, 3.0));
    }

    @Test
    void odometerChangeMovesNextLogsPreviousOdometer() {
        FuelLogResponse edited = service.update(first.id(), new FuelLogRequest(DAY, null, 12400.0, 10.0, 3.0));

        assertEquals(400, edited.milesDriven());
        FuelLog next = repository.findById(second.id()).orElseThrow();
        assertEquals(12400, next.getPreviousOdometer());
        assertEquals(200, next.getMilesDriven());
        // Only the very next log is affected.
        assertEquals(12600, repository.findById(third.id()).orElseThrow().getPreviousOdometer());
    }

    @Test
    void editingOtherFieldsLeavesNextLogAlone() {
        FuelLogResponse edited = service.update(second.id(),
                new FuelLogRequest(DAY.plusDays(8), null, 12600.0, 12.0, 3.5));

        assertEquals(DAY.plusDays(8), edited.date());
        assertEquals(25, edited.mpg(), 1e-9);
        assertEquals(42, edited.totalCost(), 1e-9);
        assertEquals(12600, repository.findById(third.id()).orElseThrow().getPreviousOdometer());
    }

    @Test
    void odometerCannotReachNextLogsOdometer() {
        assertThrows(InvalidFuelLogException.class,
                () -> service.update(first.id(), new FuelLogRequest(DAY, null, 12600.0, 10.0, 3.0)));
        assertEquals(12300, repository.findById(first.id()).orElseThrow().getOdometer());
    }

    @Test
    void odometerMustStayAbovePreviousOdometer() {
        assertThrows(InvalidFuelLogException.class,
                () -> service.update(second.id(), new FuelLogRequest(DAY, null, 12300.0, 10.0, 3.0)));
    }

    @Test
    void onlyOldestLogCanChangePreviousOdometer() {
        FuelLogResponse oldest = service.update(first.id(), new FuelLogRequest(DAY, 11900.0, 12300.0, 10.0, 3.0));
        assertEquals(11900, oldest.previousOdometer());

        FuelLogResponse middle = service.update(second.id(),
                new FuelLogRequest(DAY.plusDays(7), 1.0, 12600.0, 10.0, 3.0));
        assertEquals(12300, middle.previousOdometer());
    }

    @Test
    void latestLogCanMoveUpFreely() {
        FuelLogResponse edited = service.update(third.id(),
                new FuelLogRequest(DAY.plusDays(14), null, 13000.0, 10.0, 3.0));
        assertEquals(400, edited.milesDriven());
    }

    @Test
    void newLogStartsAtHighestOdometerIgnoringTypedPrevious() {
        FuelLogResponse created = service.create(new FuelLogRequest(DAY.plusDays(21), 1.0, 13200.0, 10.0, 3.0));
        assertEquals(12900, created.previousOdometer());
    }

    @Test
    void firstLogNeedsPreviousOdometer() {
        repository.deleteAll();
        assertThrows(InvalidFuelLogException.class,
                () -> service.create(new FuelLogRequest(DAY, null, 12300.0, 10.0, 3.0)));
    }

    @Test
    void findAllIsNewestFirst() {
        assertEquals(java.util.List.of(third.id(), second.id(), first.id()),
                service.findAll().stream().map(FuelLogResponse::id).toList());
    }

    @Test
    void deleteRemovesOnlyThatLog() {
        service.delete(second.id());
        assertEquals(java.util.List.of(third.id(), first.id()),
                service.findAll().stream().map(FuelLogResponse::id).toList());
    }

    @Test
    void deletingUnknownLogIsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(-1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void unknownLogIsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(-1L, new FuelLogRequest(DAY, null, 12400.0, 10.0, 3.0)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
