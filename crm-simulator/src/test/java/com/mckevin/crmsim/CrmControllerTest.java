package com.mckevin.crmsim;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CrmControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ChaosSettings chaos;

    @AfterEach
    void resetChaos() {
        chaos.set(new ChaosSettings.Chaos(0, 0));
    }

    @Test
    void searchesClientsWithCrmVocabulary() throws Exception {
        mvc.perform(get("/crm/v1/clients").param("q", "martin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].num_client").value("C-10234"))
                .andExpect(jsonPath("$[0].client_depuis").value("2014-03-01"));
    }

    @Test
    void contractDatesAreRelativeToToday() throws Exception {
        String expected = LocalDate.now(ZoneId.of("Europe/Paris")).plusDays(12).toString();
        mvc.perform(get("/crm/v1/clients/C-10234/contrats"))
                .andExpect(jsonPath("$[0].produit").value("AUT"))
                .andExpect(jsonPath("$[0].date_echeance").value(expected));
    }

    @Test
    void unknownClientIs404() throws Exception {
        mvc.perform(get("/crm/v1/clients/C-00000")).andExpect(status().isNotFound());
    }

    @Test
    void chaosMakesTheCrmFail() throws Exception {
        mvc.perform(put("/admin/chaos").contentType(MediaType.APPLICATION_JSON).content("{\"failure_rate\":1,\"latency_ms\":0}"))
                .andExpect(status().isOk());
        mvc.perform(get("/crm/v1/clients")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/admin/chaos")).andExpect(jsonPath("$.failure_rate").value(1.0));
    }
}
