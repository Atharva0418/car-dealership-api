package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atharva.dealership.dto.CreateVehicleRequest;
import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/*
 * Red-phase integration coverage for Spring wiring plus JPA persistence.
 * This intentionally avoids end-to-end HTTP/server tests.
 */
@SpringBootTest
class VehicleCreationIntegrationSmokeTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
    }

    @Test
    void createVehiclePersistsVehicleWithGeneratedId() {
        Vehicle createdVehicle = vehicleService.create(new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        assertNotNull(createdVehicle.getId());
        assertEquals(1, vehicleRepository.count());
        assertFalse(vehicleRepository.findAll().isEmpty());

        Vehicle persistedVehicle = vehicleRepository.findById(createdVehicle.getId()).orElseThrow();
        assertEquals("Toyota", persistedVehicle.getMake());
        assertEquals("Camry", persistedVehicle.getModel());
        assertEquals("Sedan", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("28000.00"), persistedVehicle.getPrice());
        assertEquals(5, persistedVehicle.getQuantityInStock());
    }

    @Test
    void rejectedVehicleIsNotPersisted() {
        CreateVehicleRequest invalidRequest = new CreateVehicleRequest(
                "",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5);

        assertThrows(ValidationError.class, () -> vehicleService.create(invalidRequest));

        assertEquals(0, vehicleRepository.count());
    }
}
