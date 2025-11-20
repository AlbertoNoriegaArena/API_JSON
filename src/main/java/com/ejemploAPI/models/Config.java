package com.ejemploAPI.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "Config{" +
                "id=" + id +
                ", defaultValue='" + defaultValue + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", attribute=" + attribute +
                ", parent=" + parent +
                ", applicationNode='" + applicationNode + '\'' +
                ", isCustom=" + isCustom +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Config config = (Config) o;
        return Objects.equals(id, config.id) && Objects.equals(defaultValue, config.defaultValue) && Objects.equals(descripcion, config.descripcion) && Objects.equals(attribute, config.attribute) && Objects.equals(parent, config.parent) && Objects.equals(applicationNode, config.applicationNode) && Objects.equals(isCustom, config.isCustom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, defaultValue, descripcion, attribute, parent, applicationNode, isCustom);
    }
}