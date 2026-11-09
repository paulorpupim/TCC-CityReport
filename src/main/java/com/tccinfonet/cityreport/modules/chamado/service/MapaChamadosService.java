package com.tccinfonet.cityreport.modules.chamado.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.chamado.dto.PontoChamadoDTO;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapaChamadosService {

    private static final Set<String> PERIODOS_PERMITIDOS = Set.of("7", "30", "90", "180", "ano-atual");

    private final ChamadoRepository chamadoRepository;

    public ResultadoMapa consultar(String modoSolicitado, String valorSolicitado) {
        return consultar(modoSolicitado, valorSolicitado, LocalDate.now());
    }

    // A data pode ser informada pelos testes para verificar a virada de ano.
    ResultadoMapa consultar(String modoSolicitado, String valorSolicitado, LocalDate hoje) {
        int anoAtual = hoje.getYear();
        LocalDateTime primeiraData = chamadoRepository.buscarPrimeiraDataCriacao();
        int primeiroAno = primeiraData == null ? anoAtual : Math.min(primeiraData.getYear(), anoAtual);
        List<Integer> anosDisponiveis = IntStream.rangeClosed(primeiroAno, anoAtual)
                .boxed().sorted(Comparator.reverseOrder()).toList();

        boolean filtrarPorAno = "ano".equalsIgnoreCase(modoSolicitado);
        String modo = filtrarPorAno ? "ano" : "periodo";
        String valor;
        LocalDate dataInicial;
        LocalDate dataFinalExclusiva;
        String descricaoPeriodo;

        if (filtrarPorAno) {
            int anoEscolhido = anoAtual;
            try {
                if (valorSolicitado != null) anoEscolhido = Integer.parseInt(valorSolicitado);
            } catch (NumberFormatException ex) {
                anoEscolhido = anoAtual;
            }
            if (anoEscolhido < primeiroAno || anoEscolhido > anoAtual) anoEscolhido = anoAtual;

            valor = String.valueOf(anoEscolhido);
            dataInicial = LocalDate.of(anoEscolhido, 1, 1);
            dataFinalExclusiva = dataInicial.plusYears(1);
            descricaoPeriodo = "Todos os chamados abertos em " + anoEscolhido;
        } else {
            valor = valorSolicitado != null && PERIODOS_PERMITIDOS.contains(valorSolicitado) ? valorSolicitado : "90";
            LocalDate inicioAno = LocalDate.of(anoAtual, 1, 1);
            dataInicial = "ano-atual".equals(valor)
                    ? inicioAno
                    : hoje.minusDays(Integer.parseInt(valor) - 1L);
            // O filtro por periodo nunca inclui registros de anos anteriores.
            if (dataInicial.isBefore(inicioAno)) dataInicial = inicioAno;
            dataFinalExclusiva = hoje.plusDays(1);
            descricaoPeriodo = "ano-atual".equals(valor)
                    ? "Todos os chamados abertos no ano atual (" + anoAtual + ")"
                    : "Chamados abertos nos últimos " + valor + " dias do ano atual";
        }

        LocalDateTime inicio = dataInicial.atStartOfDay();
        LocalDateTime fimExclusivo = dataFinalExclusiva.atStartOfDay();

        // O banco filtra primeiro por data; o Java valida as coordenadas para o Leaflet.
        List<PontoChamadoDTO> pontos = chamadoRepository.listarPontosDoMapa(inicio, fimExclusivo)
                .stream().filter(PontoChamadoDTO::temCoordenadasValidas).toList();
        long total = chamadoRepository.countByDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(
                inicio, fimExclusivo);
        long semLocalizacao = Math.max(0, total - pontos.size());

        return new ResultadoMapa(pontos, total, semLocalizacao, modo, valor,
                anoAtual, anosDisponiveis, descricaoPeriodo);
    }

    public record ResultadoMapa(
            List<PontoChamadoDTO> pontos,
            long total,
            long semLocalizacao,
            String modo,
            String valor,
            int anoAtual,
            List<Integer> anosDisponiveis,
            String descricaoPeriodo) {}
}
