package com.viniciuscecatto.servicoauditordlq.application.ports.out;

import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;

public interface RegistroAuditoriaRepositoryPort {

    RegistroAuditoriaErro salvar(RegistroAuditoriaErro registroAuditoriaErro);
}
