package com.workflowdemo.backend.attachment;

import java.time.Instant;
import java.util.UUID;

import com.workflowdemo.backend.masterdata.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_attachments")
public class ApplicationAttachment {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_by_employee_id", nullable = false)
    private UUID uploadedByEmployeeId;

    @Column(name = "uploaded_by_name", nullable = false)
    private String uploadedByName;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ApplicationAttachment() {
    }

    ApplicationAttachment(
        UUID id,
        UUID applicationId,
        String originalFilename,
        String objectKey,
        String contentType,
        long sizeBytes,
        Employee uploadedBy
    ) {
        this.id = id;
        this.applicationId = applicationId;
        this.originalFilename = originalFilename;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedByEmployeeId = uploadedBy.getId();
        this.uploadedByName = uploadedBy.getName();
        this.uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
