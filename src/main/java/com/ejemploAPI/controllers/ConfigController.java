package com.ejemploAPI.controllers;

import com.ejemploAPI.services.ConfigService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/json-config")
public class ConfigController {

    private ConfigService jsonConfigService;

    // Inyección por constructor
    public ConfigController(ConfigService jsonConfigService) {
        this.jsonConfigService = jsonConfigService;
    }

    @PostMapping("/import")
    public ResponseEntity<String> importJson(@RequestBody String rawJson) {
        jsonConfigService.importJson(rawJson);
        return ResponseEntity.ok("JSON importado correctamente");
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportJson() {
        return ResponseEntity.ok(jsonConfigService.exportToJson());
    }
}
