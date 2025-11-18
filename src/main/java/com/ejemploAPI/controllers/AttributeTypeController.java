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

    @GetMapping("/{typeName}")
    public ResponseEntity<AttributeType> getType(@PathVariable String typeName) {
        Optional<AttributeType> maybe = attributeTypeRepository.findByTypeIgnoreCase(typeName);
        return maybe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
