package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/*
 * Red-phase integration coverage for DELETE /api/vehicles/{id} behavior at the
 * service + repository boundary. Inventory purchase/restock history is
 * intentionally out of scope until the inventory feature is designed.
 */
@SpringBootTest
class VehicleDeletionIntegrationTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
    }

    @Test
    void deleteExistingVehicleRemovesVehicleFromRepository() {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        vehicleService.deleteById(existingVehicle.getId());

        assertEquals(0, vehicleRepository.count());
        assertFalse(vehicleRepository.findById(existingVehicle.getId()).isPresent());
    }

    @Test
    void deleteMissingVehicleReturnsNotFoundAndDoesNotDeleteOtherVehicles() {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Honda",
                "Civic",
                "Hatchback",
                new BigDecimal("25500.00"),
                3));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> vehicleService.deleteById(existingVehicle.getId() + 999L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals(1, vehicleRepository.count());
        assertFalse(vehicleRepository.findById(existingVehicle.getId()).isEmpty());
    }

    @Test
    void deletingSameVehicleTwiceReturnsNotFoundOnSecondAttempt() {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Mazda",
                "CX-5",
                "SUV",
                new BigDecimal("32000.00"),
                2));

        vehicleService.deleteById(existingVehicle.getId());
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> vehicleService.deleteById(existingVehicle.getId()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void deleteWithNonPositiveIdReturnsBadRequestAndDoesNotDeleteExistingVehicles() {
        vehicleRepository.save(new Vehicle(
                "Subaru",
                "Outback",
                "Wagon",
                new BigDecimal("34000.00"),
                4));

        assertThrows(ValidationError.class, () -> vehicleService.deleteById(0L));

        assertEquals(1, vehicleRepository.count());
    }
}
