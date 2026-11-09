package com.tccinfonet.cityreport.modules.relatorio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class RelatorioAnaliseServiceTest {

    @Test
    void calculaTempoDaAberturaAteAprovacao() {
        LocalDateTime abertura = LocalDateTime.of(2026, 11, 1, 8, 0);
        assertEquals(90 * 60L, RelatorioAnaliseService.segundosParaResolver(
                abertura, abertura.plusMinutes(90)));
        assertEquals("1 h 30 min", RelatorioAnaliseService.formatarDuracao(5400L));
        assertEquals("2 d 3 h", RelatorioAnaliseService.formatarDuracao(183600L));
    }

    @Test
    void desconsideraDatasInvalidasESemResolucoes() {
        LocalDateTime abertura = LocalDateTime.of(2026, 11, 1, 8, 0);
        assertNull(RelatorioAnaliseService.segundosParaResolver(abertura, null));
        assertNull(RelatorioAnaliseService.segundosParaResolver(null, abertura));
        assertNull(RelatorioAnaliseService.segundosParaResolver(abertura, abertura.minusMinutes(1)));
        var analise = new RelatorioAnaliseService.AnaliseRelatorio(0, null, List.of(), List.of());
        assertEquals("Não disponível", analise.tempoMedioFormatado());
    }
}
