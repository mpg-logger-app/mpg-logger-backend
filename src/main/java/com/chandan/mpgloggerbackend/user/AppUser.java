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

    @Column(nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected AppUser() {
    }

    public AppUser(String username, String name, String passwordHash) {
        this.username = username;
        this.name = name;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
