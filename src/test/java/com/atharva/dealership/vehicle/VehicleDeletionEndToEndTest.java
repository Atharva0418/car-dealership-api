package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

/*
 * Red-phase end-to-end coverage for DELETE /api/vehicles/{id}.
 * Expected authorization contract:
 * - missing/invalid bearer token: 401 Unauthorized
 * - authenticated non-admin: 403 Forbidden
 * - authenticated admin: delete is allowed
 */
@SpringBootTest
@AutoConfigureMockMvc
class VehicleDeletionEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
        adminToken = jwtService.generateAccessToken("admin@example.com");
        customerToken = jwtService.generateAccessToken("customer@example.com");
    }

    @Test
    void deleteVehicleAsAdminReturnsNoContentAndRemovesVehicleFromListing() throws Exception {
        Vehicle existingVehicle = saveVehicle();

        mockMvc.perform(delete("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertEquals(0, vehicleRepository.count());
        assertFalse(vehicleRepository.findById(existingVehicle.getId()).isPresent());

        mockMvc.perform(get("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    void deleteVehicleWithoutBearerTokenReturnsUnauthorizedAndDoesNotDeleteVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicle();

        mockMvc.perform(delete("/api/vehicles/{id}", existingVehicle.getId()))
                .andExpect(status().isUnauthorized());

        assertEquals(1, vehicleRepository.count());
        assertFalse(vehicleRepository.findById(existingVehicle.getId()).isEmpty());
    }

    @Test
    void deleteVehicleWithInvalidBearerTokenReturnsUnauthorizedAndDoesNotDeleteVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicle();

        mockMvc.perform(delete("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt"))
                .andExpect(status().isUnauthorized());

        assertEquals(1, vehicleRepository.count());
        assertFalse(vehicleRepository.findById(existingVehicle.getId()).isEmpty());
    }

    @Test
    void deleteVehicleAsNonAdminReturnsForbiddenAndDoesNotDeleteVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicle();

        mockMvc.perform(delete("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());

        assertEquals(1, vehicleRepository.count());
        assertFalse(vehicleRepository.findById(existingVehicle.getId()).isEmpty());
    }

    @Test
    void deleteMissingVehicleAsAdminReturnsNotFoundAndDoesNotDeleteExistingVehicle() throws Exception {
        saveVehicle();

        mockMvc.perform(delete("/api/vehicles/{id}", 999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());

        assertEquals(1, vehicleRepository.count());
    }

    @Test
    void deleteVehicleWithMalformedIdAsAdminReturnsBadRequestAndDoesNotDeleteVehicle() throws Exception {
        saveVehicle();

        mockMvc.perform(delete("/api/vehicles/not-a-number")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());

        assertEquals(1, vehicleRepository.count());
    }

    @Test
    void deletingSameVehicleTwiceAsAdminReturnsNotFoundOnSecondRequest() throws Exception {
        Vehicle existingVehicle = saveVehicle();

        mockMvc.perform(delete("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());

        assertEquals(0, vehicleRepository.count());
    }

    private Vehicle saveVehicle() {
        return vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
