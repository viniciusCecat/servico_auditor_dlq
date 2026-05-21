package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence.jpa;

import com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence.entity.RegistroAuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegistroAuditoriaJpaRepository extends JpaRepository<RegistroAuditoriaEntity, UUID> {
}
