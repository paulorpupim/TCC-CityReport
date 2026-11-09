package com.tccinfonet.cityreport.modules.relatorio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;
import com.tccinfonet.cityreport.modules.relatorio.dto.FiltroRelatorioDTO;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock private ChamadoRepository chamadoRepository;
    @InjectMocks private RelatorioService relatorioService;

    private final LocalDate hoje = LocalDate.of(2026, 11, 8);

    @Test
    void utilizaMesAtualComoPeriodoPadrao() {
        var filtro = FiltroRelatorioDTO.criar(null, null, null, null, null, null, hoje);
        assertEquals(LocalDate.of(2026, 11, 1), filtro.dataInicial());
        assertEquals(hoje, filtro.dataFinal());
    }

    @Test
    void rejeitaIntervaloInvertidoEPrioridadeInvalida() {
        assertThrows(IllegalArgumentException.class, () -> FiltroRelatorioDTO.criar(
                hoje, hoje.minusDays(1), null, null, null, null, hoje));
        assertThrows(IllegalArgumentException.class, () -> FiltroRelatorioDTO.criar(
                null, null, null, null, "URGENTE", null, hoje));
    }

    @Test
    void contadoresNaoConfundemResolvidosEOutrosEncerramentos() {
        when(chamadoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(chamadoRepository.count(any(Specification.class))).thenReturn(0L);
        var filtro = FiltroRelatorioDTO.criar(null, null, null, null, null, null, hoje);
        var resultado = relatorioService.consultar(filtro, 0);
        assertEquals(0L, resultado.total());
        assertEquals(0L, resultado.resolvidos());
        assertEquals(0L, resultado.emAndamento());
        assertEquals(0L, resultado.encerradosSemResolucao());
    }

    @Test
    void csvProtegeFormulasEEscapaAspas() throws Exception {
        assertEquals("\"'=1+1\"", RelatorioService.escaparCampoCsv("=1+1"));
        assertEquals("\"um \"\"texto\"\"\"", RelatorioService.escaparCampoCsv("um \"texto\""));

        when(chamadoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        var filtro = FiltroRelatorioDTO.criar(null, null, null, null, null, null, hoje);
        StringWriter destino = new StringWriter();
        relatorioService.exportarCsv(filtro, destino);
        assertTrue(destino.toString().startsWith("\uFEFF\"Protocolo\";"));
    }
}
