package com.atharva.dealership.vehicle;

import com.atharva.dealership.dto.CreateVehicleRequest;
import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import java.util.List;
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
}
