package com.atharva.dealership.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProtectedTestController())
            .addFilters(new TestAuthorizationFilter(), new JwtAuthenticationFilter(jwtService))
            .build();

    @Test
    void publicAuthEndpointsRemainAccessibleWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsMissingBearerToken() throws Exception {
        mockMvc.perform(get("/protected-test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointRejectsInvalidBearerToken() throws Exception {
        when(jwtService.isValidAccessToken("bad.jwt")).thenReturn(false);

        mockMvc.perform(get("/protected-test")
                        .header("Authorization", "Bearer bad.jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointAcceptsValidBearerToken() throws Exception {
        when(jwtService.isValidAccessToken("good.jwt")).thenReturn(true);
        when(jwtService.extractSubject("good.jwt")).thenReturn("valid.user@example.com");

        mockMvc.perform(get("/protected-test")
                        .header("Authorization", "Bearer good.jwt"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected"));
    }

    @RestController
    static class ProtectedTestController {

        @PostMapping("/api/auth/login")
        void login() {
        }

        @GetMapping("/protected-test")
        String protectedEndpoint() {
            return "protected";
        }
    }

    static class TestAuthorizationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                jakarta.servlet.FilterChain filterChain) throws java.io.IOException, ServletException {
            SecurityContextHolder.clearContext();
            try {
                filterChain.doFilter(request, response);
                if (request.getRequestURI().startsWith("/api/auth/")) {
                    return;
                }
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    response.setStatus(403);
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
