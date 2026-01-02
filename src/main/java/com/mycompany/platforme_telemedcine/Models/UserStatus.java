package com.mycompany.platforme_telemedcine.Models;

public enum UserStatus {
    PENDING,      // Waiting for admin approval
    APPROVED,     // Admin approved, can login
    REJECTED,     // Admin rejected
    SUSPENDED;     // Temporarily blocked

    // If you need to check if a user can log in, add a method
    public boolean canLogin() {
        return this == APPROVED;
    }

    // If you need to check if a user is active
    public boolean isActive() {
        return this == APPROVED || this == PENDING;
    }

    // If you need to check if a user is blocked
    public boolean isBlocked() {
        return this == REJECTED || this == SUSPENDED;
    }


    public boolean equalsIgnoreCase(String status) {
        return this.name().equalsIgnoreCase(status);
    }
}