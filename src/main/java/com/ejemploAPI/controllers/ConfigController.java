package com.ejemploAPI.controllers;

import com.ejemploAPI.config.exceptions.DuplicateKeyException;
import com.ejemploAPI.config.exceptions.InvalidEnumValueException;
import com.ejemploAPI.dtos.ConfigDTO;
import com.ejemploAPI.services.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public List<ConfigDTO> list() {
        log.info("Listando todos los Config");
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
    public ResponseEntity<String> importJson(@RequestBody byte[] rawJsonBytes) {
        //Pasamos un array de bytes en lugar de una Map para evitar que Spring parsee el Json y así lograr que lance Exception por clave duplicada
        try {
            // Convertir los bytes en un String
            String rawJson = new String(rawJsonBytes);
            log.info("Inicio importación JSON");
            configService.importJson(rawJson);
            return ResponseEntity.ok("JSON importado correctamente");
        } catch (DuplicateKeyException e) {
            log.warn("JSON inválido: clave duplicada detectada. Detalle: {}", e.getMessage());
            return ResponseEntity.badRequest().body("JSON inválido: clave duplicada detectada");
        } catch (InvalidEnumValueException e) {
            log.warn("Error al procesar JSON: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error al procesar JSON: " + e.getMessage() + "valores permitidos: " + e.getAllowedValues());
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Generar un Json con los datos que tenemos en la base de datos")
    public ResponseEntity<String> exportJson() {
        try {
            String json = configService.exportToJson();
            return ResponseEntity.ok(json);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error al generar la exportación\"}");
        }
    }
}
