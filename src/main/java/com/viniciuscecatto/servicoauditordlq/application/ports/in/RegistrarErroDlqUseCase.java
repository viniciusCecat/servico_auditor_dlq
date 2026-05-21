package com.viniciuscecatto.servicoauditordlq.application.ports.in;

import com.viniciuscecatto.servicoauditordlq.application.command.RegistrarErroDlqCommand;
import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;

public interface RegistrarErroDlqUseCase {

    RegistroAuditoriaErro registrar(RegistrarErroDlqCommand command);
}
