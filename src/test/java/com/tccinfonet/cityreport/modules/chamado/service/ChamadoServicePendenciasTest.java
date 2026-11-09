package com.tccinfonet.cityreport.modules.chamado.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;

@ExtendWith(MockitoExtension.class)
class ChamadoServicePendenciasTest {

    @Mock private ChamadoRepository chamadoRepository;
    @InjectMocks private ChamadoService chamadoService;

    @Test
    void semPendenciasNaoBuscaListaDeChamados() {
        when(chamadoRepository.countByCidadaoIdAndStatus(8L, StatusChamado.AGUARDANDO_INFORMACOES))
                .thenReturn(0L);

        var resultado = chamadoService.consultarPendenciasInformacoes(8L);

        assertEquals(0L, resultado.total());
        assertTrue(resultado.chamados().isEmpty());
        verify(chamadoRepository, never()).findByCidadaoIdAndStatusOrderByDataCriacaoDesc(
                8L, StatusChamado.AGUARDANDO_INFORMACOES, PageRequest.of(0, 3));
    }

    @Test
    void exibeNoMaximoTresMasMantemTotalDePendencias() {
        Chamado um = new Chamado();
        um.setId(21L);
        Chamado dois = new Chamado();
        dois.setId(22L);
        Chamado tres = new Chamado();
        tres.setId(23L);

        when(chamadoRepository.countByCidadaoIdAndStatus(8L, StatusChamado.AGUARDANDO_INFORMACOES))
                .thenReturn(5L);
        when(chamadoRepository.findByCidadaoIdAndStatusOrderByDataCriacaoDesc(
                8L, StatusChamado.AGUARDANDO_INFORMACOES, PageRequest.of(0, 3)))
                .thenReturn(List.of(um, dois, tres));

        var resultado = chamadoService.consultarPendenciasInformacoes(8L);

        assertEquals(5L, resultado.total());
        assertEquals(List.of(um, dois, tres), resultado.chamados());
    }
}
