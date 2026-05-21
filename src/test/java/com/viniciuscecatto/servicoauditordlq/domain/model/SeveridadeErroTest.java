package com.viniciuscecatto.servicoauditordlq.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeveridadeErroTest {

    @Test
    void deveClassificarComoHighQuandoQuantidadeForMaiorQueCem() {
        assertEquals(SeveridadeErro.HIGH, SeveridadeErro.aPartirDaQuantidadeTotal(101));
    }

    @Test
    void deveClassificarComoMediumQuandoQuantidadeEstiverEntreCinquentaECem() {
        assertEquals(SeveridadeErro.MEDIUM, SeveridadeErro.aPartirDaQuantidadeTotal(50));
        assertEquals(SeveridadeErro.MEDIUM, SeveridadeErro.aPartirDaQuantidadeTotal(100));
    }

    @Test
    void deveClassificarComoLowQuandoQuantidadeForMenorQueCinquenta() {
        assertEquals(SeveridadeErro.LOW, SeveridadeErro.aPartirDaQuantidadeTotal(49));
    }
}
