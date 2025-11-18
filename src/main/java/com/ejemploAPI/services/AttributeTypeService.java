package com.ejemploAPI.services;

import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.models.AttributeTypeValue;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import com.ejemploAPI.repositories.AttributeTypeValueRepository;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AttributeTypeService {

    @Autowired
    private AttributeTypeRepository attributeTypeRepository;

    @Autowired
    private AttributeTypeValueRepository attributeTypeValueRepository;

    public AttributeType ensureEnumType(String typeName) {
        Optional<AttributeType> maybe = attributeTypeRepository.findByTypeIgnoreCaseAndIsListAndIsEnum(typeName, false,
                true);
        if (maybe.isPresent())
            return maybe.get();

        AttributeType at = new AttributeType();
        at.setType(typeName);
        at.setIsEnum(true);
        at.setIsList(false);
        return attributeTypeRepository.save(at);
    }

    public void addValuesToAttributeType(AttributeType attributeType, List<String> values) {
        if (attributeType == null || values == null || values.isEmpty())
            return;
        for (String v : values) {
            if (v == null)
                continue;
            Optional<AttributeTypeValue> ex = attributeTypeValueRepository
                    .findFirstByAttributeTypeIdAndValue(attributeType.getId(), v);
            if (ex.isEmpty()) {
                AttributeTypeValue atv = new AttributeTypeValue();
                atv.setAttributeType(attributeType);
                atv.setValue(v);
                attributeTypeValueRepository.save(atv);
            }
        }
    }

    public boolean isValueAllowed(AttributeType attributeType, String value) {
        if (attributeType == null)
            return true;
        if (!Boolean.TRUE.equals(attributeType.getIsEnum()))
            return true;
        if (value == null)
            return false;
        return attributeTypeValueRepository
                .findFirstByAttributeTypeIdAndValue(attributeType.getId(), value)
                .isPresent();
    }

    public List<String> getAllowedValues(AttributeType attributeType) {
        List<String> out = new ArrayList<>();
        if (attributeType == null)
            return out;

        List<AttributeTypeValue> vals = attributeTypeValueRepository.findByAttributeTypeId(attributeType.getId());

        for (AttributeTypeValue v : vals) {
            out.add(v.getValue());
        }

        return out;
    }

    public String findClosestAllowedValue(AttributeType attrType, String inputValue) {
        if (inputValue == null || attrType == null)
            return null;

        String normalizedInput = normalizarTextos(inputValue);

        List<String> allowed = getAllowedValues(attrType);

        // Búsqueda exacta pero normalizada (sin acentos y en minúsculas)
        for (String allowedVal : allowed) {
            if (normalizarTextos(allowedVal).equals(normalizedInput)) {
                return allowedVal; // devuelve el valor REAL de BBDD
            }
        }

        return null; // no coincide
    }

    public static String normalizarTextos(String text) {
        if (text == null)
            return null;
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{M}", "") // elimina acentos
                .toLowerCase()
                .trim();
    }

    // Si es una lista de enumerados, busca o crea el tipo correspondiente sin machacar el original
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttributeType findOrCreateListEnumType(AttributeType baseEnumType) {
        if (baseEnumType == null || !Boolean.TRUE.equals(baseEnumType.getIsEnum())
                || Boolean.TRUE.equals(baseEnumType.getIsList())) {
            return baseEnumType;
        }

        return attributeTypeRepository
                .findByTypeAndIsListAndIsEnum(baseEnumType.getType(), true, true)
                .orElseGet(() -> {
                    AttributeType listCopy = new AttributeType();
                    listCopy.setType(baseEnumType.getType());
                    listCopy.setIsEnum(true);
                    listCopy.setIsList(true);
                    return attributeTypeRepository.save(listCopy);
                });
    }
}
