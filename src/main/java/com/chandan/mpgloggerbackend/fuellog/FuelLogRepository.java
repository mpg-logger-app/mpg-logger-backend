package com.chandan.mpgloggerbackend.fuellog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {

    /** Swift: @Query(sort: \FuelLog.odometer, order: .reverse) */
    List<FuelLog> findAllByOrderByOdometerDesc();

    /** Swift: fuelLogs.first (the log with the highest odometer). */
    Optional<FuelLog> findTopByOrderByOdometerDesc();
}
