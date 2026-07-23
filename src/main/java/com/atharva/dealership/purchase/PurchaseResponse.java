package com.atharva.dealership.purchase;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseResponse(
        Long id,
        Long vehicleId,
        String make,
        String model,
        String category,
        BigDecimal price,
        Instant purchasedAt) {

    public static PurchaseResponse from(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getVehicleId(),
                purchase.getVehicleMake(),
                purchase.getVehicleModel(),
                purchase.getVehicleCategory(),
                purchase.getPurchasePrice(),
                purchase.getPurchasedAt());
    }
}
