package com.mckevin.agentcrm.adapter.in.web;

import com.mckevin.agentcrm.adapter.in.web.ApiDtos.AcceptRequest;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.Customer360Dto;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.DismissRequest;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.InteractionDto;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.InteractionRequest;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.SearchDto;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.TaskDto;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.TaskRequest;
import com.mckevin.agentcrm.application.CustomerService;
import com.mckevin.agentcrm.application.FollowUpService;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.recommendation.RecommendationCode;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Clients", description = "Recherche, vue 360, échanges, tâches et recommandations")
public class CustomerController {

    private final CustomerService customers;
    private final FollowUpService followUp;
    private final CurrentAgent currentAgent;
    private final Clock clock;

    public CustomerController(CustomerService customers, FollowUpService followUp, CurrentAgent currentAgent, Clock clock) {
        this.customers = customers;
        this.followUp = followUp;
        this.currentAgent = currentAgent;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Recherche (CRM, ou copies locales en mode dégradé) limitée au portefeuille du conseiller")
    public SearchDto search(@RequestParam(name = "q", required = false) String query) {
        return SearchDto.of(customers.search(currentAgent.get(), query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Vue client à 360° : contrats, sinistres, échanges, tâches et recommandations")
    public Customer360Dto view(@PathVariable String id) {
        return Customer360Dto.of(customers.view(currentAgent.get(), new CustomerId(id)), LocalDate.now(clock));
    }

    @PostMapping("/{id}/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Trace un échange, avec relance facultative")
    public InteractionDto logInteraction(@PathVariable String id, @Valid @RequestBody InteractionRequest r) {
        return InteractionDto.of(followUp.logInteraction(currentAgent.get(), new CustomerId(id),
                new FollowUpService.LogInteractionCommand(r.channel(), r.direction(), r.subject(), r.notes(), r.occurredAt(),
                        r.followUpOn(), r.followUpTitle())));
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une tâche de suivi")
    public TaskDto createTask(@PathVariable String id, @Valid @RequestBody TaskRequest r) {
        FollowUpTask task = followUp.createTask(currentAgent.get(), new CustomerId(id),
                new FollowUpService.CreateTaskCommand(r.title(), r.notes(), r.priority(), r.dueDate(), r.assigneeId()));
        return TaskDto.of(task, null, LocalDate.now(clock));
    }

    @PostMapping("/{id}/recommendations/{code}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Accepte une recommandation : elle devient une tâche")
    public TaskDto accept(@PathVariable String id, @PathVariable RecommendationCode code,
                          @RequestBody(required = false) AcceptRequest r) {
        FollowUpTask task = followUp.accept(currentAgent.get(), new CustomerId(id), code,
                r == null ? "" : r.reference(), r == null ? null : r.dueDate());
        return TaskDto.of(task, null, LocalDate.now(clock));
    }

    @PostMapping("/{id}/recommendations/{code}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Écarte une recommandation pendant 90 jours, avec un motif")
    public void dismiss(@PathVariable String id, @PathVariable RecommendationCode code,
                        @Valid @RequestBody DismissRequest r) {
        followUp.dismiss(currentAgent.get(), new CustomerId(id), code, r.reference(), r.reason());
    }
}
