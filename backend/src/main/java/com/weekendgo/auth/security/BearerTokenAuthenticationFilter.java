package com.weekendgo.auth.security;

import com.weekendgo.auth.AuthTokenStore;
import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.auth.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenStore authTokenStore;
    private final UserAccountRepository userAccountRepository;

    public BearerTokenAuthenticationFilter(
            AuthTokenStore authTokenStore,
            UserAccountRepository userAccountRepository
    ) {
        this.authTokenStore = authTokenStore;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length());
            authTokenStore.findUserId(token)
                    .flatMap(userAccountRepository::findById)
                    .ifPresent(account -> {
                        AuthenticatedUser user = new AuthenticatedUser(account);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        filterChain.doFilter(request, response);
    }
}
