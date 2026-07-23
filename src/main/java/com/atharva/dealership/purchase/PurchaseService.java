package com.atharva.dealership.purchase;

import com.atharva.dealership.exception.AuthenticationError;
import com.atharva.dealership.exception.ValidationError;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import com.atharva.dealership.vehicle.Vehicle;
import com.atharva.dealership.vehicle.VehicleRepository;
import java.time.Clock;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final Clock clock;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            Clock clock) {
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.clock = clock;
    }

    @Transactional
    public Purchase purchaseVehicle(Long vehicleId, String customerEmail) {
        log.info("Starting purchase creation for vehicle id: {}", vehicleId);
        User customer = findCustomer(customerEmail);
        Vehicle vehicle = findVehicle(vehicleId);

        if (vehicle.getQuantityInStock() == null || vehicle.getQuantityInStock() <= 0) {
            log.warn("Purchase creation failed because vehicle id {} is out of stock", vehicleId);
            throw new ValidationError("Vehicle is out of stock.");
        }

        Vehicle updatedVehicle = new Vehicle(
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getCategory(),
                vehicle.getPrice(),
                vehicle.getQuantityInStock() - 1);
        vehicleRepository.save(updatedVehicle);

        Purchase purchase = new Purchase(
                customer,
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getCategory(),
                vehicle.getPrice(),
                clock.instant());
        Purchase savedPurchase = purchaseRepository.save(purchase);
        log.info("Purchase creation completed successfully for vehicle id: {}", vehicleId);
        return savedPurchase;
    }

    @Transactional(readOnly = true)
    public List<Purchase> findMyPurchases(String customerEmail) {
        log.info("Starting purchase listing for customer email: {}", customerEmail);
        User customer = findCustomer(customerEmail);
        List<Purchase> purchases = purchaseRepository.findByUserOrderByPurchasedAtDesc(customer);
        log.info("Purchase listing completed successfully with {} purchases", purchases.size());
        return purchases;
    }

    private User findCustomer(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("Purchase request failed because authenticated user email is missing");
            throw new AuthenticationError("Authenticated user is required.");
        }

        return userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> {
                    log.warn("Purchase request failed because user {} was not found", customerEmail);
                    return new AuthenticationError("Authenticated user was not found.");
                });
    }

    private Vehicle findVehicle(Long vehicleId) {
        if (vehicleId == null || vehicleId <= 0) {
            log.warn("Purchase validation failed: vehicle id is missing or non-positive");
            throw new ValidationError("Vehicle id must be greater than zero.");
        }

        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> {
                    log.warn("Purchase creation failed because vehicle id {} was not found", vehicleId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found.");
                });
    }
}
