package com.mckevin.crmsim;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class CrmController {

    private final CrmData data;
    private final ChaosSettings chaos;

    public CrmController(CrmData data, ChaosSettings chaos) {
        this.data = data;
        this.chaos = chaos;
    }

    @GetMapping("/crm/v1/clients")
    public List<CrmData.Client> search(@RequestParam(name = "q", required = false) String query) {
        return data.search(query);
    }

    @GetMapping("/crm/v1/clients/{numClient}")
    public ResponseEntity<CrmData.Client> client(@PathVariable String numClient) {
        return ResponseEntity.of(data.client(numClient));
    }

    @GetMapping("/crm/v1/clients/{numClient}/contrats")
    public List<CrmData.Contrat> contrats(@PathVariable String numClient) {
        return data.contrats(numClient);
    }

    @GetMapping("/crm/v1/clients/{numClient}/sinistres")
    public List<CrmData.Sinistre> sinistres(@PathVariable String numClient) {
        return data.sinistres(numClient);
    }

    @GetMapping("/admin/chaos")
    public ChaosSettings.Chaos chaos() {
        return chaos.get();
    }

    @PutMapping("/admin/chaos")
    public ChaosSettings.Chaos updateChaos(@RequestBody ChaosSettings.Chaos settings) {
        return chaos.set(settings);
    }
}
