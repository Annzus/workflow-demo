package com.workflowdemo.backend.workflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.workflowdemo.backend.formdefinition.WorkflowDefinition;
import com.workflowdemo.backend.formdefinition.WorkflowDefinitionRepository;
import com.workflowdemo.backend.masterdata.EmployeeRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workflow-definitions")
class WorkflowDefinitionController {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final EmployeeRepository employeeRepository;

    WorkflowDefinitionController(
        WorkflowDefinitionRepository workflowDefinitionRepository,
        WorkflowVersionRepository versionRepository,
        WorkflowNodeRepository nodeRepository,
        WorkflowEdgeRepository edgeRepository,
        EmployeeRepository employeeRepository
    ) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.versionRepository = versionRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    List<WorkflowDefinitionSummaryResponse> workflowDefinitions() {
        return workflowDefinitionRepository.findByActiveTrueOrderByWorkflowCodeAsc()
            .stream()
            .flatMap(workflowDefinition -> publishedVersion(workflowDefinition.getId())
                .stream()
                .map(activeVersion -> {
                long nodeCount = nodeRepository.findByWorkflowVersionIdOrderByDisplayOrderAsc(activeVersion.getId())
                    .size();
                return WorkflowDefinitionSummaryResponse.from(workflowDefinition, activeVersion, nodeCount);
                }))
            .toList();
    }

    @GetMapping("/{workflowCode}")
    WorkflowDefinitionDetailResponse workflowDefinition(@PathVariable String workflowCode) {
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository.findByWorkflowCodeAndActiveTrue(workflowCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
        WorkflowVersion activeVersion = activeVersion(workflowDefinition.getId());
        return WorkflowDefinitionDetailResponse.from(
            workflowDefinition,
            activeVersion,
            nodeRepository.findByWorkflowVersionIdOrderByDisplayOrderAsc(activeVersion.getId()),
            edgeRepository.findByWorkflowVersionIdOrderByDisplayOrderAsc(activeVersion.getId())
        );
    }

    @PostMapping("/{workflowCode}/draft")
    @Transactional
    WorkflowDraftResponse saveDraft(
        @PathVariable String workflowCode,
        @Valid @RequestBody SaveWorkflowDraftRequest request
    ) {
        validateWorkflow(request.nodes(), request.edges());
        validateApprovalNodes(request.nodes());
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository
            .findByWorkflowCodeAndActiveTrue(normalizeCode(workflowCode))
            .map(existingWorkflowDefinition -> {
                existingWorkflowDefinition.updateName(trimRequired(request.workflowName(), "workflowName"));
                return existingWorkflowDefinition;
            })
            .orElseGet(() -> new WorkflowDefinition(
                normalizeCode(workflowCode),
                trimRequired(request.workflowName(), "workflowName")
            ));
        WorkflowDefinition savedWorkflowDefinition = workflowDefinitionRepository.save(workflowDefinition);
        int nextVersionNumber = versionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(
                savedWorkflowDefinition.getId()
            )
            .stream()
            .findFirst()
            .map(version -> version.getVersionNumber() + 1)
            .orElse(1);
        WorkflowVersion draftVersion = versionRepository.save(
            new WorkflowVersion(savedWorkflowDefinition.getId(), nextVersionNumber, false)
        );
        List<WorkflowNode> nodes = request.nodes().stream()
            .map(node -> node.toEntity(draftVersion.getId()))
            .toList();
        List<WorkflowEdge> edges = request.edges().stream()
            .map(edge -> edge.toEntity(draftVersion.getId()))
            .toList();
        nodeRepository.saveAll(nodes);
        edgeRepository.saveAll(edges);

        return WorkflowDraftResponse.from(savedWorkflowDefinition, draftVersion, nodes, edges);
    }

    @PostMapping("/{workflowCode}/publish")
    @Transactional
    WorkflowDraftResponse publishLatestDraft(@PathVariable String workflowCode) {
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository
            .findByWorkflowCodeAndActiveTrue(normalizeCode(workflowCode))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
        WorkflowVersion draftVersion = versionRepository
            .findFirstByWorkflowDefinitionIdAndPublishedFalseOrderByVersionNumberDesc(workflowDefinition.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft workflow version not found"));
        draftVersion.publish();
        WorkflowVersion savedVersion = versionRepository.save(draftVersion);
        return WorkflowDraftResponse.from(
            workflowDefinition,
            savedVersion,
            nodeRepository.findByWorkflowVersionIdOrderByDisplayOrderAsc(savedVersion.getId()),
            edgeRepository.findByWorkflowVersionIdOrderByDisplayOrderAsc(savedVersion.getId())
        );
    }

    private WorkflowVersion activeVersion(UUID workflowDefinitionId) {
        return publishedVersion(workflowDefinitionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published workflow version not found"));
    }

    private Optional<WorkflowVersion> publishedVersion(UUID workflowDefinitionId) {
        return versionRepository.findFirstByWorkflowDefinitionIdAndPublishedTrueOrderByVersionNumberDesc(
            workflowDefinitionId
        );
    }

    private static void validateWorkflow(List<SaveWorkflowNodeRequest> nodes, List<SaveWorkflowEdgeRequest> edges) {
        Map<String, Long> countByNodeKey = nodes.stream()
            .collect(Collectors.groupingBy(node -> trimRequired(node.nodeKey(), "nodeKey"), Collectors.counting()));
        boolean hasDuplicateNodeKey = countByNodeKey.values().stream().anyMatch(count -> count > 1);
        if (hasDuplicateNodeKey) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Node key must be unique");
        }
        boolean hasApprovalNode = nodes.stream().anyMatch(node -> "APPROVAL".equals(normalizeCode(node.nodeType())));
        if (!hasApprovalNode) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval node is required");
        }
        Set<String> nodeKeys = countByNodeKey.keySet();
        boolean hasInvalidEdge = edges.stream()
            .anyMatch(edge ->
                !nodeKeys.contains(trimRequired(edge.sourceNodeKey(), "sourceNodeKey"))
                    || !nodeKeys.contains(trimRequired(edge.targetNodeKey(), "targetNodeKey"))
            );
        if (hasInvalidEdge) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow edge references unknown node");
        }
    }

    private void validateApprovalNodes(List<SaveWorkflowNodeRequest> nodes) {
        nodes.stream()
            .filter(node -> "APPROVAL".equals(normalizeCode(node.nodeType())))
            .forEach(this::validateApprovalNode);
    }

    private void validateApprovalNode(SaveWorkflowNodeRequest node) {
        String approverType = normalizeCode(node.approverType());
        if ("FIXED_EMPLOYEE".equals(approverType)) {
            String employeeCode = trimRequired(node.employeeCode(), "employeeCode");
            employeeRepository.findByEmployeeCodeAndActiveTrue(employeeCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow approver not found"));
            return;
        }
        if ("POSITION".equals(approverType)) {
            String positionCode = normalizeCode(node.positionCode());
            boolean hasApprover = employeeRepository.findActiveEmployeesByPositionCode(positionCode)
                .stream()
                .findFirst()
                .isPresent();
            if (!hasApprover) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow approver not found");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval node approver is not configured");
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

    record WorkflowDefinitionSummaryResponse(
        String workflowCode,
        String workflowName,
        int activeVersion,
        long nodeCount
    ) {
        static WorkflowDefinitionSummaryResponse from(
            WorkflowDefinition workflowDefinition,
            WorkflowVersion activeVersion,
            long nodeCount
        ) {
            return new WorkflowDefinitionSummaryResponse(
                workflowDefinition.getWorkflowCode(),
                workflowDefinition.getWorkflowName(),
                activeVersion.getVersionNumber(),
                nodeCount
            );
        }
    }

    record WorkflowDefinitionDetailResponse(
        String workflowCode,
        String workflowName,
        int activeVersion,
        List<WorkflowNodeResponse> nodes,
        List<WorkflowEdgeResponse> edges
    ) {
        static WorkflowDefinitionDetailResponse from(
            WorkflowDefinition workflowDefinition,
            WorkflowVersion activeVersion,
            List<WorkflowNode> nodes,
            List<WorkflowEdge> edges
        ) {
            return new WorkflowDefinitionDetailResponse(
                workflowDefinition.getWorkflowCode(),
                workflowDefinition.getWorkflowName(),
                activeVersion.getVersionNumber(),
                nodes.stream().map(WorkflowNodeResponse::from).toList(),
                edges.stream().map(WorkflowEdgeResponse::from).toList()
            );
        }
    }

    record WorkflowNodeResponse(
        String nodeKey,
        String nodeName,
        String nodeType,
        String approverType,
        String positionCode,
        String employeeCode,
        int displayOrder,
        int xPosition,
        int yPosition
    ) {
        static WorkflowNodeResponse from(WorkflowNode node) {
            return new WorkflowNodeResponse(
                node.getNodeKey(),
                node.getNodeName(),
                node.getNodeType(),
                node.getApproverType(),
                node.getPositionCode(),
                node.getEmployeeCode(),
                node.getDisplayOrder(),
                node.getXPosition(),
                node.getYPosition()
            );
        }
    }

    record WorkflowEdgeResponse(
        String sourceNodeKey,
        String targetNodeKey,
        String conditionExpression,
        int displayOrder
    ) {
        static WorkflowEdgeResponse from(WorkflowEdge edge) {
            return new WorkflowEdgeResponse(
                edge.getSourceNodeKey(),
                edge.getTargetNodeKey(),
                edge.getConditionExpression(),
                edge.getDisplayOrder()
            );
        }
    }

    record WorkflowDraftResponse(
        String workflowCode,
        String workflowName,
        int versionNumber,
        boolean published,
        List<WorkflowNodeResponse> nodes,
        List<WorkflowEdgeResponse> edges
    ) {
        static WorkflowDraftResponse from(
            WorkflowDefinition workflowDefinition,
            WorkflowVersion version,
            List<WorkflowNode> nodes,
            List<WorkflowEdge> edges
        ) {
            return new WorkflowDraftResponse(
                workflowDefinition.getWorkflowCode(),
                workflowDefinition.getWorkflowName(),
                version.getVersionNumber(),
                version.isPublished(),
                nodes.stream().map(WorkflowNodeResponse::from).toList(),
                edges.stream().map(WorkflowEdgeResponse::from).toList()
            );
        }
    }

    record SaveWorkflowDraftRequest(
        @NotBlank String workflowName,
        @NotEmpty List<@Valid SaveWorkflowNodeRequest> nodes,
        @NotEmpty List<@Valid SaveWorkflowEdgeRequest> edges
    ) {
    }

    record SaveWorkflowNodeRequest(
        @NotBlank String nodeKey,
        @NotBlank String nodeName,
        @NotBlank String nodeType,
        String approverType,
        String positionCode,
        String employeeCode,
        int displayOrder,
        int xPosition,
        int yPosition
    ) {
        WorkflowNode toEntity(UUID workflowVersionId) {
            return new WorkflowNode(
                workflowVersionId,
                trimRequired(nodeKey, "nodeKey"),
                trimRequired(nodeName, "nodeName"),
                normalizeCode(nodeType),
                approverType == null || approverType.isBlank() ? null : normalizeCode(approverType),
                positionCode == null || positionCode.isBlank() ? null : normalizeCode(positionCode),
                employeeCode == null || employeeCode.isBlank() ? null : employeeCode.trim(),
                displayOrder,
                xPosition,
                yPosition
            );
        }
    }

    record SaveWorkflowEdgeRequest(
        @NotBlank String sourceNodeKey,
        @NotBlank String targetNodeKey,
        String conditionExpression,
        int displayOrder
    ) {
        WorkflowEdge toEntity(UUID workflowVersionId) {
            return new WorkflowEdge(
                workflowVersionId,
                trimRequired(sourceNodeKey, "sourceNodeKey"),
                trimRequired(targetNodeKey, "targetNodeKey"),
                conditionExpression == null || conditionExpression.isBlank() ? null : conditionExpression.trim(),
                displayOrder
            );
        }
    }
}
