package com.ejemploAPI.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ejemploAPI.models.Attribute;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {}
