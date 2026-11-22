package com.mckevin.agentcrm.application.port;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.task.FollowUpTask;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    FollowUpTask save(FollowUpTask task);

    Optional<FollowUpTask> findById(long id);

    List<FollowUpTask> findByCustomer(CustomerId customerId);

    List<FollowUpTask> findOpenByAssignee(long assigneeId);

    List<FollowUpTask> findAllOpen();

    List<FollowUpTask> findByAssignee(long assigneeId);
}
