package com.workflowdemo.backend.masterdata;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    private UUID id;

    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "position_name", nullable = false)
    private String positionName;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getName() {
        return name;
    }

    String getEmail() {
        return email;
    }

    String getOrganizationName() {
        return organizationName;
    }

    String getPositionName() {
        return positionName;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
