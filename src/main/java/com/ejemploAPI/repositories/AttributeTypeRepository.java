package com.ejemploAPI.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ejemploAPI.models.AttributeType;

@Repository
public interface AttributeTypeRepository extends JpaRepository<AttributeType, Long> {}
