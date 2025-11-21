package com.ejemploAPI.controllers;

import com.ejemploAPI.dtos.AttributeDTO;
import com.ejemploAPI.mappers.AttributeMapper;
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
@RequestMapping("/api/attributes")
public class AttributeController {

    private static final Logger log = LoggerFactory.getLogger(AttributeController.class);

    private final AttributeRepository attributeRepository;
    private final AttributeTypeRepository attributeTypeRepository;

    public AttributeController(AttributeRepository attributeRepository, AttributeTypeRepository attributeTypeRepository) {
        this.attributeRepository = attributeRepository;
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @GetMapping
    public List<AttributeDTO> list() {
        log.info("Solicitud recibida: listar todos los atributos");
        List<AttributeDTO> list = attributeRepository.findAll().stream()
                .map(AttributeMapper::toDTO)
                .toList();
        log.info("Cantidad de atributos encontrados: {}", list.size());
        return list;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeDTO> get(@PathVariable Long id) {
        log.info("Solicitud recibida: obtener atributo con ID {}", id);

        return attributeRepository.findById(id)
                .map(AttributeMapper::toDTO)
                .map(dto -> {
                    log.info("Atributo encontrado: ID {}", id);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    log.warn("Atributo no encontrado con ID {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AttributeDTO dto) {
        log.info("Solicitud recibida: crear atributo con nombre '{}'", dto.getName());

        if (dto.getName() != null && attributeRepository.findByName(dto.getName()).isPresent()) {
            log.warn("Conflicto: ya existe un atributo con el nombre '{}'", dto.getName());
            return ResponseEntity.status(409)
                    .body("Ya existe un atributo con el nombre '" + dto.getName() + "'.");
        }

        AttributeType type = null;
        if (dto.getAttributeTypeId() != null) {
            log.info("Validando existencia de AttributeType con ID {}", dto.getAttributeTypeId());
            type = attributeTypeRepository.findById(dto.getAttributeTypeId()).orElse(null);
            if (type == null) {
                log.warn("No existe AttributeType con ID {}", dto.getAttributeTypeId());
                return ResponseEntity.badRequest().body("El AttributeType con id " + dto.getAttributeTypeId() + " no existe.");
            }
        }

        Attribute saved = attributeRepository.save(AttributeMapper.toEntity(dto, type));
        log.info("Atributo creado correctamente con ID {}", saved.getId());

        return ResponseEntity.status(201).body(AttributeMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AttributeDTO dto) {
        log.info("Solicitud recibida: actualizar atributo con ID {}", id);

        Optional<Attribute> maybe = attributeRepository.findById(id);
        if (maybe.isEmpty()) {
            log.warn("No se encontró atributo para actualizar. ID {}", id);
            return ResponseEntity.notFound().build();
        }

        Attribute a = maybe.get();

        AttributeType type = null;
        if (dto.getAttributeTypeId() != null) {
            log.info("Validando AttributeType para actualización. ID {}", dto.getAttributeTypeId());
            type = attributeTypeRepository.findById(dto.getAttributeTypeId()).orElse(null);
            if (type == null) {
                log.warn("No existe AttributeType con ID {}. No se actualiza este campo.", dto.getAttributeTypeId());
            }
        }

        AttributeMapper.updateEntity(a, dto, type);
        attributeRepository.save(a);
        log.info("Atributo actualizado correctamente. ID {}", id);

        return ResponseEntity.ok(AttributeMapper.toDTO(a));
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
