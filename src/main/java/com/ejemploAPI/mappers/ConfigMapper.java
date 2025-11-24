package com.ejemploAPI.mappers;

import com.ejemploAPI.dtos.ConfigDTO;
import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.Config;
import org.springframework.beans.BeanUtils;

public class ConfigMapper {

    public static ConfigDTO toDTO(Config entity) {
        ConfigDTO dto = new ConfigDTO();
        dto.setId(entity.getId());
        dto.setDefaultValue(entity.getDefaultValue());
        dto.setDescripcion(entity.getDescripcion());
        dto.setApplicationNode(entity.getApplicationNode());
        dto.setIsCustom(entity.getIsCustom());

        if (entity.getAttribute() != null)
            dto.setAttributeId(entity.getAttribute().getId());

        if (entity.getParent() != null)
            dto.setParentId(entity.getParent().getId());

        return dto;
    }

    public static Config toEntity(ConfigDTO dto) {
        Config entity = new Config();
        entity.setId(dto.getId());
        entity.setDefaultValue(dto.getDefaultValue());
        entity.setDescripcion(dto.getDescripcion());
        entity.setApplicationNode(dto.getApplicationNode());
        entity.setIsCustom(dto.getIsCustom());
        return entity;
    }

    /**
     * Actualiza una entidad existente con los valores del DTO y relaciones.
     */
    public static void updateEntity(Config entity, ConfigDTO dto, Attribute attribute, Config parent) {
        // Copiamos campos simples usando BeanUtils
        BeanUtils.copyProperties(dto, entity, "id"); // no sobreescribimos el id

        // Asignamos relaciones
        entity.setAttribute(attribute);
        entity.setParent(parent);
    }

    /**
     * Método útil para crear entity con relaciones (POST)
     */
    public static Config toEntity(ConfigDTO dto, Attribute attribute, Config parent) {
        Config entity = toEntity(dto);
        entity.setAttribute(attribute);
        entity.setParent(parent);
        return entity;
    }

}

