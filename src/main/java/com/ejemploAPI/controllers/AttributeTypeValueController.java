package com.ejemploAPI.controllers;

import com.ejemploAPI.dtos.AttributeTypeValueDTO;
import com.ejemploAPI.mappers.AttributeTypeValueMapper;
import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.models.AttributeTypeValue;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import com.ejemploAPI.repositories.AttributeTypeValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attribute-type-values")
public class AttributeTypeValueController {

    private static final Logger log = LoggerFactory.getLogger(AttributeTypeValueController.class);

    private final AttributeTypeValueRepository valueRepository;
    private final AttributeTypeRepository typeRepository;

    public AttributeTypeValueController(AttributeTypeValueRepository valueRepository,
                                        AttributeTypeRepository typeRepository) {
        this.valueRepository = valueRepository;
        this.typeRepository = typeRepository;
    }

    @GetMapping
    public List<AttributeTypeValueDTO> list() {
        log.info("Solicitando listado completo de AttributeTypeValue");
        return valueRepository.findAll().stream()
                .map(AttributeTypeValueMapper::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeTypeValueDTO> getById(@PathVariable Long id) {
        log.info("Solicitando AttributeTypeValue con ID {}", id);
        return valueRepository.findById(id)
                .map(AttributeTypeValueMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("AttributeTypeValue no encontrado con ID {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AttributeTypeValueDTO dto) {
        log.info("Creando AttributeTypeValue: {}", dto.getValue());

        Optional<AttributeType> maybeType = typeRepository.findById(dto.getAttributeTypeId());
        if (maybeType.isEmpty()) {
            log.warn("No se encontró AttributeType con ID {}", dto.getAttributeTypeId());
            return ResponseEntity.badRequest()
                    .body("No existe AttributeType con ID " + dto.getAttributeTypeId());
        }

        AttributeTypeValue entity = AttributeTypeValueMapper.toEntity(dto);
        entity.setAttributeType(maybeType.get());

        AttributeTypeValue saved = valueRepository.save(entity);
        log.info("AttributeTypeValue creado con ID {}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AttributeTypeValueMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AttributeTypeValueDTO dto) {
        log.info("Actualizando AttributeTypeValue ID {}", id);

        Optional<AttributeTypeValue> maybe = valueRepository.findById(id);
        if (maybe.isEmpty()) {
            log.warn("AttributeTypeValue no encontrado con ID {}", id);
            return ResponseEntity.notFound().build();
        }

        AttributeTypeValue entity = maybe.get();
        entity.setValue(dto.getValue());

        if (dto.getAttributeTypeId() != null) {
            Optional<AttributeType> maybeType = typeRepository.findById(dto.getAttributeTypeId());
            maybeType.ifPresent(entity::setAttributeType);
        }

        AttributeTypeValue updated = valueRepository.save(entity);
        log.info("AttributeTypeValue actualizado correctamente ID {}", updated.getId());
        return ResponseEntity.ok(AttributeTypeValueMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("Eliminando AttributeTypeValue con ID {}", id);

        if (!valueRepository.existsById(id)) {
            log.warn("AttributeTypeValue no encontrado para eliminar ID {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Elemento no encontrado");
        }

        valueRepository.deleteById(id);
        log.info("AttributeTypeValue eliminado correctamente ID {}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
