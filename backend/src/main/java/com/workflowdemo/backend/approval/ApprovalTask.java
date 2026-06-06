package com.workflowdemo.backend.approval;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.workflowdemo.backend.masterdata.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_tasks")
public class ApprovalTask {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "approver_employee_id", nullable = false)
    private UUID approverEmployeeId;

    @Column(name = "approver_name", nullable = false)
    private String approverName;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(nullable = false)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApprovalTask() {
    }

    public ApprovalTask(UUID applicationId, Employee approver, String stepName) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.approverEmployeeId = approver.getId();
        this.approverName = approver.getName();
        this.stepName = stepName;
        this.status = "PENDING";
        this.dueDate = LocalDate.now().plusDays(3);
        this.createdAt = Instant.now();
    }

    public void approve() {
        complete("APPROVED");
    }

    public void reject() {
        complete("REJECTED");
    }

    private void complete(String completedStatus) {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Only pending approval tasks can be completed");
        }
        this.status = completedStatus;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getApproverEmployeeId() {
        return approverEmployeeId;
    }

    public String getApproverName() {
        return approverName;
    }

    public String getStepName() {
        return stepName;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
