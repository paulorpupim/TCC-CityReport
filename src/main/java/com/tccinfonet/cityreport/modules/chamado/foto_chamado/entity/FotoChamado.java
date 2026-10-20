package com.tccinfonet.cityreport.modules.chamado.foto_chamado.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.enums.TipoFoto;
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
public class FotoChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O caminho da foto e obrigatorio.")
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String caminhoArquivo;

    @NotNull(message = "O tipo da foto e obrigatorio.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFoto tipo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataEnvio;

    @NotNull(message = "O chamado e obrigatorio.")
    @ManyToOne
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @NotNull(message = "O usuario que enviou a foto e obrigatorio.")
    @ManyToOne
    @JoinColumn(name = "enviada_por_id", nullable = false)
    private Usuario enviadaPor;
}
