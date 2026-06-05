package com.workflowdemo.backend.formdefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationFormDefinitionRepository extends JpaRepository<ApplicationFormDefinition, UUID> {

    List<ApplicationFormDefinition> findByActiveTrueOrderByFormCodeAsc();

    Optional<ApplicationFormDefinition> findByFormCodeAndActiveTrue(String formCode);
}
