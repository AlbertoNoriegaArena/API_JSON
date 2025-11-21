package com.ejemploAPI.controllers;

import com.ejemploAPI.config.exceptions.InvalidEnumValueException;
import com.ejemploAPI.dtos.ConfigDTO;
import com.ejemploAPI.mappers.ConfigMapper;
import com.ejemploAPI.models.Config;
import com.ejemploAPI.services.ConfigService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/json-config")
public class ConfigController {

    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/import")
    public ResponseEntity<String> importJson(@RequestBody Map<String, Object> dataMap) {
        try {
            String json = objectMapper.writeValueAsString(dataMap);
            configService.importJson(json);
            return ResponseEntity.ok("Json importado correctamente");
        } catch (InvalidEnumValueException e) {
            // Retornamos la info de la excepción personalizada
            return ResponseEntity
                    .badRequest()
                    .body("Error al procesar json: " + e.getMessage() +
                            ". Valores permitidos: " + String.join(", ", (Iterable<String>) e.getAllowedValues()));
        } catch (JsonParseException e) {
            String msg = e.getOriginalMessage();
            if (msg != null && msg.contains("Duplicate field")) {
                return ResponseEntity.badRequest().body("JSON inválido: clave duplicada " + msg);
            } else {
                return ResponseEntity.badRequest().body("JSON inválido: error de sintaxis " + msg);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar json: " + e.getMessage());
        }
    }


    // GET BY ID: usamos el DTO para exponer los datos de forma segura
    @GetMapping("/{id}")
    public ResponseEntity<ConfigDTO> getById(@PathVariable Long id) {
        // Buscamos la entidad del modelo en la base de datos
        Optional<Config> configOptional = configService.findById(id);

        // Si existe, la mapeamos a un DTO para la respuesta
        return configOptional
                .map(config -> {
                    ConfigDTO dto = ConfigMapper.toDTO(config);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportJson() {
        try {
            String rawJson = configService.exportToJson();
            return ResponseEntity.ok(rawJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"error\": \"Error al generar la exportación\"}");
        }
    }
}
