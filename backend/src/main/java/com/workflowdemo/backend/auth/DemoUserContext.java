package com.workflowdemo.backend.auth;

import java.util.Map;

import com.workflowdemo.backend.masterdata.Employee;
import com.workflowdemo.backend.masterdata.EmployeeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DemoUserContext {

    public static final String APPLICANT_USERNAME = "demo1@growtea.co.jp";
    public static final String APPROVER_USERNAME = "demo5@growtea.co.jp";
    public static final String APPLICANT_EMPLOYEE_CODE = "1001";
    public static final String APPROVER_EMPLOYEE_CODE = "1005";

    private static final Map<String, String> EMPLOYEE_CODE_BY_USERNAME = Map.of(
        APPLICANT_USERNAME, APPLICANT_EMPLOYEE_CODE,
        APPROVER_USERNAME, APPROVER_EMPLOYEE_CODE
    );

    private final EmployeeRepository employeeRepository;

    DemoUserContext(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String employeeCode = EMPLOYEE_CODE_BY_USERNAME.get(authentication.getName());
        if (employeeCode == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown demo user");
        }
        return employeeByCode(employeeCode);
    }

    public Employee demoApprover() {
        return employeeByCode(APPROVER_EMPLOYEE_CODE);
    }

    private Employee employeeByCode(String employeeCode) {
        return employeeRepository.findByEmployeeCodeAndActiveTrue(employeeCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo employee not found"));
    }
}
