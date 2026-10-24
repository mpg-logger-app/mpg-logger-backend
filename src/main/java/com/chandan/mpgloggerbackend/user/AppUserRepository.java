package com.chandan.mpgloggerbackend.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for accounts. Spring Data writes the queries from the method names. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Looks up an account for sign-in.
     *
     * @param username a username that has been through AccountRules.normalizeUsername
     * @return the user, or empty if there is no such account
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Whether a username is taken, checked before sign-up.
     *
     * @param username a username that has been through AccountRules.normalizeUsername
     * @return true if an account already has it
     */
    boolean existsByUsername(String username);
}
