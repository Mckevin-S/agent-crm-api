package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.application.port.AgentAccounts;
import com.mckevin.agentcrm.application.port.PasswordVerifier;
import com.mckevin.agentcrm.application.port.TokenIssuer;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class AuthenticationService {

    public record Session(String token, Instant expiresAt, Agent agent, String email) {
    }

    private final AgentAccounts accounts;
    private final PasswordVerifier passwords;
    private final TokenIssuer tokens;

    public AuthenticationService(AgentAccounts accounts, PasswordVerifier passwords, TokenIssuer tokens) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public Session login(String email, String password) {
        AgentAccounts.Credentials credentials = accounts.findByEmail(email == null ? "" : email.trim())
                .filter(c -> passwords.matches(password, c.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        Agent agent = new Agent(credentials.id(), credentials.fullName(), credentials.manager());
        TokenIssuer.IssuedToken token = tokens.issue(agent, credentials.email());
        return new Session(token.value(), token.expiresAt(), agent, credentials.email());
    }
}
