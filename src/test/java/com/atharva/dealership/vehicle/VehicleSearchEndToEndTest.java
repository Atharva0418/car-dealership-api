package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import java.math.BigDecimal;
import java.util.List;
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
class VehicleSearchEndToEndTest {

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
    void searchVehiclesReturnsFilteredAvailableVehiclesForAuthenticatedUser() throws Exception {
        vehicleRepository.saveAll(List.of(
                new Vehicle("Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5),
                new Vehicle("Toyota", "RAV4", "SUV", new BigDecimal("34000.00"), 4),
                new Vehicle("Honda", "Accord", "Sedan", new BigDecimal("29000.00"), 3),
                new Vehicle("Toyota", "Corolla", "Sedan", new BigDecimal("24000.00"), 0)));

        mockMvc.perform(get("/api/vehicles/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .param("make", " toyota ")
                        .param("category", "SEDAN")
                        .param("minPrice", "25000.00")
                        .param("maxPrice", "30000.00"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].make").value("Toyota"))
                .andExpect(jsonPath("$[0].model").value("Camry"))
                .andExpect(jsonPath("$[0].category").value("Sedan"))
                .andExpect(jsonPath("$[0].price").value(28000.00))
                .andExpect(jsonPath("$[0].quantityInStock").value(5))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void searchVehiclesWithInvalidPriceRangeReturnsBadRequestAndDoesNotChangeInventory() throws Exception {
        vehicleRepository.save(new Vehicle(
                "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5));

        mockMvc.perform(get("/api/vehicles/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .param("minPrice", "30000.00")
                        .param("maxPrice", "20000.00"))
                .andExpect(status().isBadRequest());

        assertEquals(1, vehicleRepository.count());
    }

    @Test
    void searchVehiclesWithNonNumericPriceReturnsBadRequestAndDoesNotChangeInventory() throws Exception {
        vehicleRepository.save(new Vehicle(
                "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5));

        mockMvc.perform(get("/api/vehicles/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .param("minPrice", "cheap"))
                .andExpect(status().isBadRequest());

        assertEquals(1, vehicleRepository.count());
    }

    @Test
    void searchVehiclesWithoutBearerTokenReturnsForbiddenAndDoesNotChangeInventory() throws Exception {
        vehicleRepository.save(new Vehicle(
                "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5));

        mockMvc.perform(get("/api/vehicles/search")
                        .param("make", "Toyota"))
                .andExpect(status().isForbidden());

        assertEquals(1, vehicleRepository.count());
    }
}
