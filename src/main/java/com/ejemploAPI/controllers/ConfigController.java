package com.ejemploAPI.controllers;

import com.ejemploAPI.config.exceptions.InvalidEnumValueException;
import com.ejemploAPI.dtos.ConfigDTO;
import com.ejemploAPI.mappers.ConfigMapper;
import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.Config;
import com.ejemploAPI.repositories.AttributeRepository;
import com.ejemploAPI.services.ConfigService;
import com.ejemploAPI.repositories.ConfigRepository;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;
    private final ConfigRepository configRepository;
    private final AttributeRepository attributeRepository;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    public ConfigController(ConfigService configService, ConfigRepository configRepository, AttributeRepository attributeRepository) {
        this.configService = configService;
        this.configRepository = configRepository;
        this.attributeRepository = attributeRepository;
        this.objectMapper = new ObjectMapper();

    }

    @GetMapping
    public List<ConfigDTO> list() {
        log.info("Solicitud recibida: listar todos los Config");
        return configService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfigDTO> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(configService.getByIdDTO(id));
        } catch (RuntimeException e) {
            log.warn("Config no encontrado con ID {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ConfigDTO> create(@RequestBody ConfigDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(configService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfigDTO> update(@PathVariable Long id, @RequestBody ConfigDTO dto) {
        try {
            return ResponseEntity.ok(configService.update(id, dto));
        } catch (RuntimeException e) {
            log.warn("No se pudo actualizar Config con ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        try {
            configService.delete(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Elemento borrado correctamente");
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar Config con ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
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
