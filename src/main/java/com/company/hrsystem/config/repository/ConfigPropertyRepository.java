package com.company.hrsystem.config.repository;

import com.company.hrsystem.config.entity.ConfigProperty;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigPropertyRepository extends JpaRepository<ConfigProperty, UUID> {
}
