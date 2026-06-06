package com.workflowdemo.backend.auth;

import com.workflowdemo.backend.masterdata.Employee;
import com.workflowdemo.backend.masterdata.EmployeeRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class AuthController {

    private static final String DEMO_EMPLOYEE_CODE = "1001";

    private final EmployeeRepository employeeRepository;

    AuthController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/me")
    CurrentUserResponse me(Authentication authentication) {
        Employee employee = employeeRepository.findByEmployeeCodeAndActiveTrue(DEMO_EMPLOYEE_CODE)
            .orElseThrow(() -> new IllegalStateException("Demo employee is not seeded"));

        return new CurrentUserResponse(
            authentication.getName(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getOrganizationName(),
            employee.getPositionName()
        );
    }

    record CurrentUserResponse(
        String username,
        String employeeCode,
        String name,
        String organizationName,
        String positionName
    ) {
    }
}
