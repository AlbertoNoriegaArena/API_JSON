package com.ejemploAPI.controllers;

import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.repositories.AttributeRepository;
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
@RequestMapping("/attributes")
public class AttributeController {

    private static final Logger log = LoggerFactory.getLogger(AttributeController.class);

    private final AttributeRepository attributeRepository;
    private final AttributeTypeRepository attributeTypeRepository;

    public AttributeController(AttributeRepository attributeRepository, AttributeTypeRepository attributeTypeRepository) {
        this.attributeRepository = attributeRepository;
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @GetMapping
    public List<Attribute> list() {
        log.info("Solicitud recibida: listar todos los atributos");
        List<Attribute> list = attributeRepository.findAll();
        log.info("Cantidad de atributos encontrados: {}", list.size());
        return list;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Attribute> get(@PathVariable Long id) {
        log.info("Solicitud recibida: obtener atributo con ID {}", id);

        return attributeRepository.findById(id)
                .map(attribute -> {
                    log.info("Atributo encontrado: ID {}", attribute.getId());
                    return ResponseEntity.ok(attribute);
                })
                .orElseGet(() -> {
                    log.warn("Atributo no encontrado con ID {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Attribute payload) {
        log.info("Solicitud recibida: crear atributo con nombre '{}'", payload.getName());

        // Validar si ya existe
        if (payload.getName() != null && attributeRepository.findByName(payload.getName()).isPresent()) {
            log.warn("Conflicto: ya existe un atributo con el nombre '{}'", payload.getName());
            return ResponseEntity.status(409)
                    .body("Ya existe un atributo con el nombre '" + payload.getName() + "'.");
        }

        // Procesar AttributeType si viene en el payload
        if (payload.getAttributeType() != null && payload.getAttributeType().getId() != null) {
            Long typeId = payload.getAttributeType().getId();
            log.info("Validando existencia de AttributeType con ID {}", typeId);

            Optional<AttributeType> at = attributeTypeRepository.findById(typeId);
            if (at.isEmpty()) {
                log.warn("No existe AttributeType con ID {}", typeId);
                return ResponseEntity.badRequest()
                        .body("El AttributeType con id " + typeId + " no existe.");
            }
            payload.setAttributeType(at.get());
        }

        Attribute saved = attributeRepository.save(payload);
        log.info("Atributo creado correctamente con ID {}", saved.getId());

        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Attribute> update(@PathVariable Long id, @RequestBody Attribute payload) {
        log.info("Solicitud recibida: actualizar atributo con ID {}", id);

        Optional<Attribute> maybe = attributeRepository.findById(id);

        if (maybe.isEmpty()) {
            log.warn("No se encontró atributo para actualizar. ID {}", id);
            return ResponseEntity.notFound().build();
        }

        Attribute a = maybe.get();
        a.setName(payload.getName());
        log.info("Actualizando nombre del atributo ID {} a '{}'", id, payload.getName());

        if (payload.getAttributeType() != null && payload.getAttributeType().getId() != null) {
            Long typeId = payload.getAttributeType().getId();
            log.info("Validando AttributeType para actualización. ID {}", typeId);

            attributeTypeRepository.findById(typeId).ifPresentOrElse(
                    a::setAttributeType,
                    () -> log.warn("No existe AttributeType con ID {}. No se actualiza este campo.", typeId)
            );
        }

        attributeRepository.save(a);
        log.info("Atributo actualizado correctamente. ID {}", id);

        return ResponseEntity.ok(a);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("Solicitud recibida: eliminar atributo con ID {}", id);

        if (!attributeRepository.existsById(id)) {
            log.warn("Intento de eliminar atributo inexistente. ID {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Elemento no encontrado");
        }

        try {
            attributeRepository.deleteById(id);
            log.info("Atributo eliminado correctamente. ID {}", id);

            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body("Elemento borrado correctamente");

        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al eliminar atributo ID {}: {}", id, e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede borrar el elemento porque tiene elementos asociados");

        } catch (Exception e) {
            log.error("Error inesperado al eliminar atributo ID {}: {}", id, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al intentar borrar el elemento");
        }
    }
}
