package com.ejemploAPI.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConfigDTO {

    private Long id;
    private String defaultValue;
    private String descripcion;

    private Long attributeId; 
    private Long parentId; 

    private String applicationNode;
    private Boolean isCustom;

}
