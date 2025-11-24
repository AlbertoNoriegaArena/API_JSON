package com.ejemploAPI.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attribute_type_value")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeTypeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attribute_type_id")
    private AttributeType attributeType;

    @Column(name = "`value`")
    private String value; // valor permitido para el enum

}