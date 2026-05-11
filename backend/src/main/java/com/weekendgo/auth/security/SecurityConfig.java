package com.weekendgo.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekendgo.auth.AuthTokenStore;
import com.weekendgo.auth.UserAccountRepository;
import com.weekendgo.common.api.ApiResponse;
import com.weekendgo.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthTokenStore authTokenStore,
            UserAccountRepository userAccountRepository,
            ObjectMapper objectMapper
    ) throws Exception {
        BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter =
                new BearerTokenAuthenticationFilter(authTokenStore, userAccountRepository);

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/workspaces/nearby",
                                "/api/workspaces/search",
                                "/api/places/**",
                                "/api/reviews/*/replies",
                                "/api/questions/*/answers",
                                "/api/map/markers"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response,
                                objectMapper,
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHORIZED",
                                "Authentication required",
                                request.getRequestURI()
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response,
                                objectMapper,
                                HttpStatus.FORBIDDEN,
                                "FORBIDDEN",
                                "Access denied",
                                request.getRequestURI()
                        ))
                )
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String code,
            String message,
            String path
    ) throws java.io.IOException {
        ErrorResponse error = ErrorResponse.of(code, message, path);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(code, message, error));
    }
}
