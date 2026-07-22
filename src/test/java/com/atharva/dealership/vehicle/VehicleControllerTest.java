package com.atharva.dealership.vehicle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

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
    void updateVehicleWithValidRequestReturnsUpdatedVehicle() throws Exception {
        when(vehicleService.update(eq(42L), any(CreateVehicleRequest.class))).thenReturn(new Vehicle(
                42L,
                "Honda",
                "Civic",
                "Hatchback",
                new BigDecimal("25500.00"),
                3));

        mockMvc.perform(put("/api/vehicles/{id}", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Honda",
                                  "model": "Civic",
                                  "category": "Hatchback",
                                  "price": 25500.00,
                                  "quantityInStock": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.make").value("Honda"))
                .andExpect(jsonPath("$.model").value("Civic"))
                .andExpect(jsonPath("$.category").value("Hatchback"))
                .andExpect(jsonPath("$.price").value(25500.00))
                .andExpect(jsonPath("$.quantityInStock").value(3));

        verify(vehicleService, times(1)).update(eq(42L), any(CreateVehicleRequest.class));
    }

    @Test
    void updateVehicleWithMissingRequiredFieldReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(put("/api/vehicles/{id}", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model": "Civic",
                                  "category": "Hatchback",
                                  "price": 25500.00,
                                  "quantityInStock": 3
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).update(any(Long.class), any(CreateVehicleRequest.class));
    }

    @Test
    void updateVehicleWithMalformedJsonReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(put("/api/vehicles/{id}", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Honda",
                                  "model":
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).update(any(Long.class), any(CreateVehicleRequest.class));
    }

    @Test
    void updateMissingVehicleReturnsNotFound() throws Exception {
        when(vehicleService.update(eq(404L), any(CreateVehicleRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found."));

        mockMvc.perform(put("/api/vehicles/{id}", 404L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Honda",
                                  "model": "Civic",
                                  "category": "Hatchback",
                                  "price": 25500.00,
                                  "quantityInStock": 3
                                }
                                """))
                .andExpect(status().isNotFound());

        verify(vehicleService, times(1)).update(eq(404L), any(CreateVehicleRequest.class));
    }

    @Test
    void listVehiclesReturnsOkWithVehicleArray() throws Exception {
        when(vehicleService.findVehicles()).thenReturn(List.of(
                new Vehicle(42L, "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5),
                new Vehicle(43L, "Honda", "Civic", "Sedan", new BigDecimal("25000.00"), 0)));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].make").value("Toyota"))
                .andExpect(jsonPath("$[0].model").value("Camry"))
                .andExpect(jsonPath("$[0].category").value("Sedan"))
                .andExpect(jsonPath("$[0].price").value(28000.00))
                .andExpect(jsonPath("$[0].quantityInStock").value(5))
                .andExpect(jsonPath("$[1].id").value(43))
                .andExpect(jsonPath("$[1].quantityInStock").value(0));

        verify(vehicleService, times(1)).findVehicles();
    }

    @Test
    void listVehiclesWithNoMatchesReturnsEmptyArray() throws Exception {
        when(vehicleService.findVehicles()).thenReturn(List.of());

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(vehicleService, times(1)).findVehicles();
    }

    @Test
    void deleteVehicleWithExistingIdReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/vehicles/{id}", 42L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(vehicleService, times(1)).deleteById(42L);
    }

    @Test
    void deleteMissingVehicleReturnsNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found."))
                .when(vehicleService)
                .deleteById(404L);

        mockMvc.perform(delete("/api/vehicles/{id}", 404L))
                .andExpect(status().isNotFound());

        verify(vehicleService, times(1)).deleteById(404L);
    }

    @Test
    void deleteVehicleWithInvalidIdFormatReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(delete("/api/vehicles/not-a-number"))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteVehicleWithNonPositiveIdReturnsBadRequest() throws Exception {
        org.mockito.Mockito.doThrow(new ValidationError("Vehicle id must be greater than zero."))
                .when(vehicleService)
                .deleteById(0L);

        mockMvc.perform(delete("/api/vehicles/{id}", 0L))
                .andExpect(status().isBadRequest());

        verify(vehicleService, times(1)).deleteById(0L);
    }
}
