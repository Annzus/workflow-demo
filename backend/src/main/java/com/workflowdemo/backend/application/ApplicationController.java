package com.workflowdemo.backend.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.workflowdemo.backend.formdefinition.ApplicationFormDefinition;
import com.workflowdemo.backend.formdefinition.ApplicationFormDefinitionRepository;
import com.workflowdemo.backend.formdefinition.ApplicationFormField;
import com.workflowdemo.backend.formdefinition.ApplicationFormFieldRepository;
import com.workflowdemo.backend.masterdata.Employee;
import com.workflowdemo.backend.masterdata.EmployeeRepository;

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

    private static final String DEMO_APPLICANT_EMPLOYEE_CODE = "1001";

    private final ApplicationFormDefinitionRepository formDefinitionRepository;
    private final ApplicationFormFieldRepository formFieldRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkflowApplicationRepository applicationRepository;
    private final ApplicationFieldValueRepository fieldValueRepository;

    ApplicationController(
        ApplicationFormDefinitionRepository formDefinitionRepository,
        ApplicationFormFieldRepository formFieldRepository,
        EmployeeRepository employeeRepository,
        WorkflowApplicationRepository applicationRepository,
        ApplicationFieldValueRepository fieldValueRepository
    ) {
        this.formDefinitionRepository = formDefinitionRepository;
        this.formFieldRepository = formFieldRepository;
        this.employeeRepository = employeeRepository;
        this.applicationRepository = applicationRepository;
        this.fieldValueRepository = fieldValueRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<ApplicationSummaryResponse> applications() {
        Employee applicant = demoApplicant();
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
        Employee applicant = demoApplicant();
        WorkflowApplication application = applicationRepository.findById(id)
            .filter(foundApplication -> foundApplication.getApplicantEmployeeId().equals(applicant.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findById(application.getFormDefinitionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));
        List<ApplicationFieldValue> fieldValues =
            fieldValueRepository.findByApplicationIdOrderByDisplayOrderAsc(application.getId());

        return ApplicationDetailResponse.from(application, formDefinition, applicant, fieldValues);
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DraftApplicationResponse createDraft(@Valid @RequestBody CreateDraftApplicationRequest request) {
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findByFormCodeAndActiveTrue(request.formCode())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));
        Employee applicant = demoApplicant();
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
        Employee applicant = demoApplicant();
        WorkflowApplication application = applicationRepository.findById(id)
            .filter(foundApplication -> foundApplication.getApplicantEmployeeId().equals(applicant.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findById(application.getFormDefinitionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));

        try {
            application.submit();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        List<ApplicationFieldValue> fieldValues =
            fieldValueRepository.findByApplicationIdOrderByDisplayOrderAsc(application.getId());

        return ApplicationDetailResponse.from(application, formDefinition, applicant, fieldValues);
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

    private Employee demoApplicant() {
        return employeeRepository.findByEmployeeCodeAndActiveTrue(DEMO_APPLICANT_EMPLOYEE_CODE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo applicant not found"));
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
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
        List<ApplicationFieldValueResponse> values
    ) {
        static ApplicationDetailResponse from(
            WorkflowApplication application,
            ApplicationFormDefinition formDefinition,
            Employee applicant,
            List<ApplicationFieldValue> fieldValues
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
                fieldValues.stream().map(ApplicationFieldValueResponse::from).toList()
            );
        }
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
}
