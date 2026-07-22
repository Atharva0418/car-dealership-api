package com.atharva.dealership.user;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.CUSTOMER;

    protected User() {
    }

    public User(String email, String password) {
        this(email, password, UserRole.CUSTOMER);
    }

    public User(String email, String password, UserRole role) {
        this.email = email;
        this.password = password;
        this.role = role == null ? UserRole.CUSTOMER : role;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role == null ? UserRole.CUSTOMER : role;
    }
}
