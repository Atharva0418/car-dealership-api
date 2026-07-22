package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * Red-phase search contract:
 * - VehicleService#searchVehicles(String make, String model, String category,
 *   BigDecimal minPrice, BigDecimal maxPrice)
 * - Search is case-insensitive and trims text filters
 * - Search returns matching vehicles with any stock quantity
 * - minPrice and maxPrice are inclusive
 * - Invalid price ranges throw ValidationError
 */
@ExtendWith(MockitoExtension.class)
class VehicleSearchServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void searchVehiclesWithTrimmedCaseInsensitiveFiltersReturnsMatchingVehiclesOnly() {
        Vehicle matchingVehicle = new Vehicle(
                42L, "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 3);
        Vehicle differentMake = new Vehicle(
                43L, "Honda", "Civic", "Sedan", new BigDecimal("25000.00"), 4);
        Vehicle outOfStockMatch = new Vehicle(
                44L, "TOYOTA", "Camry", "Sedan", new BigDecimal("27000.00"), 0);
        when(vehicleRepository.findAll()).thenReturn(List.of(matchingVehicle, differentMake, outOfStockMatch));

        List<Vehicle> vehicles = vehicleService.searchVehicles(
                "  toyota  ",
                "CAMRY",
                " sedan ",
                new BigDecimal("27000.00"),
                new BigDecimal("28000.00"));

        assertEquals(List.of(matchingVehicle, outOfStockMatch), vehicles);
        verify(vehicleRepository, times(1)).findAll();
    }

    @Test
    void searchVehiclesWithInclusivePriceRangeReturnsVehiclesAtBothBoundaries() {
        Vehicle minimumPriceVehicle = new Vehicle(
                42L, "Toyota", "Corolla", "Sedan", new BigDecimal("20000.00"), 2);
        Vehicle maximumPriceVehicle = new Vehicle(
                43L, "Toyota", "Camry", "Sedan", new BigDecimal("30000.00"), 2);
        Vehicle belowRangeVehicle = new Vehicle(
                44L, "Toyota", "Yaris", "Hatchback", new BigDecimal("19999.99"), 2);
        Vehicle aboveRangeVehicle = new Vehicle(
                45L, "Toyota", "Avalon", "Sedan", new BigDecimal("30000.01"), 2);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                minimumPriceVehicle,
                maximumPriceVehicle,
                belowRangeVehicle,
                aboveRangeVehicle));

        List<Vehicle> vehicles = vehicleService.searchVehicles(
                null,
                null,
                null,
                new BigDecimal("20000.00"),
                new BigDecimal("30000.00"));

        assertEquals(List.of(minimumPriceVehicle, maximumPriceVehicle), vehicles);
        verify(vehicleRepository, times(1)).findAll();
    }

    @Test
    void searchVehiclesWithMinimumPriceGreaterThanMaximumPriceThrowsValidationError() {
        assertThrows(ValidationError.class, () -> vehicleService.searchVehicles(
                null,
                null,
                null,
                new BigDecimal("30000.00"),
                new BigDecimal("20000.00")));
    }

    @Test
    void searchVehiclesWithNegativePriceFilterThrowsValidationError() {
        assertThrows(ValidationError.class, () -> vehicleService.searchVehicles(
                null,
                null,
                null,
                new BigDecimal("-1.00"),
                null));
    }
}
