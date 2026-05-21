package com.viniciuscecatto.servicoauditordlq.application.service;

import com.viniciuscecatto.servicoauditordlq.application.command.RegistrarErroDlqCommand;
import com.viniciuscecatto.servicoauditordlq.application.ports.out.RegistroAuditoriaRepositoryPort;
import com.viniciuscecatto.servicoauditordlq.domain.model.EventoPedidoFalho;
import com.viniciuscecatto.servicoauditordlq.domain.model.ItemPedidoFalho;
import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;
import com.viniciuscecatto.servicoauditordlq.domain.model.SeveridadeErro;
import com.viniciuscecatto.servicoauditordlq.domain.model.StatusAnalise;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrarErroDlqServiceTest {

    @Test
    void devePersistirRegistroComSeveridadeCalculada() {
        RegistroAuditoriaRepositoryPort repositoryPort = mock(RegistroAuditoriaRepositoryPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-07T14:30:00Z"), ZoneOffset.UTC);
        RegistrarErroDlqService service = new RegistrarErroDlqService(repositoryPort, clock);

        EventoPedidoFalho evento = new EventoPedidoFalho(
                "80010000",
                1,
                List.of(new ItemPedidoFalho(1, 70), new ItemPedidoFalho(2, 40)),
                "SQS_QUEUE",
                Instant.parse("2024-05-20T14:30:00Z")
        );

        when(repositoryPort.salvar(any(RegistroAuditoriaErro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistroAuditoriaErro registro = service.registrar(
                new RegistrarErroDlqCommand(
                        "T04N_VINICIUS_CECATTO-DLQ.fifo",
                        "{\"zipCode\":\"80010000\"}",
                        "errorMessage=Falha no banco",
                        evento
                )
        );

        assertEquals("T04N_VINICIUS_CECATTO-DLQ.fifo", registro.queueName());
        assertEquals(StatusAnalise.PENDING_ANALYSIS, registro.status());
        assertEquals(SeveridadeErro.HIGH, registro.severity());
        assertEquals("errorMessage=Falha no banco", registro.failureReason());
        assertEquals(Instant.parse("2026-05-07T14:30:00Z"), registro.timestamp());
    }

    @Test
    void deveMarcarComoHighQuandoNaoForPossivelTriarAutomaticamente() {
        RegistroAuditoriaRepositoryPort repositoryPort = mock(RegistroAuditoriaRepositoryPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-07T14:30:00Z"), ZoneOffset.UTC);
        RegistrarErroDlqService service = new RegistrarErroDlqService(repositoryPort, clock);

        when(repositoryPort.salvar(any(RegistroAuditoriaErro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistroAuditoriaErro registro = service.registrar(
                new RegistrarErroDlqCommand(
                        "T04N_VINICIUS_CECATTO-DLQ.fifo",
                        "payload invalido",
                        "payload quebrado",
                        null
                )
        );

        assertEquals(SeveridadeErro.HIGH, registro.severity());
    }
}
