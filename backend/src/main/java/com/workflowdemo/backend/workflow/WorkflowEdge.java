package com.workflowdemo.backend.workflow;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_edges")
public class WorkflowEdge {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "source_node_key", nullable = false)
    private String sourceNodeKey;

    @Column(name = "target_node_key", nullable = false)
    private String targetNodeKey;

    @Column(name = "condition_expression")
    private String conditionExpression;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public String getSourceNodeKey() {
        return sourceNodeKey;
    }

    public String getTargetNodeKey() {
        return targetNodeKey;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
