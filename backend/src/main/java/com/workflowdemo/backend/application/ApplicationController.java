package com.workflowdemo.backend.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.workflowdemo.backend.approval.ApprovalHistory;
import com.workflowdemo.backend.approval.ApprovalHistoryRepository;
import com.workflowdemo.backend.approval.ApprovalTask;
import com.workflowdemo.backend.approval.ApprovalTaskRepository;
import com.workflowdemo.backend.auth.DemoUserContext;
import com.workflowdemo.backend.formdefinition.ApplicationFormDefinition;
import com.workflowdemo.backend.formdefinition.ApplicationFormDefinitionRepository;
import com.workflowdemo.backend.formdefinition.ApplicationFormField;
import com.workflowdemo.backend.formdefinition.ApplicationFormFieldRepository;
import com.workflowdemo.backend.masterdata.Employee;
import com.workflowdemo.backend.masterdata.EmployeeRepository;
import com.workflowdemo.backend.workflow.WorkflowNode;
import com.workflowdemo.backend.workflow.WorkflowRouteService;
import com.workflowdemo.backend.workflow.WorkflowRouteService.WorkflowRoute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/applications")
class ApplicationController {

    private final ApplicationFormDefinitionRepository formDefinitionRepository;
    private final ApplicationFormFieldRepository formFieldRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkflowApplicationRepository applicationRepository;
    private final ApplicationFieldValueRepository fieldValueRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final DemoUserContext demoUserContext;
    private final WorkflowRouteService workflowRouteService;

    ApplicationController(
        ApplicationFormDefinitionRepository formDefinitionRepository,
        ApplicationFormFieldRepository formFieldRepository,
        EmployeeRepository employeeRepository,
        WorkflowApplicationRepository applicationRepository,
        ApplicationFieldValueRepository fieldValueRepository,
        ApprovalTaskRepository approvalTaskRepository,
        ApprovalHistoryRepository approvalHistoryRepository,
        DemoUserContext demoUserContext,
        WorkflowRouteService workflowRouteService
    ) {
        this.formDefinitionRepository = formDefinitionRepository;
        this.formFieldRepository = formFieldRepository;
        this.employeeRepository = employeeRepository;
        this.applicationRepository = applicationRepository;
        this.fieldValueRepository = fieldValueRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.demoUserContext = demoUserContext;
        this.workflowRouteService = workflowRouteService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<ApplicationSummaryResponse> applications() {
        Employee applicant = demoUserContext.currentEmployee();
        List<WorkflowApplication> applications =
            applicationRepository.findByApplicantEmployeeIdOrderByCreatedAtDesc(applicant.getId());
        Map<UUID, ApplicationFormDefinition> formsById = formDefinitionRepository.findAllById(
                applications.stream()
                    .map(WorkflowApplication::getFormDefinitionId)
                    .toList()
            )
            .stream()
            .collect(Collectors.toMap(ApplicationFormDefinition::getId, Function.identity()));

        return applications.stream()
            .map(application -> ApplicationSummaryResponse.from(
                application,
                formsById.get(application.getFormDefinitionId()),
                applicant
            ))
            .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    ApplicationDetailResponse application(@PathVariable UUID id) {
        Employee currentEmployee = demoUserContext.currentEmployee();
        WorkflowApplication application = applicationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        List<ApprovalTask> approvalTasks = approvalTaskRepository.findByApplicationIdOrderByCreatedAtAsc(
            application.getId()
        );
        ensureApplicationReadable(application, currentEmployee, approvalTasks);
        Employee applicant = applicantFor(application);
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findById(application.getFormDefinitionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));
        WorkflowRoute workflowRoute = workflowRouteService.routeForApplication(
            formDefinition,
            application.getWorkflowVersionId()
        );
        List<ApplicationFieldValue> fieldValues =
            fieldValueRepository.findByApplicationIdOrderByDisplayOrderAsc(application.getId());

        return ApplicationDetailResponse.from(
            application,
            formDefinition,
            applicant,
            fieldValues,
            buildApprovalRoute(application, applicant, workflowRoute, approvalTasks)
        );
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DraftApplicationResponse createDraft(@Valid @RequestBody CreateDraftApplicationRequest request) {
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findByFormCodeAndActiveTrue(request.formCode())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));
        Employee applicant = demoUserContext.currentEmployee();
        List<ApplicationFormField> fields =
            formFieldRepository.findByFormDefinitionIdAndActiveTrueOrderByDisplayOrderAsc(formDefinition.getId());

        validateRequiredFields(fields, request.values());

        WorkflowApplication application = applicationRepository.save(
            new WorkflowApplication(formDefinition.getId(), request.title(), applicant.getId())
        );
        List<ApplicationFieldValue> fieldValues = fields.stream()
            .map(field -> new ApplicationFieldValue(
                application.getId(),
                field,
                normalizeValue(request.values().get(field.getFieldKey()))
            ))
            .toList();
        fieldValueRepository.saveAll(fieldValues);

        return new DraftApplicationResponse(
            application.getId(),
            application.getApplicationNumber(),
            application.getTitle(),
            application.getStatus(),
            applicant.getName(),
            formDefinition.getFormName(),
            fieldValues.size()
        );
    }

    @PostMapping("/{id}/submit")
    @Transactional
    ApplicationDetailResponse submitApplication(@PathVariable UUID id) {
        Employee applicant = demoUserContext.currentEmployee();
        WorkflowApplication application = applicationRepository.findById(id)
            .filter(foundApplication -> foundApplication.getApplicantEmployeeId().equals(applicant.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findById(application.getFormDefinitionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));
        WorkflowRoute workflowRoute = workflowRouteService.routeForForm(formDefinition);

        try {
            application.submit(workflowRoute.versionId());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        approvalTaskRepository.save(
            new ApprovalTask(application.getId(), workflowRoute.approver(), workflowRoute.approvalNode().getNodeName())
        );
        approvalHistoryRepository.save(new ApprovalHistory(application.getId(), applicant, "SUBMIT", "申請を提出"));
        List<ApplicationFieldValue> fieldValues =
            fieldValueRepository.findByApplicationIdOrderByDisplayOrderAsc(application.getId());
        List<ApprovalTask> approvalTasks = approvalTaskRepository.findByApplicationIdOrderByCreatedAtAsc(
            application.getId()
        );

        return ApplicationDetailResponse.from(
            application,
            formDefinition,
            applicant,
            fieldValues,
            buildApprovalRoute(application, applicant, workflowRoute, approvalTasks)
        );
    }

    @GetMapping("/{id}/history")
    @Transactional(readOnly = true)
    List<ApplicationHistoryResponse> applicationHistory(@PathVariable UUID id) {
        Employee currentEmployee = demoUserContext.currentEmployee();
        WorkflowApplication application = applicationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        ensureApplicationReadable(
            application,
            currentEmployee,
            approvalTaskRepository.findByApplicationIdOrderByCreatedAtAsc(application.getId())
        );

        return approvalHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(application.getId())
            .stream()
            .map(ApplicationHistoryResponse::from)
            .toList();
    }

    private static void validateRequiredFields(List<ApplicationFormField> fields, Map<String, String> values) {
        fields.stream()
            .filter(ApplicationFormField::isRequired)
            .filter(field -> normalizeValue(values.get(field.getFieldKey())).isBlank())
            .findFirst()
            .ifPresent(field -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field.getLabel() + " is required");
            });
    }

    private Employee applicantFor(WorkflowApplication application) {
        return employeeRepository.findById(application.getApplicantEmployeeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Applicant not found"));
    }

    private static void ensureApplicationReadable(
        WorkflowApplication application,
        Employee currentEmployee,
        List<ApprovalTask> approvalTasks
    ) {
        if (application.getApplicantEmployeeId().equals(currentEmployee.getId())) {
            return;
        }
        boolean currentApprover = approvalTasks.stream()
            .anyMatch(task -> task.getApproverEmployeeId().equals(currentEmployee.getId()));
        if (!currentApprover) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<ApprovalRouteStepResponse> buildApprovalRoute(
        WorkflowApplication application,
        Employee applicant,
        WorkflowRoute workflowRoute,
        List<ApprovalTask> approvalTasks
    ) {
        ApprovalTask approvalTask = approvalTasks.isEmpty() ? null : approvalTasks.getFirst();
        return workflowRoute.nodes().stream()
            .map(node -> approvalRouteStep(application, applicant, workflowRoute, approvalTask, node))
            .toList();
    }

    private static ApprovalRouteStepResponse approvalRouteStep(
        WorkflowApplication application,
        Employee applicant,
        WorkflowRoute workflowRoute,
        ApprovalTask approvalTask,
        WorkflowNode node
    ) {
        if ("APPLICANT".equals(node.getNodeType())) {
            return new ApprovalRouteStepResponse(
                node.getNodeKey(),
                node.getNodeName(),
                applicant.getName(),
                applicant.getPositionName(),
                applicantStepStatus(application.getStatus()),
                application.getSubmittedAt() == null ? null : application.getSubmittedAt().toString()
            );
        }
        if ("APPROVAL".equals(node.getNodeType())) {
            return new ApprovalRouteStepResponse(
                node.getNodeKey(),
                approvalTask == null ? node.getNodeName() : approvalTask.getStepName(),
                approvalTask == null ? workflowRoute.approver().getName() : approvalTask.getApproverName(),
                "承認者",
                approvalStepStatus(application.getStatus(), approvalTask),
                approvalTask == null || approvalTask.getCompletedAt() == null
                    ? null
                    : approvalTask.getCompletedAt().toString()
            );
        }
        return new ApprovalRouteStepResponse(
            node.getNodeKey(),
            node.getNodeName(),
            "",
            "最終状態",
            finalStepStatus(application.getStatus()),
            finalCompletedAt(application, approvalTask)
        );
    }

    private static String applicantStepStatus(String applicationStatus) {
        return "DRAFT".equals(applicationStatus) ? "CURRENT" : "COMPLETED";
    }

    private static String approvalStepStatus(String applicationStatus, ApprovalTask approvalTask) {
        if ("REJECTED".equals(applicationStatus)) {
            return "REJECTED";
        }
        if ("APPROVED".equals(applicationStatus)) {
            return "COMPLETED";
        }
        if ("SUBMITTED".equals(applicationStatus)) {
            if (approvalTask == null) {
                return "WAITING";
            }
            return "PENDING".equals(approvalTask.getStatus()) ? "CURRENT" : "COMPLETED";
        }
        return "WAITING";
    }

    private static String finalStepStatus(String applicationStatus) {
        if ("APPROVED".equals(applicationStatus)) {
            return "COMPLETED";
        }
        if ("REJECTED".equals(applicationStatus)) {
            return "REJECTED";
        }
        return "WAITING";
    }

    private static String finalCompletedAt(WorkflowApplication application, ApprovalTask approvalTask) {
        if (!"APPROVED".equals(application.getStatus()) && !"REJECTED".equals(application.getStatus())) {
            return null;
        }
        return approvalTask == null || approvalTask.getCompletedAt() == null
            ? null
            : approvalTask.getCompletedAt().toString();
    }

    record CreateDraftApplicationRequest(
        @NotBlank String formCode,
        @NotBlank String title,
        @NotEmpty Map<String, String> values
    ) {
    }

    record DraftApplicationResponse(
        UUID id,
        String applicationNumber,
        String title,
        String status,
        String applicantName,
        String formName,
        int fieldValueCount
    ) {
    }

    record ApplicationSummaryResponse(
        UUID id,
        String applicationNumber,
        String title,
        String status,
        String applicantName,
        String formName,
        String createdAt,
        String submittedAt
    ) {
        static ApplicationSummaryResponse from(
            WorkflowApplication application,
            ApplicationFormDefinition formDefinition,
            Employee applicant
        ) {
            return new ApplicationSummaryResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getTitle(),
                application.getStatus(),
                applicant.getName(),
                formDefinition == null ? "" : formDefinition.getFormName(),
                application.getCreatedAt().toString(),
                application.getSubmittedAt() == null ? null : application.getSubmittedAt().toString()
            );
        }
    }

    record ApplicationDetailResponse(
        UUID id,
        String applicationNumber,
        String title,
        String status,
        String applicantName,
        String formName,
        String createdAt,
        String submittedAt,
        List<ApplicationFieldValueResponse> values,
        List<ApprovalRouteStepResponse> approvalRoute
    ) {
        static ApplicationDetailResponse from(
            WorkflowApplication application,
            ApplicationFormDefinition formDefinition,
            Employee applicant,
            List<ApplicationFieldValue> fieldValues,
            List<ApprovalRouteStepResponse> approvalRoute
        ) {
            return new ApplicationDetailResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getTitle(),
                application.getStatus(),
                applicant.getName(),
                formDefinition.getFormName(),
                application.getCreatedAt().toString(),
                application.getSubmittedAt() == null ? null : application.getSubmittedAt().toString(),
                fieldValues.stream().map(ApplicationFieldValueResponse::from).toList(),
                approvalRoute
            );
        }
    }

    record ApprovalRouteStepResponse(
        String stepKey,
        String stepName,
        String actorName,
        String roleName,
        String status,
        String completedAt
    ) {
    }

    record ApplicationFieldValueResponse(
        String fieldKey,
        String label,
        String dataType,
        String value,
        int displayOrder
    ) {
        static ApplicationFieldValueResponse from(ApplicationFieldValue fieldValue) {
            return new ApplicationFieldValueResponse(
                fieldValue.getFieldKey(),
                fieldValue.getLabel(),
                fieldValue.getDataType(),
                fieldValue.getValueText(),
                fieldValue.getDisplayOrder()
            );
        }
    }

    record ApplicationHistoryResponse(
        UUID id,
        String actorName,
        String action,
        String comment,
        String createdAt
    ) {
        static ApplicationHistoryResponse from(ApprovalHistory history) {
            return new ApplicationHistoryResponse(
                history.getId(),
                history.getActorName(),
                history.getAction(),
                history.getComment(),
                history.getCreatedAt().toString()
            );
        }
    }
}
