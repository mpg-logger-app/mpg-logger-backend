package com.chandan.mpgloggerbackend.user;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/** Lets a controller method take an {@code AppUser} parameter: the signed-in user found by {@link AuthInterceptor}. */
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    /**
     * @param parameter a controller method parameter
     * @return true for parameters of type {@link AppUser}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(AppUser.class);
    }

    /**
     * Hands the controller the user that {@link AuthInterceptor} put on the request.
     *
     * @param parameter     the {@code AppUser} parameter
     * @param mavContainer  not used
     * @param webRequest    the current request
     * @param binderFactory not used
     * @return the signed-in user
     * @throws ResponseStatusException 401 if there is none, which only happens if a public path
     *                                 (see WebConfig) asks for the user by mistake
     */
    @Override
    public AppUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object user = webRequest.getAttribute(AuthInterceptor.USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        return (AppUser) user;
    }
}
