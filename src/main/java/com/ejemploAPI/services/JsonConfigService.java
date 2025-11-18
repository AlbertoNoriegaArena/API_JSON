package com.ejemploAPI.services;

import com.ejemploAPI.config.exceptions.InvalidEnumValueException;
import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.models.Config;
import com.ejemploAPI.repositories.AttributeRepository;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import com.ejemploAPI.repositories.ConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class JsonConfigService {

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private AttributeRepository attributeRepository;

    @Autowired
    private AttributeTypeRepository attributeTypeRepository;

    @Autowired
    private AttributeTypeService attributeTypeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void importJson(Map<String, Object> jsonMap) {
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            processJsonNode(entry.getKey(), entry.getValue(), null);
        }
    }

    private void processJsonNode(String attributeName, Object value, Long parentId) {
        Attribute attr = getOrCreateAttribute(attributeName, value);
        if (attr == null) return;

        Config config = new Config();
        config.setAttribute(attr);
        if (parentId != null) {
            Optional<Config> parent = configRepository.findById(parentId);
            parent.ifPresent(config::setParent);
        }

        if (value instanceof Map) {
            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);

            Map<String, Object> mapValue = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : mapValue.entrySet()) {
                processJsonNode(entry.getKey(), entry.getValue(), savedConfig.getId());
            }
        } else if (value instanceof List) {
            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);

            List<?> listValue = (List<?>) value;
            for (int i = 0; i < listValue.size(); i++) {
                Object item = listValue.get(i);
                if (item instanceof Map) {
                    processJsonNode(attributeName + "_item_" + i, item, savedConfig.getId());
                } else {
                    String itemValue = item != null ? item.toString() : "";
                    Config itemConfig = new Config();
                    itemConfig.setAttribute(attr);
                    itemConfig.setParent(savedConfig);

                    // Mapear al valor permitido en BBDD si es enum
                    if (attr.getAttributeType() != null && Boolean.TRUE.equals(attr.getAttributeType().getIsEnum())) {
                        String mappedValue = attributeTypeService.findClosestAllowedValue(attr.getAttributeType(), itemValue);

                        if (mappedValue != null) {
                            // Valor válido → guardamos el permitido
                            itemConfig.setDefaultValue(mappedValue);
                        } else {
                        // Valor inválido → rechazamos la importación con 400 e incluimos valores permitidos
                        List<String> valoresPermitidosEnum = attributeTypeService.getAllowedValues(attr.getAttributeType());
                        throw new InvalidEnumValueException(attributeName, itemValue, valoresPermitidosEnum);

                        }
                    } else {
                        itemConfig.setDefaultValue(itemValue);
                    }

                    saveOrGetConfig(itemConfig);
                }
            }
        } else {
            String primitiveValue = value != null ? value.toString() : "";

            // Mapear valor permitido si es enum
            if (attr.getAttributeType() != null && Boolean.TRUE.equals(attr.getAttributeType().getIsEnum())) {
                String mappedValue = attributeTypeService.findClosestAllowedValue(attr.getAttributeType(), primitiveValue);
                if (mappedValue != null) {
                    config.setDefaultValue(mappedValue);
                } else {
                    // Valor no permitido: rechazamos la importación con 400 e incluimos valores permitidos
                    List<String> valoresPermitidosEnum = attributeTypeService.getAllowedValues(attr.getAttributeType());
                    throw new InvalidEnumValueException(attributeName, primitiveValue, valoresPermitidosEnum);
                }
            } else {
                config.setDefaultValue(primitiveValue);
            }

            saveOrGetConfig(config);
        }
    }

    private Config saveOrGetConfig(Config cfg) {
        Long attributeId = cfg.getAttribute() != null ? cfg.getAttribute().getId() : null;
        Long parentId = cfg.getParent() != null ? cfg.getParent().getId() : null;
        String defVal = cfg.getDefaultValue();
        List<Config> candidates = parentId != null ?
                configRepository.findByParentIdOrderByIdAsc(parentId) :
                configRepository.findByParentIsNull();

        for (Config c : candidates) {
            Long cAttrId = c.getAttribute() != null ? c.getAttribute().getId() : null;
            String cDef = c.getDefaultValue();
            if (attributeId != null && attributeId.equals(cAttrId)) {
                if ((defVal == null && cDef == null) || (defVal != null && defVal.equals(cDef))) {
                    return c;
                }
            }
        }

        return configRepository.save(cfg);
    }

    private Attribute getOrCreateAttribute(String name, Object value) {
        Optional<Attribute> existing = attributeRepository.findFirstByName(name);
        if (existing.isPresent()) {
            Attribute attr = existing.get();
            // Si el attribute ya existe, intentar forzar la asociación al AttributeType enum
            // cuando exista uno con el mismo nombre (case-insensitive).
            try {
                Optional<AttributeType> maybeType = attributeTypeRepository.findByTypeIgnoreCaseAndIsListAndIsEnum(name, false, true);
                if (maybeType.isPresent()) {
                    AttributeType enumType = maybeType.get();
                    // Sobrescribe la asociación si el attribute no tenía tipo o no era enum
                    if (attr.getAttributeType() == null || !Boolean.TRUE.equals(attr.getAttributeType().getIsEnum())) {
                        attr.setAttributeType(enumType);
                        attributeRepository.save(attr);
                    }
                }
            } catch (Exception ignored) {}
            return attr;
        }

        Attribute attr = new Attribute();
        attr.setName(name);

        AttributeType attrType = determineAttributeType(value, name);
        attr.setAttributeType(attrType);

        return attributeRepository.save(attr);
    }

    private AttributeType determineAttributeType(Object value, String attributeName) {
        Optional<AttributeType> enumType =
            attributeTypeRepository.findByTypeIgnoreCaseAndIsListAndIsEnum(attributeName, false, true);
        if (enumType.isPresent()) return enumType.get();

        String typeStr;
        boolean isList = false;
        boolean isEnum = false;

        if (value instanceof Map) typeStr = "NODE";
        else if (value instanceof List) {
            isList = true;
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map) typeStr = "NODE";
                else if (first instanceof Number) typeStr = "NUMERIC";
                else if (first instanceof Boolean) typeStr = "BOOLEAN";
                else typeStr = "STRING";
            } else typeStr = "STRING";
        } else if (value instanceof Boolean) typeStr = "BOOLEAN";
        else if (value instanceof Number) typeStr = "NUMERIC";
        else typeStr = "STRING";

        Optional<AttributeType> existing =
            attributeTypeRepository.findByTypeIgnoreCaseAndIsListAndIsEnum(typeStr, isList, false);
        if (existing.isPresent()) return existing.get();

        AttributeType type = new AttributeType();
        type.setType(typeStr);
        type.setIsList(isList);
        type.setIsEnum(isEnum);

        return attributeTypeRepository.save(type);
    }

    public String exportToJson() {
        List<Config> rootConfigs = configRepository.findByParentIsNull();
        Map<String, Object> result = new LinkedHashMap<>();

        for (Config config : rootConfigs) {
            if (config.getAttribute() != null) {
                String attrName = config.getAttribute().getName();
                Object value = buildJsonValue(config);
                result.put(attrName, value);
            }
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Object buildJsonValue(Config config) {
        List<Config> children = configRepository.findByParentIdOrderByIdAsc(config.getId());

        if (children.isEmpty()) {
            String value = config.getDefaultValue();
            if (value == null || value.isEmpty()) return null;

            AttributeType attrType = config.getAttribute() != null ? config.getAttribute().getAttributeType() : null;

            // Si es enum, devolver valor real de BBDD
            if (attrType != null && Boolean.TRUE.equals(attrType.getIsEnum())) {
                String allowedValue = attributeTypeService.findClosestAllowedValue(attrType, value);
                if (allowedValue != null) return allowedValue;
            }

            if (attrType != null) {
                switch (attrType.getType()) {
                    case "BOOLEAN": return Boolean.parseBoolean(value);
                    case "NUMERIC":
                        try { return Double.parseDouble(value); }
                        catch (NumberFormatException e) { return value; }
                    default: return value;
                }
            }
            return value;
        } else if (config.getAttribute() != null &&
                config.getAttribute().getAttributeType() != null &&
                Boolean.TRUE.equals(config.getAttribute().getAttributeType().getIsList())) {

            List<Object> list = new java.util.ArrayList<>();
            for (Config child : children) list.add(buildJsonValue(child));
            return list;

        } else {
            Map<String, Object> obj = new LinkedHashMap<>();
            for (Config child : children) {
                if (child.getAttribute() != null) {
                    String childAttrName = child.getAttribute().getName();
                    if (childAttrName.contains("_item_"))
                        childAttrName = childAttrName.substring(0, childAttrName.lastIndexOf("_item_"));
                    Object childValue = buildJsonValue(child);
                    obj.put(childAttrName, childValue);
                }
            }
            return obj;
        }
    }
}