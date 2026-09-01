package com.computershare.regfiling.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Explicit table name: "user" is a reserved word in H2 (and several other SQL dialects) and
// collides with the USER() function — matches db-schema.sql's "users" table name anyway.
@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;

    private String displayName;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    protected User() {
        // JPA
    }

    public User(String id, String displayName, UserRole role) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }
}
