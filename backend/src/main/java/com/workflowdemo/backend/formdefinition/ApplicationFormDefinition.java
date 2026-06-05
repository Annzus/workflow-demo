package com.workflowdemo.backend.formdefinition;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_form_definitions")
class ApplicationFormDefinition {

    @Id
    private UUID id;

    @Column(name = "form_code", nullable = false)
    private String formCode;

    @Column(name = "form_name", nullable = false)
    private String formName;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    UUID getId() {
        return id;
    }

    String getFormCode() {
        return formCode;
    }

    String getFormName() {
        return formName;
    }

    UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
