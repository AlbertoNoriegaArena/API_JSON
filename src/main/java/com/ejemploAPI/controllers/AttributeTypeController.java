package com.ejemploAPI.controllers;

import com.ejemploAPI.dtos.AttributeTypeDTO;
import com.ejemploAPI.mappers.AttributeTypeMapper;
import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attribute-types")
public class AttributeTypeController {

    private static final Logger log = LoggerFactory.getLogger(AttributeTypeController.class);

    private final AttributeTypeRepository attributeTypeRepository;

    public AttributeTypeController(AttributeTypeRepository attributeTypeRepository) {
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todos los tipos de atributos")
    public List<AttributeTypeDTO> list() {
        log.info("Solicitud recibida: listar todos los AttributeType");
        List<AttributeType> list = attributeTypeRepository.findAll();
        log.info("Cantidad de registros encontrados: {}", list.size());
        return list.stream().map(AttributeTypeMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Listar un tipo de atributo por id")
    public ResponseEntity<AttributeTypeDTO> getById(@PathVariable Long id) {
        log.info("Solicitud para obtener AttributeType por ID: {}", id);
        return attributeTypeRepository.findById(id)
                .map(at -> {
                    log.info("AttributeType encontrado: {}", at.getId());
                    return ResponseEntity.ok(AttributeTypeMapper.toDTO(at));
                })
                .orElseGet(() -> {
                    log.warn("AttributeType no encontrado con ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo tipo de atributo")
    public ResponseEntity<AttributeTypeDTO> create(@RequestBody AttributeTypeDTO dto) {
        log.info("Solicitud para crear AttributeType: {}", dto);
        AttributeType entity = AttributeTypeMapper.toEntity(dto);
        entity.setId(null); // asegurar creación
        AttributeType saved = attributeTypeRepository.save(entity);
        log.info("AttributeType creado con ID: {}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AttributeTypeMapper.toDTO(saved));
    }

    @Operation(summary = "Editar un tipo de atributo")
    @PutMapping("/{id}")
    public ResponseEntity<AttributeTypeDTO> update(@PathVariable Long id, @RequestBody AttributeTypeDTO dto) {
        log.info("Solicitud para actualizar AttributeType con ID: {}", id);

        Optional<AttributeType> maybe = attributeTypeRepository.findById(id);
        if (maybe.isEmpty()) {
            log.warn("No se encontró AttributeType para actualizar. ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        AttributeType existing = maybe.get();
        existing.setType(dto.getType());
        existing.setIsEnum(dto.getIsEnum());
        existing.setIsList(dto.getIsList());

        AttributeType updated = attributeTypeRepository.save(existing);
        log.info("AttributeType actualizado correctamente: {}", updated.getId());
        return ResponseEntity.ok(AttributeTypeMapper.toDTO(updated));
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar un tipo de atributo")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("Solicitud para eliminar AttributeType con ID: {}", id);

        if (!attributeTypeRepository.existsById(id)) {
            log.warn("No se puede eliminar: AttributeType no encontrado. ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Elemento no encontrado");
        }

        try {
            attributeTypeRepository.deleteById(id);
            log.info("AttributeType eliminado correctamente. ID: {}", id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Elemento borrado correctamente");

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
