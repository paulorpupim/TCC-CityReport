package com.tccinfonet.cityreport.modules.relatorio.dto;

import java.time.LocalDateTime;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;

/** Somente os dados operacionais que o atendente precisa consultar/exportar. */
public record LinhaRelatorioDTO(
        Long id,
        String protocolo,
        String titulo,
        String setor,
        String categoria,
        String urgenciaPercebida,
        String prioridadeOficial,
        String status,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao) {

    public static LinhaRelatorioDTO de(Chamado chamado) {
        return new LinhaRelatorioDTO(
                chamado.getId(), chamado.getProtocolo(), chamado.getTitulo(),
                chamado.getCategoria().getSetor().getNome(),
                chamado.getCategoria().getNome(),
                descricaoPrioridade(chamado.getUrgenciaPercebida()),
                descricaoPrioridade(chamado.getPrioridadeOficial()),
                descricaoStatus(chamado.getStatus()),
                chamado.getDataCriacao(), chamado.getDataConclusao());
    }

    public static String descricaoPrioridade(Prioridade prioridade) {
        if (prioridade == null) return "Não definida";
        return switch (prioridade) {
            case BAIXA -> "Baixa";
            case MEDIA -> "Média";
            case ALTA -> "Alta";
            case CRITICA -> "Crítica";
        };
    }

    public static String descricaoStatus(StatusChamado status) {
        if (status == null) return "Não informado";
        return switch (status) {
            case ENVIADO -> "Enviado";
            case AGUARDANDO_INFORMACOES -> "Aguardando informações";
            case ABERTO -> "Aberto";
            case ATRIBUIDO -> "Atribuído";
            case EM_EXECUCAO -> "Em execução";
            case PENDENTE -> "Pendente";
            case AGUARDANDO_VALIDACAO -> "Aguardando validação";
            case RESOLVIDO -> "Resolvido";
            case RECUSADO -> "Recusado";
            case CANCELADO -> "Cancelado";
            case DUPLICADO -> "Duplicado";
        };
    }
}
