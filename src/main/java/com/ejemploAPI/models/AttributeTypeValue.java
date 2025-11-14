package com.ejemploAPI.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "attribute_type_value")
public class AttributeTypeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attribute_type_id")
    private AttributeType attributeType;

    private String value; // valor permitido para el enum

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AttributeType getAttributeType() { return attributeType; }
    public void setAttributeType(AttributeType attributeType) { this.attributeType = attributeType; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}