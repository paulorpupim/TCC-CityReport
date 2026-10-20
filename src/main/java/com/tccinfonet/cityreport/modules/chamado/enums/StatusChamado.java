package com.tccinfonet.cityreport.modules.chamado.enums;

// Etapas e situacoes possiveis durante o atendimento de um chamado.
public enum StatusChamado {
    ENVIADO,
    AGUARDANDO_INFORMACOES,
    ABERTO,
    ATRIBUIDO,
    EM_EXECUCAO,
    PENDENTE,
    AGUARDANDO_VALIDACAO,
    RESOLVIDO,
    RECUSADO,
    CANCELADO,
    DUPLICADO
}
