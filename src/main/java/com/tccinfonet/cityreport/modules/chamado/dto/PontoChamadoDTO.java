package com.tccinfonet.cityreport.modules.chamado.dto;

import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;

/** Dados limitados ao necessario para o mapa do atendente. */
public record PontoChamadoDTO(
        Long id,
        String protocolo,
        String titulo,
        String endereco,
        Long setorId,
        String setor,
        Long categoriaId,
        String categoria,
        StatusChamado status,
        Prioridade prioridadeOficial,
        Prioridade urgenciaPercebida,
        Double latitude,
        Double longitude) {

    public boolean temCoordenadasValidas() {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}
