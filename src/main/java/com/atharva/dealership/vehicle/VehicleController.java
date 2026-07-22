package com.atharva.dealership.vehicle;

import com.atharva.dealership.dto.CreateVehicleRequest;
import com.atharva.dealership.dto.RestockVehicleRequest;
import com.atharva.dealership.exception.ValidationError;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> update(@PathVariable Long id, @RequestBody CreateVehicleRequest request) {
        log.info("Received vehicle update request for vehicle id: {}", id);
        if (request == null) {
            log.warn("Rejecting vehicle update request with missing request body for vehicle id: {}", id);
            throw new ValidationError("Vehicle request is required.");
        }
        if (request.make() == null || request.price() == null || request.quantityInStock() == null) {
            log.warn("Rejecting vehicle update request with missing required fields for vehicle id: {}", id);
            throw new ValidationError("Vehicle make, price, and quantity in stock are required.");
        }

        Vehicle vehicle = vehicleService.update(id, request);
        log.info("Vehicle update request handled successfully for vehicle id: {}", vehicle.getId());
        return ResponseEntity.ok(vehicle);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Received vehicle deletion request for vehicle id: {}", id);
        vehicleService.deleteById(id);
        log.info("Vehicle deletion request handled successfully for vehicle id: {}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/purchase")
    public ResponseEntity<Vehicle> purchase(@PathVariable Long id) {
        log.info("Received vehicle purchase request for vehicle id: {}", id);
        Vehicle vehicle = vehicleService.purchase(id);
        log.info("Vehicle purchase request handled successfully for vehicle id: {}", vehicle.getId());
        return ResponseEntity.ok(vehicle);
    }

    @PostMapping("/{id}/restock")
    public ResponseEntity<Vehicle> restock(@PathVariable Long id, @RequestBody RestockVehicleRequest request) {
        log.info("Received vehicle restock request for vehicle id: {}", id);
        if (request == null || request.quantity() == null) {
            log.warn("Rejecting vehicle restock request with missing quantity for vehicle id: {}", id);
            throw new ValidationError("Restock quantity is required.");
        }

        Vehicle vehicle = vehicleService.restock(id, request.quantity());
        log.info("Vehicle restock request handled successfully for vehicle id: {}", vehicle.getId());
        return ResponseEntity.ok(vehicle);
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> listVehicles() {
        log.info("Starting vehicle listing request");
        List<Vehicle> vehicles = vehicleService.findVehicles();
        log.info("Vehicle listing request completed successfully with {} vehicles", vehicles.size());
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Vehicle>> searchVehicles(
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        log.info("Starting vehicle search request");
        List<Vehicle> vehicles = vehicleService.searchVehicles(make, model, category, minPrice, maxPrice);
        log.info("Vehicle search request completed successfully with {} vehicles", vehicles.size());
        return ResponseEntity.ok(vehicles);
    }
}
