package com.tccinfonet.cityreport.modules.usuario.dto;

import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioRequestDTO {

    @NotBlank(message = "O nome e obrigatorio.")
    @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres.")
    private String nome;

    @NotBlank(message = "O e-mail e obrigatorio.")
    @Email(message = "Informe um e-mail valido.")
    @Size(max = 150, message = "O e-mail deve ter no maximo 150 caracteres.")
    private String email;

    private String senha;

    private PerfilUsuario perfil;

    // Obrigatorio somente quando o perfil for FUNCIONARIO.
    private Long setorId;
}

