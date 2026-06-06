package com.workflowdemo.backend.application;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "applications")
public class WorkflowApplication {

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

    @Column(name = "workflow_version_id")
    private UUID workflowVersionId;

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

    public void submit(UUID workflowVersionId) {
        if (!"DRAFT".equals(status)) {
            throw new IllegalStateException("Only draft applications can be submitted");
        }
        this.status = "SUBMITTED";
        this.submittedAt = Instant.now();
        this.workflowVersionId = workflowVersionId;
    }

    public void approve() {
        if (!"SUBMITTED".equals(status)) {
            throw new IllegalStateException("Only submitted applications can be approved");
        }
        this.status = "APPROVED";
    }

    public void reject() {
        if (!"SUBMITTED".equals(status)) {
            throw new IllegalStateException("Only submitted applications can be rejected");
        }
        this.status = "REJECTED";
    }

    public UUID getId() {
        return id;
    }

    public UUID getFormDefinitionId() {
        return formDefinitionId;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public String getTitle() {
        return title;
    }

    public UUID getApplicantEmployeeId() {
        return applicantEmployeeId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
