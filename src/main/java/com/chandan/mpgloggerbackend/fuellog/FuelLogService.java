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

    /** Swift: deleteLogs(at:) */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuel log " + id + " not found.");
        }
        repository.deleteById(id);
    }
}
