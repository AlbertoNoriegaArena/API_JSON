package com.ejemploAPI.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ejemploAPI.models.AttributeType;
import java.util.Optional;

@Repository
public interface AttributeTypeRepository extends JpaRepository<AttributeType, Long> {
	Optional<AttributeType> findByTypeAndIsListAndIsEnum(String type, Boolean isList, Boolean isEnum);
}
