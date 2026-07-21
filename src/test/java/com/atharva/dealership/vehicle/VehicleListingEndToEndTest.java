package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import java.math.BigDecimal;
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
class VehicleListingEndToEndTest {

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
        authToken = jwtService.generateAccessToken("test.user@example.com");
    }

    @Test
    void getVehiclesReturnsOnlyAvailableVehiclesForAuthenticatedUser() throws Exception {
        vehicleRepository.save(new Vehicle(
                "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5));
        vehicleRepository.save(new Vehicle(
                "Honda", "Civic", "Sedan", new BigDecimal("25000.00"), 0));

        mockMvc.perform(get("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].make").value("Toyota"))
                .andExpect(jsonPath("$[0].quantityInStock").value(5))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void getVehiclesWithoutBearerTokenReturnsForbiddenAndDoesNotChangeInventory() throws Exception {
        vehicleRepository.save(new Vehicle(
                "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isForbidden());

        assertEquals(1, vehicleRepository.count());
    }
}
