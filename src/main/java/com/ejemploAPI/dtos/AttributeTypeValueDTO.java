package com.ejemploAPI.dtos;

public class AttributeTypeValueDTO {

    private Long id;
    private Long attributeTypeId;
    private String value;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getAttributeTypeId() {
        return attributeTypeId;
    }
    public void setAttributeTypeId(Long attributeTypeId) {
        this.attributeTypeId = attributeTypeId;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    
}
