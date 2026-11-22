package com.mckevin.crmsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simule le CRM historique d'un assureur : son vocabulaire (num_client, produit « MRH »...) diffère volontairement
 * de celui de l'application cliente, qui doit le traduire (couche anticorruption).
 */
@SpringBootApplication
public class CrmSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmSimulatorApplication.class, args);
    }
}
