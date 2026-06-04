package com.workflowdemo.backend.masterdata;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    List<Organization> findByActiveTrueOrderByOrganizationCodeAsc();
}
