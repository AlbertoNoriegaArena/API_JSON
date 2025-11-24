package com.ejemploAPI.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttributeDTO {

    private Long id;
    private String name;

    private Long attributeTypeId; // relación simplificada


}
