package com.ejemploAPI.mappers;

import com.ejemploAPI.dtos.AttributeTypeDTO;
import com.ejemploAPI.models.AttributeType;

public class AttributeTypeMapper {

    public static AttributeTypeDTO toDTO(AttributeType entity) {
        AttributeTypeDTO dto = new AttributeTypeDTO();
        dto.setId(entity.getId());
        dto.setIsEnum(entity.getIsEnum());
        dto.setIsList(entity.getIsList());
        dto.setType(entity.getType());
        return dto;
    }

    public static AttributeType toEntity(AttributeTypeDTO dto) {
        AttributeType entity = new AttributeType();
        entity.setId(dto.getId());
        entity.setIsEnum(dto.getIsEnum());
        entity.setIsList(dto.getIsList());
        entity.setType(dto.getType());
        return entity;
    }
}
