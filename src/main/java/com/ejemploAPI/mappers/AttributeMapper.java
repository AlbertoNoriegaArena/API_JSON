package com.ejemploAPI.mappers;

import com.ejemploAPI.dtos.AttributeDTO;
import com.ejemploAPI.models.Attribute;
import com.ejemploAPI.models.AttributeType;

public class AttributeMapper {

    public static AttributeDTO toDTO(Attribute entity) {
        AttributeDTO dto = new AttributeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());

        if (entity.getAttributeType() != null)
            dto.setAttributeTypeId(entity.getAttributeType().getId());

        return dto;
    }

    // DTO -> Entity (nuevo método que recibe AttributeType)
    public static Attribute toEntity(AttributeDTO dto, AttributeType type) {
        Attribute entity = new Attribute();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setAttributeType(type); // asigna el AttributeType si existe
        return entity;
    }

    // Para actualizar una entidad existente con DTO
    public static void updateEntity(Attribute entity, AttributeDTO dto, AttributeType type) {
        entity.setName(dto.getName());
        if (type != null) {
            entity.setAttributeType(type);
        }
    }
}
