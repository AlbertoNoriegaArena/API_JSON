package com.ejemploAPI.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "config")
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String defaultValue;

    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "attribute_id")
    private Attribute attribute;

    @ManyToOne
    @JoinColumn(name = "parent")
    private Config parent;

    private String applicationNode; 
    private Boolean isCustom; 

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Config getParent() {
        return parent;
    }

    public void setParent(Config parent) {
        this.parent = parent;
    }

    public String getApplicationNode() {
        return applicationNode;
    }

    public void setApplicationNode(String applicationNode) {
        this.applicationNode = applicationNode;
    }

    public Boolean getIsCustom() {
        return isCustom;
    }

    public void setIsCustom(Boolean custom) {
        isCustom = custom;
    }
}