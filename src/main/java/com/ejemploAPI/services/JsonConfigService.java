package com.ejemploAPI.services;

import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.models.Config;
import com.ejemploAPI.repositories.AttributeRepository;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import com.ejemploAPI.repositories.ConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class JsonConfigService {

    private final ConfigRepository configRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeTypeRepository attributeTypeRepository;
    private final ObjectMapper objectMapper;

    public JsonConfigService(ConfigRepository configRepository, AttributeRepository attributeRepository,
                             AttributeTypeRepository attributeTypeRepository) {
        this.configRepository = configRepository;
        this.attributeRepository = attributeRepository;
        this.attributeTypeRepository = attributeTypeRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Importa un JSON genérico y lo almacena en la BD según la estructura jerárquica
     */
    public void importJson(Map<String, Object> jsonMap) {
        // Procesa los elementos raíz (sin padre)
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            processJsonNode(entry.getKey(), entry.getValue(), null);
        }
    }

    /**
     * Procesa recursivamente cada nodo del JSON
     */
    private void processJsonNode(String attributeName, Object value, Long parentId) {
        // Obtener o crear el Attribute
        Attribute attr = getOrCreateAttribute(attributeName, value);
        if (attr == null) return;

        // Crear el Config
        Config config = new Config();
        config.setAttribute(attr);
        if (parentId != null) {
            Optional<Config> parent = configRepository.findById(parentId);
            parent.ifPresent(config::setParent);
        }

        // Determinar el tipo de valor y procesarlo
        if (value instanceof Map) {
            // Es un objeto: tipo NODE
            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);
            
            // Procesar hijos recursivamente
            Map<String, Object> mapValue = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : mapValue.entrySet()) {
                processJsonNode(entry.getKey(), entry.getValue(), savedConfig.getId());
            }
        } else if (value instanceof List) {
            // Es una lista
            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);
            
            List<?> listValue = (List<?>) value;
            for (int i = 0; i < listValue.size(); i++) {
                Object item = listValue.get(i);
                if (item instanceof Map) {
                    // Lista de objetos
                    String itemAttrName = attributeName + "_item_" + i;
                    processJsonNode(itemAttrName, item, savedConfig.getId());
                } else {
                    // Lista de valores primitivos
                    String itemValue = item != null ? item.toString() : "";
                    Config itemConfig = new Config();
                    itemConfig.setAttribute(attr);
                    itemConfig.setParent(savedConfig);
                    itemConfig.setDefaultValue(itemValue);
                    saveOrGetConfig(itemConfig);
                }
            }
        } else {
            // Es un valor primitivo (STRING, NUMERIC, BOOLEAN)
            config.setDefaultValue(value != null ? value.toString() : "");
            saveOrGetConfig(config);
        }
    }

    /**
     * Guarda el config si no existe uno equivalente (mismo attribute, mismo parent y mismo defaultValue),
     * o devuelve el existente para evitar duplicados.
     */
    private Config saveOrGetConfig(Config cfg) {
        Long attributeId = cfg.getAttribute() != null ? cfg.getAttribute().getId() : null;
        Long parentId = cfg.getParent() != null ? cfg.getParent().getId() : null;
        String defVal = cfg.getDefaultValue();
        // Buscar entre los hijos del parent (o entre raíces si parentId == null)
        List<Config> candidates;
        if (parentId != null) {
            candidates = configRepository.findByParentIdOrderByIdAsc(parentId);
        } else {
            candidates = configRepository.findByParentIsNull();
        }

        for (Config c : candidates) {
            Long cAttrId = c.getAttribute() != null ? c.getAttribute().getId() : null;
            String cDef = c.getDefaultValue();
            if (attributeId != null && attributeId.equals(cAttrId)) {
                if (defVal == null && cDef == null) {
                    return c;
                }
                if (defVal != null && defVal.equals(cDef)) {
                    return c;
                }
            }
        }

        return configRepository.save(cfg);
    }

    //Obtiene un Attribute existente o lo crea

    private Attribute getOrCreateAttribute(String name, Object value) {
        // Buscar por name usando método de repositorio
        Optional<Attribute> existing = attributeRepository.findFirstByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Crear nuevo Attribute
        Attribute attr = new Attribute();
        attr.setName(name);

        // Determinar el tipo de atributo
        AttributeType attrType = determineAttributeType(value);
        attr.setAttributeType(attrType);

        return attributeRepository.save(attr);
    }

    /**
     * Determina el tipo de atributo según el valor
     */
    private AttributeType determineAttributeType(Object value) {
        String typeStr;
        Boolean isList = false;
        Boolean isEnum = false;

        if (value instanceof Map) {
            typeStr = "NODE";
            isList = false;
        } else if (value instanceof List) {
            isList = true;
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map) {
                    typeStr = "NODE";
                } else if (first instanceof Number) {
                    typeStr = "NUMERIC";
                } else if (first instanceof Boolean) {
                    typeStr = "BOOLEAN";
                } else {
                    typeStr = "STRING";
                }
            } else {
                // Lista vacía: asumir STRING list
                typeStr = "STRING";
            }
        } else if (value instanceof Boolean) {
            typeStr = "BOOLEAN";
        } else if (value instanceof Number) {
            typeStr = "NUMERIC";
        } else {
            typeStr = "STRING";
        }

        // Intentar reusar AttributeType existente
        Optional<AttributeType> existing = attributeTypeRepository.findByTypeAndIsListAndIsEnum(typeStr, isList, isEnum);
        if (existing.isPresent()) {
            return existing.get();
        }

        AttributeType type = new AttributeType();
        type.setType(typeStr);
        type.setIsList(isList);
        type.setIsEnum(isEnum);

        return attributeTypeRepository.save(type);
    }

    /**
     * Exporta la configuración a JSON genérico
     */
    public String exportToJson() {
        // Obtener los Configs raíz (sin padre)
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

    /**
     * Construye recursivamente el valor JSON para un Config
     */
    private Object buildJsonValue(Config config) {
        // Obtener hijos
        List<Config> children = configRepository.findByParentIdOrderByIdAsc(config.getId());

        if (children.isEmpty()) {
            // Es una hoja: retornar el valor primitivo
            String value = config.getDefaultValue();
            if (value == null || value.isEmpty()) {
                return null;
            }

            AttributeType attrType = config.getAttribute() != null ? 
                    config.getAttribute().getAttributeType() : null;

            if (attrType != null) {
                switch (attrType.getType()) {
                    case "BOOLEAN":
                        return Boolean.parseBoolean(value);
                    case "NUMERIC":
                        try {
                            return Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            return value;
                        }
                    default:
                        return value;
                }
            }
            return value;
        } else if (config.getAttribute() != null &&
                   config.getAttribute().getAttributeType() != null &&
                   config.getAttribute().getAttributeType().getIsList() != null &&
                   config.getAttribute().getAttributeType().getIsList()) {
            // Es una lista
            List<Object> list = new java.util.ArrayList<>();
            for (Config child : children) {
                list.add(buildJsonValue(child));
            }
            return list;
        } else {
            // Es un objeto (NODE)
            Map<String, Object> obj = new LinkedHashMap<>();
            for (Config child : children) {
                if (child.getAttribute() != null) {
                    String childAttrName = child.getAttribute().getName();
                    // Limpiar sufijos generados automáticamente
                    if (childAttrName.contains("_item_")) {
                        childAttrName = childAttrName.substring(0, childAttrName.lastIndexOf("_item_"));
                    }
                    Object childValue = buildJsonValue(child);
                    obj.put(childAttrName, childValue);
                }
            }
            return obj;
        }
    }
}