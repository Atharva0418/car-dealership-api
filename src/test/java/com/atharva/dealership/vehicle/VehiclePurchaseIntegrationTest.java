package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/*
 * Red-phase integration coverage for inventory purchase behavior at the
 * service + repository boundary.
 */
@SpringBootTest
class VehiclePurchaseIntegrationTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
    }

    @Test
    void purchaseVehiclePersistsDecrementedQuantity() {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5));

        Vehicle purchasedVehicle = vehicleService.purchase(existingVehicle.getId());

        assertEquals(existingVehicle.getId(), purchasedVehicle.getId());
        assertEquals(4, purchasedVehicle.getQuantityInStock());
        assertEquals(1, vehicleRepository.count());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(4, persistedVehicle.getQuantityInStock());
    }

    @Test
    void purchaseLastVehiclePersistsZeroQuantityAndKeepsVehicleInListing() {
        Vehicle existingVehicle = vehicleRepository.save(new Vehicle(
                "Honda",
                "Civic",
                "Sedan",
                new BigDecimal("25000.00"),
                1));

        Vehicle purchasedVehicle = vehicleService.purchase(existingVehicle.getId());

        assertEquals(0, purchasedVehicle.getQuantityInStock());
        assertEquals(1, vehicleRepository.count());

        Vehicle persistedVehicle = vehicleRepository.findById(existingVehicle.getId()).orElseThrow();
        assertEquals(0, persistedVehicle.getQuantityInStock());

        List<Vehicle> vehicles = vehicleService.findVehicles();
        assertEquals(1, vehicles.size());
        assertEquals(existingVehicle.getId(), vehicles.getFirst().getId());
        assertEquals(0, vehicles.getFirst().getQuantityInStock());
    }
}
