package com.chandan.mpgloggerbackend.fuellog;

import com.chandan.mpgloggerbackend.user.AppUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Each user has their own odometer history: "previous odometer" and "next log" only look
 * at that user's logs. Another user's log is reported as not found, the same as a missing one.
 */
@Service
public class FuelLogService {

    private final FuelLogRepository repository;

    public FuelLogService(FuelLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FuelLogResponse> findAll(AppUser user) {
        return repository.findAllByUserIdOrderByOdometerDesc(user.getId()).stream()
                .map(FuelLogResponse::from)
                .toList();
    }

    /** Swift: logFuelEntry() */
    @Transactional
    public FuelLogResponse create(AppUser user, FuelLogRequest request) {
        Double latestOdometer = repository.findTopByUserIdOrderByOdometerDesc(user.getId())
                .map(FuelLog::getOdometer)
                .orElse(null);

        double previous = FuelLogRules.resolvePreviousOdometer(latestOdometer, request.previousOdometer());

        FuelLogRules.validate(previous, request.odometer(), request.gallons(), request.pricePerGallon());

        FuelLog saved = repository.save(new FuelLog(
                user,
                request.date(),
                previous,
                request.odometer(),
                request.gallons(),
                request.pricePerGallon()));

        return FuelLogResponse.from(saved);
    }

    /**
     * Edits a log. If its odometer changes, the next log's previous odometer moves with it,
     * so that log's miles driven and MPG change too.
     */
    @Transactional
    public FuelLogResponse update(AppUser user, Long id, FuelLogRequest request) {
        FuelLog log = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> notFound(id));

        double oldOdometer = log.getOdometer();
        boolean isOldest = repository
                .findTopByUserIdAndOdometerLessThanOrderByOdometerDesc(user.getId(), oldOdometer).isEmpty();
        FuelLog next = repository
                .findTopByUserIdAndOdometerGreaterThanOrderByOdometerAsc(user.getId(), oldOdometer).orElse(null);

        // Only the oldest log's previous odometer was typed in; the rest come from the log before them.
        double previous = isOldest && request.previousOdometer() != null
                ? request.previousOdometer()
                : log.getPreviousOdometer();

        FuelLogRules.validate(previous, request.odometer(), request.gallons(), request.pricePerGallon());
        FuelLogRules.validateBeforeNext(request.odometer(), next == null ? null : next.getOdometer());

        log.update(request.date(), previous, request.odometer(), request.gallons(), request.pricePerGallon());

        if (next != null && request.odometer() != oldOdometer) {
            next.setPreviousOdometer(request.odometer());
        }

        return FuelLogResponse.from(log);
    }

    /** Swift: deleteLogs(at:) */
    @Transactional
    public void delete(AppUser user, Long id) {
        FuelLog log = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> notFound(id));
        repository.delete(log);
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuel log " + id + " not found.");
    }
}
