package com.viniciuscecatto.servicoauditordlq.application.command;

import com.viniciuscecatto.servicoauditordlq.domain.model.EventoPedidoFalho;

public record RegistrarErroDlqCommand(
        String queueName,
        String payload,
        String failureReason,
        EventoPedidoFalho eventoPedidoFalho
) {
}
