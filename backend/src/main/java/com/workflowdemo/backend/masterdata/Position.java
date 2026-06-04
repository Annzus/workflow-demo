package com.workflowdemo.backend.masterdata;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "positions")
class Position {

    @Id
    private UUID id;

    @Column(name = "position_code", nullable = false)
    private String positionCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "approval_rank", nullable = false)
    private int approvalRank;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    UUID getId() {
        return id;
    }

    String getPositionCode() {
        return positionCode;
    }

    String getName() {
        return name;
    }

    int getApprovalRank() {
        return approvalRank;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
