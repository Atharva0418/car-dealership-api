package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atharva.dealership.dto.CreateVehicleRequest;
import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * Red-phase assumptions for the production API:
 * - VehicleService#create(CreateVehicleRequest request)
 * - VehicleRepository extends JpaRepository<Vehicle, Long>
 * - Vehicle has generated Long id plus make, model, category, price, and quantityInStock
 * - CreateVehicleRequest uses BigDecimal for price to avoid floating-point money handling
 * - Invalid input throws ValidationError and does not call VehicleRepository#save
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void createWithValidVehiclePersistsTrimmedVehicleAndReturnsSavedVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "  Toyota  ",
                "  Camry  ",
                "  Sedan  ",
                new BigDecimal("28000.00"),
                5);
        Vehicle savedVehicle = new Vehicle(
                42L,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5);

        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        Vehicle result = vehicleService.create(request);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository, times(1)).save(vehicleCaptor.capture());

        Vehicle persistedVehicle = vehicleCaptor.getValue();
        assertEquals("Toyota", persistedVehicle.getMake());
        assertEquals("Camry", persistedVehicle.getModel());
        assertEquals("Sedan", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("28000.00"), persistedVehicle.getPrice());
        assertEquals(5, persistedVehicle.getQuantityInStock());
        assertEquals(42L, result.getId());
    }

    @Test
    void createWithNullRequestThrowsValidationErrorAndDoesNotPersistVehicle() {
        assertThrows(ValidationError.class, () -> vehicleService.create(null));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithBlankMakeThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = validRequestWithMake("   ");

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithBlankModelThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                " ",
                "Sedan",
                new BigDecimal("28000.00"),
                5);

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithBlankCategoryThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "\t",
                new BigDecimal("28000.00"),
                5);

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithNullPriceThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "Sedan",
                null,
                5);

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithZeroPriceThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "Sedan",
                BigDecimal.ZERO,
                5);

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithNegativePriceThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("-1.00"),
                5);

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithNullQuantityThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                null);

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createWithNegativeQuantityThrowsValidationErrorAndDoesNotPersistVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                -1);

        assertThrows(ValidationError.class, () -> vehicleService.create(request));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createAllowsZeroQuantityForOutOfStockVehicle() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                0);
        Vehicle savedVehicle = new Vehicle(
                43L,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                0);

        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        Vehicle result = vehicleService.create(request);

        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
        assertEquals(0, result.getQuantityInStock());
    }

    private CreateVehicleRequest validRequestWithMake(String make) {
        return new CreateVehicleRequest(
                make,
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5);
    }
}
