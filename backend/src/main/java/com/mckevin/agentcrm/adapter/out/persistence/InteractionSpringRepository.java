package com.mckevin.agentcrm.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface InteractionSpringRepository extends JpaRepository<InteractionEntity, Long> {

    List<InteractionEntity> findByCustomerIdOrderByOccurredAtDesc(String customerId);

    @Query("select max(i.occurredAt) from InteractionEntity i where i.customerId = :customerId")
    Instant lastOccurrence(@Param("customerId") String customerId);

    @Query("select i.customerId, max(i.occurredAt) from InteractionEntity i where i.customerId in :ids group by i.customerId")
    List<Object[]> lastOccurrences(@Param("ids") Collection<String> ids);

    long countByAgentIdAndOccurredAtAfter(long agentId, Instant since);
}
