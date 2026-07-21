package com.atharva.dealership.vehicle;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import com.atharva.dealership.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

/*
 * Red-phase security coverage for the vehicle creation route.
 * The controller below is test-only; production VehicleController still must be built later.
 */
class VehicleEndpointSecurityTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new VehicleCreationTestController())
            .addFilters(new TestAuthorizationFilter(), new JwtAuthenticationFilter(jwtService))
            .build();

    @Test
    void createVehicleRejectsRequestWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createVehicleRejectsRequestWithInvalidBearerToken() throws Exception {
        when(jwtService.isValidAccessToken("bad.jwt")).thenReturn(false);

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", "Bearer bad.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createVehicleAllowsRequestWithValidBearerToken() throws Exception {
        when(jwtService.isValidAccessToken("good.jwt")).thenReturn(true);
        when(jwtService.extractSubject("good.jwt")).thenReturn("valid.user@example.com");

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", "Bearer good.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson()))
                .andExpect(status().isCreated())
                .andExpect(content().string("created"));
    }

    private String validVehicleJson() {
        return """
                {
                  "make": "Toyota",
                  "model": "Camry",
                  "category": "Sedan",
                  "price": 28000.00,
                  "quantityInStock": 5
                }
                """;
    }

    @RestController
    static class VehicleCreationTestController {

        @PostMapping("/api/vehicles")
        org.springframework.http.ResponseEntity<String> create() {
            return org.springframework.http.ResponseEntity.status(201).body("created");
        }
    }

    static class TestAuthorizationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                FilterChain filterChain) throws IOException, ServletException {
            SecurityContextHolder.clearContext();
            try {
                filterChain.doFilter(request, response);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    response.setStatus(403);
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
