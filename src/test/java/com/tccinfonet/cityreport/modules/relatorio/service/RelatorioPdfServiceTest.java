package com.tccinfonet.cityreport.modules.relatorio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import com.tccinfonet.cityreport.modules.categoria.repository.CategoriaRepository;
import com.tccinfonet.cityreport.modules.relatorio.dto.FiltroRelatorioDTO;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioAnaliseService.AnaliseRelatorio;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioService.ResultadoRelatorio;
import com.tccinfonet.cityreport.modules.setor.repository.SetorRepository;

@ExtendWith(MockitoExtension.class)
class RelatorioPdfServiceTest {

    @Mock private RelatorioService relatorioService;
    @Mock private RelatorioAnaliseService relatorioAnaliseService;
    @Mock private SetorRepository setorRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @InjectMocks private RelatorioPdfService relatorioPdfService;

    @Test
    void exportaPdfValidoMesmoSemChamados() {
        FiltroRelatorioDTO filtro = FiltroRelatorioDTO.criar(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8),
                null, null, null, null, LocalDate.of(2026, 11, 8));
        when(relatorioService.consultar(eq(filtro), eq(0)))
                .thenReturn(new ResultadoRelatorio(Page.empty(), 0, 0, 0, 0));
        when(relatorioAnaliseService.analisar(eq(filtro)))
                .thenReturn(new AnaliseRelatorio(0, null, List.of(), List.of()));
        byte[] pdf = relatorioPdfService.gerar(filtro);
        assertTrue(pdf.length > 100);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
    }
}
