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
import org.springframework.test.web.servlet.MockMvc;

/*
 * Red-phase end-to-end coverage for POST /api/vehicles/{id}/purchase.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VehiclePurchaseEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String customerToken;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
        customerToken = jwtService.generateAccessToken("customer@example.com", "CUSTOMER");
    }

    @Test
    void purchaseVehicleWhenAuthenticatedReturnsOkAndPersistsDecrementedQuantity() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(5);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingVehicle.getId()))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Camry"))
                .andExpect(jsonPath("$.category").value("Sedan"))
                .andExpect(jsonPath("$.price").value(28000.00))
                .andExpect(jsonPath("$.quantityInStock").value(4));

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(4, persistedVehicle.getQuantityInStock());
    }

    @Test
    void purchaseVehicleWithoutBearerTokenReturnsUnauthorizedAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(5);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", existingVehicle.getId()))
                .andExpect(status().isUnauthorized());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(5, persistedVehicle.getQuantityInStock());
    }

    @Test
    void purchaseOutOfStockVehicleWhenAuthenticatedReturnsBadRequestAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(0);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isBadRequest());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(0, persistedVehicle.getQuantityInStock());

        mockMvc.perform(get("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    void purchaseVehicleAsAdminReturnsForbiddenAndDoesNotModifyVehicle() throws Exception {
        Vehicle existingVehicle = saveVehicleWithQuantity(5);
        String adminToken = jwtService.generateAccessToken("admin@example.com", "ADMIN");

        mockMvc.perform(post("/api/vehicles/{id}/purchase", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isForbidden());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(5, persistedVehicle.getQuantityInStock());
    }

    private Vehicle saveVehicleWithQuantity(int quantityInStock) {
        return vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                quantityInStock));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
