package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import com.mckevin.agentcrm.domain.task.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class TaskAdapter implements TaskRepository {

    private final TaskSpringRepository repository;

    TaskAdapter(TaskSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public FollowUpTask save(FollowUpTask task) {
        return repository.save(TaskEntity.from(task)).toDomain();
    }

    @Override
    public Optional<FollowUpTask> findById(long id) {
        return repository.findById(id).map(TaskEntity::toDomain);
    }

    @Override
    public List<FollowUpTask> findByCustomer(CustomerId customerId) {
        return repository.findByCustomerId(customerId.value()).stream().map(TaskEntity::toDomain).toList();
    }

    @Override
    public List<FollowUpTask> findOpenByAssignee(long assigneeId) {
        return repository.findByAssigneeIdAndStatus(assigneeId, TaskStatus.OPEN).stream().map(TaskEntity::toDomain).toList();
    }

    @Override
    public List<FollowUpTask> findAllOpen() {
        return repository.findByStatus(TaskStatus.OPEN).stream().map(TaskEntity::toDomain).toList();
    }

    @Override
    public List<FollowUpTask> findByAssignee(long assigneeId) {
        return repository.findByAssigneeId(assigneeId).stream().map(TaskEntity::toDomain).toList();
    }
}
