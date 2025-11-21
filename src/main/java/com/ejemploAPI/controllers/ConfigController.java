package com.ejemploAPI.controllers;

import com.ejemploAPI.config.exceptions.InvalidEnumValueException;
import com.ejemploAPI.dtos.ConfigDTO;
import com.ejemploAPI.mappers.ConfigMapper;
import com.ejemploAPI.models.Config;
import com.ejemploAPI.services.ConfigService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/import")
    @Operation(summary = "Guardar JSON en base de datos", description = "Un ejemplo sería \n {\n" +
            "  \"nombre\": \"Juan\",\n" +
            "  \"cantidad\": 200,\n" +
            "  \"comprobado\": true,\n" +
            "  \"lista números\": [2, 4, 6, 8],\n" +
            "  \"dirección\": {\n" +
            "    \"provincia\": \"Cantabria\",\n" +
            "    \"ciudad\": \"Santander\",\n" +
            "    \"calle\": \"1 de Mayo\",\n" +
            "    \"PruebaHijo\": {\n" +
            "         \"test1\": 10,\n" +
            "    \"test2\": \"test\",\n" +
            "     \"test3\": {}\n" +
            "     }\n" +
            "  },\n" +
            "  \"color\": \"AMARILLO\",\n" +
            "  \"día\": \"MARTES\",\n" +
            "  \"colores\": [ \"AMARILLO\" , \"ROJO\", \"VERDE\" ],\n" +
            "  \"lista de días\" : [\"lunes\" , \"martes\" , \"jueves\" , \"VIERNES\" ],\n" +
            "  \"lista de colores\": [ \"azul\", \"verde\" ],\n" +
            "   \"prueba\" : 2.3365,\n" +
            "   \"meses\" : [\"enero\" , \"abril\"]\n" +
            "   \n" +
            "}")
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
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Config config = configService.findById(id);
            ConfigDTO dto = ConfigMapper.toDTO(config);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("No se encontró registro con id: " + id);
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Generar un Json con los datos que tenemos en la base de datos")
    public ResponseEntity<String> exportJson() {
        try {
            String rawJson = configService.exportToJson();
            return ResponseEntity.ok(rawJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"error\": \"Error al generar la exportación\"}");
        }
    }
}
