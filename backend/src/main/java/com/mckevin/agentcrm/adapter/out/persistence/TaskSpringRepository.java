package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.domain.task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskSpringRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByCustomerId(String customerId);

    List<TaskEntity> findByAssigneeIdAndStatus(long assigneeId, TaskStatus status);

    List<TaskEntity> findByAssigneeId(long assigneeId);

    List<TaskEntity> findByStatus(TaskStatus status);
}
