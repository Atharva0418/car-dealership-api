package com.atharva.dealership.vehicle;

import com.atharva.dealership.dto.CreateVehicleRequest;
import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle create(CreateVehicleRequest request) {
        log.info("Starting vehicle creation");
        validate(request);
        Vehicle vehicle = new Vehicle(
                request.make().trim(),
                request.model().trim(),
                request.category().trim(),
                request.price(),
                request.quantityInStock());
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle persisted with id: {}", savedVehicle.getId());
        return savedVehicle;
    }

    public List<Vehicle> findAvailableVehicles() {
        log.info("Starting available vehicle listing");
        List<Vehicle> availableVehicles = vehicleRepository.findAll().stream()
                .filter(vehicle -> vehicle.getQuantityInStock() != null)
                .filter(vehicle -> vehicle.getQuantityInStock() > 0)
                .toList();
        log.info("Available vehicle listing completed successfully with {} vehicles", availableVehicles.size());
        return availableVehicles;
    }

    public List<Vehicle> searchAvailableVehicles(
            String make,
            String model,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        log.info("Starting available vehicle search");
        validateSearchPrices(minPrice, maxPrice);

        List<Vehicle> vehicles = vehicleRepository.findAll().stream()
                .filter(vehicle -> vehicle.getQuantityInStock() != null)
                .filter(vehicle -> vehicle.getQuantityInStock() > 0)
                .filter(vehicle -> matchesText(make, vehicle.getMake()))
                .filter(vehicle -> matchesText(model, vehicle.getModel()))
                .filter(vehicle -> matchesText(category, vehicle.getCategory()))
                .filter(vehicle -> minPrice == null || vehicle.getPrice().compareTo(minPrice) >= 0)
                .filter(vehicle -> maxPrice == null || vehicle.getPrice().compareTo(maxPrice) <= 0)
                .toList();

        log.info("Available vehicle search completed successfully with {} vehicles", vehicles.size());
        return vehicles;
    }

    private void validate(CreateVehicleRequest request) {
        if (request == null) {
            log.warn("Vehicle creation validation failed: request is missing");
            throw new ValidationError("Vehicle request is required.");
        }
        if (request.make() == null || request.make().isBlank()) {
            log.warn("Vehicle creation validation failed: make is missing");
            throw new ValidationError("Vehicle make is required.");
        }
        if (request.model() == null || request.model().isBlank()) {
            log.warn("Vehicle creation validation failed: model is missing");
            throw new ValidationError("Vehicle model is required.");
        }
        if (request.category() == null || request.category().isBlank()) {
            log.warn("Vehicle creation validation failed: category is missing");
            throw new ValidationError("Vehicle category is required.");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Vehicle creation validation failed: price is not greater than zero");
            throw new ValidationError("Vehicle price must be greater than zero.");
        }
        if (request.quantityInStock() == null || request.quantityInStock() < 0) {
            log.warn("Vehicle creation validation failed: quantity in stock is negative or missing");
            throw new ValidationError("Vehicle quantity in stock cannot be negative.");
        }
    }

    private void validateSearchPrices(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Vehicle search validation failed: minimum price is negative");
            throw new ValidationError("Minimum price cannot be negative.");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Vehicle search validation failed: maximum price is negative");
            throw new ValidationError("Maximum price cannot be negative.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            log.warn("Vehicle search validation failed: minimum price is greater than maximum price");
            throw new ValidationError("Minimum price cannot be greater than maximum price.");
        }
    }

    private boolean matchesText(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return actual.toLowerCase(Locale.ROOT)
                .equals(expected.trim().toLowerCase(Locale.ROOT));
    }
}
