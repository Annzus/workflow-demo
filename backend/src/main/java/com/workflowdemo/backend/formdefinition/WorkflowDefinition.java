package com.workflowdemo.backend.formdefinition;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_definitions")
class WorkflowDefinition {

    @Id
    private UUID id;

    @Column(name = "workflow_code", nullable = false)
    private String workflowCode;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    UUID getId() {
        return id;
    }

    String getWorkflowCode() {
        return workflowCode;
    }

    String getWorkflowName() {
        return workflowName;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
