package com.chandan.mpgloggerbackend.fuellog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for fuel logs. Spring Data writes the queries from the method names.
 * Every query is scoped to one user, so nobody sees or changes another user's logs.
 */
public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {

    /**
     * Swift: @Query(sort: \FuelLog.odometer, order: .reverse)
     *
     * @param userId whose logs
     * @return all of that user's logs, highest odometer first
     */
    List<FuelLog> findAllByUserIdOrderByOdometerDesc(Long userId);

    /**
     * Swift: fuelLogs.first (the log with the highest odometer).
     *
     * @param userId whose logs
     * @return the user's latest log, or empty if they have none
     */
    Optional<FuelLog> findTopByUserIdOrderByOdometerDesc(Long userId);

    /**
     * The log, but only if it belongs to this user.
     *
     * @param id     the log's id
     * @param userId who must own it
     * @return the log, or empty if it doesn't exist or belongs to someone else
     */
    Optional<FuelLog> findByIdAndUserId(Long id, Long userId);

    /**
     * The log just before the one with this odometer reading.
     *
     * @param userId   whose logs
     * @param odometer the reading to look below
     * @return the previous (older) log, or empty for the oldest log
     */
    Optional<FuelLog> findTopByUserIdAndOdometerLessThanOrderByOdometerDesc(Long userId, double odometer);

    /**
     * The log just after the one with this odometer reading.
     *
     * @param userId   whose logs
     * @param odometer the reading to look above
     * @return the next (newer) log, or empty for the latest log
     */
    Optional<FuelLog> findTopByUserIdAndOdometerGreaterThanOrderByOdometerAsc(Long userId, double odometer);
}
