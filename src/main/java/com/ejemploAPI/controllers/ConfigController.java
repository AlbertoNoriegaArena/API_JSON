package com.ejemploAPI.controllers;

import com.ejemploAPI.models.Config;
import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.repositories.ConfigRepository;
import com.ejemploAPI.repositories.AttributeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/configs")
public class ConfigController {

    private final ConfigRepository configRepository;
    private final AttributeRepository attributeRepository;

    public ConfigController(ConfigRepository configRepository, AttributeRepository attributeRepository) {
        this.configRepository = configRepository;
        this.attributeRepository = attributeRepository;
    }

    @GetMapping
    public List<Config> list() {
        return configRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Config> get(@PathVariable Long id) {
        return configRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Config> create(@RequestBody Config payload) {
        // Attach attribute if provided by id
        if (payload.getAttribute() != null && payload.getAttribute().getId() != null) {
            Optional<Attribute> a = attributeRepository.findById(payload.getAttribute().getId());
            a.ifPresent(payload::setAttribute);
        }
        // Attach parent if provided
        if (payload.getParent() != null && payload.getParent().getId() != null) {
            configRepository.findById(payload.getParent().getId()).ifPresent(payload::setParent);
        }
        Config saved = configRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Config> update(@PathVariable Long id, @RequestBody Config payload) {
        Optional<Config> maybe = configRepository.findById(id);
        if (maybe.isEmpty()) return ResponseEntity.notFound().build();
        Config c = maybe.get();
        c.setDefaultValue(payload.getDefaultValue());
        c.setDescripcion(payload.getDescripcion());
        if (payload.getAttribute() != null && payload.getAttribute().getId() != null) {
            attributeRepository.findById(payload.getAttribute().getId()).ifPresent(c::setAttribute);
        }
        if (payload.getParent() != null && payload.getParent().getId() != null) {
            configRepository.findById(payload.getParent().getId()).ifPresent(c::setParent);
        }
        configRepository.save(c);
        return ResponseEntity.ok(c);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!configRepository.existsById(id)) return ResponseEntity.notFound().build();
        configRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
