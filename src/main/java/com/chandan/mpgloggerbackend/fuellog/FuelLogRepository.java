package com.chandan.mpgloggerbackend.fuellog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Every query is scoped to one user, so nobody sees or changes another user's logs. */
public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {

    /** Swift: @Query(sort: \FuelLog.odometer, order: .reverse) */
    List<FuelLog> findAllByUserIdOrderByOdometerDesc(Long userId);

    /** Swift: fuelLogs.first (the log with the highest odometer). */
    Optional<FuelLog> findTopByUserIdOrderByOdometerDesc(Long userId);

    /** The log, but only if it belongs to this user. */
    Optional<FuelLog> findByIdAndUserId(Long id, Long userId);

    /** The log just before the one with this odometer reading (empty for the oldest log). */
    Optional<FuelLog> findTopByUserIdAndOdometerLessThanOrderByOdometerDesc(Long userId, double odometer);

    /** The log just after the one with this odometer reading (empty for the latest log). */
    Optional<FuelLog> findTopByUserIdAndOdometerGreaterThanOrderByOdometerAsc(Long userId, double odometer);
}
