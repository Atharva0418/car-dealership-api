package com.atharva.dealership.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atharva.dealership.exception.AuthenticationError;
import com.atharva.dealership.exception.ValidationError;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import com.atharva.dealership.vehicle.Vehicle;
import com.atharva.dealership.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final Instant PURCHASE_TIME = Instant.parse("2026-07-23T05:00:00Z");

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(PURCHASE_TIME, ZoneOffset.UTC);
        purchaseService = new PurchaseService(purchaseRepository, userRepository, vehicleRepository, fixedClock);
    }

    @Test
    void purchaseVehicleCreatesPurchaseSnapshotForAuthenticatedCustomer() {
        User customer = new User("customer@example.com", "$2a$10$password");
        Vehicle vehicle = new Vehicle(42L, "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 5);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(42L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Purchase purchase = purchaseService.purchaseVehicle(42L, "customer@example.com");

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository, times(1)).save(vehicleCaptor.capture());
        assertEquals(4, vehicleCaptor.getValue().getQuantityInStock());

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(1)).save(purchaseCaptor.capture());

        Purchase savedPurchase = purchaseCaptor.getValue();
        assertEquals(customer, savedPurchase.getUser());
        assertEquals(42L, savedPurchase.getVehicleId());
        assertEquals("Toyota", savedPurchase.getVehicleMake());
        assertEquals("Camry", savedPurchase.getVehicleModel());
        assertEquals("Sedan", savedPurchase.getVehicleCategory());
        assertEquals(new BigDecimal("28000.00"), savedPurchase.getPurchasePrice());
        assertEquals(PURCHASE_TIME, savedPurchase.getPurchasedAt());
        assertEquals(savedPurchase, purchase);
    }

    @Test
    void purchaseVehicleWithOutOfStockVehicleDoesNotCreatePurchaseOrModifyStock() {
        User customer = new User("customer@example.com", "$2a$10$password");
        Vehicle vehicle = new Vehicle(42L, "Toyota", "Camry", "Sedan", new BigDecimal("28000.00"), 0);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(42L)).thenReturn(Optional.of(vehicle));

        assertThrows(ValidationError.class, () -> purchaseService.purchaseVehicle(42L, "customer@example.com"));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void purchaseVehicleWithMissingUserFailsBeforeChangingVehicle() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationError.class, () -> purchaseService.purchaseVehicle(42L, "missing@example.com"));

        verify(vehicleRepository, never()).findById(any(Long.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void purchaseVehicleWithMissingVehicleDoesNotCreatePurchase() {
        User customer = new User("customer@example.com", "$2a$10$password");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.purchaseVehicle(404L, "customer@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void findMyPurchasesUsesAuthenticatedCustomerOnly() {
        User customer = new User("customer@example.com", "$2a$10$password");
        List<Purchase> expectedPurchases = List.of();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(purchaseRepository.findByUserOrderByPurchasedAtDesc(customer)).thenReturn(expectedPurchases);

        List<Purchase> purchases = purchaseService.findMyPurchases("customer@example.com");

        assertEquals(expectedPurchases, purchases);
        verify(purchaseRepository, times(1)).findByUserOrderByPurchasedAtDesc(customer);
    }
}
