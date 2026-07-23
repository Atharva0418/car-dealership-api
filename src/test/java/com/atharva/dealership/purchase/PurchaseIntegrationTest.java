package com.atharva.dealership.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atharva.dealership.exception.ValidationError;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import com.atharva.dealership.vehicle.Vehicle;
import com.atharva.dealership.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PurchaseIntegrationTest {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clearData() {
        purchaseRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void purchaseVehiclePersistsPurchaseHistoryAndDecrementsInventory() {
        User customer = saveUser("customer@example.com");
        Vehicle vehicle = saveVehicle("Toyota", "Camry", "Sedan", "28000.00", 5);

        Purchase purchase = purchaseService.purchaseVehicle(vehicle.getId(), customer.getEmail());

        assertEquals(1, purchaseRepository.count());

        Purchase persistedPurchase = purchaseRepository.findById(purchase.getId()).orElseThrow();
        assertEquals(customer.getEmail(), persistedPurchase.getUser().getEmail());
        assertEquals(vehicle.getId(), persistedPurchase.getVehicleId());
        assertEquals("Toyota", persistedPurchase.getVehicleMake());
        assertEquals("Camry", persistedPurchase.getVehicleModel());
        assertEquals("Sedan", persistedPurchase.getVehicleCategory());
        assertEquals(new BigDecimal("28000.00"), persistedPurchase.getPurchasePrice());

        Vehicle persistedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(4, persistedVehicle.getQuantityInStock());
    }

    @Test
    void findMyPurchasesReturnsOnlyPurchasesForThatCustomerNewestFirst() {
        User customer = saveUser("customer@example.com");
        User otherCustomer = saveUser("other.customer@example.com");
        Vehicle camry = saveVehicle("Toyota", "Camry", "Sedan", "28000.00", 3);
        Vehicle civic = saveVehicle("Honda", "Civic", "Sedan", "25000.00", 3);
        Vehicle bronco = saveVehicle("Ford", "Bronco", "SUV", "42000.00", 3);

        purchaseService.purchaseVehicle(camry.getId(), customer.getEmail());
        purchaseService.purchaseVehicle(bronco.getId(), otherCustomer.getEmail());
        purchaseService.purchaseVehicle(civic.getId(), customer.getEmail());

        List<Purchase> purchases = purchaseService.findMyPurchases(customer.getEmail());

        assertEquals(2, purchases.size());
        assertEquals(civic.getId(), purchases.get(0).getVehicleId());
        assertEquals(camry.getId(), purchases.get(1).getVehicleId());
    }

    @Test
    void outOfStockPurchaseDoesNotPersistPurchaseHistory() {
        User customer = saveUser("customer@example.com");
        Vehicle vehicle = saveVehicle("Toyota", "Supra", "Coupe", "55000.00", 0);

        assertThrows(ValidationError.class, () -> purchaseService.purchaseVehicle(vehicle.getId(), customer.getEmail()));

        assertEquals(0, purchaseRepository.count());

        Vehicle persistedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(0, persistedVehicle.getQuantityInStock());
    }

    @Test
    void purchaseHistoryKeepsSnapshotAfterVehicleIsChanged() {
        User customer = saveUser("customer@example.com");
        Vehicle vehicle = saveVehicle("BMW", "330i", "Sedan", "46000.00", 2);

        purchaseService.purchaseVehicle(vehicle.getId(), customer.getEmail());

        vehicleRepository.save(new Vehicle(
                vehicle.getId(),
                "BMW",
                "M340i",
                "Performance Sedan",
                new BigDecimal("59000.00"),
                1));

        Purchase purchase = purchaseService.findMyPurchases(customer.getEmail()).getFirst();
        assertEquals("BMW", purchase.getVehicleMake());
        assertEquals("330i", purchase.getVehicleModel());
        assertEquals("Sedan", purchase.getVehicleCategory());
        assertEquals(new BigDecimal("46000.00"), purchase.getPurchasePrice());
    }

    private User saveUser(String email) {
        return userRepository.save(new User(email, "$2a$10$password"));
    }

    private Vehicle saveVehicle(
            String make,
            String model,
            String category,
            String price,
            int quantityInStock) {
        return vehicleRepository.save(new Vehicle(
                make,
                model,
                category,
                new BigDecimal(price),
                quantityInStock));
    }
}
