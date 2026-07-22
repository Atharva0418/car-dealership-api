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
import java.util.List;
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

    @Test
    void updateWithValidVehiclePersistsTrimmedDetailsAndPreservesVehicleId() {
        long vehicleId = 42L;
        Vehicle existingVehicle = new Vehicle(
                vehicleId,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5);
        CreateVehicleRequest request = new CreateVehicleRequest(
                "  Honda  ",
                "  Civic  ",
                "  Hatchback  ",
                new BigDecimal("25500.00"),
                3);
        Vehicle savedVehicle = new Vehicle(
                vehicleId,
                "Honda",
                "Civic",
                "Hatchback",
                new BigDecimal("25500.00"),
                3);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        Vehicle result = vehicleService.update(vehicleId, request);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository, times(1)).findById(vehicleId);
        verify(vehicleRepository, times(1)).save(vehicleCaptor.capture());

        Vehicle persistedVehicle = vehicleCaptor.getValue();
        assertEquals(vehicleId, persistedVehicle.getId());
        assertEquals("Honda", persistedVehicle.getMake());
        assertEquals("Civic", persistedVehicle.getModel());
        assertEquals("Hatchback", persistedVehicle.getCategory());
        assertEquals(new BigDecimal("25500.00"), persistedVehicle.getPrice());
        assertEquals(3, persistedVehicle.getQuantityInStock());
        assertEquals(vehicleId, result.getId());
    }

    @Test
    void updateWithMissingVehicleDoesNotPersistVehicle() {
        long missingVehicleId = 404L;
        when(vehicleRepository.findById(missingVehicleId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> vehicleService.update(missingVehicleId, validUpdateRequest()));

        verify(vehicleRepository, times(1)).findById(missingVehicleId);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void updateWithBlankRequiredFieldThrowsValidationErrorAndDoesNotPersistVehicle() {
        long vehicleId = 42L;
        CreateVehicleRequest request = new CreateVehicleRequest(
                "Toyota",
                "   ",
                "Sedan",
                new BigDecimal("28000.00"),
                5);

        assertThrows(ValidationError.class, () -> vehicleService.update(vehicleId, request));

        verify(vehicleRepository, never()).findById(vehicleId);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void updateAllowsZeroQuantityForOutOfStockVehicle() {
        long vehicleId = 42L;
        Vehicle existingVehicle = new Vehicle(
                vehicleId,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5);
        CreateVehicleRequest request = new CreateVehicleRequest(
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
                0);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        Vehicle result = vehicleService.update(vehicleId, request);

        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
        assertEquals(0, result.getQuantityInStock());
    }

    @Test
    void findAvailableVehiclesReturnsOnlyVehiclesWithPositiveStock() {
        Vehicle availableVehicle = new Vehicle(
                42L, "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5);
        Vehicle outOfStockVehicle = new Vehicle(
                43L, "Honda", "Civic", "Sedan", new BigDecimal("25000.00"), 0);
        when(vehicleRepository.findAll()).thenReturn(List.of(availableVehicle, outOfStockVehicle));

        List<Vehicle> result = vehicleService.findAvailableVehicles();

        assertEquals(List.of(availableVehicle), result);
        verify(vehicleRepository, times(1)).findAll();
    }

    @Test
    void findAvailableVehiclesWithEmptyRepositoryReturnsEmptyList() {
        when(vehicleRepository.findAll()).thenReturn(List.of());

        List<Vehicle> result = vehicleService.findAvailableVehicles();

        assertEquals(List.of(), result);
        verify(vehicleRepository, times(1)).findAll();
    }

    @Test
    void deleteExistingVehicleDeletesVehicleById() {
        long vehicleId = 42L;
        Vehicle existingVehicle = new Vehicle(
                vehicleId,
                "Toyota",
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(existingVehicle));

        vehicleService.deleteById(vehicleId);

        verify(vehicleRepository, times(1)).findById(vehicleId);
        verify(vehicleRepository, times(1)).delete(existingVehicle);
    }

    @Test
    void deleteMissingVehicleThrowsNotFoundAndDoesNotDeleteVehicle() {
        long missingVehicleId = 404L;
        when(vehicleRepository.findById(missingVehicleId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> vehicleService.deleteById(missingVehicleId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(vehicleRepository, times(1)).findById(missingVehicleId);
        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }

    @Test
    void deleteWithNonPositiveIdThrowsValidationErrorAndDoesNotCallRepository() {
        assertThrows(ValidationError.class, () -> vehicleService.deleteById(0L));

        verify(vehicleRepository, never()).findById(any(Long.class));
        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }

    private CreateVehicleRequest validRequestWithMake(String make) {
        return new CreateVehicleRequest(
                make,
                "Camry",
                "Sedan",
                new BigDecimal("28000.00"),
                5);
    }

    private CreateVehicleRequest validUpdateRequest() {
        return new CreateVehicleRequest(
                "Honda",
                "Civic",
                "Hatchback",
                new BigDecimal("25500.00"),
                3);
    }
}
