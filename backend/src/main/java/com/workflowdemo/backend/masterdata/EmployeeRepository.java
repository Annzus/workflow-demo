package com.workflowdemo.backend.masterdata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByActiveTrueOrderByEmployeeCodeAsc();

    Optional<Employee> findByEmployeeCodeAndActiveTrue(String employeeCode);

    @Query("""
        select e
        from Employee e, Position p
        where e.active = true
            and p.active = true
            and p.positionCode = :positionCode
            and e.positionName = p.name
        order by e.employeeCode asc
        """)
    List<Employee> findActiveEmployeesByPositionCode(@Param("positionCode") String positionCode);
}
