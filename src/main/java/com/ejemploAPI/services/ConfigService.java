package com.ejemploAPI.services;

import com.ejemploAPI.config.exceptions.DuplicateKeyException;
import com.ejemploAPI.config.exceptions.InvalidEnumValueException;
import com.ejemploAPI.config.exceptions.InvalidJsonFormatException;
import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.models.Config;
import com.ejemploAPI.repositories.AttributeRepository;
import com.ejemploAPI.repositories.AttributeTypeRepository;
import com.ejemploAPI.repositories.ConfigRepository;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class ConfigService {

    private final ConfigRepository configRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeTypeRepository attributeTypeRepository;
    private final AttributeTypeService attributeTypeService;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    public ConfigService(ConfigRepository configRepository, AttributeRepository attributeRepository,
                         AttributeTypeRepository attributeTypeRepository, AttributeTypeService attributeTypeService) {
        this.configRepository = configRepository;
        this.attributeRepository = attributeRepository;
        this.attributeTypeRepository = attributeTypeRepository;
        this.attributeTypeService = attributeTypeService;
        this.objectMapper = new ObjectMapper();
        // Detecta claves duplicadas en el JSON y lanza excepción si las hay
        this.objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    // Devolver Config por su id
    public Config findById(Long id) {
        log.debug("Buscando Config con id = {}", id);
        return configRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("No se encontró Config con id = {}", id);
                    return new RuntimeException("No se encontró Config con id = " + id);
                });
    }


    public void importJson(String rawJson) {
        AtomicInteger nodosProcesados = new AtomicInteger(0);
        AtomicInteger nodosCreados = new AtomicInteger(0);
        AtomicInteger nodosEliminados = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        log.debug("Inicio de importación JSON. Longitud del string recibido: {}", rawJson.length());
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(rawJson, Map.class);
            // Detectar si cada nodo necesita un AttributeType
            preScanAndRegisterTypes(jsonMap);

            // Procesar cada nodo recursivamente
            for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                processJsonNode(entry.getKey(), entry.getValue(), null, nodosProcesados, nodosCreados, nodosEliminados, 0);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("Importación JSON finalizada en {} ms. Procesados={}, Creados={}, Eliminados={}",
                    elapsedTime, nodosProcesados.get(), nodosCreados.get(), nodosEliminados.get());

        } catch (JsonParseException e) {
            String msg = e.getOriginalMessage();
            if (msg != null && msg.contains("Duplicate field")) {
                throw new DuplicateKeyException("JSON inválido: clave duplicada " + msg);
            } else {
                throw new InvalidJsonFormatException("JSON inválido: error de sintaxis " + msg, e);
            }
        }  catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /* Métodos para inferir enums
    Busca si un valor o lista coincide con algún AttributeType marcado como enum
    Esto permite que si tu JSON tiene "color": "ROJO", se asocie automáticamente al AttributeType Color si existe
    */
    private AttributeType findEnumTypeMatchingValue(String value) {
        if (value == null) return null;
        List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
        if (enumTypes == null || enumTypes.isEmpty()) return null;

        for (AttributeType at : enumTypes) {
            if (attributeTypeService.findClosestAllowedValue(at, value) != null) {
                return at;
            }
        }
        return null;
    }

    private AttributeType findEnumTypeMatchingList(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
        if (enumTypes == null || enumTypes.isEmpty()) return null;

        for (AttributeType at : enumTypes) {
            boolean allMatch = true;
            for (Object item : list) {
                if (item == null) continue;
                if (attributeTypeService.findClosestAllowedValue(at, item.toString()) == null) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) return at;
        }
        return null;
    }

    /*  Métodos del pre Scan
        Recorre todo el JSON antes de persistir, y asegura que cada atributo tenga un AttributeType
    */

    private void preScanAndRegisterTypes(Map<String, Object> jsonMap) {
        if (jsonMap == null) return;
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            preScanNode(entry.getKey(), entry.getValue());
        }
    }

    private void preScanNode(String name, Object value) {
        if (value instanceof Map) {
            ((Map<?, ?>) value).forEach((k, v) -> preScanNode((String) k, v));
        } else if (value instanceof List) {
            ensureAttributeForList(name, (List<?>) value);
            for (Object item : (List<?>) value) {
                if (item instanceof Map) {
                    ((Map<?, ?>) item).forEach((k, v) -> preScanNode((String) k, v));
                }
            }
        } else {
            ensureAttributeForPrimitive(name, value);
        }
    }

    // Registra los atributos en base de datos si no existen.
    private void ensureAttributeForList(String name, List<?> listValue) {
        Optional<Attribute> existing = attributeRepository.findByName(name);
        if (existing.isPresent()) {
            Attribute attr = existing.get();
            if (attr.getAttributeType() != null &&
                    Boolean.TRUE.equals(attr.getAttributeType().getIsEnum()) &&
                    Boolean.TRUE.equals(attr.getAttributeType().getIsList()))
                return;

            AttributeType inferred = findEnumTypeMatchingList(listValue);
            if (inferred != null) {
                AttributeType listEnum = inferred;
                if (!Boolean.TRUE.equals(listEnum.getIsList())) {
                    listEnum = attributeTypeService.findOrCreateListEnumType(inferred);
                }
                attr.setAttributeType(listEnum);
                attributeRepository.save(attr);
            }
            return;
        }

        Attribute attr = new Attribute();
        attr.setName(name);
        AttributeType at = determineAttributeType(listValue, name);
        attr.setAttributeType(at);
        attributeRepository.save(attr);
    }

    private void ensureAttributeForPrimitive(String name, Object value) {
        Optional<Attribute> existing = attributeRepository.findByName(name);
        if (existing.isPresent()) {
            Attribute attr = existing.get();
            if (attr.getAttributeType() != null && Boolean.TRUE.equals(attr.getAttributeType().getIsEnum()))
                return;

            if (value != null) {
                AttributeType match = findEnumTypeMatchingValue(value.toString());
                if (match != null) {
                    match.setIsList(false);
                    attr.setAttributeType(match);
                    attributeRepository.save(attr);
                }
            }
            return;
        }

        Attribute attr = new Attribute();
        attr.setName(name);
        AttributeType at = determineAttributeType(value, name);
        attr.setAttributeType(at);
        attributeRepository.save(attr);
    }

    /* Determina el tipo del atributo
       NUMERIC, BOOLEAN, STRING, NODE (otro objeto JSON)
       Si es lista (isList) o enum (isEnum)
       Si no existe, lo crea en la base de datos
     */
    private AttributeType determineAttributeType(Object value, String attributeName) {
        boolean isList = value instanceof List;
        boolean isEnum = false;
        String typeStr;

        if (value instanceof Map) typeStr = "NODE";
        else if (value instanceof List) {
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

        if (!isList && value != null) {
            AttributeType match = findEnumTypeMatchingValue(value.toString());
            if (match != null) return match;
        }

        if (isList) {
            AttributeType match = findEnumTypeMatchingList((List<?>) value);
            if (match != null) return match;
        }

        Optional<AttributeType> existing = attributeTypeRepository.findByTypeAndIsListAndIsEnum(typeStr, isList, false);
        if (existing.isPresent()) return existing.get();

        AttributeType type = new AttributeType();
        type.setType(typeStr);
        type.setIsList(isList);
        type.setIsEnum(isEnum);

        return attributeTypeRepository.save(type);
    }

    /*  Aquí es donde realmente se construye la jerarquía Config en la BBDD
        Dependiendo del tipo de nodo:
        Map => nodo padre => se crean hijos recursivamente
        List => nodo tipo lista => se crean Config para cada ítem
        Primitivo → valor simple => se guarda en defaultValue
        Además maneja: inferencia de enums para listas y eliminación recursiva de hijos antiguos antes de crear nuevos
     */
    private void processJsonNode(String attributeName, Object value, Long parentId,
                                 AtomicInteger nodosProcesados,
                                 AtomicInteger nodosCreados,
                                 AtomicInteger nodosEliminados,
                                 int level) {

        nodosProcesados.incrementAndGet();
        Attribute attr = getOrCreateAttribute(attributeName, value);

        Config config = new Config();
        config.setAttribute(attr);

        String indent = "  ".repeat(level); // indentación para logs

        if (parentId != null) {
            log.debug("{}[PADRE] Procesando nodo '{}'{}", indent, attributeName, parentId != null ? " con parentId=" + parentId : "");
            configRepository.findById(parentId).ifPresent(config::setParent);
            log.debug("[HIJO] Procesando nodo '{}' con parentId={}", attributeName, parentId);
        }

        if (value instanceof Map) {
            log.debug("{}[PADRE] Procesando nodo '{}'{}", indent, attributeName,
                    parentId != null ? " con parentId=" + parentId : " (raíz)");
            // Nodo tipo MAP
            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);

            // Borrar hijos antiguos
            List<Config> existingChildren = configRepository.findByParentIdOrderByIdAsc(savedConfig.getId());
            existingChildren.forEach(c -> deleteConfigRecursively(c, nodosEliminados));

            // Procesar hijos
            ((Map<?, ?>) value).forEach((k, v) -> processJsonNode((String) k, v, savedConfig.getId(),
                    nodosProcesados, nodosCreados, nodosEliminados, level + 1));

        } else if (value instanceof List) {
            // Nodo tipo LISTA
            log.debug("{}Procesando lista '{}' tamaño={}", indent, attributeName, ((List<?>) value).size());
            List<?> listValue = (List<?>) value;

            // Inferir AttributeType enum para la lista si no existe
            AttributeType attrType = attr.getAttributeType();
            if (attrType == null || !Boolean.TRUE.equals(attrType.getIsEnum()) || !Boolean.TRUE.equals(attrType.getIsList())) {
                AttributeType inferred = inferEnumTypeForList(attributeName, listValue);
                if (inferred != null) {
                    if (!Boolean.TRUE.equals(inferred.getIsList())) {
                        inferred = attributeTypeService.findOrCreateListEnumType(inferred);
                    }
                    attr.setAttributeType(inferred);
                    attributeRepository.save(attr);
                }
            }

            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);

            // Borrar hijos antiguos
            List<Config> existingChildren = configRepository.findByParentIdOrderByIdAsc(savedConfig.getId());
            existingChildren.forEach(c -> deleteConfigRecursively(c, nodosEliminados));

            AttributeType listAttrType = attr.getAttributeType();

            for (int i = 0; i < listValue.size(); i++) {
                Object item = listValue.get(i);
                if (item instanceof Map) {
                    processJsonNode(attributeName + "_item_" + i, item, savedConfig.getId(),
                            nodosProcesados, nodosCreados, nodosEliminados, level);
                } else {
                    String itemValue = item != null ? item.toString() : "";
                    log.debug("{}[LISTA] {}[{}] = '{}'", indent, attributeName, i, itemValue);
                    Config itemConfig = new Config();
                    itemConfig.setAttribute(attr);
                    itemConfig.setParent(savedConfig);

                    if (listAttrType != null && Boolean.TRUE.equals(listAttrType.getIsEnum())) {
                        // Buscar tipo base del enum (no-list)
                        AttributeType baseEnumType = attributeTypeRepository
                                .findByTypeAndIsListAndIsEnum(listAttrType.getType(), false, true)
                                .orElse(listAttrType);

                        String mappedValue = attributeTypeService.findClosestAllowedValue(baseEnumType, itemValue);
                        if (mappedValue != null) {
                            itemConfig.setDefaultValue(mappedValue); // Guarda valor real de BBDD
                        } else {
                            List<String> valoresValidos = attributeTypeService.getAllowedValues(baseEnumType);
                            log.error("{}[LISTA][ERROR] El valor '{}' no es válido para el enum '{}' en la posición {}. Valores permitidos: {}",
                                    indent, itemValue, attributeName, i, valoresValidos);
                            throw new InvalidEnumValueException(attributeName, itemValue,
                                    valoresValidos);
                        }
                    } else {
                        itemConfig.setDefaultValue(itemValue);
                    }

                    saveOrGetConfig(itemConfig);
                    nodosCreados.incrementAndGet();
                }
            }

        } else {
            // Nodo tipo primitivo
            String primitiveValue = value != null ? value.toString() : "";
            log.debug("[PRIMITIVO] Nodo '{}' = '{}'", attributeName, primitiveValue);

            // Intentar asociar a enum si existe
            AttributeType match = findEnumTypeMatchingValue(primitiveValue);
            if (match != null && attr.getAttributeType() == null) {
                match.setIsList(false);
                attr.setAttributeType(match);
                attributeRepository.save(attr);
            }

            config.setDefaultValue(primitiveValue);
            saveOrGetConfig(config);
            nodosCreados.incrementAndGet();
        }
    }

    /* Si la config ya existe para ese atributo y padre, la actualiza. Si no existe, la guarda
       Maneja listas y nodos padre-hijo
     */
    private Config saveOrGetConfig(Config cfg) {
        Long attributeId = cfg.getAttribute() != null ? cfg.getAttribute().getId() : null;
        Long parentId = cfg.getParent() != null ? cfg.getParent().getId() : null;

        try {
            boolean cfgIsListAttribute = cfg.getAttribute() != null
                    && cfg.getAttribute().getAttributeType() != null
                    && Boolean.TRUE.equals(cfg.getAttribute().getAttributeType().getIsList());

            if (cfgIsListAttribute && parentId != null) {
                return configRepository.save(cfg);
            }
        } catch (Exception ex) {
            log.warn("No se pudo determinar si es lista, flujo normal. Error: {}", ex.getMessage());
        }

        List<Config> siblings = parentId != null
                ? configRepository.findByParentIdOrderByIdAsc(parentId)
                : configRepository.findByParentIsNull();

        for (Config c : siblings) {
            if (c.getAttribute() != null && c.getAttribute().getId().equals(attributeId)) {
                boolean esLista = c.getAttribute().getAttributeType() != null &&
                        Boolean.TRUE.equals(c.getAttribute().getAttributeType().getIsList());
                if (!esLista) {
                    c.setDefaultValue(cfg.getDefaultValue());
                    return configRepository.save(c);
                }
                return c;
            }
        }
        return configRepository.save(cfg);
    }

    // Busca un atributo por nombre o lo crea con su AttributeType
    private Attribute getOrCreateAttribute(String name, Object value) {
        Optional<Attribute> existing = attributeRepository.findByName(name);
        if (existing.isPresent()) return existing.get();

        Attribute attr = new Attribute();
        attr.setName(name);
        AttributeType attrType = determineAttributeType(value, name);
        attr.setAttributeType(attrType);
        return attributeRepository.save(attr);
    }

    // Borra un nodo y todos sus hijos de la BBDD
    private void deleteConfigRecursively(Config config, AtomicInteger nodosEliminados) {
        List<Config> children = configRepository.findByParentIdOrderByIdAsc(config.getId());
        for (Config child : children) {
            deleteConfigRecursively(child, nodosEliminados);
        }
        configRepository.delete(config);
        nodosEliminados.incrementAndGet();
    }

    // Inferir enums a las listas
    private AttributeType inferEnumTypeForList(String attributeName, List<?> items) {
        log.debug("Intentando inferir ENUM para la lista '{}', tamaño {}", attributeName, items.size());

        // Traer todos los AttributeType que sean enums
        List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
        if (enumTypes == null || enumTypes.isEmpty()) return null;

        AttributeType bestMatch = null;
        int maxMatches = 0;

        for (AttributeType at : enumTypes) {
            int matches = 0;
            for (Object item : items) {
                if (item == null) continue;
                String s = item.toString();
                String mapped = attributeTypeService.findClosestAllowedValue(at, s);
                if (mapped != null) matches++;
            }

            if (matches > maxMatches) {
                maxMatches = matches;
                bestMatch = at;
            }
        }

        // Retornar solo si hay al menos un match
        return maxMatches > 0 ? bestMatch : null;
    }

    /* Exportación completa a JSON
       Recorre todos los Config raíz (parent = null) y reconstruye un objeto JSON
       Llama recursivamente a buildJsonValue
    */
    public String exportToJson() {
        log.debug("Iniciando exportación a JSON...");
        List<Config> rootConfigs = configRepository.findByParentIsNull();
        log.debug("Nodos raíz encontrados: {}", rootConfigs.size());
        Map<String, Object> result = new LinkedHashMap<>();
        AtomicInteger totalNodesExported = new AtomicInteger(0);

        for (Config config : rootConfigs) {
            if (config.getAttribute() != null) {
                String attrName = config.getAttribute().getName();
                log.debug("[ROOT] Exportando nodo raíz '{}'", attrName);
                Object value = buildJsonValue(config, totalNodesExported);
                result.put(attrName, value);
            } else {
                log.warn("Nodo raíz sin atributo asociado, id={}", config.getId());
            }
        }

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            log.debug("Exportación finalizada. Nodos exportados: {}. Longitud del JSON: {} caracteres",
                    totalNodesExported.get(), json.length());
            return json;
        } catch (Exception e) {
            log.error("Error generando el JSON", e);
            return "{}";
        }
    }

    /* Maneja listas, enums, tipos primitivos y nodos hijos
       Convierte los valores de string a Boolean, Numeric o enum según corresponda
       Agrupa los items de lista usando el sufijo _item_
     */
    private Object buildJsonValue(Config config, AtomicInteger totalNodesExported) {
        totalNodesExported.incrementAndGet();
        String attrName = config.getAttribute() != null ? config.getAttribute().getName() : "(sin atributo)";
        Long id = config.getId();
        List<Config> children = configRepository.findByParentIdOrderByIdAsc(config.getId());
        AttributeType attrType = config.getAttribute() != null ? config.getAttribute().getAttributeType() : null;

        // Manejo de listas
        if (attrType != null && Boolean.TRUE.equals(attrType.getIsList())) {
            log.debug("[EXPORT-LIST] '{}' (id={}) contiene {} elementos",
                    attrName, id, children.size());
            List<Object> list = new ArrayList<>();
            for (Config child : children) {

                log.debug("[EXPORT-LIST]   -> Item hijo '{}' (id={}) valor='{}'",
                        child.getAttribute() != null ? child.getAttribute().getName() : "(sin atributo)",
                        child.getId(),
                        child.getDefaultValue()
                );
                String childValue = child.getDefaultValue();
                if (childValue == null) continue;

                // Enum list
                if (Boolean.TRUE.equals(attrType.getIsEnum())) {
                    AttributeType baseEnumType = attributeTypeRepository
                            .findByTypeAndIsListAndIsEnum(attrType.getType(), false, true)
                            .orElse(attrType);

                    String allowedValue = attributeTypeService.findClosestAllowedValue(baseEnumType, childValue);
                    if (allowedValue != null) list.add(allowedValue);

                } else {
                    // Boolean, Numeric o String
                    switch (attrType.getType()) {
                        case "BOOLEAN":
                            list.add(Boolean.parseBoolean(childValue));
                            break;
                        case "NUMERIC":
                            list.add(parseNumeros(childValue));
                            break;
                        default:
                            list.add(childValue);
                    }
                }
            }
            return list;
        }

        // Valor primitivo
        if (children.isEmpty()) {
            log.debug("[EXPORT-PRIMITIVE] '{}' (id={}) = '{}'", attrName, id, config.getDefaultValue());
            String value = config.getDefaultValue();
            if (value == null) return null;

            if (attrType != null && Boolean.TRUE.equals(attrType.getIsEnum())) {
                String allowedValue = attributeTypeService.findClosestAllowedValue(attrType, value);
                if (allowedValue != null) return allowedValue;
            }

            if (attrType != null) {
                switch (attrType.getType()) {
                    case "BOOLEAN":
                        return Boolean.parseBoolean(value);
                    case "NUMERIC":
                        return parseNumeros(value);
                    default:
                        return value;
                }
            }

            return value;
        }

        // Nodos
        Map<String, Object> obj = new LinkedHashMap<>();
        for (Config child : children) {
            if (child.getAttribute() != null) {
                String childAttrName = child.getAttribute().getName();
                // Manejo de listas: nombres con _item_ se agrupan por padre
                if (childAttrName.contains("_item_")) {
                    childAttrName = childAttrName.substring(0, childAttrName.lastIndexOf("_item_"));
                }
                Object childValue = buildJsonValue(child, totalNodesExported);
                obj.put(childAttrName, childValue);
            } else {
                log.warn("Nodo hijo sin atributo, id={}", child.getId());
            }
        }
        return obj;
    }

    // Parseo de números
    private Object parseNumeros(String value) {
        if (value == null) return null;
        try {
            if (value.matches("^-?\\d+$")) {
                long longValue = Long.parseLong(value);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) return (int) longValue;
                return longValue;
            }
            return Double.parseDouble(value);
        } catch (Exception e) {
            return value;
        }
    }
}
