package com.ejemploAPI.controllers;

import com.ejemploAPI.services.JsonConfigService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/json-config")
public class JsonConfigController {

    private JsonConfigService jsonConfigService;

    // Inyección por constructor
    public JsonConfigController(JsonConfigService jsonConfigService) {
        this.jsonConfigService = jsonConfigService;
    }


    @PostMapping("/import")
    public ResponseEntity<String> importJson(@RequestBody Map<String, Object> jsonData) {
        jsonConfigService.importJson(jsonData);
        return ResponseEntity.ok("JSON importado correctamente");
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportJson() {
        return ResponseEntity.ok(jsonConfigService.exportToJson());
    }
}
