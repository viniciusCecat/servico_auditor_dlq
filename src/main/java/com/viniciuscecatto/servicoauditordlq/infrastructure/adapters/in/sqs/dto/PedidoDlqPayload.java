package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoDlqPayload(
        String zipCode,
        Integer customerId,
        List<PedidoDlqItemPayload> orderItems,
        String origin,
        Instant occurredAt
) {
}
