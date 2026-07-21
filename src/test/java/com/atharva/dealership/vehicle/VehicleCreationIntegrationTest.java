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
class VehicleCreationIntegrationTest {

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

    @Test
    void updateVehiclePersistsChangesWithoutCreatingDuplicateVehicle() {
        Vehicle originalVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        Vehicle updatedVehicle = vehicleService.update(originalVehicle.getId(), new CreateVehicleRequest(
                " Honda ",
                " Civic ",
                " Hatchback ",
                new BigDecimal("25500.00"),
                0));

        assertEquals(originalVehicle.getId(), updatedVehicle.getId());
        assertEquals(1, vehicleRepository.count());

        Vehicle persistedVehicle = vehicleRepository.findById(originalVehicle.getId()).orElseThrow();
        assertEquals("Honda", persistedVehicle.getMake());
        assertEquals("Civic", persistedVehicle.getModel());
        assertEquals("Hatchback", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("25500.00"), persistedVehicle.getPrice());
        assertEquals(0, persistedVehicle.getQuantityInStock());
    }

    @Test
    void updateMissingVehicleDoesNotCreateVehicle() {
        assertThrows(RuntimeException.class, () -> vehicleService.update(999L, new CreateVehicleRequest(
                "Honda",
                "Civic",
                "Hatchback",
                new BigDecimal("25500.00"),
                3)));

        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void rejectedUpdateDoesNotModifyExistingVehicle() {
        Vehicle originalVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        assertThrows(ValidationError.class, () -> vehicleService.update(originalVehicle.getId(), new CreateVehicleRequest(
                "Honda",
                "Civic",
                "Hatchback",
                BigDecimal.ZERO,
                3)));

        Vehicle persistedVehicle = vehicleRepository.findById(originalVehicle.getId()).orElseThrow();
        assertEquals("Toyota", persistedVehicle.getMake());
        assertEquals("Camry", persistedVehicle.getModel());
        assertEquals("Sedan", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("28000.00"), persistedVehicle.getPrice());
        assertEquals(5, persistedVehicle.getQuantityInStock());
    }
}
