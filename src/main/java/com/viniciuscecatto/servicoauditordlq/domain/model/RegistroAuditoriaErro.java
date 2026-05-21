package com.viniciuscecatto.servicoauditordlq.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RegistroAuditoriaErro(
        UUID errorId,
        String queueName,
        String payload,
        Instant timestamp,
        StatusAnalise status,
        SeveridadeErro severity,
        String failureReason
) {

    public static RegistroAuditoriaErro novo(
            String queueName,
            String payload,
            SeveridadeErro severity,
            String failureReason,
            Instant timestamp
    ) {
        Objects.requireNonNull(severity, "A severidade do erro e obrigatoria.");
        Objects.requireNonNull(timestamp, "O timestamp da auditoria e obrigatorio.");

        return new RegistroAuditoriaErro(
                UUID.randomUUID(),
                Objects.requireNonNull(queueName, "O nome da fila e obrigatorio."),
                Objects.requireNonNull(payload, "O payload bruto e obrigatorio."),
                timestamp,
                StatusAnalise.PENDING_ANALYSIS,
                severity,
                failureReason == null || failureReason.isBlank()
                        ? "Motivo original indisponivel na mensagem da DLQ."
                        : failureReason
        );
    }
}
