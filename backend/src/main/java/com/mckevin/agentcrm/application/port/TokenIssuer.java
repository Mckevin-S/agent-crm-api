package com.mckevin.agentcrm.application.port;

import com.mckevin.agentcrm.application.Agent;

import java.time.Instant;

public interface TokenIssuer {

    record IssuedToken(String value, Instant expiresAt) {
    }

    IssuedToken issue(Agent agent, String email);
}
