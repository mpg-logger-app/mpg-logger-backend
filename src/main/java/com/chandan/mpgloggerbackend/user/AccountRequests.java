package com.chandan.mpgloggerbackend.user;

/** Request and response bodies for the auth and account endpoints. Checked by AccountRules, not bean validation, for readable messages. */
public final class AccountRequests {

    private AccountRequests() {
    }

    /** POST /api/auth/signup */
    public record SignUp(String username, String name, String password) {
    }

    /** POST /api/auth/signin */
    public record SignIn(String username, String password) {
    }

    /** PUT /api/account (only the name for now; password changes are coming later). */
    public record UpdateAccount(String name) {
    }

    /** What the API returns for the signed-in user. Never includes the password hash. */
    public record AccountResponse(Long id, String username, String name) {

        public static AccountResponse from(AppUser user) {
            return new AccountResponse(user.getId(), user.getUsername(), user.getName());
        }
    }
}
