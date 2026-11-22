package com.mckevin.agentcrm.adapter.in.web;

import com.mckevin.agentcrm.adapter.in.web.ApiDtos.RescheduleRequest;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.TaskDto;
import com.mckevin.agentcrm.application.FollowUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tâches")
public class TaskController {

    private final FollowUpService followUp;
    private final CurrentAgent currentAgent;
    private final Clock clock;

    public TaskController(FollowUpService followUp, CurrentAgent currentAgent, Clock clock) {
        this.followUp = followUp;
        this.currentAgent = currentAgent;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Tâches du conseiller connecté (un manager peut préciser agentId)")
    public List<TaskDto> list(@RequestParam(required = false) Long agentId,
                              @RequestParam(defaultValue = "false") boolean openOnly) {
        LocalDate today = LocalDate.now(clock);
        return followUp.tasksOf(currentAgent.get(), agentId, openOnly).stream()
                .map(t -> TaskDto.of(t.task(), t.customerName(), today)).toList();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Marque la tâche comme faite")
    public TaskDto complete(@PathVariable long id) {
        return TaskDto.of(followUp.completeTask(currentAgent.get(), id), null, LocalDate.now(clock));
    }

    @PutMapping("/{id}/due-date")
    @Operation(summary = "Reporte l'échéance")
    public TaskDto reschedule(@PathVariable long id, @Valid @RequestBody RescheduleRequest request) {
        return TaskDto.of(followUp.rescheduleTask(currentAgent.get(), id, request.dueDate()), null, LocalDate.now(clock));
    }
}
