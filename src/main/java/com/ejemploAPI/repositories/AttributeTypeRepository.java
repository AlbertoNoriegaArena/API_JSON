package com.ejemploAPI.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ejemploAPI.models.AttributeType;
import java.util.Optional;

@Repository
public interface AttributeTypeRepository extends JpaRepository<AttributeType, Long> {
	Optional<AttributeType> findByTypeAndIsListAndIsEnum(String type, Boolean isList, Boolean isEnum);

	// Case-insensitive lookup useful to match JSON attribute names to enum types
	Optional<AttributeType> findByTypeIgnoreCaseAndIsListAndIsEnum(String type, Boolean isList, Boolean isEnum);
	Optional<AttributeType> findByTypeIgnoreCase(String type);
}
