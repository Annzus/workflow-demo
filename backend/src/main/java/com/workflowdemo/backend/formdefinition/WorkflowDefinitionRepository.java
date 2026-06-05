package com.workflowdemo.backend.formdefinition;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {
}
