package com.computershare.regfiling.security;

import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Mocked-SSO auth filter (see openapi.yaml securitySchemes.bearerAuth). Real corporate SSO
 * integration is explicitly out of MVP scope (PRD constraint — no real integration attempted).
 * Token shape: "Bearer {userId}-token" resolves directly to a seeded User; this is a stand-in,
 * not a real token format, and must not be treated as a security pattern to reuse in production.
 *
 * Not a @Component: registered explicitly (and scoped to /api/*) via WebConfig's
 * FilterRegistrationBean, so it does not double-register through Spring Boot's default
 * auto-registration of Filter beans.
 */
public class AuthFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTR = "currentUser";

    private final UserRepository userRepository;

    public AuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // CORS preflight requests never carry the Authorization header (browser-enforced) — must
        // pass through untouched so Spring's CORS handling can answer it, or the browser blocks
        // the real request on a 401 preflight response with no CORS headers.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        Optional<User> user = resolveUser(header);

        if (user.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Missing or invalid Authorization bearer token\"}");
            return;
        }

        request.setAttribute(CURRENT_USER_ATTR, user.get());
        chain.doFilter(request, response);
    }

    private Optional<User> resolveUser(String header) {
        if (header == null || !header.startsWith("Bearer ") || !header.endsWith("-token")) {
            return Optional.empty();
        }
        String token = header.substring("Bearer ".length());
        String userId = token.substring(0, token.length() - "-token".length());
        return userRepository.findById(userId);
    }
}
