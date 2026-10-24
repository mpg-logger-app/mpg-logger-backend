package com.chandan.mpgloggerbackend.user;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Database access for sessions. Sessions are looked up by the hash of the cookie token. */
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * @param tokenHash the SHA-256 of a cookie token
     * @return the session, or empty if there is none (expired ones included; the caller checks)
     */
    Optional<UserSession> findByTokenHash(String tokenHash);

    /**
     * Ends a session. Does nothing if there is no such session.
     *
     * @param tokenHash the SHA-256 of the cookie token
     */
    void deleteByTokenHash(String tokenHash);

    /**
     * Housekeeping: run on each sign-in so old sessions don't pile up.
     *
     * @param now sessions that expired before this are deleted
     */
    @Modifying
    @Query("delete from UserSession s where s.expiresAt < :now")
    void deleteExpired(Instant now);
}
