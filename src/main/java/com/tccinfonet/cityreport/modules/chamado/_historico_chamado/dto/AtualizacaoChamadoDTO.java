package com.tccinfonet.cityreport.modules.chamado._historico_chamado.dto;

import java.time.LocalDateTime;

import com.tccinfonet.cityreport.modules.chamado._historico_chamado.enums.TipoAtualizacao;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Exibe o ultimo registro relevante de cada categoria, sem substituir o historico completo.
@Getter
@AllArgsConstructor
public class AtualizacaoChamadoDTO {
    private TipoAtualizacao tipo;
    private String titulo;
    private String texto;
    private LocalDateTime dataHora;
}
