package com.ejemploAPI.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AttributeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean isEnum;
    private Boolean isList;

    private String type;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Boolean getIsEnum() { return isEnum; }
    public void setIsEnum(Boolean isEnum) { this.isEnum = isEnum; }

    public Boolean getIsList() { return isList; }
    public void setIsList(Boolean isList) { this.isList = isList; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

