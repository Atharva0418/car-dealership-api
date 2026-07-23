package com.atharva.dealership.purchase;

import com.atharva.dealership.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleCategory;

    @Column(precision = 19, scale = 2)
    private BigDecimal purchasePrice;

    private Instant purchasedAt;

    protected Purchase() {
    }

    public Purchase(
            User user,
            Long vehicleId,
            String vehicleMake,
            String vehicleModel,
            String vehicleCategory,
            BigDecimal purchasePrice,
            Instant purchasedAt) {
        this.user = user;
        this.vehicleId = vehicleId;
        this.vehicleMake = vehicleMake;
        this.vehicleModel = vehicleModel;
        this.vehicleCategory = vehicleCategory;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = purchasedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public String getVehicleCategory() {
        return vehicleCategory;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }
}
