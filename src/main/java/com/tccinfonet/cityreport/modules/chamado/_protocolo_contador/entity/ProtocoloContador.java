package com.tccinfonet.cityreport.modules.chamado._protocolo_contador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProtocoloContador {

    @Id
    private Integer ano;

    @Column(name = "ultimo_numero", nullable = false)
    private Long ultimoNumero;
}
