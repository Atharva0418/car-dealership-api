package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.auth.JwtService;
import java.lang.reflect.Method;
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
class VehicleAdminAuthorizationEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String adminToken;
    private String customerTokenWithAdminLookingEmail;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
        adminToken = accessToken("evaluator.admin@example.com", "ADMIN");
        customerTokenWithAdminLookingEmail = accessToken("admin.customer@example.com", "CUSTOMER");
    }

    @Test
    void customerRoleCannotCreateVehicleEvenWhenEmailLocalPartContainsAdmin() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerTokenWithAdminLookingEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("Toyota", "Camry", "Sedan", "28000.00", 5)))
                .andExpect(status().isForbidden());

        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void customerRoleCannotUpdateVehicleAndExistingVehicleRemainsUnchanged() throws Exception {
        Vehicle existingVehicle = saveVehicle(5);

        mockMvc.perform(put("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerTokenWithAdminLookingEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("Honda", "Civic", "Hatchback", "25500.00", 3)))
                .andExpect(status().isForbidden());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals("Toyota", persistedVehicle.getMake());
        assertEquals("Camry", persistedVehicle.getModel());
        assertEquals("Sedan", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("28000.00"), persistedVehicle.getPrice());
        assertEquals(5, persistedVehicle.getQuantityInStock());
    }

    @Test
    void customerRoleCannotDeleteVehicleEvenWhenEmailLocalPartContainsAdmin() throws Exception {
        Vehicle existingVehicle = saveVehicle(5);

        mockMvc.perform(delete("/api/vehicles/{id}", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerTokenWithAdminLookingEmail)))
                .andExpect(status().isForbidden());

        assertTrue(vehicleRepository.findById(existingVehicle.getId()).isPresent());
        assertEquals(1, vehicleRepository.count());
    }

    @Test
    void customerRoleCannotRestockVehicleEvenWhenEmailLocalPartContainsAdmin() throws Exception {
        Vehicle existingVehicle = saveVehicle(2);

        mockMvc.perform(post("/api/vehicles/{id}/restock", existingVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerTokenWithAdminLookingEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 4
                                }
                                """))
                .andExpect(status().isForbidden());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(2, persistedVehicle.getQuantityInStock());
    }

    @Test
    void adminRoleCanCreateUpdateRestockAndDeleteVehicle() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("Toyota", "Camry", "Sedan", "28000.00", 5)))
                .andExpect(status().isCreated());

        Vehicle createdVehicle = vehicleRepository.findAll().getFirst();
        mockMvc.perform(put("/api/vehicles/{id}", createdVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("Honda", "Civic", "Hatchback", "25500.00", 3)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vehicles/{id}/restock", createdVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/vehicles/{id}", createdVehicle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertEquals(0, vehicleRepository.count());
    }

    private Vehicle saveVehicle(int quantityInStock) {
        return vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                quantityInStock));
    }

    private String accessToken(String subject, String role) {
        try {
            Method method = JwtService.class.getDeclaredMethod("generateAccessToken", String.class, String.class);
            return (String) method.invoke(jwtService, subject, role);
        } catch (NoSuchMethodException error) {
            return jwtService.generateAccessToken(subject);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("JwtService must generate access tokens with a role claim.", error);
        }
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String vehicleJson(String make, String model, String category, String price, int quantityInStock) {
        return """
                {
                  "make": "%s",
                  "model": "%s",
                  "category": "%s",
                  "price": %s,
                  "quantityInStock": %d
                }
                """.formatted(make, model, category, price, quantityInStock);
    }
}
