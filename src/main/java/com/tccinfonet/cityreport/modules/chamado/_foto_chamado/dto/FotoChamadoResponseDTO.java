package com.tccinfonet.cityreport.modules.chamado._foto_chamado.dto;

import java.time.LocalDateTime;

import com.tccinfonet.cityreport.modules.chamado._foto_chamado.entity.FotoChamado;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.enums.TipoFoto;

public record FotoChamadoResponseDTO(
        Long id,
        TipoFoto tipo,
        LocalDateTime dataEnvio
) {
    public static FotoChamadoResponseDTO de(FotoChamado foto) {
        return new FotoChamadoResponseDTO(
                foto.getId(),
                foto.getTipo(),
                foto.getDataEnvio()
        );
    }
}
