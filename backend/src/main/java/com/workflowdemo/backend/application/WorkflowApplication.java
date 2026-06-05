package com.workflowdemo.backend.application;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "applications")
class WorkflowApplication {

    @Id
    private UUID id;

    @Column(name = "application_number", nullable = false)
    private String applicationNumber;

    @Column(name = "form_definition_id", nullable = false)
    private UUID formDefinitionId;

    @Column(nullable = false)
    private String title;

    @Column(name = "applicant_employee_id", nullable = false)
    private UUID applicantEmployeeId;

    @Column(nullable = false)
    private String status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowApplication() {
    }

    WorkflowApplication(UUID formDefinitionId, String title, UUID applicantEmployeeId) {
        this.id = UUID.randomUUID();
        Instant now = Instant.now();
        this.applicationNumber = "APP-" + now.toString().replaceAll("[^0-9]", "").substring(0, 14)
            + "-" + this.id.toString().substring(0, 8).toUpperCase();
        this.formDefinitionId = formDefinitionId;
        this.title = title;
        this.applicantEmployeeId = applicantEmployeeId;
        this.status = "DRAFT";
        this.createdAt = now;
    }

    UUID getId() {
        return id;
    }

    UUID getFormDefinitionId() {
        return formDefinitionId;
    }

    String getApplicationNumber() {
        return applicationNumber;
    }

    String getTitle() {
        return title;
    }

    UUID getApplicantEmployeeId() {
        return applicantEmployeeId;
    }

    String getStatus() {
        return status;
    }

    Instant getSubmittedAt() {
        return submittedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
