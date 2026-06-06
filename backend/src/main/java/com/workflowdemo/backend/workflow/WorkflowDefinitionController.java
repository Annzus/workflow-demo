package com.workflowdemo.backend.workflow;

import java.util.List;
import java.util.UUID;

import com.workflowdemo.backend.formdefinition.WorkflowDefinition;
import com.workflowdemo.backend.formdefinition.WorkflowDefinitionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    WorkflowDefinitionController(
        WorkflowDefinitionRepository workflowDefinitionRepository,
        WorkflowVersionRepository versionRepository,
        WorkflowNodeRepository nodeRepository,
        WorkflowEdgeRepository edgeRepository
    ) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.versionRepository = versionRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @GetMapping
    List<WorkflowDefinitionSummaryResponse> workflowDefinitions() {
        return workflowDefinitionRepository.findByActiveTrueOrderByWorkflowCodeAsc()
            .stream()
            .map(workflowDefinition -> {
                WorkflowVersion activeVersion = activeVersion(workflowDefinition.getId());
                long nodeCount = nodeRepository.findByWorkflowVersionIdOrderByDisplayOrderAsc(activeVersion.getId())
                    .size();
                return WorkflowDefinitionSummaryResponse.from(workflowDefinition, activeVersion, nodeCount);
            })
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

    private WorkflowVersion activeVersion(UUID workflowDefinitionId) {
        return versionRepository.findFirstByWorkflowDefinitionIdAndPublishedTrueOrderByVersionNumberDesc(
                workflowDefinitionId
            )
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published workflow version not found"));
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
}
