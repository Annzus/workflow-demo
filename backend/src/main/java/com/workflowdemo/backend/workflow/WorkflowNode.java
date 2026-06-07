package com.workflowdemo.backend.workflow;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_nodes")
public class WorkflowNode {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(name = "node_type", nullable = false)
    private String nodeType;

    @Column(name = "approver_type")
    private String approverType;

    @Column(name = "position_code")
    private String positionCode;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "x_position", nullable = false)
    private int xPosition;

    @Column(name = "y_position", nullable = false)
    private int yPosition;

    protected WorkflowNode() {
    }

    WorkflowNode(
        UUID workflowVersionId,
        String nodeKey,
        String nodeName,
        String nodeType,
        String approverType,
        String positionCode,
        String employeeCode,
        int displayOrder,
        int xPosition,
        int yPosition
    ) {
        this.id = UUID.randomUUID();
        this.workflowVersionId = workflowVersionId;
        this.nodeKey = nodeKey;
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.approverType = approverType;
        this.positionCode = positionCode;
        this.employeeCode = employeeCode;
        this.displayOrder = displayOrder;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getApproverType() {
        return approverType;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public int getXPosition() {
        return xPosition;
    }

    public int getYPosition() {
        return yPosition;
    }
}
