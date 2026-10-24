package com.chandan.mpgloggerbackend.user;

import com.chandan.mpgloggerbackend.user.AccountRequests.SignIn;
import com.chandan.mpgloggerbackend.user.AccountRequests.SignUp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Sign-up, sign-in, sessions and account details. */
@Service
public class AccountService {

    /**
     * How long a session lasts without being used. Browsers cap cookies at 400 days, and the
     * expiry is pushed forward as the app is used, so in practice you stay signed in until you sign out.
     */
    static final Duration SESSION_LENGTH = Duration.ofDays(400);

    /** Pushing the expiry forward is a database write, so it's done at most this often per session. */
    static final Duration RENEW_AFTER = Duration.ofDays(1);

    private static final String BAD_CREDENTIALS = "Wrong username or password.";

    private final AppUserRepository users;
    private final UserSessionRepository sessions;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    /** Checked against when the username doesn't exist, so a wrong username takes as long as a wrong password. */
    private final String dummyHash = passwordEncoder.encode("not-a-real-password");

    /**
     * @param users    where accounts are stored
     * @param sessions where signed-in browsers are stored
     */
    public AccountService(AppUserRepository users, UserSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    /**
     * Creates an account. The username is lowercased and the password is stored only as a BCrypt hash.
     *
     * @param request the username, name and password
     * @return the saved user
     * @throws InvalidAccountException if a rule in {@link AccountRules} is broken
     * @throws ResponseStatusException 409 if the username is already taken
     */
    @Transactional
    public AppUser signUp(SignUp request) {
        String username = AccountRules.normalizeUsername(request.username());
        AccountRules.validateUsername(username);
        String name = AccountRules.validateName(request.name());
        AccountRules.validatePassword(request.password());

        if (users.existsByUsername(username)) {
            throw usernameTaken();
        }
        try {
            return users.saveAndFlush(new AppUser(username, name, passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups with the same username at the same moment: the unique constraint catches the second.
            throw usernameTaken();
        }
    }

    /**
     * Checks a username and password. A wrong username and a wrong password get the same answer
     * and take the same time, so it can't be used to find out which usernames exist.
     *
     * @param request the username (any case) and password
     * @return the user
     * @throws ResponseStatusException 401 "Wrong username or password."
     */
    @Transactional(readOnly = true)
    public AppUser signIn(SignIn request) {
        String username = AccountRules.normalizeUsername(request.username());
        String password = request.password() == null ? "" : request.password();
        Optional<AppUser> user = users.findByUsername(username);
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        if (!passwordEncoder.matches(password, hash) || user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        return user.get();
    }

    /**
     * Starts a session and returns the token for the cookie. Only its hash is saved.
     * Also clears out expired sessions.
     *
     * @param user who is signing in
     * @return a random 256-bit token, base64url-encoded, for the session cookie
     */
    @Transactional
    public String startSession(AppUser user) {
        Instant now = Instant.now();
        sessions.deleteExpired(now);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new UserSession(user, hash(token), now, now.plus(SESSION_LENGTH)));
        return token;
    }

    /**
     * The session for this cookie token, if it exists and hasn't expired.
     *
     * @param token the value of the session cookie
     * @return the session (with its user), or empty
     */
    @Transactional(readOnly = true)
    public Optional<UserSession> findActiveSession(String token) {
        Instant now = Instant.now();
        return sessions.findByTokenHash(hash(token)).filter(s -> s.getExpiresAt().isAfter(now));
    }

    /**
     * Pushes the session's expiry forward (at most once a day).
     *
     * @param session a session from {@link #findActiveSession}
     * @return true if it moved, meaning the cookie should be sent again with the new expiry
     */
    @Transactional
    public boolean renewSession(UserSession session) {
        Instant now = Instant.now();
        Instant renewed = now.plus(SESSION_LENGTH);
        if (renewed.isBefore(session.getExpiresAt().plus(RENEW_AFTER))) {
            return false;
        }
        session.setExpiresAt(renewed);
        sessions.save(session);
        return true;
    }

    /**
     * Signs out one browser. Does nothing if the session had already ended.
     *
     * @param token the value of the session cookie
     */
    @Transactional
    public void endSession(String token) {
        sessions.deleteByTokenHash(hash(token));
    }

    /**
     * Changes a user's name.
     *
     * @param user the signed-in user
     * @param name the new name
     * @return the updated user
     * @throws InvalidAccountException if the name breaks a rule
     * @throws ResponseStatusException 401 if the account no longer exists
     */
    @Transactional
    public AppUser updateName(AppUser user, String name) {
        String validName = AccountRules.validateName(name);
        AppUser stored = users.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in."));
        stored.setName(validName);
        return stored;
    }

    /** @return the 409 error for a username someone else already has */
    private static ResponseStatusException usernameTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken.");
    }

    /**
     * What is stored for a session token: its SHA-256, so a leaked database can't be used to sign in.
     *
     * @param token the cookie value
     * @return the SHA-256 of the token, as 64 lowercase hex characters
     */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
