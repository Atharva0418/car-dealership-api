package com.atharva.dealership.vehicle;

import com.atharva.dealership.dto.CreateVehicleRequest;
import com.atharva.dealership.exception.ValidationError;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<Vehicle> create(@RequestBody CreateVehicleRequest request) {
        log.info("Received vehicle creation request");
        if (request == null) {
            log.warn("Rejecting vehicle creation request with missing request body");
            throw new ValidationError("Vehicle request is required.");
        }
        if (request.make() == null || request.price() == null || request.quantityInStock() == null) {
            log.warn("Rejecting vehicle creation request with missing required fields");
            throw new ValidationError("Vehicle make, price, and quantity in stock are required.");
        }

        Vehicle vehicle = vehicleService.create(request);
        log.info("Vehicle creation request handled successfully for vehicle id: {}", vehicle.getId());
        return ResponseEntity.created(URI.create("/api/vehicles/" + vehicle.getId())).body(vehicle);
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> listAvailableVehicles() {
        log.info("Starting available vehicle listing request");
        List<Vehicle> availableVehicles = vehicleService.findAvailableVehicles();
        log.info("Available vehicle listing request completed successfully with {} vehicles", availableVehicles.size());
        return ResponseEntity.ok(availableVehicles);
    }
}
