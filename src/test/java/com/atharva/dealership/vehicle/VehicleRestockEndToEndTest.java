package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/*
 * Red-phase end-to-end coverage for POST /api/vehicles/{id}/restock.
 * Expected authorization contract:
 * - missing/invalid bearer token: 401 Unauthorized
 * - authenticated non-admin: 403 Forbidden
 * - authenticated admin: restock is allowed
 */
@SpringBootTest
@AutoConfigureMockMvc
class VehicleRestockEndToEndTest {

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
    void restockVehicleAsAdminReturnsOkAndPersistsIncrementedQuantity() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingVehicle.getId()))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Camry"))
                .andExpect(jsonPath("$.category").value("Sedan"))
                .andExpect(jsonPath("$.price").value(28000.00))
                .andExpect(jsonPath("$.quantityInStock").value(8));

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(8, persistedVehicle.getQuantityInStock());
    }

    @Test
    void restockOutOfStockVehicleAsAdminMakesVehicleAvailableAgain() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(0);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityInStock").value(2));

        mockMvc.perform(get("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(existingVehicle.getId()))
                .andExpect(jsonPath("$[0].quantityInStock").value(2));
    }

    @Test
    void restockVehicleWithoutBearerTokenReturnsUnauthorizedAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(5)))
                .andExpect(status().isUnauthorized());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(3, persistedVehicle.getQuantityInStock());
    }

    @Test
    void restockVehicleWithInvalidBearerTokenReturnsUnauthorizedAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(5)))
                .andExpect(status().isUnauthorized());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(3, persistedVehicle.getQuantityInStock());
    }

    @Test
    void restockVehicleAsNonAdminReturnsForbiddenAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(5)))
                .andExpect(status().isForbidden());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(3, persistedVehicle.getQuantityInStock());
    }

    @Test
    void restockMissingVehicleAsAdminReturnsNotFoundAndDoesNotCreateVehicle() throws Exception {
        mockMvc.perform(post("/api/vehicles/{id}/restock", 999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(5)))
                .andExpect(status().isNotFound());

        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void restockVehicleWithZeroQuantityReturnsBadRequestAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(0)))
                .andExpect(status().isBadRequest());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(3, persistedVehicle.getQuantityInStock());
    }

    @Test
    void restockVehicleWithMissingQuantityReturnsBadRequestAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(3, persistedVehicle.getQuantityInStock());
    }

    @Test
    void restockVehicleWithMalformedIdReturnsBadRequestAndDoesNotModifyVehicle() throws Exception {
        saveVehicleWithQuantity(3);

        mockMvc.perform(post("/api/vehicles/not-a-number/restock")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restockJson(5)))
                .andExpect(status().isBadRequest());

        assertEquals(1, vehicleRepository.count());
    }

    private Vehicle saveVehicleWithQuantity(int quantityInStock) {
        return vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                quantityInStock));
    }

    private String restockJson(int quantity) {
        return """
                {
                  "quantity": %d
                }
                """.formatted(quantity);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
