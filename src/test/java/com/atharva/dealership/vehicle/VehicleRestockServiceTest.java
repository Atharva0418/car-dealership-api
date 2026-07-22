package com.atharva.dealership.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/*
 * Red-phase unit coverage for VehicleService#restock(Long id, Integer quantity).
 */
@ExtendWith(MockitoExtension.class)
class VehicleRestockServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void restockExistingVehicleIncrementsQuantityAndPreservesVehicleDetails() {
        long vehicleId = 42L;
        Vehicle existingVehicle = new Vehicle(
                vehicleId,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                3);
        Vehicle savedVehicle = new Vehicle(
                vehicleId,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                8);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        Vehicle result = vehicleService.restock(vehicleId, 5);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository, times(1)).findById(vehicleId);
        verify(vehicleRepository, times(1)).save(vehicleCaptor.capture());

        Vehicle persistedVehicle = vehicleCaptor.getValue();
        assertEquals(vehicleId, persistedVehicle.getId());
        assertEquals("Toyota", persistedVehicle.getMake());
        assertEquals("Camry", persistedVehicle.getModel());
        assertEquals("Sedan", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("28000.00"), persistedVehicle.getPrice());
        assertEquals(8, persistedVehicle.getQuantityInStock());
        assertEquals(8, result.getQuantityInStock());
    }

    @Test
    void restockOutOfStockVehicleAllowsPositiveQuantity() {
        long vehicleId = 42L;
        Vehicle existingVehicle = new Vehicle(
                vehicleId,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                0);
        Vehicle savedVehicle = new Vehicle(
                vehicleId,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                4);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        Vehicle result = vehicleService.restock(vehicleId, 4);

        assertEquals(4, result.getQuantityInStock());
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void restockMissingVehicleThrowsNotFoundAndDoesNotSave() {
        long missingVehicleId = 404L;
        when(vehicleRepository.findById(missingVehicleId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> vehicleService.restock(missingVehicleId, 5));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(vehicleRepository, times(1)).findById(missingVehicleId);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void restockWithNonPositiveIdThrowsValidationErrorAndDoesNotCallRepository() {
        assertThrows(ValidationError.class, () -> vehicleService.restock(0L, 5));

        verify(vehicleRepository, never()).findById(any(Long.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void restockWithNullQuantityThrowsValidationErrorAndDoesNotCallRepository() {
        assertThrows(ValidationError.class, () -> vehicleService.restock(42L, null));

        verify(vehicleRepository, never()).findById(any(Long.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void restockWithZeroQuantityThrowsValidationErrorAndDoesNotCallRepository() {
        assertThrows(ValidationError.class, () -> vehicleService.restock(42L, 0));

        verify(vehicleRepository, never()).findById(any(Long.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void restockWithNegativeQuantityThrowsValidationErrorAndDoesNotCallRepository() {
        assertThrows(ValidationError.class, () -> vehicleService.restock(42L, -1));

        verify(vehicleRepository, never()).findById(any(Long.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
