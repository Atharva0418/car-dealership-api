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
class VehicleSearchIntegrationTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clearData() {
        vehicleRepository.deleteAll();
    }

    @Test
    void searchVehiclesCombinesMakeCategoryAndPriceRangeAgainstPersistedVehicles() {
        vehicleRepository.saveAll(List.of(
                new Vehicle("Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5),
                new Vehicle("Toyota", "RAV4", "SUV", new BigDecimal("34000.00"), 4),
                new Vehicle("Honda", "Accord", "Sedan", new BigDecimal("29000.00"), 3),
                new Vehicle("Toyota", "Corolla", "Sedan", new BigDecimal("19000.00"), 6)));

        List<Vehicle> vehicles = vehicleService.searchVehicles(
                "toyota",
                null,
                "sedan",
                new BigDecimal("20000.00"),
                new BigDecimal("30000.00"));

        assertEquals(1, vehicles.size());
        assertEquals("Toyota", vehicles.getFirst().getMake());
        assertEquals("Camry", vehicles.getFirst().getModel());
        assertEquals(new BigDecimal("28000.00"), vehicles.getFirst().getPrice());
    }

    @Test
    void searchVehiclesIncludesOutOfStockVehiclesWhenTheyMatchFilters() {
        vehicleRepository.saveAll(List.of(
                new Vehicle("Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 0),
                new Vehicle("Toyota", "Corolla", "Sedan", new BigDecimal("24000.00"), 2)));

        List<Vehicle> vehicles = vehicleService.searchVehicles(
                "TOYOTA",
                null,
                "SEDAN",
                null,
                null);

        assertEquals(2, vehicles.size());
        assertEquals("Camry", vehicles.getFirst().getModel());
        assertEquals(0, vehicles.getFirst().getQuantityInStock());
        assertEquals("Corolla", vehicles.get(1).getModel());
        assertEquals(2, vehicles.get(1).getQuantityInStock());
    }

    @Test
    void searchVehiclesWithNoMatchesReturnsEmptyList() {
        vehicleRepository.save(new Vehicle(
                "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5));

        List<Vehicle> vehicles = vehicleService.searchVehicles(
                "Ford",
                null,
                null,
                null,
                null);

        assertTrue(vehicles.isEmpty());
    }
}
