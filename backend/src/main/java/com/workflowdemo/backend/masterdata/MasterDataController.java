package com.workflowdemo.backend.masterdata;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data")
class MasterDataController {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final PositionRepository positionRepository;

    MasterDataController(
        EmployeeRepository employeeRepository,
        OrganizationRepository organizationRepository,
        PositionRepository positionRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.organizationRepository = organizationRepository;
        this.positionRepository = positionRepository;
    }

    @GetMapping("/employees")
    List<EmployeeResponse> employees() {
        return employeeRepository.findByActiveTrueOrderByEmployeeCodeAsc()
            .stream()
            .map(EmployeeResponse::from)
            .toList();
    }

    @GetMapping("/organizations")
    List<OrganizationResponse> organizations() {
        return organizationRepository.findByActiveTrueOrderByOrganizationCodeAsc()
            .stream()
            .map(OrganizationResponse::from)
            .toList();
    }

    @GetMapping("/positions")
    List<PositionResponse> positions() {
        return positionRepository.findByActiveTrueOrderByApprovalRankAsc()
            .stream()
            .map(PositionResponse::from)
            .toList();
    }

    record EmployeeResponse(
        String employeeCode,
        String name,
        String organizationName,
        String positionName
    ) {
        static EmployeeResponse from(Employee employee) {
            return new EmployeeResponse(
                employee.getEmployeeCode(),
                employee.getName(),
                employee.getOrganizationName(),
                employee.getPositionName()
            );
        }
    }

    record OrganizationResponse(
        String organizationCode,
        String name,
        String parentOrganizationCode,
        LocalDate validFrom,
        LocalDate validTo
    ) {
        static OrganizationResponse from(Organization organization) {
            return new OrganizationResponse(
                organization.getOrganizationCode(),
                organization.getName(),
                organization.getParentOrganizationCode(),
                organization.getValidFrom(),
                organization.getValidTo()
            );
        }
    }

    record PositionResponse(
        String positionCode,
        String name,
        int approvalRank
    ) {
        static PositionResponse from(Position position) {
            return new PositionResponse(
                position.getPositionCode(),
                position.getName(),
                position.getApprovalRank()
            );
        }
    }
}
