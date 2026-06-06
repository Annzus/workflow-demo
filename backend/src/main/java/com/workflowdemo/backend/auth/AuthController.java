package com.workflowdemo.backend.auth;

import com.workflowdemo.backend.masterdata.Employee;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class AuthController {

    private final DemoUserContext demoUserContext;

    AuthController(DemoUserContext demoUserContext) {
        this.demoUserContext = demoUserContext;
    }

    @GetMapping("/me")
    CurrentUserResponse me(Authentication authentication) {
        Employee employee = demoUserContext.currentEmployee();

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
