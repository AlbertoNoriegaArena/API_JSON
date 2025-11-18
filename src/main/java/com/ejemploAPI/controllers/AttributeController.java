package com.ejemploAPI.controllers;

import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.repositories.AttributeRepository;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/attributes")
public class AttributeController {

    private final AttributeRepository attributeRepository;
    private final AttributeTypeRepository attributeTypeRepository;

    public AttributeController(AttributeRepository attributeRepository, AttributeTypeRepository attributeTypeRepository) {
        this.attributeRepository = attributeRepository;
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @GetMapping
    public List<Attribute> list() {
        return attributeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Attribute> get(@PathVariable Long id) {
        return attributeRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Attribute> create(@RequestBody Attribute payload) {
        // if payload contains attributeType id, try to set it
        if (payload.getAttributeType() != null && payload.getAttributeType().getId() != null) {
            Optional<AttributeType> at = attributeTypeRepository.findById(payload.getAttributeType().getId());
            at.ifPresent(payload::setAttributeType);
        }
        Attribute saved = attributeRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Attribute> update(@PathVariable Long id, @RequestBody Attribute payload) {
        Optional<Attribute> maybe = attributeRepository.findById(id);
        if (maybe.isEmpty()) return ResponseEntity.notFound().build();
        Attribute a = maybe.get();
        a.setName(payload.getName());
        if (payload.getAttributeType() != null && payload.getAttributeType().getId() != null) {
            attributeTypeRepository.findById(payload.getAttributeType().getId()).ifPresent(a::setAttributeType);
        }
        attributeRepository.save(a);
        return ResponseEntity.ok(a);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!attributeRepository.existsById(id)) return ResponseEntity.notFound().build();
        attributeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
