package com.workflowdemo.backend.workflow;

import java.util.List;
import java.util.UUID;

import com.workflowdemo.backend.formdefinition.ApplicationFormDefinition;
import com.workflowdemo.backend.masterdata.Employee;
import com.workflowdemo.backend.masterdata.EmployeeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowRouteService {

    private final WorkflowVersionRepository versionRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final EmployeeRepository employeeRepository;

    WorkflowRouteService(
        WorkflowVersionRepository versionRepository,
        WorkflowNodeRepository nodeRepository,
        EmployeeRepository employeeRepository
    ) {
        this.versionRepository = versionRepository;
        this.nodeRepository = nodeRepository;
        this.employeeRepository = employeeRepository;
    }

    public WorkflowRoute routeForForm(ApplicationFormDefinition formDefinition) {
        WorkflowVersion version = versionRepository
            .findFirstByWorkflowDefinitionIdAndPublishedTrueOrderByVersionNumberDesc(
                formDefinition.getWorkflowDefinitionId()
            )
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published workflow version not found"));
        return routeForVersion(version);
    }

    public WorkflowRoute routeForApplication(ApplicationFormDefinition formDefinition, UUID workflowVersionId) {
        if (workflowVersionId == null) {
            return routeForForm(formDefinition);
        }
        WorkflowVersion version = versionRepository.findById(workflowVersionId)
            .filter(foundVersion -> foundVersion.getWorkflowDefinitionId().equals(formDefinition.getWorkflowDefinitionId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow version not found"));
        return routeForVersion(version);
    }

    private WorkflowRoute routeForVersion(WorkflowVersion version) {
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowVersionIdOrderByDisplayOrderAsc(version.getId());
        WorkflowNode approvalNode = nodes.stream()
            .filter(node -> "APPROVAL".equals(node.getNodeType()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval node is not configured"));
        Employee approver = resolveApprover(approvalNode);
        return new WorkflowRoute(version, nodes, approvalNode, approver);
    }

    private Employee resolveApprover(WorkflowNode approvalNode) {
        if ("FIXED_EMPLOYEE".equals(approvalNode.getApproverType()) && approvalNode.getEmployeeCode() != null) {
            return employeeRepository.findByEmployeeCodeAndActiveTrue(approvalNode.getEmployeeCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow approver not found"));
        }
        if ("POSITION".equals(approvalNode.getApproverType()) && approvalNode.getPositionCode() != null) {
            return employeeRepository.findActiveEmployeesByPositionCode(approvalNode.getPositionCode())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow approver not found"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval node approver is not configured");
    }

    public record WorkflowRoute(
        WorkflowVersion version,
        List<WorkflowNode> nodes,
        WorkflowNode approvalNode,
        Employee approver
    ) {
        public UUID versionId() {
            return version.getId();
        }
    }
}
