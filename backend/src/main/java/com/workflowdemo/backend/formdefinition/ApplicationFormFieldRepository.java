package com.workflowdemo.backend.formdefinition;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationFormFieldRepository extends JpaRepository<ApplicationFormField, UUID> {

    List<ApplicationFormField> findByFormDefinitionIdAndActiveTrueOrderByDisplayOrderAsc(UUID formDefinitionId);

    @Query("""
        select f.formDefinitionId, count(f)
        from ApplicationFormField f
        where f.active = true and f.formDefinitionId in :formDefinitionIds
        group by f.formDefinitionId
        """)
    List<Object[]> countActiveFieldsByFormDefinitionIds(List<UUID> formDefinitionIds);
}
