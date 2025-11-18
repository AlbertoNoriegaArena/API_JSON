package com.ejemploAPI.repositories;

import com.ejemploAPI.models.AttributeTypeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeTypeValueRepository extends JpaRepository<AttributeTypeValue, Long> {
    List<AttributeTypeValue> findByAttributeTypeId(Long attributeTypeId);
    Optional<AttributeTypeValue> findFirstByAttributeTypeIdAndValue(Long attributeTypeId, String value);
}
