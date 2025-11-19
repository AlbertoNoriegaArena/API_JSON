package com.ejemploAPI.controllers;

import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.services.AttributeTypeService;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/attribute-types")
public class AttributeTypeController {

    private final AttributeTypeService attributeTypeService;
    private final AttributeTypeRepository attributeTypeRepository;

    public AttributeTypeController(AttributeTypeService attributeTypeService, AttributeTypeRepository attributeTypeRepository) {
        this.attributeTypeService = attributeTypeService;
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @GetMapping
    public List<AttributeType> list() {
        return attributeTypeRepository.findAll();
    }

    @PostMapping("/{typeName}/values")
    public ResponseEntity<?> addValues(@PathVariable String typeName, @RequestBody List<String> values) {
        AttributeType at = attributeTypeService.ensureEnumType(typeName);
        attributeTypeService.addValuesToAttributeType(at, values);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{typeName}/values")
    public ResponseEntity<?> getValues(@PathVariable String typeName) {
        Optional<AttributeType> maybe = attributeTypeRepository.findByTypeIgnoreCaseAndIsListAndIsEnum(typeName, false, true);
        if (maybe.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(attributeTypeService.getAllowedValues(maybe.get()));
    }

    @GetMapping("/by-name/{typeName}")
    public ResponseEntity<AttributeType> getType(@PathVariable String typeName) {
        Optional<AttributeType> maybe = attributeTypeRepository.findByTypeIgnoreCase(typeName);
        return maybe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeType> getById(@PathVariable Long id) {
        return attributeTypeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public AttributeType create(@RequestBody AttributeType attributeType) {
        // Aseguramos que no tenga ID para que sea una creación
        attributeType.setId(null);
        return attributeTypeRepository.save(attributeType);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeType> update(@PathVariable Long id, @RequestBody AttributeType payload) {
        return attributeTypeRepository.findById(id).map(existingType -> {
            existingType.setType(payload.getType());
            existingType.setIsEnum(payload.getIsEnum());
            existingType.setIsList(payload.getIsList());
            return ResponseEntity.ok(attributeTypeRepository.save(existingType));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!attributeTypeRepository.existsById(id)) return ResponseEntity.notFound().build();
        attributeTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
