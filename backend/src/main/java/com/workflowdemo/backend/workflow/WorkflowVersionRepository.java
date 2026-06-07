package com.workflowdemo.backend.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {

    List<WorkflowVersion> findByWorkflowDefinitionIdOrderByVersionNumberDesc(UUID workflowDefinitionId);

    Optional<WorkflowVersion> findFirstByWorkflowDefinitionIdAndPublishedTrueOrderByVersionNumberDesc(
        UUID workflowDefinitionId
    );

    Optional<WorkflowVersion> findFirstByWorkflowDefinitionIdAndPublishedFalseOrderByVersionNumberDesc(
        UUID workflowDefinitionId
    );
}
