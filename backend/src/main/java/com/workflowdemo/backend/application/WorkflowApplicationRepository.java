package com.workflowdemo.backend.application;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowApplicationRepository extends JpaRepository<WorkflowApplication, UUID> {

    List<WorkflowApplication> findByApplicantEmployeeIdOrderByCreatedAtDesc(UUID applicantEmployeeId);
}
