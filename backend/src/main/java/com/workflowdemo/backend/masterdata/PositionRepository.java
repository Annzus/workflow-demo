package com.workflowdemo.backend.masterdata;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PositionRepository extends JpaRepository<Position, UUID> {

    List<Position> findByActiveTrueOrderByApprovalRankAsc();
}
