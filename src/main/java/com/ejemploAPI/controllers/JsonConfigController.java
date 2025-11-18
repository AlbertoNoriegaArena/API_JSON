package com.ejemploAPI.controllers;

import com.ejemploAPI.services.JsonConfigService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/json-config")
public class JsonConfigController {

    @Autowired
    private JsonConfigService jsonConfigService;

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
