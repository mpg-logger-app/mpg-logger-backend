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

    /** @param accounts does the work; this class only maps HTTP to it */
    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    /**
     * {@code POST /api/auth/signup}: creates the account and signs straight in.
     *
     * @param body     the username, name and password
     * @param request  the incoming request (decides whether the cookie is Secure)
     * @param response where the session cookie is set
     * @return {@code 201 Created} with the new account; 400 if a rule is broken, 409 if the username is taken
     */
    @PostMapping("/signup")
    public ResponseEntity<AccountResponse> signUp(@RequestBody SignUp body,
            HttpServletRequest request, HttpServletResponse response) {
        AppUser user = accounts.signUp(body);
        SessionCookie.write(request, response, accounts.startSession(user));
        return ResponseEntity.created(URI.create("/api/account")).body(AccountResponse.from(user));
    }

    /**
     * {@code POST /api/auth/signin}: signs in. 401 with the same message whether the username or the
     * password is wrong.
     *
     * @param body     the username and password
     * @param request  the incoming request (decides whether the cookie is Secure)
     * @param response where the session cookie is set
     * @return the account
     */
    @PostMapping("/signin")
    public AccountResponse signIn(@RequestBody SignIn body,
            HttpServletRequest request, HttpServletResponse response) {
        AppUser user = accounts.signIn(body);
        SessionCookie.write(request, response, accounts.startSession(user));
        return AccountResponse.from(user);
    }

    /**
     * {@code POST /api/auth/signout}: ends this browser's session. Always succeeds, even if it had already ended.
     *
     * @param request  carries the session cookie, if any
     * @param response where the cookie is cleared
     * @return {@code 204 No Content}
     */
    @PostMapping("/signout")
    public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response) {
        SessionCookie.read(request).ifPresent(accounts::endSession);
        SessionCookie.clear(request, response);
        return ResponseEntity.noContent().build();
    }
}
