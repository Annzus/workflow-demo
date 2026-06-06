package com.workflowdemo.backend.approval;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    List<ApprovalHistory> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
