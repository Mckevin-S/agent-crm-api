package com.mckevin.agentcrm.application.port;

public interface PasswordVerifier {

    boolean matches(String rawPassword, String hash);
}
