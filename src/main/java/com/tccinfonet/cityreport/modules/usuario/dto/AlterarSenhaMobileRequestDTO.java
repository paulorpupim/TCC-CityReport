package com.tccinfonet.cityreport.modules.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A senha atual e obrigatoria. A confirmacao evita trocas por erro de digitacao.
public record AlterarSenhaMobileRequestDTO(
        @NotBlank(message = "Informe sua senha atual.")
        String senhaAtual,
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, message = "A nova senha deve ter pelo menos 8 caracteres.")
        String novaSenha,
        @NotBlank(message = "Confirme a nova senha.")
        String confirmarNovaSenha
) {}
