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

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    /** Who is signed in; 401 if nobody. The React app calls this on load to restore the session. */
    @GetMapping
    public AccountResponse get(AppUser user) {
        return AccountResponse.from(user);
    }

    /** Changes the name. The username can't be changed, and password changes are coming later. */
    @PutMapping
    public AccountResponse update(AppUser user, @RequestBody UpdateAccount body) {
        return AccountResponse.from(accounts.updateName(user, body.name()));
    }
}
