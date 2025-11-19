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
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final AttributeTypeService attributeTypeService;

    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(JsonConfigService.class);

    // Inyección por constructor
    public JsonConfigService(ConfigRepository configRepository, AttributeRepository attributeRepository,
            AttributeTypeRepository attributeTypeRepository, AttributeTypeService attributeTypeService) {
        this.configRepository = configRepository;
        this.attributeRepository = attributeRepository;
        this.attributeTypeRepository = attributeTypeRepository;
        this.attributeTypeService = attributeTypeService;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    // Recibe el json como string y lo procesa
    public void importJson(String rawJson) {
        log.info("Important JSON iniciada. Longitud del string recibido: {}", rawJson.length());

        try {
            Map<String, Object> jsonMap = objectMapper.readValue(rawJson, Map.class);
            log.debug("JSON parseado correctamente. Keys raíz: {}", jsonMap.keySet());
            preScanAndRegisterTypes(jsonMap);
            log.debug("Pre-scan completado");

            for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                log.debug("Procesando nodo raíz: {}", entry.getKey());
                processJsonNode(entry.getKey(), entry.getValue(), null);
            }

            log.info("Importación JSON finalizada correctamente.");

        } catch (JsonParseException e) {
            log.error("Error de parseo JSON: {}", e.getOriginalMessage());
            String msg = e.getOriginalMessage();
            if (msg != null && msg.contains("Duplicate field")) {
                throw new DuplicateKeyException("JSON inválido: clave duplicada " + msg);
            } else {
                throw new InvalidJsonFormatException("JSON inválido: error de sintaxis " + msg, e);
            }
        } catch (Exception e) {
            log.error("Error inesperado importando JSON", e);
            throw new RuntimeException("Error procesando JSON", e);
        }
    }

    // Intenta encontrar un AttributeType enum existente que se ajuste a los valores de la lista
    // Cuenta cuantos elementos de la lista coinciden con los valores permitidos y devuelve el enum con más coincidencias
    private AttributeType inferEnumTypeForList(String attributeName, List<?> items) {

        log.debug("Intentando inferir ENUM para la lista '{}', tamaño {}", attributeName, items.size());
        List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
        if (enumTypes == null || enumTypes.isEmpty())
            return null;

        AttributeType best = null;
        int bestMatches = 0;
        for (AttributeType at : enumTypes) {
            int matches = 0;
            for (Object item : items) {
                if (item == null)
                    continue;
                String s = item.toString();
                if (attributeTypeService.findClosestAllowedValue(at, s) != null)
                    matches++;
            }
            if (matches > bestMatches) {
                bestMatches = matches;
                best = at;
            }
        }
        return bestMatches > 0 ? best : null;
    }

    // Registra que recorre el Json y registra atributos y tipos antes de guardar valores
    private void preScanAndRegisterTypes(Map<String, Object> jsonMap) {
        if (jsonMap == null)
            return;
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            preScanNode(entry.getKey(), entry.getValue());
        }
    }

    // Analiza un nodo y crea su Attribute si no existe
    private void preScanNode(String name, Object value) {
        // Si es un mapa llama recursivamente a sus campos
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> e : map.entrySet())
                preScanNode(e.getKey(), e.getValue());
            // Si es una lista, registra atributo como lista (ensureAttributeForList) y procesar elementos
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;

            ensureAttributeForList(name, list);

            for (Object item : list) {
                if (item instanceof Map) {
                    Map<String, Object> mapItem = (Map<String, Object>) item;
                    for (Map.Entry<String, Object> e : mapItem.entrySet())
                        preScanNode(e.getKey(), e.getValue());
                }
            }
        } else {
            ensureAttributeForPrimitive(name, value);
        }
    }

    // Asegurar que un Attribute para la lista exista y tenga el tipo correcto
    private void ensureAttributeForList(String name, List<?> listValue) {
        try {
            Optional<Attribute> existing = attributeRepository.findByName(name);
            if (existing.isPresent()) {
                Attribute attr = existing.get();

                // si ya es enum-list, nada que hacer
                if (attr.getAttributeType() != null && Boolean.TRUE.equals(attr.getAttributeType().getIsEnum())
                        && Boolean.TRUE.equals(attr.getAttributeType().getIsList()))
                    return;

                // inferir enum por contenido
                AttributeType inferred = inferEnumTypeForList(name, listValue);
                if (inferred != null) {
                    AttributeType listEnum;

                    // si el enum existente no es lista, crear un nuevo AttributeType para lista
                    if (!Boolean.TRUE.equals(inferred.getIsList())) {
                        listEnum = attributeTypeService.findOrCreateListEnumType(inferred);
                    } else {
                        listEnum = inferred;
                    }

                    attr.setAttributeType(listEnum);
                    attributeRepository.save(attr);
                }
                return;
            }

            // si no existe -> crear atributo con determineAttributeType (ya manejará
            // isList)
            Attribute attr = new Attribute();
            attr.setName(name);
            AttributeType at = determineAttributeType(listValue, name);
            attr.setAttributeType(at);
            attributeRepository.save(attr);
        } catch (Exception ignored) {
        }
    }

    //Asegurar que un Attribute para un valor primitivo exista y tenga el tipo correcto
    private void ensureAttributeForPrimitive(String name, Object value) {
        try {
            Optional<Attribute> existing = attributeRepository.findByName(name);
            if (existing.isPresent()) {
                Attribute attr = existing.get();
                if (attr.getAttributeType() != null && Boolean.TRUE.equals(attr.getAttributeType().getIsEnum()))
                    return;
                String valStr = value != null ? value.toString() : null;
                if (valStr != null && !valStr.isEmpty()) {
                    List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
                    if (enumTypes != null) {
                        for (AttributeType at : enumTypes) {
                            if (attributeTypeService.findClosestAllowedValue(at, valStr) != null) {
                                attr.setAttributeType(at);
                                attributeRepository.save(attr);
                                return;
                            }
                        }
                    }
                }
                return;
            }

            // not existing -> create attribute (determineAttributeType will infer enum by
            // content)
            Attribute attr = new Attribute();
            attr.setName(name);
            AttributeType at = determineAttributeType(value, name);
            attr.setAttributeType(at);
            attributeRepository.save(attr);
        } catch (Exception ignored) {
        }
    }

    // Método gestiona la persistencia real y la validación
    private void processJsonNode(String attributeName, Object value, Long parentId) {

        log.debug("Procesando nodo '{}', parentId={}", attributeName, parentId);
        
        Attribute attr = getOrCreateAttribute(attributeName, value);
        if (attr == null)
            return;

        Config config = new Config();
        config.setAttribute(attr);
        if (parentId != null) {
            Optional<Config> parent = configRepository.findById(parentId);
            parent.ifPresent(config::setParent);
        }
        if (value instanceof Map) {
            log.debug("Nodo '{}' detectado como MAP. Reemplazo completo. parentId={}", attributeName, parentId);

            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);

            Map<String, Object> mapValue = (Map<String, Object>) value;

            // BORRAR hijos existentes (recursivo) Full Replace
            List<Config> existingChildren = configRepository.findByParentIdOrderByIdAsc(savedConfig.getId());

            if (!existingChildren.isEmpty()) {
                for (Config child : existingChildren) {
                    log.debug("Eliminando recursivamente hijo antiguo '{}' (id={}) de '{}'",
                            child.getAttribute() != null ? child.getAttribute().getName() : "unknown",
                            child.getId(),
                            attributeName);

                    deleteConfigRecursively(child);  // <----- BORRADO EN CASCADA REAL
                }
            }

            // Insertar nuevos hijos del JSON
            for (Map.Entry<String, Object> entry : mapValue.entrySet()) {
                log.debug("Insertando nuevo hijo '{}' dentro de '{}'", entry.getKey(), attributeName);
                processJsonNode(entry.getKey(), entry.getValue(), savedConfig.getId());
            }
        } else if (value instanceof List) {
            log.debug("Nodo '{}' detectado como LIST de {} elementos", attributeName, ((List<?>) value).size());
            config.setDefaultValue(null);
            Config savedConfig = saveOrGetConfig(config);

            List<?> listValue = (List<?>) value;

            // FULL-REPLACE: borrar todos los hijos anteriores
            List<Config> existingChildren = configRepository.findByParentIdOrderByIdAsc(savedConfig.getId());
            if (!existingChildren.isEmpty()) {
                configRepository.deleteAll(existingChildren);
            }
            // Si el atributo aún no está marcado como enum, intentar inferir un
            // AttributeType enum
            // a partir del contenido de la lista.
            if (attr.getAttributeType() == null || !Boolean.TRUE.equals(attr.getAttributeType().getIsEnum())) {
                AttributeType inferred = inferEnumTypeForList(attributeName, listValue);
                if (inferred != null) {
                    AttributeType listEnumType = inferred;
                    // Asegurarse de que isList=true para este enum
                    if (!Boolean.TRUE.equals(listEnumType.getIsList())) {
                        // No modificar el 'inferred' original, clonarlo como lista
                        listEnumType = attributeTypeService.findOrCreateListEnumType(inferred);
                    }
                    attr.setAttributeType(listEnumType);
                    attributeRepository.save(attr);
                }
            }
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
                    AttributeType listAttributeType = attr.getAttributeType();
                    if (listAttributeType != null && Boolean.TRUE.equals(listAttributeType.getIsEnum())) {
                        // Para validar, necesitamos el AttributeType base (no-lista) del enum.
                        // El tipo base tiene el mismo 'type' pero con isList=false.
                        AttributeType baseEnumType = attributeTypeRepository // findByTypeIgnoreCaseAndIsListAndIsEnum
                                .findByTypeAndIsListAndIsEnum(listAttributeType.getType(), false, true)
                                .orElse(listAttributeType); // Fallback al tipo de la lista si no se encuentra el base

                        String mappedValue = attributeTypeService.findClosestAllowedValue(baseEnumType, itemValue);

                        if (mappedValue != null) {
                            // Valor válido → guardamos el permitido
                            itemConfig.setDefaultValue(mappedValue);
                        } else {
                            // Valor inválido → rechazamos la importación con 400 e incluimos valores permitidos
                            throw new InvalidEnumValueException(attributeName, itemValue,
                                    attributeTypeService.getAllowedValues(baseEnumType));
                        }
                    } else {
                        itemConfig.setDefaultValue(itemValue);
                    }

                    saveOrGetConfig(itemConfig);
                }
            }
        } else {
            log.debug("Nodo '{}' detectado como valor primitivo: {}", attributeName, value);
            String primitiveValue = value != null ? value.toString() : "";

            // Si el attribute no está marcado como enum, intentar inferir por contenido
            if ((attr.getAttributeType() == null || !Boolean.TRUE.equals(attr.getAttributeType().getIsEnum()))
                    && primitiveValue != null && !primitiveValue.isEmpty()) {
                List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
                if (enumTypes != null && !enumTypes.isEmpty()) {
                    for (AttributeType at : enumTypes) {
                        String mapped = attributeTypeService.findClosestAllowedValue(at, primitiveValue);
                        if (mapped != null) {
                            // asociar como enum (no-list) y usarlo
                            at.setIsList(false);
                            attr.setAttributeType(at);
                            attributeRepository.save(attr);
                            break;
                        }
                    }
                }
            }

            // Mapear valor permitido si es enum
            if (attr.getAttributeType() != null && Boolean.TRUE.equals(attr.getAttributeType().getIsEnum())) {
                String mappedValue = attributeTypeService.findClosestAllowedValue(attr.getAttributeType(),
                        primitiveValue);
                if (mappedValue != null) {
                    config.setDefaultValue(mappedValue);
                } else {
                    // Valor no permitido: rechazamos la importación con 400 e incluimos valores
                    // permitidos
                    List<String> valoresPermitidosEnum = attributeTypeService.getAllowedValues(attr.getAttributeType());
                    throw new InvalidEnumValueException(attributeName, primitiveValue, valoresPermitidosEnum);
                }
            } else {
                config.setDefaultValue(primitiveValue);
            }

            saveOrGetConfig(config);
        }
    }

    // Guardar nuevos registros de Config o devolver los existentes si ya existe
    private Config saveOrGetConfig(Config cfg) {
        Long attributeId = cfg.getAttribute() != null ? cfg.getAttribute().getId() : null;
        Long parentId = cfg.getParent() != null ? cfg.getParent().getId() : null;

        // Si es un elemento de List y tiene parent => siempre crear nuevo
        try {
            boolean cfgIsListAttribute = cfg.getAttribute() != null
                    && cfg.getAttribute().getAttributeType() != null
                    && Boolean.TRUE.equals(cfg.getAttribute().getAttributeType().getIsList());

            if (cfgIsListAttribute && parentId != null) {
                log.debug(
                        "Guardando nuevo elemento de lista para attributeId={} parentId={} valor='{}'",
                        attributeId,
                        parentId,
                        cfg.getDefaultValue()
                );

                return configRepository.save(cfg);
            }
        } catch (Exception ex) {
            // por seguridad: si algo falla en comprobación de tipo, caemos al flujo normal
            log.warn("No se pudo determinar si el atributo es lista; se intentará el flujo normal. Error: {}", ex.getMessage());
        }

        // Flujo normal, buscar sibling por parent y sobrescribir PRIMITIVO existente si procede
        List<Config> siblings = parentId != null
                ? configRepository.findByParentIdOrderByIdAsc(parentId)
                : configRepository.findByParentIsNull();

        for (Config c : siblings) {
            if (c.getAttribute() != null && c.getAttribute().getId().equals(attributeId)) {

                boolean esLista = c.getAttribute().getAttributeType() != null &&
                        Boolean.TRUE.equals(c.getAttribute().getAttributeType().getIsList());

                if (!esLista) {
                    // PRIMITIVO → sobrescribir valor
                    log.debug("Sobrescribiendo valor del config id={} (attributeId={}) con valor='{}'",
                            c.getId(), attributeId, cfg.getDefaultValue());
                    c.setDefaultValue(cfg.getDefaultValue());
                    return configRepository.save(c);
                }

                // Si llegamos aquí y isList==true y parent==null, devolvemos el nodo (caso raíz lista)
                // pero si es lista y parent==null, probablemente sea el "root" que representa la lista; devolverlo para uso por processJsonNode
                return c;
            }
        }

        // No existe → crear uno nuevo
        log.debug("Creando nuevo config para attributeId={} parentId={}", attributeId, parentId);
        return configRepository.save(cfg);
    }

    // Obtener un Attribute existente o crearlo si no existe
    private Attribute getOrCreateAttribute(String name, Object value) {
        log.debug("Obteniendo/creando atributo '{}'", name);
        Optional<Attribute> existing = attributeRepository.findByName(name);
        if (existing.isPresent()) {
            Attribute attr = existing.get();
            // Si el attribute ya existe, intentar forzar la asociación al AttributeType
            // enum
            // cuando exista uno con el mismo nombre (case-insensitive).
            try {
                boolean valIsList = value instanceof List;
                Optional<AttributeType> maybeType = attributeTypeRepository.findByTypeAndIsListAndIsEnum(name,
                        valIsList, true);
                if (maybeType.isPresent() && value instanceof List) {
                    AttributeType enumType = maybeType.get();
                    // Sobrescribe la asociación si el attribute no tenía tipo o no era enum
                    if (attr.getAttributeType() == null || !Boolean.TRUE.equals(attr.getAttributeType().getIsEnum())) {
                        attr.setAttributeType(enumType);
                        attributeRepository.save(attr);
                    }
                }
                // Si el valor es primitivo y el atributo actual no es enum, intentar inferir
                // por contenido
                else if (!(value instanceof Map) && !(value instanceof List) && (attr.getAttributeType() == null
                        || !Boolean.TRUE.equals(attr.getAttributeType().getIsEnum()))) {
                    String valStr = value != null ? value.toString() : null;
                    if (valStr != null && !valStr.isEmpty()) {
                        List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
                        if (enumTypes != null) {
                            for (AttributeType at : enumTypes) {
                                if (attributeTypeService.findClosestAllowedValue(at, valStr) != null) {
                                    attr.setAttributeType(at);
                                    attributeRepository.save(attr);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return attr;
        }

        Attribute attr = new Attribute();
        attr.setName(name);

        AttributeType attrType = determineAttributeType(value, name);
        attr.setAttributeType(attrType);

        return attributeRepository.save(attr);
    }

    // Determina el tipo de un atributo ( NODO , STRING, NUMERIC, BOOLEAN)
    private AttributeType determineAttributeType(Object value, String attributeName) {
        String typeStr;
        boolean isList = false;
        boolean isEnum = false;

        if (value instanceof Map)
            typeStr = "NODE";
        else if (value instanceof List) {
            isList = true;
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map)
                    typeStr = "NODE";
                else if (first instanceof Number)
                    typeStr = "NUMERIC";
                else if (first instanceof Boolean)
                    typeStr = "BOOLEAN";
                else
                    typeStr = "STRING";
            } else
                typeStr = "STRING";
        } else if (value instanceof Boolean)
            typeStr = "BOOLEAN";
        else if (value instanceof Number)
            typeStr = "NUMERIC";
        else
            typeStr = "STRING";

        // Buscar si ya existe un AttributeType enum con el flag isList correcto
        Optional<AttributeType> enumType = attributeTypeRepository.findByTypeAndIsListAndIsEnum(attributeName, isList,
                true);
        if (enumType.isPresent())
            return enumType.get();

        // Si es un valor primitivo (no lista ni nodo), intentar inferir por contenido
        if (!isList && !(value instanceof Map) && value != null) {
            String valStr = value.toString();
            try {
                List<AttributeType> enumTypes = attributeTypeRepository.findByIsEnum(true);
                if (enumTypes != null && !enumTypes.isEmpty()) {
                    for (AttributeType at : enumTypes) {
                        if (attributeTypeService.findClosestAllowedValue(at, valStr) != null) {
                            return at; // asociar al enum que acepta el valor
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Optional<AttributeType> existing = attributeTypeRepository.findByTypeAndIsListAndIsEnum(typeStr, isList, false);
        if (existing.isPresent())
            return existing.get();

        AttributeType type = new AttributeType();
        type.setType(typeStr);
        type.setIsList(isList);
        type.setIsEnum(isEnum);

        return attributeTypeRepository.save(type);
    }

    // Método para que los datos en BBDD y generar un JSON
    public String exportToJson() {
        log.info("Iniciando exportación a JSON...");
        List<Config> rootConfigs = configRepository.findByParentIsNull();
        log.info("Nodos raíz encontrados: {}", rootConfigs.size());
        Map<String, Object> result = new LinkedHashMap<>();

        for (Config config : rootConfigs) {
            if (config.getAttribute() != null) {
                String attrName = config.getAttribute().getName();
                log.debug("[ROOT] Exportando nodo raíz '{}'", attrName);
                Object value = buildJsonValue(config);
                result.put(attrName, value);
            } else {
                log.warn("Nodo raíz sin atributo asociado, id={}", config.getId());
            }
        }

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            log.info("Exportación finalizada. Longitud del JSON: {} caracteres", json.length());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error generando el JSON" , e);
            return "{}";
        }
    }

    // Recontruye el valor de config como Object
    private Object buildJsonValue(Config config) {
        List<Config> children = configRepository.findByParentIdOrderByIdAsc(config.getId());
        AttributeType attrType = config.getAttribute() != null ? config.getAttribute().getAttributeType() : null;

        // MANEJO DE LISTAS
        if (attrType != null && Boolean.TRUE.equals(attrType.getIsList())) {

            List<Object> list = new java.util.ArrayList<>();

            for (Config child : children) {
                String childValue = child.getDefaultValue();
                if (childValue == null || childValue.isEmpty())
                    continue;

                // ENUM LIST
                if (Boolean.TRUE.equals(attrType.getIsEnum())) {

                    // Buscar el tipo base del enum (no-list)
                    AttributeType baseEnumType = attributeTypeRepository // findByTypeIgnoreCaseAndIsListAndIsEnum
                            .findByTypeAndIsListAndIsEnum(attrType.getType(), false, true)
                            .orElse(attrType);

                    String allowedValue = attributeTypeService.findClosestAllowedValue(baseEnumType, childValue);
                    if (allowedValue != null)
                        list.add(allowedValue);

                } else {
                    // LISTA NUMÉRICA / BOOLEAN / STRING
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

        // VALOR PRIMITIVO (NO LISTA)
        if (children.isEmpty()) {
            String value = config.getDefaultValue();
            if (value == null || value.isEmpty())
                return null;

            // ENUM → devolver el valor real
            if (attrType != null && Boolean.TRUE.equals(attrType.getIsEnum())) {
                String allowedValue = attributeTypeService.findClosestAllowedValue(attrType, value);
                if (allowedValue != null)
                    return allowedValue;
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

        // OBJETO / NODE
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

    // Método para parsear los numeros (int, Long y Double)
    private Object parseNumeros(String value) {
        if (value == null)
            return null;
        try {
            // Entero puro: -?123
            if (value.matches("^-?\\d+$")) {
                long longValue = Long.parseLong(value);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            }

            // Decimal: 1.23 -> Double
            return Double.parseDouble(value);

        } catch (Exception e) {
            // fallback: devolvemos el String original si no es número
            return value;
        }
    }

    // Borra un nodo Config y todos sus hijos recursivamente.
    private void deleteConfigRecursively(Config config) {
        List<Config> children = configRepository.findByParentIdOrderByIdAsc(config.getId());

        for (Config child : children) {
            deleteConfigRecursively(child); // borrar primero los hijos
        }

        log.debug("Borrando nodo config id={} (attribute={})",
                config.getId(),
                config.getAttribute() != null ? config.getAttribute().getName() : "null");

        configRepository.delete(config);
    }


}