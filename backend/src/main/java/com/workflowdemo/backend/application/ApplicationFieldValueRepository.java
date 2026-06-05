package com.workflowdemo.backend.application;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ApplicationFieldValueRepository extends JpaRepository<ApplicationFieldValue, UUID> {

    List<ApplicationFieldValue> findByApplicationIdOrderByDisplayOrderAsc(UUID applicationId);
}
