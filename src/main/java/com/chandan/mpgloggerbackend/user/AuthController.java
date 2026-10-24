package com.chandan.mpgloggerbackend.user;

import com.chandan.mpgloggerbackend.user.AccountRequests.AccountResponse;
import com.chandan.mpgloggerbackend.user.AccountRequests.SignIn;
import com.chandan.mpgloggerbackend.user.AccountRequests.SignUp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign-up, sign-in and sign-out. Signing up or in sets the session cookie. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    /** Creates the account and signs straight in. 409 if the username is taken. */
    @PostMapping("/signup")
    public ResponseEntity<AccountResponse> signUp(@RequestBody SignUp body,
            HttpServletRequest request, HttpServletResponse response) {
        AppUser user = accounts.signUp(body);
        SessionCookie.write(request, response, accounts.startSession(user));
        return ResponseEntity.created(URI.create("/api/account")).body(AccountResponse.from(user));
    }

    /** 401 with the same message whether the username or the password is wrong. */
    @PostMapping("/signin")
    public AccountResponse signIn(@RequestBody SignIn body,
            HttpServletRequest request, HttpServletResponse response) {
        AppUser user = accounts.signIn(body);
        SessionCookie.write(request, response, accounts.startSession(user));
        return AccountResponse.from(user);
    }

    /** Ends this browser's session. Always succeeds, even if it had already ended. */
    @PostMapping("/signout")
    public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response) {
        SessionCookie.read(request).ifPresent(accounts::endSession);
        SessionCookie.clear(request, response);
        return ResponseEntity.noContent().build();
    }
}
