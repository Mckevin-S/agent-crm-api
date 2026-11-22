package com.mckevin.agentcrm.adapter.out.crm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.crm")
public record CrmProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {

    public CrmProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(1) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(2) : readTimeout;
    }
}
