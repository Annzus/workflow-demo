package com.workflowdemo.backend.formdefinition;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinition {

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

    protected WorkflowDefinition() {
    }

    public WorkflowDefinition(String workflowCode, String workflowName) {
        this.id = UUID.randomUUID();
        this.workflowCode = workflowCode;
        this.workflowName = workflowName;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void updateName(String workflowName) {
        this.workflowName = workflowName;
    }

    public UUID getId() {
        return id;
    }

    public String getWorkflowCode() {
        return workflowCode;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
