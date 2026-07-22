package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Red-phase integration coverage for restocking at the service + repository boundary.
 */
@SpringBootTest
class VehicleRestockIntegrationTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
    }

    @Test
    void restockVehiclePersistsIncrementedQuantity() {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        Vehicle restockedVehicle = vehicleService.restock(existingVehicle.getId(), 5);

        assertEquals(existingVehicle.getId(), restockedVehicle.getId());
        assertEquals(8, restockedVehicle.getQuantityInStock());
        assertEquals(1, vehicleRepository.count());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(8, persistedVehicle.getQuantityInStock());
    }

    @Test
    void restockOutOfStockVehicleMakesItAvailableAgain() {
        Vehicle existingVehicle = saveVehicleWithQuantity(0);

        Vehicle restockedVehicle = vehicleService.restock(existingVehicle.getId(), 2);

        assertEquals(2, restockedVehicle.getQuantityInStock());
        assertEquals(1, vehicleService.findAvailableVehicles().size());
        assertEquals(existingVehicle.getId(), vehicleService.findAvailableVehicles().getFirst().getId());
    }

    @Test
    void restockMissingVehicleDoesNotCreateVehicle() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> vehicleService.restock(999L, 5));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void restockWithInvalidQuantityDoesNotModifyVehicle() {
        Vehicle existingVehicle = saveVehicleWithQuantity(3);

        assertThrows(ValidationError.class, () -> vehicleService.restock(existingVehicle.getId(), 0));

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(3, persistedVehicle.getQuantityInStock());
    }

    private Vehicle saveVehicleWithQuantity(int quantityInStock) {
        return vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                quantityInStock));
    }
}
