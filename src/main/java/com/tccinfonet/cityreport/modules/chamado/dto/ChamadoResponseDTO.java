package com.tccinfonet.cityreport.modules.chamado.dto;

import java.time.LocalDateTime;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;

public record ChamadoResponseDTO(
        Long id,
        String titulo,
        String descricao,
        String categoria,
        String endereco,
        Double latitude,
        Double longitude,
        Prioridade urgenciaPercebida,
        Prioridade prioridadeOficial,
        StatusChamado status,
        LocalDateTime dataCriacao
) {

    public static ChamadoResponseDTO de(Chamado chamado) {
        return new ChamadoResponseDTO(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getCategoria().getNome(),
                chamado.getEndereco(),
                chamado.getLatitude(),
                chamado.getLongitude(),
                chamado.getUrgenciaPercebida(),
                chamado.getPrioridadeOficial(),
                chamado.getStatus(),
                chamado.getDataCriacao()
        );
    }
}
