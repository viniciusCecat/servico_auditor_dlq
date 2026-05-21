package com.viniciuscecatto.servicoauditordlq.domain.model;

import java.time.Instant;
import java.util.List;

public record EventoPedidoFalho(
        String zipCode,
        Integer customerId,
        List<ItemPedidoFalho> orderItems,
        String origin,
        Instant occurredAt
) {

    public int quantidadeTotalProdutos() {
        return orderItems == null
                ? 0
                : orderItems.stream()
                .mapToInt(ItemPedidoFalho::quantidade)
                .sum();
    }
}
