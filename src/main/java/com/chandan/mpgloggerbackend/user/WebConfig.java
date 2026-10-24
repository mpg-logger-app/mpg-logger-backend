package com.chandan.mpgloggerbackend.user;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Every /api endpoint needs a signed-in user, except the ones that sign you in or out. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Endpoints that work without a session. */
    static final List<String> PUBLIC_PATHS = List.of("/api/auth/signup", "/api/auth/signin", "/api/auth/signout");

    private final AuthInterceptor authInterceptor;

    /** @param authInterceptor checks the session cookie */
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Puts {@link AuthInterceptor} in front of every /api path except {@link #PUBLIC_PATHS}.
     *
     * @param registry Spring MVC's interceptor list
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(PUBLIC_PATHS);
    }

    /**
     * Lets controller methods take the signed-in {@link AppUser} as a parameter.
     *
     * @param resolvers Spring MVC's argument resolver list
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserResolver());
    }
}
