package com.workflowdemo.backend.approval;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, UUID> {

    List<ApprovalTask> findByApproverEmployeeIdAndStatusOrderByCreatedAtDesc(UUID approverEmployeeId, String status);

    Optional<ApprovalTask> findByApplicationIdAndStatus(UUID applicationId, String status);
}
