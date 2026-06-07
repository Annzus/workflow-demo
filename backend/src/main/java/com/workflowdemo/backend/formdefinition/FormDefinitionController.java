package com.workflowdemo.backend.formdefinition;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/form-definitions")
class FormDefinitionController {

    private final ApplicationFormDefinitionRepository formDefinitionRepository;
    private final ApplicationFormFieldRepository formFieldRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    FormDefinitionController(
        ApplicationFormDefinitionRepository formDefinitionRepository,
        ApplicationFormFieldRepository formFieldRepository,
        WorkflowDefinitionRepository workflowDefinitionRepository
    ) {
        this.formDefinitionRepository = formDefinitionRepository;
        this.formFieldRepository = formFieldRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
    }

    @GetMapping
    List<FormDefinitionSummaryResponse> formDefinitions() {
        List<ApplicationFormDefinition> formDefinitions =
            formDefinitionRepository.findByActiveTrueOrderByFormCodeAsc();
        Map<UUID, WorkflowDefinition> workflowsById = workflowDefinitionRepository.findAllById(
                formDefinitions.stream()
                    .map(ApplicationFormDefinition::getWorkflowDefinitionId)
                    .toList()
            )
            .stream()
            .collect(Collectors.toMap(WorkflowDefinition::getId, Function.identity()));
        Map<UUID, Long> fieldCountsByFormDefinitionId = formFieldRepository.countActiveFieldsByFormDefinitionIds(
                formDefinitions.stream()
                    .map(ApplicationFormDefinition::getId)
                    .toList()
            )
            .stream()
            .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long) row[1]
            ));

        return formDefinitions.stream()
            .map(formDefinition -> FormDefinitionSummaryResponse.from(
                formDefinition,
                workflowsById.get(formDefinition.getWorkflowDefinitionId()),
                fieldCountsByFormDefinitionId.getOrDefault(formDefinition.getId(), 0L)
            ))
            .toList();
    }

    @GetMapping("/{formCode}")
    FormDefinitionDetailResponse formDefinition(@PathVariable String formCode) {
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findByFormCodeAndActiveTrue(formCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form definition not found"));
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository.findById(formDefinition.getWorkflowDefinitionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
        List<ApplicationFormField> fields =
            formFieldRepository.findByFormDefinitionIdAndActiveTrueOrderByDisplayOrderAsc(formDefinition.getId());

        return FormDefinitionDetailResponse.from(formDefinition, workflowDefinition, fields);
    }

    @PostMapping
    @Transactional
    FormDefinitionDetailResponse saveFormDefinition(@Valid @RequestBody SaveFormDefinitionRequest request) {
        validateFields(request.fields());
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository
            .findByWorkflowCodeAndActiveTrue(normalizeCode(request.workflowCode()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
        String formCode = normalizeCode(request.formCode());
        ApplicationFormDefinition formDefinition = formDefinitionRepository.findByFormCodeAndActiveTrue(formCode)
            .map(existingFormDefinition -> {
                existingFormDefinition.update(trimRequired(request.formName(), "formName"), workflowDefinition.getId());
                return existingFormDefinition;
            })
            .orElseGet(() -> new ApplicationFormDefinition(
                formCode,
                trimRequired(request.formName(), "formName"),
                workflowDefinition.getId()
            ));
        ApplicationFormDefinition savedFormDefinition = formDefinitionRepository.save(formDefinition);

        formFieldRepository.deleteAll(
            formFieldRepository.findByFormDefinitionIdOrderByDisplayOrderAsc(savedFormDefinition.getId())
        );
        formFieldRepository.flush();
        List<ApplicationFormField> fields = request.fields().stream()
            .map(field -> field.toEntity(savedFormDefinition.getId()))
            .toList();
        formFieldRepository.saveAll(fields);

        return FormDefinitionDetailResponse.from(savedFormDefinition, workflowDefinition, fields);
    }

    private static void validateFields(List<SaveFormFieldRequest> fields) {
        Map<String, Long> countByFieldKey = fields.stream()
            .collect(Collectors.groupingBy(field -> trimRequired(field.fieldKey(), "fieldKey"), Collectors.counting()));
        boolean hasDuplicateFieldKey = countByFieldKey.values().stream().anyMatch(count -> count > 1);
        if (hasDuplicateFieldKey) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field key must be unique");
        }
    }

    private static String normalizeCode(String value) {
        return trimRequired(value, "code").toUpperCase();
    }

    private static String trimRequired(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return trimmed;
    }

    record FormDefinitionSummaryResponse(
        String formCode,
        String formName,
        String workflowCode,
        String workflowName,
        long fieldCount
    ) {
        static FormDefinitionSummaryResponse from(
            ApplicationFormDefinition formDefinition,
            WorkflowDefinition workflowDefinition,
            long fieldCount
        ) {
            return new FormDefinitionSummaryResponse(
                formDefinition.getFormCode(),
                formDefinition.getFormName(),
                workflowDefinition == null ? "" : workflowDefinition.getWorkflowCode(),
                workflowDefinition == null ? "" : workflowDefinition.getWorkflowName(),
                fieldCount
            );
        }
    }

    record FormDefinitionDetailResponse(
        String formCode,
        String formName,
        String workflowCode,
        String workflowName,
        List<FormFieldResponse> fields
    ) {
        static FormDefinitionDetailResponse from(
            ApplicationFormDefinition formDefinition,
            WorkflowDefinition workflowDefinition,
            List<ApplicationFormField> fields
        ) {
            return new FormDefinitionDetailResponse(
                formDefinition.getFormCode(),
                formDefinition.getFormName(),
                workflowDefinition.getWorkflowCode(),
                workflowDefinition.getWorkflowName(),
                fields.stream().map(FormFieldResponse::from).toList()
            );
        }
    }

    record FormFieldResponse(
        String fieldKey,
        String label,
        String dataType,
        boolean required,
        String placeholder,
        String initialValueType,
        int displayOrder
    ) {
        static FormFieldResponse from(ApplicationFormField field) {
            return new FormFieldResponse(
                field.getFieldKey(),
                field.getLabel(),
                field.getDataType(),
                field.isRequired(),
                field.getPlaceholder(),
                field.getInitialValueType(),
                field.getDisplayOrder()
            );
        }
    }

    record SaveFormDefinitionRequest(
        @NotBlank String formCode,
        @NotBlank String formName,
        @NotBlank String workflowCode,
        @NotEmpty List<@Valid SaveFormFieldRequest> fields
    ) {
    }

    record SaveFormFieldRequest(
        @NotBlank String fieldKey,
        @NotBlank String label,
        @NotBlank String dataType,
        boolean required,
        String placeholder,
        String initialValueType,
        int displayOrder
    ) {
        ApplicationFormField toEntity(UUID formDefinitionId) {
            return new ApplicationFormField(
                formDefinitionId,
                trimRequired(fieldKey, "fieldKey"),
                trimRequired(label, "label"),
                normalizeCode(dataType),
                required,
                placeholder == null || placeholder.isBlank() ? null : placeholder.trim(),
                initialValueType == null || initialValueType.isBlank() ? "MANUAL" : normalizeCode(initialValueType),
                displayOrder
            );
        }
    }
}
