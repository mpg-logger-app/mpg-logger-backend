package com.chandan.mpgloggerbackend.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Pass a username that has been through AccountRules.normalizeUsername. */
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
