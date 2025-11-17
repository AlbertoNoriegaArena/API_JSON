package com.ejemploAPI.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ejemploAPI.models.Config;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
	List<Config> findByParentIsNull();
	List<Config> findByParentIdOrderByIdAsc(Long parentId);
	Optional<Config> findFirstByAttributeIdAndParentIdAndDefaultValue(Long attributeId, Long parentId, String defaultValue);
}
