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

    // Form 4 reporting-person profile — populated only for EXECUTIVE users (copied onto a Filing
    // at generation time by FormGenerationService). LEGAL_COMPLIANCE users are never looked up as
    // a Filing's executive, so these stay null/false for legal-1/legal-2 — see DataSeeder.
    private String lastName;
    private String firstName;
    private String middleName;

    private String street;
    private String city;
    private String state;
    private String zip;

    private boolean relationshipDirector;
    private boolean relationshipOfficer;
    private boolean relationshipTenPercentOwner;
    private boolean relationshipOther;
    private String officerTitle;

    // Fabricated starting share count. FormGenerationService adds/subtracts each new trade's
    // shares against THIS fixed baseline to derive Filing.sharesOwnedFollowingTransaction — not a
    // running ledger across the executive's prior filings (deliberate MVP simplification).
    private Integer baselineShareholding;

    protected User() {
        // JPA
    }

    public User(String id, String displayName, UserRole role) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
    }

    public static Builder builder(String id, String displayName, UserRole role) {
        return new Builder(id, displayName, role);
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

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

    public boolean isRelationshipDirector() {
        return relationshipDirector;
    }

    public boolean isRelationshipOfficer() {
        return relationshipOfficer;
    }

    public boolean isRelationshipTenPercentOwner() {
        return relationshipTenPercentOwner;
    }

    public boolean isRelationshipOther() {
        return relationshipOther;
    }

    public String getOfficerTitle() {
        return officerTitle;
    }

    public Integer getBaselineShareholding() {
        return baselineShareholding;
    }

    /** Ergonomic construction only — DataSeeder is the sole caller. User stays setter-free after
     * construction (Hibernate hydrates via field access, inferred from @Id on the field, so it
     * never needs this builder or any setter). */
    public static final class Builder {
        private final String id;
        private final String displayName;
        private final UserRole role;
        private String lastName;
        private String firstName;
        private String middleName;
        private String street;
        private String city;
        private String state;
        private String zip;
        private boolean relationshipDirector;
        private boolean relationshipOfficer;
        private boolean relationshipTenPercentOwner;
        private boolean relationshipOther;
        private String officerTitle;
        private Integer baselineShareholding;

        private Builder(String id, String displayName, UserRole role) {
            this.id = id;
            this.displayName = displayName;
            this.role = role;
        }

        public Builder name(String lastName, String firstName, String middleName) {
            this.lastName = lastName;
            this.firstName = firstName;
            this.middleName = middleName;
            return this;
        }

        public Builder address(String street, String city, String state, String zip) {
            this.street = street;
            this.city = city;
            this.state = state;
            this.zip = zip;
            return this;
        }

        public Builder director() {
            this.relationshipDirector = true;
            return this;
        }

        public Builder officer(String officerTitle) {
            this.relationshipOfficer = true;
            this.officerTitle = officerTitle;
            return this;
        }

        public Builder tenPercentOwner() {
            this.relationshipTenPercentOwner = true;
            return this;
        }

        public Builder other() {
            this.relationshipOther = true;
            return this;
        }

        public Builder baselineShareholding(int baselineShareholding) {
            this.baselineShareholding = baselineShareholding;
            return this;
        }

        public User build() {
            User user = new User(id, displayName, role);
            user.lastName = lastName;
            user.firstName = firstName;
            user.middleName = middleName;
            user.street = street;
            user.city = city;
            user.state = state;
            user.zip = zip;
            user.relationshipDirector = relationshipDirector;
            user.relationshipOfficer = relationshipOfficer;
            user.relationshipTenPercentOwner = relationshipTenPercentOwner;
            user.relationshipOther = relationshipOther;
            user.officerTitle = officerTitle;
            user.baselineShareholding = baselineShareholding;
            return user;
        }
    }
}
