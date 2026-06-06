package com.workflowdemo.backend.attachment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ApplicationAttachmentRepository extends JpaRepository<ApplicationAttachment, UUID> {

    List<ApplicationAttachment> findByApplicationIdOrderByUploadedAtDesc(UUID applicationId);
}
