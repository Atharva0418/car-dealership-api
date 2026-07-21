package com.atharva.dealership.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;

@WebMvcTest(controllers = JwtAuthenticationFilterTest.ProtectedTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationFilterTest.TestBeans.class})
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void publicAuthEndpointsRemainAccessibleWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
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

        @GetMapping("/protected-test")
        String protectedEndpoint() {
            return "protected";
        }
    }

    @Configuration
    @EnableWebSecurity
    @EnableAutoConfiguration
    static class TestBeans {

        @Bean
        PasswordEncoderConfig passwordEncoderConfig() {
            return new PasswordEncoderConfig();
        }
    }
}
