
package com.tccinfonet.cityreport.modules.usuario.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tccinfonet.cityreport.modules.setor.entity.Setor;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome e obrigatorio.")
    @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String nome;
    
    @NotBlank(message = "O email e obrigatorio.")
    @Email(message = "Informe um email valido.")
    @Size(max = 150, message = "O email deve ter no maximo 150 caracteres.")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // JsonIgnore evita expor o hash caso a entidade seja serializada em JSON.
    @JsonIgnore
    @NotBlank(message = "A senha e obrigatoria.")
    @Column(nullable = false, length = 255)
    private String senha;

    @NotNull(message = "O perfil e obrigatorio.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne
    @JoinColumn(name = "setor_id")
    private Setor setor;
}
