package com.workflowdemo.backend.application;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface WorkflowApplicationRepository extends JpaRepository<WorkflowApplication, UUID> {
}
