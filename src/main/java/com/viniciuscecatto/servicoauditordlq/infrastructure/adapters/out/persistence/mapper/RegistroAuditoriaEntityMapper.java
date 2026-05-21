package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence.mapper;

import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;
import com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence.entity.RegistroAuditoriaEntity;
import org.springframework.stereotype.Component;

@Component
public class RegistroAuditoriaEntityMapper {

    public RegistroAuditoriaEntity toEntity(RegistroAuditoriaErro domain) {
        RegistroAuditoriaEntity entity = new RegistroAuditoriaEntity();
        entity.setErrorId(domain.errorId());
        entity.setQueueName(domain.queueName());
        entity.setPayload(domain.payload());
        entity.setTimestamp(domain.timestamp());
        entity.setStatus(domain.status());
        entity.setSeverity(domain.severity());
        entity.setFailureReason(domain.failureReason());
        return entity;
    }

    public RegistroAuditoriaErro toDomain(RegistroAuditoriaEntity entity) {
        return new RegistroAuditoriaErro(
                entity.getErrorId(),
                entity.getQueueName(),
                entity.getPayload(),
                entity.getTimestamp(),
                entity.getStatus(),
                entity.getSeverity(),
                entity.getFailureReason()
        );
    }
}
