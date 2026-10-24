package com.chandan.mpgloggerbackend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chandan.mpgloggerbackend.TestUsers;
import com.chandan.mpgloggerbackend.fuellog.FuelLogRepository;
import com.chandan.mpgloggerbackend.user.AccountRequests.SignIn;
import com.chandan.mpgloggerbackend.user.AccountRequests.SignUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@link AccountService} cases that are hard to reach through the HTTP API: the dummy password,
 * a deleted account, and two sign-ups racing for the same username.
 */
@SpringBootTest
class AccountServiceTest {

    @Autowired
    private AccountService accounts;

    @Autowired
    private FuelLogRepository logs;

    @Autowired
    private UserSessionRepository sessions;

    @Autowired
    private AppUserRepository users;

    /** Starts every test with empty tables. */
    @BeforeEach
    void setUp() {
        TestUsers.deleteAll(logs, sessions, users);
    }

    /**
     * An unknown username is compared against a dummy hash. Typing that dummy's password must
     * still fail, not sign in as nobody.
     */
    @Test
    void dummyPasswordDoesNotSignInAnUnknownUser() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accounts.signIn(new SignIn("nobody", "not-a-real-password")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    /** Renaming an account that was deleted meanwhile (with its session still in hand) asks to sign in again. */
    @Test
    void renamingADeletedAccountIsUnauthorized() {
        AppUser user = accounts.signUp(new SignUp("chandan", "Chandan", "password123"));
        users.deleteAll();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accounts.updateName(user, "New Name"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    /**
     * Two sign-ups for the same username at the same moment both pass the "is it taken?" check;
     * the database's unique constraint stops the second, which must become a 409, not a 500.
     */
    @Test
    void usernameRaceIsAConflict() {
        AppUserRepository racingUsers = mock(AppUserRepository.class);
        when(racingUsers.existsByUsername("chandan")).thenReturn(false);
        when(racingUsers.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));
        AccountService service = new AccountService(racingUsers, mock(UserSessionRepository.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.signUp(new SignUp("chandan", "Chandan", "password123")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("That username is already taken.", ex.getReason());
    }
}
