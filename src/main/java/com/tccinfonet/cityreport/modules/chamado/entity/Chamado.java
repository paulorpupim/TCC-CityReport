package com.tccinfonet.cityreport.modules.chamado.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificacao publica: 2026-000001, 2027-000001, etc.
    // A coluna e unica e nunca deve ser alterada apos o cadastro.
    @Column(nullable = false, unique = true, updatable = false, length = 24)
    private String protocolo;

    @NotBlank(message = "O titulo e obrigatorio.")
    @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres.")
    @Column(nullable = false, length = 150)
    private String titulo;

    @NotBlank(message = "A descricao e obrigatoria.")
    @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres.")
    @Column(nullable = false, length = 2000)
    private String descricao;

    @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres.")
    @Column(length = 255)
    private String endereco;

    private Double latitude;
    private Double longitude;

    @NotNull(message = "Informe a urgencia percebida.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade urgenciaPercebida;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prioridade prioridadeOficial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusChamado status = StatusChamado.ENVIADO;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp 
    private LocalDateTime dataCriacao;

    private LocalDateTime primeiroInicioExecucao;

    private LocalDateTime dataConclusao;

    @ManyToOne
    @JoinColumn(name = "cidadao_id", nullable = false)
    private Usuario cidadao;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "funcionario_responsavel_id")
    private Usuario funcionarioResponsavel;

    @ManyToOne
    @JoinColumn(name = "chamado_original_id")
    private Chamado chamadoOriginal;
}
