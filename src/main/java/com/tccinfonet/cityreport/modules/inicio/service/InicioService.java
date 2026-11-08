package com.tccinfonet.cityreport.modules.inicio.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;

import lombok.RequiredArgsConstructor;

/** Consultas somente de leitura para a visao operacional da prefeitura. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InicioService {

    private static final List<StatusChamado> STATUS_ATIVOS = List.of(
            StatusChamado.ENVIADO, StatusChamado.AGUARDANDO_INFORMACOES,
            StatusChamado.ABERTO, StatusChamado.ATRIBUIDO,
            StatusChamado.EM_EXECUCAO, StatusChamado.PENDENTE,
            StatusChamado.AGUARDANDO_VALIDACAO);

    private static final List<StatusChamado> STATUS_COM_PRIORIDADE_OFICIAL = List.of(
            StatusChamado.ABERTO, StatusChamado.ATRIBUIDO,
            StatusChamado.EM_EXECUCAO, StatusChamado.PENDENTE,
            StatusChamado.AGUARDANDO_VALIDACAO);

    private static final List<Prioridade> PRIORIDADES_ALTAS =
            List.of(Prioridade.ALTA, Prioridade.CRITICA);

    private final ChamadoRepository chamadoRepository;

    public Painel consultar() {
        LocalDateTime agora = LocalDateTime.now();

        Map<StatusChamado, Long> porStatus = new EnumMap<>(StatusChamado.class);
        for (Object[] linha : chamadoRepository.contarPorStatus()) {
            porStatus.put((StatusChamado) linha[0], (Long) linha[1]);
        }

        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long ativos = STATUS_ATIVOS.stream().mapToLong(status -> quantidade(porStatus, status)).sum();
        long emAtendimento = quantidade(porStatus, StatusChamado.ATRIBUIDO)
                + quantidade(porStatus, StatusChamado.EM_EXECUCAO)
                + quantidade(porStatus, StatusChamado.PENDENTE);

        Indicadores indicadores = new Indicadores(
                total,
                ativos,
                quantidade(porStatus, StatusChamado.ENVIADO),
                quantidade(porStatus, StatusChamado.ABERTO),
                emAtendimento,
                quantidade(porStatus, StatusChamado.AGUARDANDO_VALIDACAO),
                quantidade(porStatus, StatusChamado.AGUARDANDO_INFORMACOES),
                chamadoRepository.countByStatusAndUrgenciaPercebidaIn(
                        StatusChamado.ENVIADO, PRIORIDADES_ALTAS),
                chamadoRepository.countByStatusInAndPrioridadeOficialIn(
                        STATUS_COM_PRIORIDADE_OFICIAL, PRIORIDADES_ALTAS),
                chamadoRepository.countByStatusInAndDataCriacaoBefore(
                        STATUS_ATIVOS, agora.minusDays(7)),
                chamadoRepository.countByStatusAndDataConclusaoGreaterThanEqual(
                        StatusChamado.RESOLVIDO, agora.minusDays(30)));

        List<Distribuicao> setores = transformarDistribuicao(
                chamadoRepository.contarAtivosPorSetor(STATUS_ATIVOS, PageRequest.of(0, 5)),
                ativos, false);
        List<Distribuicao> categorias = transformarDistribuicao(
                chamadoRepository.contarAtivosPorCategoria(STATUS_ATIVOS, PageRequest.of(0, 5)),
                ativos, true);

        // A lista prioriza CRITICA antes de ALTA e, dentro da prioridade, os mais antigos.
        List<ChamadoPrioritario> prioritarios = chamadoRepository.buscarChamadosPrioritarios(
                        STATUS_COM_PRIORIDADE_OFICIAL, PRIORIDADES_ALTAS,
                        Prioridade.CRITICA, PageRequest.of(0, 6))
                .stream()
                .map(chamado -> new ChamadoPrioritario(
                        chamado.getId(), chamado.getProtocolo(), chamado.getTitulo(),
                        chamado.getCategoria().getSetor().getNome(), chamado.getStatus(),
                        chamado.getPrioridadeOficial(), chamado.getDataCriacao()))
                .toList();

        return new Painel(indicadores, setores, categorias, prioritarios);
    }

    private long quantidade(Map<StatusChamado, Long> porStatus, StatusChamado status) {
        return porStatus.getOrDefault(status, 0L);
    }

    private List<Distribuicao> transformarDistribuicao(
            List<Object[]> linhas, long totalAtivos, boolean incluirSetor) {
        return linhas.stream()
                .map(linha -> {
                    String nome = (String) linha[0];
                    if (incluirSetor) {
                        // Categorias de setores diferentes podem ter o mesmo nome.
                        nome += " · " + (String) linha[1];
                    }
                    long quantidade = (Long) linha[incluirSetor ? 2 : 1];
                    int percentual = totalAtivos == 0 ? 0
                            : (int) Math.round(quantidade * 100.0 / totalAtivos);
                    return new Distribuicao(nome, quantidade, percentual);
                })
                .toList();
    }

    public record Painel(
            Indicadores indicadores,
            List<Distribuicao> setores,
            List<Distribuicao> categorias,
            List<ChamadoPrioritario> prioritarios) {
    }

    public record Indicadores(
            long totalRegistrados,
            long ativos,
            long aguardandoTriagem,
            long aguardandoAtribuicao,
            long emAtendimento,
            long aguardandoValidacao,
            long aguardandoCidadao,
            long urgenciasInformadasNaTriagem,
            long prioridadeOficialAlta,
            long ativosComMaisDeSeteDias,
            long resolvidosUltimosTrintaDias) {
    }

    public record Distribuicao(String nome, long quantidade, int percentual) {
    }

    public record ChamadoPrioritario(
            Long id, String protocolo, String titulo, String setor,
            StatusChamado status, Prioridade prioridade, LocalDateTime dataCriacao) {
    }
}
