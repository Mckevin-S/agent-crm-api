package com.mckevin.crmsim;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/** Ajoute de la latence et des erreurs 503 aléatoires sur l'API du CRM, selon {@link ChaosSettings}. */
@Component
public class ChaosFilter extends OncePerRequestFilter {

    private final ChaosSettings settings;

    public ChaosFilter(ChaosSettings settings) {
        this.settings = settings;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/crm/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ChaosSettings.Chaos chaos = settings.get();
        if (chaos.latencyMs() > 0) {
            try {
                Thread.sleep(chaos.latencyMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (chaos.failureRate() > 0 && ThreadLocalRandom.current().nextDouble() < chaos.failureRate()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write("{\"erreur\":\"CRM indisponible (panne simulée)\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
