package com.tccinfonet.cityreport.modules.chamado.dto;

import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CriarChamadoDTO {
    @NotBlank(message = "O titulo e obrigatorio.")
    @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres.")
    private String titulo;

    @NotBlank(message = "A descricao e obrigatoria.")
    @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres.")
    private String descricao;

    @NotNull(message = "Selecione uma categoria.")
    private Long categoriaId;

    @NotNull(message = "Informe a urgencia percebida.")
    private Prioridade urgenciaPercebida;

    // O endereco e opcional quando o cidadao informa coordenadas pelo mapa.
    @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres.")
    private String endereco;

    private Double latitude;
    private Double longitude;
}
