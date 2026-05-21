package com.viniciuscecatto.servicoauditordlq.domain.model;

public record ItemPedidoFalho(
        Integer sku,
        Integer amount
) {

    public int quantidade() {
        return amount == null ? 0 : amount;
    }
}
