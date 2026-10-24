package com.chandan.mpgloggerbackend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A signed-in browser. The browser keeps a random token in a cookie; only its SHA-256
 * hash is stored here, so a leaked database can't be used to sign in.
 * Sessions are kept in the database (not server memory) so they survive restarts and redeploys.
 */
@Entity
@Table(name = "user_session")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Eager (the default for @ManyToOne): every authenticated request needs the user.
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** When the user signed in on this browser; kept in the database for reference. */
    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    /** Required by JPA. */
    protected UserSession() {
    }

    /**
     * A new session.
     *
     * @param user      who signed in
     * @param tokenHash the SHA-256 of the cookie token (see AccountService.hash)
     * @param createdAt when they signed in
     * @param expiresAt when the session ends unless renewed
     */
    public UserSession(AppUser user, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** @return who this session belongs to */
    public AppUser getUser() {
        return user;
    }

    /** @return the SHA-256 of the cookie token */
    public String getTokenHash() {
        return tokenHash;
    }

    /** @return when the session ends unless renewed */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** @param expiresAt the new end of the session (see AccountService.renewSession) */
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
