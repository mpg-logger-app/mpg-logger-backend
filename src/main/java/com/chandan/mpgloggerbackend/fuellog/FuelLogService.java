package com.chandan.mpgloggerbackend.fuellog;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FuelLogService {

    private final FuelLogRepository repository;

    public FuelLogService(FuelLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FuelLogResponse> findAll() {
        return repository.findAllByOrderByOdometerDesc().stream()
                .map(FuelLogResponse::from)
                .toList();
    }

    /** Swift: logFuelEntry() */
    @Transactional
    public FuelLogResponse create(FuelLogRequest request) {
        Double latestOdometer = repository.findTopByOrderByOdometerDesc()
                .map(FuelLog::getOdometer)
                .orElse(null);

        double previous = FuelLogRules.resolvePreviousOdometer(latestOdometer, request.previousOdometer());

        FuelLogRules.validate(previous, request.odometer(), request.gallons(), request.pricePerGallon());

        FuelLog saved = repository.save(new FuelLog(
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
    public FuelLogResponse update(Long id, FuelLogRequest request) {
        FuelLog log = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuel log " + id + " not found."));

        double oldOdometer = log.getOdometer();
        boolean isOldest = repository.findTopByOdometerLessThanOrderByOdometerDesc(oldOdometer).isEmpty();
        FuelLog next = repository.findTopByOdometerGreaterThanOrderByOdometerAsc(oldOdometer).orElse(null);

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
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuel log " + id + " not found.");
        }
        repository.deleteById(id);
    }
}
