package com.mckevin.agentcrm.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioSpringRepository extends JpaRepository<PortfolioAssignmentEntity, String> {

    List<PortfolioAssignmentEntity> findByAgentIdOrderByCustomerId(long agentId);
}
