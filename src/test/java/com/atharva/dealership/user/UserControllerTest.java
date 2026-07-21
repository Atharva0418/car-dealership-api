package com.atharva.dealership.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.dto.RegisterUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/*
 * Assumed controller contract for the green step:
 * - UserController(UserService userService)
 * - POST /api/users/register accepts JSON matching RegisterUserRequest
 * - malformed request shape maps to HTTP 400
 * - UserService.register(RegisterUserRequest) is not called for malformed input
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
    }

    @Test
    void registerWithMissingEmailReturnsBadRequestAndDoesNotCallService() throws Exception {
        String requestJson = """
                {
                  "password": "StrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(userService, times(0)).register(any(RegisterUserRequest.class));
    }

    @Test
    void registerWithMissingPasswordReturnsBadRequestAndDoesNotCallService() throws Exception {
        String requestJson = """
                {
                  "email": "valid.user@example.com"
                }
                """;

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(userService, times(0)).register(any(RegisterUserRequest.class));
    }
}
