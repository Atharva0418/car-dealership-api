package com.atharva.dealership.security;

import com.atharva.dealership.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        log.debug("Starting JWT authentication filter for {} {}", request.getMethod(), request.getRequestURI());
        boolean publicEndpoint = isPublicEndpoint(request);

        String authorization = request.getHeader("Authorization");
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtService.isValidAccessToken(token)) {
                String subject = jwtService.extractSubject(token);
                String role = jwtService.extractRole(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(subject, null, authoritiesFor(role));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated request for subject {} on {}", subject, request.getRequestURI());
            } else {
                log.debug("Ignoring invalid bearer token on {}", request.getRequestURI());
                if (!publicEndpoint) {
                    response.setStatus(statusForRejectedAuthentication(request));
                    return;
                }
            }
        } else {
            log.trace("No bearer token found on {}", request.getRequestURI());
            if (!publicEndpoint) {
                response.setStatus(statusForRejectedAuthentication(request));
                return;
            }
        }

        filterChain.doFilter(request, response);
        log.debug("JWT authentication filter completed for {} {}", request.getMethod(), request.getRequestURI());
    }

    private List<SimpleGrantedAuthority> authoritiesFor(String role) {
        if ("ADMIN".equals(role)) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return List.of();
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth/")
                || ("OPTIONS".equals(request.getMethod()) && request.getRequestURI().startsWith("/api/"))
                || ("HEAD".equals(request.getMethod()) && "/api/health".equals(request.getRequestURI()));
    }

    private int statusForRejectedAuthentication(HttpServletRequest request) {
        if ("DELETE".equals(request.getMethod()) && request.getRequestURI().startsWith("/api/vehicles/")) {
            return HttpServletResponse.SC_UNAUTHORIZED;
        }
        if ("POST".equals(request.getMethod())
                && request.getRequestURI().startsWith("/api/vehicles/")
                && request.getRequestURI().endsWith("/purchase")) {
            return HttpServletResponse.SC_UNAUTHORIZED;
        }
        if ("POST".equals(request.getMethod())
                && request.getRequestURI().startsWith("/api/vehicles/")
                && request.getRequestURI().endsWith("/restock")) {
            return HttpServletResponse.SC_UNAUTHORIZED;
        }
        return HttpServletResponse.SC_FORBIDDEN;
    }
}
