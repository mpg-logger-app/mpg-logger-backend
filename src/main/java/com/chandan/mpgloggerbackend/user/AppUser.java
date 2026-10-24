package com.chandan.mpgloggerbackend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Someone who can sign in. Each fuel log belongs to one user.
 * The table is "app_user" because "user" is an SQL keyword.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always stored lowercase (see AccountRules.normalizeUsername), so "Chandan" and "chandan" are the same user. */
    @Column(nullable = false, unique = true, length = AccountRules.USERNAME_MAX)
    private String username;

    @Column(nullable = false, length = AccountRules.NAME_MAX)
    private String name;

    /** BCrypt hash; the password itself is never stored. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** When the account was created; kept in the database for reference, not used by the app. */
    @Column(nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected AppUser() {
    }

    /**
     * A new account, created now.
     *
     * @param username     already normalized and validated (see AccountRules)
     * @param name         already validated
     * @param passwordHash the BCrypt hash of the password
     */
    public AppUser(String username, String name, String passwordHash) {
        this.username = username;
        this.name = name;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    /** @return the database id, or null before the user is saved */
    public Long getId() {
        return id;
    }

    /** @return the username, lowercase */
    public String getUsername() {
        return username;
    }

    /** @return the name shown in the app */
    public String getName() {
        return name;
    }

    /** @param name the new name, already validated (see AccountRules.validateName) */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the BCrypt hash of the password */
    public String getPasswordHash() {
        return passwordHash;
    }
}
