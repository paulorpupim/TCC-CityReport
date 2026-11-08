package com.tccinfonet.cityreport.modules.chamado.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.chamado.dto.PontoChamadoDTO;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapaChamadosService {

    private final ChamadoRepository chamadoRepository;

    public ResultadoMapa consultar() {
        // Coordenadas nulas e valores fora dos limites nao devem gerar marcadores.
        List<PontoChamadoDTO> pontos = chamadoRepository.listarPontosDoMapa().stream()
                .filter(PontoChamadoDTO::temCoordenadasValidas)
                .toList();
        long total = chamadoRepository.count();
        long semLocalizacao = Math.max(0, total - pontos.size());
        return new ResultadoMapa(pontos, total, semLocalizacao);
    }

    public record ResultadoMapa(List<PontoChamadoDTO> pontos, long total, long semLocalizacao) {}
}
