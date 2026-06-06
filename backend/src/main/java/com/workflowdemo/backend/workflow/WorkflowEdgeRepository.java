package com.workflowdemo.backend.workflow;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEdgeRepository extends JpaRepository<WorkflowEdge, UUID> {

    List<WorkflowEdge> findByWorkflowVersionIdOrderByDisplayOrderAsc(UUID workflowVersionId);
}
