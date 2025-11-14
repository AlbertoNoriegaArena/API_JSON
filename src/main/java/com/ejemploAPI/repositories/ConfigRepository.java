package com.ejemploAPI.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ejemploAPI.models.Config;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {}
