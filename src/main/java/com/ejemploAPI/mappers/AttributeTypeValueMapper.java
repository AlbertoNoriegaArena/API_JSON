package com.ejemploAPI.mappers;

import com.ejemploAPI.dtos.AttributeTypeValueDTO;
import com.ejemploAPI.models.AttributeTypeValue;

public class AttributeTypeValueMapper {

    public static AttributeTypeValueDTO toDTO(AttributeTypeValue entity) {
        AttributeTypeValueDTO dto = new AttributeTypeValueDTO();
        dto.setId(entity.getId());
        dto.setValue(entity.getValue());

        if (entity.getAttributeType() != null)
            dto.setAttributeTypeId(entity.getAttributeType().getId());

        return dto;
    }

    public static AttributeTypeValue toEntity(AttributeTypeValueDTO dto) {
        AttributeTypeValue entity = new AttributeTypeValue();
        entity.setId(dto.getId());
        entity.setValue(dto.getValue());
        return entity;
    }
}
