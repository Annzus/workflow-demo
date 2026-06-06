package com.workflowdemo.backend.approval;

import java.time.Instant;
import java.util.UUID;

import com.workflowdemo.backend.masterdata.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_histories")
public class ApprovalHistory {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "actor_employee_id", nullable = false)
    private UUID actorEmployeeId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(nullable = false)
    private String action;

    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApprovalHistory() {
    }

    public ApprovalHistory(UUID applicationId, Employee actor, String action, String comment) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.actorEmployeeId = actor.getId();
        this.actorName = actor.getName();
        this.action = action;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
