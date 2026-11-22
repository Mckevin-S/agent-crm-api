package com.mckevin.agentcrm.adapter.in.web;

import com.mckevin.agentcrm.adapter.in.web.ApiDtos.CrmStateDto;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.DashboardDto;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.OutageRequest;
import com.mckevin.agentcrm.application.CrmService;
import com.mckevin.agentcrm.application.DashboardService;
import com.mckevin.agentcrm.application.Views.TeamMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Pilotage", description = "Tableau de bord, équipe et état du CRM")
public class DashboardController {

    private final DashboardService dashboards;
    private final CrmService crm;
    private final CurrentAgent currentAgent;
    private final Clock clock;

    public DashboardController(DashboardService dashboards, CrmService crm, CurrentAgent currentAgent, Clock clock) {
        this.dashboards = dashboards;
        this.crm = crm;
        this.currentAgent = currentAgent;
        this.clock = clock;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Tableau de bord : indicateurs du portefeuille, recommandations prioritaires, tâches à venir")
    public DashboardDto dashboard() {
        return DashboardDto.of(dashboards.dashboard(currentAgent.get()), LocalDate.now(clock));
    }

    @GetMapping("/team")
    @Operation(summary = "Vue équipe (managers)")
    public List<TeamMember> team() {
        return dashboards.team(currentAgent.get());
    }

    @GetMapping("/crm/status")
    @Operation(summary = "État de la connexion au CRM (disjoncteur)")
    public CrmStateDto crmStatus() {
        return CrmStateDto.of(crm.state());
    }

    @PutMapping("/crm/outage")
    @Operation(summary = "Démonstration : simule une panne du CRM (managers)")
    public CrmStateDto simulateOutage(@RequestBody OutageRequest request) {
        return CrmStateDto.of(crm.simulateOutage(currentAgent.get(), request.outage()));
    }
}
