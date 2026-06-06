package com.workflowdemo.backend.formdefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    List<WorkflowDefinition> findByActiveTrueOrderByWorkflowCodeAsc();

    Optional<WorkflowDefinition> findByWorkflowCodeAndActiveTrue(String workflowCode);
}
