package com.chandan.mpgloggerbackend.user;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    /** Housekeeping: run on each sign-in so old sessions don't pile up. */
    @Modifying
    @Query("delete from UserSession s where s.expiresAt < :now")
    void deleteExpired(Instant now);
}
