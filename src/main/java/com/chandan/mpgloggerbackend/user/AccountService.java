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

    public AccountService(AppUserRepository users, UserSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

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

    /** Starts a session and returns the token for the cookie. Only its hash is saved. */
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

    /** The session for this cookie token, if it exists and hasn't expired. */
    @Transactional(readOnly = true)
    public Optional<UserSession> findActiveSession(String token) {
        Instant now = Instant.now();
        return sessions.findByTokenHash(hash(token)).filter(s -> s.getExpiresAt().isAfter(now));
    }

    /**
     * Pushes the session's expiry forward (at most once a day). Returns true if it moved,
     * meaning the cookie should be sent again with the new expiry.
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

    @Transactional
    public void endSession(String token) {
        sessions.deleteByTokenHash(hash(token));
    }

    @Transactional
    public AppUser updateName(AppUser user, String name) {
        String validName = AccountRules.validateName(name);
        AppUser stored = users.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in."));
        stored.setName(validName);
        return stored;
    }

    private static ResponseStatusException usernameTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken.");
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
