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

    /** @param repository where the logs are stored */
    public FuelLogService(FuelLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Swift: the @Query list of fuel logs.
     *
     * @param user whose logs
     * @return the user's logs, highest odometer first
     */
    @Transactional(readOnly = true)
    public List<FuelLogResponse> findAll(AppUser user) {
        return repository.findAllByUserIdOrderByOdometerDesc(user.getId()).stream()
                .map(FuelLogResponse::from)
                .toList();
    }

    /**
     * Swift: logFuelEntry(). The new log starts where the user's latest log ended; only the
     * very first log uses the previous odometer from the request.
     *
     * @param user    who the log is for
     * @param request the fill-up
     * @return the saved log
     * @throws InvalidFuelLogException if a rule is broken
     */
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
     *
     * @param user    who must own the log
     * @param id      the log to edit
     * @param request the new values; previousOdometer is only used for the oldest log
     * @return the edited log
     * @throws ResponseStatusException 404 if the log doesn't exist or isn't this user's
     * @throws InvalidFuelLogException if a rule is broken, including passing the next log's odometer
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

    /**
     * Swift: deleteLogs(at:)
     *
     * @param user who must own the log
     * @param id   the log to delete
     * @throws ResponseStatusException 404 if the log doesn't exist or isn't this user's
     */
    @Transactional
    public void delete(AppUser user, Long id) {
        FuelLog log = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> notFound(id));
        repository.delete(log);
    }

    /**
     * The error for a log that doesn't exist, or belongs to someone else (which isn't revealed).
     *
     * @param id the log that was asked for
     * @return a 404 exception
     */
    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuel log " + id + " not found.");
    }
}
