package com.tccinfonet.cityreport.modules.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Dados que o aplicativo envia ao fazer login.
public record LoginMobileRequestDTO(
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail valido.")
        String email,
        @NotBlank(message = "Informe a senha.")
        String senha
) {}
