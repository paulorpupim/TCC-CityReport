package com.tccinfonet.cityreport.modules.relatorio.dto;

import java.time.LocalDate;
import java.util.Set;

import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;

/** Filtros do relatório; as datas referem-se à abertura do chamado. */
public record FiltroRelatorioDTO(
        LocalDate dataInicial,
        LocalDate dataFinal,
        Long setorId,
        Long categoriaId,
        String prioridade,
        StatusChamado status) {

    private static final Set<String> PRIORIDADES = Set.of(
            "BAIXA", "MEDIA", "ALTA", "CRITICA", "SEM_PRIORIDADE");

    public static FiltroRelatorioDTO criar(LocalDate inicio, LocalDate fim,
            Long setorId, Long categoriaId, String prioridade,
            StatusChamado status, LocalDate hoje) {
        LocalDate inicial = inicio == null ? hoje.withDayOfMonth(1) : inicio;
        LocalDate finalPeriodo = fim == null ? hoje : fim;

        if (inicial.isAfter(finalPeriodo)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        if (setorId != null && setorId <= 0 || categoriaId != null && categoriaId <= 0) {
            throw new IllegalArgumentException("Selecione um setor ou categoria válido.");
        }
        String prioridadeNormalizada = prioridade == null || prioridade.isBlank()
                ? null : prioridade.trim().toUpperCase(java.util.Locale.ROOT);
        if (prioridadeNormalizada != null && !PRIORIDADES.contains(prioridadeNormalizada)) {
            throw new IllegalArgumentException("Selecione uma prioridade válida.");
        }
        return new FiltroRelatorioDTO(inicial, finalPeriodo,
                setorId, categoriaId, prioridadeNormalizada, status);
    }
}
