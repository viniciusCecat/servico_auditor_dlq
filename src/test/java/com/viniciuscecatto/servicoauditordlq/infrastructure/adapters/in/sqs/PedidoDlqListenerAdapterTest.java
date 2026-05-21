package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciuscecatto.servicoauditordlq.application.ports.in.RegistrarErroDlqUseCase;
import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;
import com.viniciuscecatto.servicoauditordlq.domain.model.SeveridadeErro;
import com.viniciuscecatto.servicoauditordlq.domain.model.StatusAnalise;
import com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs.mapper.PedidoDlqPayloadMapper;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedidoDlqListenerAdapterTest {

    @Test
    void deveReconhecerMensagemSomenteDepoisDaPersistencia() {
        RegistrarErroDlqUseCase useCase = mock(RegistrarErroDlqUseCase.class);
        PedidoDlqListenerAdapter adapter = new PedidoDlqListenerAdapter(
                useCase,
                new PedidoDlqPayloadMapper(new ObjectMapper()),
                "T04N_VINICIUS_CECATTO-DLQ.fifo"
        );

        Acknowledgement acknowledgement = mock(Acknowledgement.class);
        Message originalMessage = Message.builder()
                .messageId("msg-1")
                .attributesWithStrings(Map.of("ApproximateReceiveCount", "3"))
                .messageAttributes(Map.of(
                        "errorMessage",
                        MessageAttributeValue.builder().stringValue("Falha no pedido original").dataType("String").build()
                ))
                .build();

        when(useCase.registrar(any())).thenReturn(new RegistroAuditoriaErro(
                UUID.randomUUID(),
                "T04N_VINICIUS_CECATTO-DLQ.fifo",
                "{\"zipCode\":\"80010000\"}",
                Instant.parse("2026-05-07T14:30:00Z"),
                StatusAnalise.PENDING_ANALYSIS,
                SeveridadeErro.LOW,
                "errorMessage=Falha no pedido original"
        ));

        adapter.receberMensagem("""
                {
                  "zipCode": "80010000",
                  "customerId": 1,
                  "orderItems": [
                    { "sku": 1, "amount": 5 }
                  ],
                  "origin": "SQS_QUEUE",
                  "occurredAt": "2024-05-20T14:30:00Z"
                }
                """, acknowledgement, originalMessage);

        verify(useCase).registrar(any());
        verify(acknowledgement).acknowledge();
    }

    @Test
    void naoDeveReconhecerMensagemQuandoPersistenciaFalhar() {
        RegistrarErroDlqUseCase useCase = mock(RegistrarErroDlqUseCase.class);
        PedidoDlqListenerAdapter adapter = new PedidoDlqListenerAdapter(
                useCase,
                new PedidoDlqPayloadMapper(new ObjectMapper()),
                "T04N_VINICIUS_CECATTO-DLQ.fifo"
        );

        Acknowledgement acknowledgement = mock(Acknowledgement.class);
        Message originalMessage = Message.builder()
                .messageId("msg-2")
                .attributesWithStrings(Map.of("ApproximateReceiveCount", "3"))
                .build();

        doThrow(new IllegalStateException("falha ao salvar")).when(useCase).registrar(any());

        assertThrows(IllegalStateException.class, () -> adapter.receberMensagem("""
                {
                  "zipCode": "80010000",
                  "customerId": 1,
                  "orderItems": [
                    { "sku": 1, "amount": 5 }
                  ],
                  "origin": "SQS_QUEUE",
                  "occurredAt": "2024-05-20T14:30:00Z"
                }
                """, acknowledgement, originalMessage));
    }
}
