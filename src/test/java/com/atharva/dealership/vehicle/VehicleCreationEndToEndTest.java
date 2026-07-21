package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    void shouldUpdateVehicleWhenAuthenticatedAndReturnUpdatedVehicle() throws Exception {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        mockMvc.perform(put("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateVehicleJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingVehicle.getId()))
                .andExpect(jsonPath("$.make").value("Honda"))
                .andExpect(jsonPath("$.model").value("Civic"))
                .andExpect(jsonPath("$.category").value("Hatchback"))
                .andExpect(jsonPath("$.price").value(25500.00))
                .andExpect(jsonPath("$.quantityInStock").value(3));

        assertEquals(1, vehicleRepository.count());
        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals("Honda", persistedVehicle.getMake());
        assertEquals("Civic", persistedVehicle.getModel());
        assertEquals("Hatchback", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("25500.00"), persistedVehicle.getPrice());
        assertEquals(3, persistedVehicle.getQuantityInStock());
    }

    @Test
    void updateVehicleWithoutBearerTokenReturnsForbiddenAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        mockMvc.perform(put("/api/vehicles/{id}", existingVehicle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateVehicleJson()))
                .andExpect(status().isForbidden());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals("Toyota", persistedVehicle.getMake());
        assertEquals("Camry", persistedVehicle.getModel());
        assertEquals("Sedan", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("28000.00"), persistedVehicle.getPrice());
        assertEquals(5, persistedVehicle.getQuantityInStock());
    }

    @Test
    void updateMissingVehicleReturnsNotFoundAndDoesNotCreateVehicle() throws Exception {
        mockMvc.perform(put("/api/vehicles/{id}", 999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateVehicleJson()))
                .andExpect(status().isNotFound());

        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void updateVehicleWithInvalidPayloadReturnsBadRequestAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        mockMvc.perform(put("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidVehicleJson()))
                .andExpect(status().isBadRequest());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals("Toyota", persistedVehicle.getMake());
        assertEquals("Camry", persistedVehicle.getModel());
        assertEquals("Sedan", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("28000.00"), persistedVehicle.getPrice());
        assertEquals(5, persistedVehicle.getQuantityInStock());
    }

    @Test
    void updateVehicleToZeroStockRemovesVehicleFromAvailableListing() throws Exception {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        mockMvc.perform(put("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outOfStockVehicleJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityInStock").value(0));

        mockMvc.perform(get("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
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

    private String updateVehicleJson() {
        return """
                {
                  "make": "Honda",
                  "model": "Civic",
                  "category": "Hatchback",
                  "price": 25500.00,
                  "quantityInStock": 3
                }
                """;
    }

    private String outOfStockVehicleJson() {
        return """
                {
                  "make": "Toyota",
                  "model": "Camry",
                  "category": "Sedan",
                  "price": 28000.00,
                  "quantityInStock": 0
                }
                """;
    }
}
