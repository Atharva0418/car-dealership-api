package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VehicleListingIntegrationTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
    }

    @Test
    void findAvailableVehiclesReturnsPersistedVehiclesWithPositiveStockOnly() {
        vehicleRepository.saveAll(List.of(
                new Vehicle("Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5),
                new Vehicle("Honda", "Civic", "Sedan", new BigDecimal("25000.00"), 0)));

        List<Vehicle> availableVehicles = vehicleService.findAvailableVehicles();

        assertEquals(1, availableVehicles.size());
        assertEquals("Toyota", availableVehicles.getFirst().getMake());
        assertEquals(5, availableVehicles.getFirst().getQuantityInStock());
    }

    @Test
    void findAvailableVehiclesWhenDatabaseIsEmptyReturnsEmptyList() {
        List<Vehicle> availableVehicles = vehicleService.findAvailableVehicles();

        assertTrue(availableVehicles.isEmpty());
    }
}
