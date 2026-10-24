package com.chandan.mpgloggerbackend.user;

/** Request and response bodies for the auth and account endpoints. Checked by AccountRules, not bean validation, for readable messages. */
public final class AccountRequests {

    /** Only holds the records below; never instantiated. */
    private AccountRequests() {
    }

    /**
     * POST /api/auth/signup
     *
     * @param username the wanted username (any case; stored lowercase)
     * @param name     the name shown in the app
     * @param password the password, hashed before it is stored
     */
    public record SignUp(String username, String name, String password) {
    }

    /**
     * POST /api/auth/signin
     *
     * @param username the username (any case)
     * @param password the password
     */
    public record SignIn(String username, String password) {
    }

    /**
     * PUT /api/account (only the name for now; password changes are coming later).
     *
     * @param name the new name
     */
    public record UpdateAccount(String name) {
    }

    /**
     * What the API returns for the signed-in user. Never includes the password hash.
     *
     * @param id       the user's id
     * @param username the username, lowercase
     * @param name     the name shown in the app
     */
    public record AccountResponse(Long id, String username, String name) {

        /**
         * Copies the public details of a user.
         *
         * @param user the stored user
         * @return the response for the API
         */
        public static AccountResponse from(AppUser user) {
            return new AccountResponse(user.getId(), user.getUsername(), user.getName());
        }
    }
}
