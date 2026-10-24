package com.chandan.mpgloggerbackend.user;

import com.chandan.mpgloggerbackend.user.AccountRequests.AccountResponse;
import com.chandan.mpgloggerbackend.user.AccountRequests.UpdateAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own details. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accounts;

    /** @param accounts does the work; this class only maps HTTP to it */
    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    /**
     * {@code GET /api/account}: who is signed in. The React app calls this on load to restore the session.
     *
     * @param user the signed-in user (no session answers 401 before this runs)
     * @return the user's id, username and name
     */
    @GetMapping
    public AccountResponse get(AppUser user) {
        return AccountResponse.from(user);
    }

    /**
     * {@code PUT /api/account}: changes the name. The username can't be changed, and password changes are
     * coming later.
     *
     * @param user the signed-in user
     * @param body the new name
     * @return the updated details; 400 if the name breaks a rule
     */
    @PutMapping
    public AccountResponse update(AppUser user, @RequestBody UpdateAccount body) {
        return AccountResponse.from(accounts.updateName(user, body.name()));
    }
}
