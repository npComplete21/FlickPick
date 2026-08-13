package com.flickpick.userservice.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection defends against a browser being tricked into
                // submitting a cookie-authenticated request to us unknowingly.
                // We have no cookies/sessions at all — every request carries its
                // own bearer token explicitly — so there's no ambient credential
                // for CSRF to exploit in the first place.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight (OPTIONS) requests never carry the
                        // Authorization header, so anyRequest().authenticated()
                        // would reject them before Spring MVC's CORS handling
                        // ever runs — the browser then reports the failed real
                        // request as a CORS error rather than a 401.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Spring Boot's own error handling internally forwards to
                        // /error to render a thrown ResponseStatusException (e.g.
                        // our 409 on duplicate signup). That forwarded request
                        // re-enters this same filter chain as if it were a fresh,
                        // unauthenticated request — without this, Security would
                        // reject it and silently overwrite our intended status
                        // with an empty 403.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                // Without this, Spring Security's default response to a missing
                // or invalid token is 403 (Forbidden) via Http403ForbiddenEntryPoint,
                // since no login form or HTTP Basic is configured to give it a
                // real "challenge" response. For a bearer-token API, 401
                // (Unauthorized — "who even are you") is the correct code for a
                // missing/invalid token; 403 should mean "I know who you are,
                // you're just not allowed."
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
