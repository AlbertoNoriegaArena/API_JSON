package com.ejemploAPI.repositories;

import com.ejemploAPI.models.AttributeTypeValue;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AttributeTypeValueRepository extends JpaRepository<AttributeTypeValue, Long> {}
