package com.atharva.dealership.vehicle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atharva.dealership.dto.CreateVehicleRequest;
import com.atharva.dealership.exception.GlobalExceptionHandler;
import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/*
 * Red-phase controller contract:
 * - POST /api/vehicles accepts CreateVehicleRequest JSON
 * - valid creation returns 201 Created and a Location header pointing to /api/vehicles/{id}
 * - malformed request shape is rejected before calling VehicleService
 * - business validation errors from VehicleService map to 400 through GlobalExceptionHandler
 */
@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new VehicleController(vehicleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createVehicleWithValidRequestReturnsCreatedVehicle() throws Exception {
        when(vehicleService.create(any(CreateVehicleRequest.class))).thenReturn(new Vehicle(
                42L,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Toyota",
                                  "model": "Camry",
                                  "category": "Sedan",
                                  "price": 28000.00,
                                  "quantityInStock": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/vehicles/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Camry"))
                .andExpect(jsonPath("$.category").value("Sedan"))
                .andExpect(jsonPath("$.price").value(28000.00))
                .andExpect(jsonPath("$.quantityInStock").value(5));

        verify(vehicleService, times(1)).create(any(CreateVehicleRequest.class));
    }

    @Test
    void createVehicleWithMissingMakeReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model": "Camry",
                                  "category": "Sedan",
                                  "price": 28000.00,
                                  "quantityInStock": 5
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).create(any(CreateVehicleRequest.class));
    }

    @Test
    void createVehicleWithMissingPriceReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Toyota",
                                  "model": "Camry",
                                  "category": "Sedan",
                                  "quantityInStock": 5
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).create(any(CreateVehicleRequest.class));
    }

    @Test
    void createVehicleWithMissingQuantityReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Toyota",
                                  "model": "Camry",
                                  "category": "Sedan",
                                  "price": 28000.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).create(any(CreateVehicleRequest.class));
    }

    @Test
    void createVehicleWithInvalidPriceFromServiceReturnsBadRequest() throws Exception {
        when(vehicleService.create(any(CreateVehicleRequest.class)))
                .thenThrow(new ValidationError("Vehicle price must be greater than zero."));

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Toyota",
                                  "model": "Camry",
                                  "category": "Sedan",
                                  "price": -1.00,
                                  "quantityInStock": 5
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vehicleService, times(1)).create(any(CreateVehicleRequest.class));
    }

    @Test
    void createVehicleWithMalformedJsonReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Toyota",
                                  "model":
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).create(any(CreateVehicleRequest.class));
    }

    @Test
    void listAvailableVehiclesReturnsOkWithVehicleArray() throws Exception {
        when(vehicleService.findAvailableVehicles()).thenReturn(List.of(
                new Vehicle(42L, "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5),
                new Vehicle(43L, "Honda", "Civic", "Sedan", new BigDecimal("25000.00"), 2)));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].make").value("Toyota"))
                .andExpect(jsonPath("$[0].model").value("Camry"))
                .andExpect(jsonPath("$[0].category").value("Sedan"))
                .andExpect(jsonPath("$[0].price").value(28000.00))
                .andExpect(jsonPath("$[0].quantityInStock").value(5))
                .andExpect(jsonPath("$[1].id").value(43));

        verify(vehicleService, times(1)).findAvailableVehicles();
    }

    @Test
    void listAvailableVehiclesWithNoMatchesReturnsEmptyArray() throws Exception {
        when(vehicleService.findAvailableVehicles()).thenReturn(List.of());

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(vehicleService, times(1)).findAvailableVehicles();
    }
}
