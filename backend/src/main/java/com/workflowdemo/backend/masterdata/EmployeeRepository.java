package com.workflowdemo.backend.masterdata;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByActiveTrueOrderByEmployeeCodeAsc();
}
