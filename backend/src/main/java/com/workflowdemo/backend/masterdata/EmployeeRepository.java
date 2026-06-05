package com.workflowdemo.backend.masterdata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByActiveTrueOrderByEmployeeCodeAsc();

    Optional<Employee> findByEmployeeCodeAndActiveTrue(String employeeCode);
}
