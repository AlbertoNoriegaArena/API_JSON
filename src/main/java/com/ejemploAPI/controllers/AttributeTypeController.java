package com.ejemploAPI.controllers;

import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.services.AttributeTypeService;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/attribute-types")
public class AttributeTypeController {

    private static final Logger log = LoggerFactory.getLogger(AttributeTypeController.class);

    private final AttributeTypeService attributeTypeService;
    private final AttributeTypeRepository attributeTypeRepository;

    public AttributeTypeController(AttributeTypeService attributeTypeService, AttributeTypeRepository attributeTypeRepository) {
        this.attributeTypeService = attributeTypeService;
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @GetMapping
    public List<AttributeType> list() {
        log.info("Solicitud recibida: listar todos los AttributeType");
        List<AttributeType> list = attributeTypeRepository.findAll();
        log.info("Cantidad de registros encontrados: {}", list.size());
        return list;
    }

    @PostMapping("/{typeName}/values")
    public ResponseEntity<?> addValues(@PathVariable String typeName, @RequestBody List<String> values) {
        log.info("Solicitud para agregar valores al tipo: {} | Valores: {}", typeName, values);

        try {
            AttributeType at = attributeTypeService.ensureEnumType(typeName);
            attributeTypeService.addValuesToAttributeType(at, values);

            log.info("Valores agregados correctamente al tipo: {}", typeName);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error agregando valores al tipo {}: {}", typeName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al agregar valores");
        }
    }

    @GetMapping("/{typeName}/values")
    public ResponseEntity<?> getValues(@PathVariable String typeName) {
        log.info("Solicitud para obtener valores del tipo: {}", typeName);

        Optional<AttributeType> maybe =
                attributeTypeRepository.findByTypeIgnoreCaseAndIsListAndIsEnum(typeName, false, true);

        if (maybe.isEmpty()) {
            log.warn("Tipo no encontrado para obtener valores: {}", typeName);
            return ResponseEntity.notFound().build();
        }

        log.info("Valores encontrados para tipo {}", typeName);
        return ResponseEntity.ok(attributeTypeService.getAllowedValues(maybe.get()));
    }

    @GetMapping("/by-name/{typeName}")
    public ResponseEntity<AttributeType> getType(@PathVariable String typeName) {
        log.info("Solicitud para obtener AttributeType por nombre: {}", typeName);

        Optional<AttributeType> maybe = attributeTypeRepository.findByTypeIgnoreCase(typeName);

        if (maybe.isEmpty()) {
            log.warn("AttributeType no encontrado por nombre: {}", typeName);
        } else {
            log.info("AttributeType encontrado: {}", maybe.get().getId());
        }

        return maybe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeType> getById(@PathVariable Long id) {
        log.info("Solicitud para obtener AttributeType por ID: {}", id);

        return attributeTypeRepository.findById(id)
                .map(at -> {
                    log.info("AttributeType encontrado: {}", at.getId());
                    return ResponseEntity.ok(at);
                })
                .orElseGet(() -> {
                    log.warn("AttributeType no encontrado con ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public AttributeType create(@RequestBody AttributeType attributeType) {
        log.info("Solicitud para crear AttributeType: {}", attributeType);

        attributeType.setId(null); // asegurar creación
        AttributeType saved = attributeTypeRepository.save(attributeType);

        log.info("AttributeType creado con ID: {}", saved.getId());
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeType> update(@PathVariable Long id, @RequestBody AttributeType payload) {
        log.info("Solicitud para actualizar AttributeType con ID: {}", id);

        return attributeTypeRepository.findById(id)
                .map(existing -> {
                    log.info("AttributeType encontrado. Actualizando... ID: {}", id);

                    existing.setType(payload.getType());
                    existing.setIsEnum(payload.getIsEnum());
                    existing.setIsList(payload.getIsList());

                    AttributeType updated = attributeTypeRepository.save(existing);

                    log.info("AttributeType actualizado correctamente: {}", updated.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> {
                    log.warn("No se encontró AttributeType para actualizar. ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("Solicitud para eliminar AttributeType con ID: {}", id);

        if (!attributeTypeRepository.existsById(id)) {
            log.warn("No se puede eliminar: AttributeType no encontrado. ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Elemento no encontrado");
        }

        try {
            attributeTypeRepository.deleteById(id);
            log.info("AttributeType eliminado correctamente. ID: {}", id);

            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body("Elemento borrado correctamente");

        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al eliminar ID {}: {}", id, e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede borrar el elemento porque tiene elementos asociados");

        } catch (Exception e) {
            log.error("Error inesperado eliminando ID {}: {}", id, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al intentar borrar el elemento");
        }
    }
}
