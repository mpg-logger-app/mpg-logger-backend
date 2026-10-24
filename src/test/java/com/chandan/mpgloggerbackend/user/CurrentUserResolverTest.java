package com.chandan.mpgloggerbackend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for {@link CurrentUserResolver}, which hands controllers the signed-in user. */
class CurrentUserResolverTest {

    private final CurrentUserResolver resolver = new CurrentUserResolver();

    /**
     * Stand-in controller method with one parameter of each kind.
     *
     * @param user  resolved by CurrentUserResolver
     * @param other not resolved by it
     */
    @SuppressWarnings("unused")
    void handler(AppUser user, String other) {
    }

    /**
     * @param index which parameter of {@link #handler}
     * @return that parameter, as Spring MVC would see it
     */
    private MethodParameter parameter(int index) throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handler", AppUser.class, String.class);
        return new MethodParameter(method, index);
    }

    /** Only {@code AppUser} parameters are filled in. */
    @Test
    void supportsOnlyAppUserParameters() throws Exception {
        assertTrue(resolver.supportsParameter(parameter(0)));
        assertFalse(resolver.supportsParameter(parameter(1)));
    }

    /** The user the interceptor left on the request is the one handed to the controller. */
    @Test
    void returnsTheUserFromTheRequest() throws Exception {
        AppUser user = new AppUser("chandan", "Chandan", "hash");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.USER_ATTRIBUTE, user);

        assertSame(user, resolver.resolveArgument(parameter(0), null, new ServletWebRequest(request), null));
    }

    /** If a public endpoint asked for the user by mistake, the request fails with 401 instead of a null user. */
    @Test
    void noUserIsUnauthorized() throws Exception {
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameter(0), null, request, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
