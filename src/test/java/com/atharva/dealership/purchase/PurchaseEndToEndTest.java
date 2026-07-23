package com.atharva.dealership.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import com.atharva.dealership.vehicle.Vehicle;
import com.atharva.dealership.vehicle.VehicleRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String customerToken;
    private String otherCustomerToken;

    @BeforeEach
    void clearData() {
        purchaseRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new User("customer@example.com", "$2a$10$customer-password"));
        userRepository.save(new User("other.customer@example.com", "$2a$10$other-customer-password"));

        customerToken = jwtService.generateAccessToken("customer@example.com", "CUSTOMER");
        otherCustomerToken = jwtService.generateAccessToken("other.customer@example.com", "CUSTOMER");
    }

    @Test
    void purchaseVehicleCreatesPurchaseForAuthenticatedCustomerAndDecrementsStock() throws Exception {
        Vehicle vehicle = saveVehicle("Toyota", "Camry", "Sedan", "28000.00", 5);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", vehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk());

        Vehicle persistedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(4, persistedVehicle.getQuantityInStock());
        assertEquals(1, purchaseRepository.count());

        mockMvc.perform(get("/api/purchases/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vehicleId").value(vehicle.getId()))
                .andExpect(jsonPath("$[0].make").value("Toyota"))
                .andExpect(jsonPath("$[0].model").value("Camry"))
                .andExpect(jsonPath("$[0].category").value("Sedan"))
                .andExpect(jsonPath("$[0].price").value(28000.00))
                .andExpect(jsonPath("$[0].purchasedAt").exists());
    }

    @Test
    void myPurchasesReturnsOnlyPurchasesForAuthenticatedCustomer() throws Exception {
        Vehicle customerVehicle = saveVehicle("Honda", "Civic", "Sedan", "25000.00", 3);
        Vehicle otherCustomerVehicle = saveVehicle("Ford", "Bronco", "SUV", "42000.00", 2);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", customerVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/vehicles/{id}/purchase", otherCustomerVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherCustomerToken)))
                .andExpect(status().isOk());

        assertEquals(2, purchaseRepository.count());

        mockMvc.perform(get("/api/purchases/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vehicleId").value(customerVehicle.getId()))
                .andExpect(jsonPath("$[0].make").value("Honda"))
                .andExpect(jsonPath("$[0].model").value("Civic"));
    }

    @Test
    void purchaseOutOfStockVehicleDoesNotCreatePurchase() throws Exception {
        Vehicle vehicle = saveVehicle("Toyota", "Supra", "Coupe", "55000.00", 0);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", vehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isBadRequest());

        Vehicle persistedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(0, persistedVehicle.getQuantityInStock());
        assertEquals(0, purchaseRepository.count());

        mockMvc.perform(get("/api/purchases/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void purchaseMissingVehicleReturnsNotFoundAndDoesNotCreatePurchase() throws Exception {
        mockMvc.perform(post("/api/vehicles/{id}/purchase", 404L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isNotFound());

        assertEquals(0, purchaseRepository.count());
    }

    @Test
    void unauthenticatedCustomerCannotPurchaseOrViewPurchases() throws Exception {
        Vehicle vehicle = saveVehicle("Mazda", "CX-5", "SUV", "31000.00", 2);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", vehicle.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/purchases/me"))
                .andExpect(status().isUnauthorized());

        Vehicle persistedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(2, persistedVehicle.getQuantityInStock());
        assertEquals(0, purchaseRepository.count());
    }

    @Test
    void myPurchasesUsesVehicleSnapshotFromPurchaseTime() throws Exception {
        Vehicle vehicle = saveVehicle("BMW", "330i", "Sedan", "46000.00", 2);

        mockMvc.perform(post("/api/vehicles/{id}/purchase", vehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk());

        vehicleRepository.save(new Vehicle(
                vehicle.getId(),
                "BMW",
                "M340i",
                "Performance Sedan",
                new BigDecimal("59000.00"),
                1));

        mockMvc.perform(get("/api/purchases/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vehicleId").value(vehicle.getId()))
                .andExpect(jsonPath("$[0].make").value("BMW"))
                .andExpect(jsonPath("$[0].model").value("330i"))
                .andExpect(jsonPath("$[0].category").value("Sedan"))
                .andExpect(jsonPath("$[0].price").value(46000.00));
    }

    private Vehicle saveVehicle(
            String make,
            String model,
            String category,
            String price,
            int quantityInStock) {
        return vehicleRepository.save(new Vehicle(
                make,
                model,
                category,
                new BigDecimal(price),
                quantityInStock));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
