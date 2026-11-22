package com.mckevin.crmsim;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/** Réglages d'injection de pannes, modifiables à chaud pour la démonstration du mode dégradé. */
@Component
public class ChaosSettings {

    public record Chaos(double failureRate, int latencyMs) {

        public Chaos {
            if (failureRate < 0 || failureRate > 1) {
                throw new IllegalArgumentException("failure_rate doit être compris entre 0 et 1");
            }
            if (latencyMs < 0 || latencyMs > 10_000) {
                throw new IllegalArgumentException("latency_ms doit être compris entre 0 et 10000");
            }
        }
    }

    private final AtomicReference<Chaos> current = new AtomicReference<>(new Chaos(0, 0));

    public Chaos get() {
        return current.get();
    }

    public Chaos set(Chaos chaos) {
        current.set(chaos);
        return chaos;
    }
}
