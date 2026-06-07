package com.workflowdemo.backend.workflow;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_versions")
public class WorkflowVersion {

    @Id
    private UUID id;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowVersion() {
    }

    WorkflowVersion(UUID workflowDefinitionId, int versionNumber, boolean published) {
        this.id = UUID.randomUUID();
        this.workflowDefinitionId = workflowDefinitionId;
        this.versionNumber = versionNumber;
        this.published = published;
        this.createdAt = Instant.now();
    }

    void publish() {
        this.published = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
