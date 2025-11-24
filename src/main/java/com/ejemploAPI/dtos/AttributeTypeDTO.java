package com.ejemploAPI.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AttributeTypeDTO {

    private Long id;
    private Boolean isEnum;
    private Boolean isList;
    private String type;

}
