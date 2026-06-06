package com.workflowdemo.backend.attachment;

import java.util.List;
import java.util.UUID;

import com.workflowdemo.backend.approval.ApprovalTask;
import com.workflowdemo.backend.approval.ApprovalTaskRepository;
import com.workflowdemo.backend.application.WorkflowApplication;
import com.workflowdemo.backend.application.WorkflowApplicationRepository;
import com.workflowdemo.backend.auth.DemoUserContext;
import com.workflowdemo.backend.masterdata.Employee;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/applications/{applicationId}/attachments")
class ApplicationAttachmentController {

    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 150;

    private final WorkflowApplicationRepository applicationRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApplicationAttachmentRepository attachmentRepository;
    private final AttachmentStorageService storageService;
    private final DemoUserContext demoUserContext;

    ApplicationAttachmentController(
        WorkflowApplicationRepository applicationRepository,
        ApprovalTaskRepository approvalTaskRepository,
        ApplicationAttachmentRepository attachmentRepository,
        AttachmentStorageService storageService,
        DemoUserContext demoUserContext
    ) {
        this.applicationRepository = applicationRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
        this.demoUserContext = demoUserContext;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<ApplicationAttachmentResponse> attachments(@PathVariable UUID applicationId) {
        ensureApplicationReadable(applicationId);
        return attachmentRepository.findByApplicationIdOrderByUploadedAtDesc(applicationId)
            .stream()
            .map(ApplicationAttachmentResponse::from)
            .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    ApplicationAttachmentResponse uploadAttachment(
        @PathVariable UUID applicationId,
        @RequestPart("file") MultipartFile file
    ) {
        Employee applicant = demoUserContext.currentEmployee();
        WorkflowApplication application = ensureApplicantAccess(applicationId, applicant);
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment file is empty");
        }

        UUID attachmentId = UUID.randomUUID();
        String originalFilename = normalizeFilename(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());
        validateAttachmentMetadata(originalFilename, contentType);
        String objectKey;
        try {
            objectKey = storageService.store(
                application.getId(),
                attachmentId,
                originalFilename,
                contentType,
                file.getSize(),
                file.getInputStream()
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Attachment upload failed", exception);
        }

        ApplicationAttachment attachment = attachmentRepository.save(
            new ApplicationAttachment(
                attachmentId,
                application.getId(),
                originalFilename,
                objectKey,
                contentType,
                file.getSize(),
                applicant
            )
        );

        return ApplicationAttachmentResponse.from(attachment);
    }

    private WorkflowApplication ensureApplicantAccess(UUID applicationId, Employee applicant) {
        return applicationRepository.findById(applicationId)
            .filter(application -> application.getApplicantEmployeeId().equals(applicant.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private void ensureApplicationReadable(UUID applicationId) {
        Employee currentEmployee = demoUserContext.currentEmployee();
        WorkflowApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (application.getApplicantEmployeeId().equals(currentEmployee.getId())) {
            return;
        }
        List<ApprovalTask> approvalTasks = approvalTaskRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        boolean currentApprover = approvalTasks.stream()
            .anyMatch(task -> task.getApproverEmployeeId().equals(currentEmployee.getId()));
        if (!currentApprover) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
    }

    private static String normalizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment";
        }
        return filename.strip();
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }

    private static void validateAttachmentMetadata(String originalFilename, String contentType) {
        if (originalFilename.length() > MAX_ORIGINAL_FILENAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment filename is too long");
        }
        if (contentType.length() > MAX_CONTENT_TYPE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment content type is too long");
        }
    }

    record ApplicationAttachmentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String uploadedByName,
        String uploadedAt
    ) {
        static ApplicationAttachmentResponse from(ApplicationAttachment attachment) {
            return new ApplicationAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploadedByName(),
                attachment.getUploadedAt().toString()
            );
        }
    }
}
