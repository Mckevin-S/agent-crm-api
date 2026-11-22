package com.mckevin.agentcrm.adapter.out.crm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CrmProperties.class)
class CrmClientConfig {

    /** Délais courts : mieux vaut basculer vite en mode dégradé que bloquer le conseiller. */
    @Bean
    RestClient crmRestClient(RestClient.Builder builder, CrmProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return builder.baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }
}
