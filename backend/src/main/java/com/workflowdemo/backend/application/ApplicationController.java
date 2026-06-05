package com.workflowdemo.backend.application;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DraftApplicationResponse createDraft(@Valid @RequestBody CreateDraftApplicationRequest request) {
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findByFormCodeAndActiveTrue(request.formCode())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));
        Employee applicant = employeeRepository.findByEmployeeCodeAndActiveTrue(DEMO_APPLICANT_EMPLOYEE_CODE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo applicant not found"));
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
            application.getApplicationNumber(),
            application.getTitle(),
            application.getStatus(),
            applicant.getName(),
            formDefinition.getFormName(),
            fieldValues.size()
        );
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
        String applicationNumber,
        String title,
        String status,
        String applicantName,
        String formName,
        int fieldValueCount
    ) {
    }
}
