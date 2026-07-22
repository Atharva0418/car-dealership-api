package com.atharva.dealership.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String make;
    private String model;
    private String category;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    private Integer quantityInStock;

    protected Vehicle() {
    }

    public Vehicle(String make, String model, String category, BigDecimal price, Integer quantityInStock) {
        this(null, make, model, category, price, quantityInStock);
    }

    public Vehicle(Long id, String make, String model, String category, BigDecimal price, Integer quantityInStock) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.category = category;
        this.price = price;
        this.quantityInStock = quantityInStock;
    }

    public Long getId() {
        return id;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getQuantityInStock() {
        return quantityInStock;
    }
}
