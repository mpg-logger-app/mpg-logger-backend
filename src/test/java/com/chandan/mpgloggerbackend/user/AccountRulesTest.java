package com.chandan.mpgloggerbackend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AccountRulesTest {

    @Test
    void usernamesAreTrimmedAndLowercased() {
        assertEquals("chandan", AccountRules.normalizeUsername("  Chandan "));
        assertEquals("", AccountRules.normalizeUsername(null));
    }

    @Test
    void validUsernamesPass() {
        AccountRules.validateUsername("abc");
        AccountRules.validateUsername("chandan.j_42");
        AccountRules.validateUsername("a".repeat(30));
    }

    @Test
    void usernameLengthIsChecked() {
        assertThrows(InvalidAccountException.class, () -> AccountRules.validateUsername("ab"));
        assertThrows(InvalidAccountException.class, () -> AccountRules.validateUsername("a".repeat(31)));
    }

    @Test
    void usernameCharactersAreChecked() {
        assertThrows(InvalidAccountException.class, () -> AccountRules.validateUsername("has space"));
        assertThrows(InvalidAccountException.class, () -> AccountRules.validateUsername("me@mail"));
    }

    @Test
    void nameIsTrimmedAndRequired() {
        assertEquals("Chandan", AccountRules.validateName("  Chandan  "));
        assertThrows(InvalidAccountException.class, () -> AccountRules.validateName("   "));
        assertThrows(InvalidAccountException.class, () -> AccountRules.validateName(null));
        assertThrows(InvalidAccountException.class, () -> AccountRules.validateName("x".repeat(51)));
    }

    @Test
    void passwordLengthIsChecked() {
        AccountRules.validatePassword("12345678");
        assertThrows(InvalidAccountException.class, () -> AccountRules.validatePassword("1234567"));
        assertThrows(InvalidAccountException.class, () -> AccountRules.validatePassword(null));
        // 72 bytes is BCrypt's limit; "é" is 2 bytes in UTF-8.
        AccountRules.validatePassword("a".repeat(72));
        assertThrows(InvalidAccountException.class, () -> AccountRules.validatePassword("é".repeat(37)));
    }
}
