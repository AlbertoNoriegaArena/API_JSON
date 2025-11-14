package com.ejemploAPI.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; 

    @ManyToOne
    @JoinColumn(name = "attribute_type_id")
    private AttributeType attributeType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AttributeType getAttributeType() { return attributeType; }
    public void setAttributeType(AttributeType attributeType) { this.attributeType = attributeType; }
}

