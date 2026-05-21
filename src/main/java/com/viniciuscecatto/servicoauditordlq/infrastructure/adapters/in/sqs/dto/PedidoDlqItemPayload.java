package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoDlqItemPayload(
        Integer sku,
        Integer amount
) {
}
