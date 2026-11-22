package com.mckevin.agentcrm.adapter.in.web;

import com.mckevin.agentcrm.adapter.in.web.ApiDtos.AgentDto;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.LoginRequest;
import com.mckevin.agentcrm.adapter.in.web.ApiDtos.SessionDto;
import com.mckevin.agentcrm.application.Agent;
import com.mckevin.agentcrm.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthenticationService authentication;
    private final CurrentAgent currentAgent;

    public AuthController(AuthenticationService authentication, CurrentAgent currentAgent) {
        this.authentication = authentication;
        this.currentAgent = currentAgent;
    }

    @PostMapping("/login")
    @Operation(summary = "Échange e-mail et mot de passe contre un jeton d'accès")
    public SessionDto login(@Valid @RequestBody LoginRequest request) {
        AuthenticationService.Session session = authentication.login(request.email(), request.password());
        Agent a = session.agent();
        return new SessionDto(session.token(), session.expiresAt(),
                new AgentDto(a.id(), a.fullName(), session.email(), a.manager(), ApiDtos.initials(a.fullName())));
    }

    @GetMapping("/me")
    @Operation(summary = "Utilisateur connecté")
    public AgentDto me() {
        Agent a = currentAgent.get();
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new AgentDto(a.id(), a.fullName(), jwt.getClaimAsString("email"), a.manager(), ApiDtos.initials(a.fullName()));
    }
}
