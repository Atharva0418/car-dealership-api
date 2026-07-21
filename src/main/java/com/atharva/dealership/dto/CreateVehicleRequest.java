package com.atharva.dealership.dto;

import java.math.BigDecimal;

public record CreateVehicleRequest(
        String make,
        String model,
        String category,
        BigDecimal price,
        Integer quantityInStock) {
}
