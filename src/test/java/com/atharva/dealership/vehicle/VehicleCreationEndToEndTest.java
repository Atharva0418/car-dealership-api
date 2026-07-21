package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleCreationEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String authToken;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
        authToken = jwtService.generateAccessToken("test.admin@example.com");
    }

    @Test
    void shouldCreateVehicleWhenAuthenticatedAsAdmin() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson()))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Camry"))
                .andExpect(jsonPath("$.category").value("Sedan"))
                .andExpect(jsonPath("$.price").value(28000.00))
                .andExpect(jsonPath("$.quantityInStock").value(5));

        assertEquals(1, vehicleRepository.count());
    }

    @Test
    void createVehicleWithoutBearerTokenReturnsForbiddenAndDoesNotPersistVehicle() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson()))
                .andExpect(status().isForbidden());

        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void createVehicleWithInvalidPayloadReturnsBadRequestAndDoesNotPersistVehicle() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidVehicleJson()))
                .andExpect(status().isBadRequest());

        assertEquals(0, vehicleRepository.count());
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

    private String invalidVehicleJson() {
        return """
                {
                  "make": "Toyota",
                  "model": "Camry",
                  "category": "Sedan",
                  "price": -1.00,
                  "quantityInStock": 5
                }
                """;
    }
}
