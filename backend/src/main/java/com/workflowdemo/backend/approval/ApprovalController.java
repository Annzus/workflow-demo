package com.workflowdemo.backend.approval;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.workflowdemo.backend.application.WorkflowApplication;
import com.workflowdemo.backend.application.WorkflowApplicationRepository;
import com.workflowdemo.backend.auth.DemoUserContext;
import com.workflowdemo.backend.formdefinition.ApplicationFormDefinition;
import com.workflowdemo.backend.formdefinition.ApplicationFormDefinitionRepository;
import com.workflowdemo.backend.masterdata.Employee;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/approval-tasks")
class ApprovalController {

    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final WorkflowApplicationRepository applicationRepository;
    private final ApplicationFormDefinitionRepository formDefinitionRepository;
    private final DemoUserContext demoUserContext;

    ApprovalController(
        ApprovalTaskRepository approvalTaskRepository,
        ApprovalHistoryRepository approvalHistoryRepository,
        WorkflowApplicationRepository applicationRepository,
        ApplicationFormDefinitionRepository formDefinitionRepository,
        DemoUserContext demoUserContext
    ) {
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.applicationRepository = applicationRepository;
        this.formDefinitionRepository = formDefinitionRepository;
        this.demoUserContext = demoUserContext;
    }

    @GetMapping("/pending")
    @Transactional(readOnly = true)
    List<ApprovalTaskResponse> pendingTasks() {
        Employee approver = demoUserContext.currentEmployee();
        List<ApprovalTask> tasks =
            approvalTaskRepository.findByApproverEmployeeIdAndStatusOrderByCreatedAtDesc(approver.getId(), "PENDING");
        Map<UUID, WorkflowApplication> applicationsById = applicationRepository.findAllById(
                tasks.stream().map(ApprovalTask::getApplicationId).toList()
            )
            .stream()
            .collect(Collectors.toMap(WorkflowApplication::getId, Function.identity()));
        Map<UUID, ApplicationFormDefinition> formsById = formDefinitionRepository.findAllById(
                applicationsById.values().stream().map(WorkflowApplication::getFormDefinitionId).toList()
            )
            .stream()
            .collect(Collectors.toMap(ApplicationFormDefinition::getId, Function.identity()));

        return tasks.stream()
            .map(task -> ApprovalTaskResponse.from(
                task,
                applicationsById.get(task.getApplicationId()),
                formsById
            ))
            .toList();
    }

    @PostMapping("/{id}/approve")
    @Transactional
    ApprovalTaskActionResponse approve(@PathVariable UUID id, @Valid @RequestBody ApprovalActionRequest request) {
        return completeTask(id, "APPROVE", request.comment());
    }

    @PostMapping("/{id}/reject")
    @Transactional
    ApprovalTaskActionResponse reject(@PathVariable UUID id, @Valid @RequestBody ApprovalActionRequest request) {
        return completeTask(id, "REJECT", request.comment());
    }

    private ApprovalTaskActionResponse completeTask(UUID taskId, String action, String comment) {
        Employee approver = demoUserContext.currentEmployee();
        ApprovalTask task = approvalTaskRepository.findById(taskId)
            .filter(foundTask -> foundTask.getApproverEmployeeId().equals(approver.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval task not found"));
        WorkflowApplication application = applicationRepository.findById(task.getApplicationId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        try {
            if ("APPROVE".equals(action)) {
                task.approve();
                application.approve();
            } else {
                task.reject();
                application.reject();
            }
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        ApprovalHistory history = approvalHistoryRepository.save(
            new ApprovalHistory(application.getId(), approver, action, normalizeComment(comment))
        );
        return new ApprovalTaskActionResponse(
            task.getId(),
            application.getId(),
            application.getStatus(),
            task.getStatus(),
            ApprovalHistoryResponse.from(history)
        );
    }

    private static String normalizeComment(String comment) {
        return comment == null ? "" : comment.trim();
    }

    record ApprovalActionRequest(String comment) {
    }

    record ApprovalTaskResponse(
        UUID id,
        UUID applicationId,
        String applicationNumber,
        String title,
        String formName,
        String approverName,
        String stepName,
        String status,
        String dueDate,
        String createdAt
    ) {
        static ApprovalTaskResponse from(
            ApprovalTask task,
            WorkflowApplication application,
            Map<UUID, ApplicationFormDefinition> formsById
        ) {
            ApplicationFormDefinition formDefinition =
                application == null ? null : formsById.get(application.getFormDefinitionId());

            return new ApprovalTaskResponse(
                task.getId(),
                task.getApplicationId(),
                application == null ? "" : application.getApplicationNumber(),
                application == null ? "" : application.getTitle(),
                formDefinition == null ? "" : formDefinition.getFormName(),
                task.getApproverName(),
                task.getStepName(),
                task.getStatus(),
                task.getDueDate() == null ? null : task.getDueDate().toString(),
                task.getCreatedAt().toString()
            );
        }
    }

    record ApprovalTaskActionResponse(
        UUID taskId,
        UUID applicationId,
        String applicationStatus,
        String taskStatus,
        ApprovalHistoryResponse history
    ) {
    }

    public record ApprovalHistoryResponse(
        UUID id,
        String actorName,
        String action,
        String comment,
        String createdAt
    ) {
        public static ApprovalHistoryResponse from(ApprovalHistory history) {
            return new ApprovalHistoryResponse(
                history.getId(),
                history.getActorName(),
                history.getAction(),
                history.getComment(),
                history.getCreatedAt().toString()
            );
        }
    }
}
