package com.ejemploAPI.controllers;

import com.ejemploAPI.services.JsonConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/json-config")
public class JsonConfigController {

    private final JsonConfigService jsonConfigService;

    public JsonConfigController(JsonConfigService jsonConfigService) {
        this.jsonConfigService = jsonConfigService;
    }

    @PostMapping("/import")
    public ResponseEntity<String> importJson(@RequestBody Map<String, Object> jsonData) {
        try {
            jsonConfigService.importJson(jsonData);
            return ResponseEntity.ok("JSON importado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al importar: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportJson() {
        try {
            String json = jsonConfigService.exportToJson();
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al exportar: " + e.getMessage());
        }
    }
}