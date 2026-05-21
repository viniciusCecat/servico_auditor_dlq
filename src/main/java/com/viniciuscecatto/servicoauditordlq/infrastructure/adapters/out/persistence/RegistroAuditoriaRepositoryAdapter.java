package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence;

import com.viniciuscecatto.servicoauditordlq.application.ports.out.RegistroAuditoriaRepositoryPort;
import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;
import com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence.jpa.RegistroAuditoriaJpaRepository;
import com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence.mapper.RegistroAuditoriaEntityMapper;
import org.springframework.stereotype.Component;

@Component
public class RegistroAuditoriaRepositoryAdapter implements RegistroAuditoriaRepositoryPort {

    private final RegistroAuditoriaJpaRepository registroAuditoriaJpaRepository;
    private final RegistroAuditoriaEntityMapper registroAuditoriaEntityMapper;

    public RegistroAuditoriaRepositoryAdapter(
            RegistroAuditoriaJpaRepository registroAuditoriaJpaRepository,
            RegistroAuditoriaEntityMapper registroAuditoriaEntityMapper
    ) {
        this.registroAuditoriaJpaRepository = registroAuditoriaJpaRepository;
        this.registroAuditoriaEntityMapper = registroAuditoriaEntityMapper;
    }

    @Override
    public RegistroAuditoriaErro salvar(RegistroAuditoriaErro registroAuditoriaErro) {
        return registroAuditoriaEntityMapper.toDomain(
                registroAuditoriaJpaRepository.save(registroAuditoriaEntityMapper.toEntity(registroAuditoriaErro))
        );
    }
}
