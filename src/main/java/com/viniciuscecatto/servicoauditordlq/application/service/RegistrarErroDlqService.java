package com.viniciuscecatto.servicoauditordlq.application.service;

import com.viniciuscecatto.servicoauditordlq.application.command.RegistrarErroDlqCommand;
import com.viniciuscecatto.servicoauditordlq.application.ports.in.RegistrarErroDlqUseCase;
import com.viniciuscecatto.servicoauditordlq.application.ports.out.RegistroAuditoriaRepositoryPort;
import com.viniciuscecatto.servicoauditordlq.domain.model.EventoPedidoFalho;
import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;
import com.viniciuscecatto.servicoauditordlq.domain.model.SeveridadeErro;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class RegistrarErroDlqService implements RegistrarErroDlqUseCase {

    private final RegistroAuditoriaRepositoryPort registroAuditoriaRepositoryPort;
    private final Clock clock;

    public RegistrarErroDlqService(
            RegistroAuditoriaRepositoryPort registroAuditoriaRepositoryPort,
            Clock clock
    ) {
        this.registroAuditoriaRepositoryPort = registroAuditoriaRepositoryPort;
        this.clock = clock;
    }

    @Override
    public RegistroAuditoriaErro registrar(RegistrarErroDlqCommand command) {
        final RegistroAuditoriaErro registro = RegistroAuditoriaErro.novo(
                command.queueName(),
                command.payload(),
                determinarSeveridade(command.eventoPedidoFalho()),
                command.failureReason(),
                clock.instant()
        );
        return registroAuditoriaRepositoryPort.salvar(registro);
    }

    private SeveridadeErro determinarSeveridade(EventoPedidoFalho eventoPedidoFalho) {
        if (eventoPedidoFalho == null) {
            return SeveridadeErro.HIGH;
        }
        return SeveridadeErro.aPartirDaQuantidadeTotal(eventoPedidoFalho.quantidadeTotalProdutos());
    }
}
