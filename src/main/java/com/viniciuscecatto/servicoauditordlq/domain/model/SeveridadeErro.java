package com.viniciuscecatto.servicoauditordlq.domain.model;

public enum SeveridadeErro {
    LOW,
    MEDIUM,
    HIGH;

    public static SeveridadeErro aPartirDaQuantidadeTotal(int quantidadeTotal) {
        if (quantidadeTotal > 100) {
            return HIGH;
        }
        if (quantidadeTotal >= 50) {
            return MEDIUM;
        }
        return LOW;
    }
}
