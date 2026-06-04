package com.workflowdemo.backend.masterdata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
class Organization {

    @Id
    private UUID id;

    @Column(name = "organization_code", nullable = false)
    private String organizationCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_organization_code")
    private String parentOrganizationCode;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    UUID getId() {
        return id;
    }

    String getOrganizationCode() {
        return organizationCode;
    }

    String getName() {
        return name;
    }

    String getParentOrganizationCode() {
        return parentOrganizationCode;
    }

    LocalDate getValidFrom() {
        return validFrom;
    }

    LocalDate getValidTo() {
        return validTo;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
