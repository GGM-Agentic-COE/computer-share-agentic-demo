package com.computershare.regfiling.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private String id;

    private String userId;

    private String filingId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    private boolean read;

    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public Notification(String userId, String filingId, NotificationType type, String message) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.filingId = filingId;
        this.type = type;
        this.message = message;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getFilingId() {
        return filingId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
