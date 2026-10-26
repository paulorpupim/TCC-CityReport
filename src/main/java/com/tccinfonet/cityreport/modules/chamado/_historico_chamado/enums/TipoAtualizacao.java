package com.tccinfonet.cityreport.modules.chamado._historico_chamado.enums;

public enum TipoAtualizacao {
    SOLICITACAO_INFORMACOES("Última solicitação de informações", "Informacoes solicitadas: "),
    RESPOSTA_INFORMACOES("Última resposta complementar", "Informacoes complementares: "),
    RECUSA_CHAMADO("Justificativa da recusa", "Chamado recusado. Justificativa: "),
    CANCELAMENTO("Justificativa do cancelamento", "Chamado cancelado. Justificativa: "),
    IMPEDIMENTO("Último impedimento da execução", "Execucao impedida. Justificativa: "),
    REPROVACAO_EXECUCAO("Última reprovação da execução", "Execucao reprovada. Justificativa: "),
    OBSERVACAO_EXECUCAO("Última observação de execução", "Observacao de execucao: ");

    private final String titulo;
    private final String prefixo;

    TipoAtualizacao(String titulo, String prefixo) {
        this.titulo = titulo;
        this.prefixo = prefixo;
    }

    public String getTitulo() { return titulo; }

    // Preserva a redacao integral no historico, removendo apenas o rotulo repetido no card.
    public String extrairTexto(String descricao) {
        return descricao.startsWith(prefixo) ? descricao.substring(prefixo.length()) : descricao;
    }

    // Compatibilidade de leitura com registros anteriores a inclusao do campo tipoAtualizacao.
    public static TipoAtualizacao identificarDescricaoAntiga(String descricao) {
        if (descricao == null) return null;
        for (TipoAtualizacao tipo : values()) {
            if (descricao.startsWith(tipo.prefixo)) return tipo;
        }
        return null;
    }
}
