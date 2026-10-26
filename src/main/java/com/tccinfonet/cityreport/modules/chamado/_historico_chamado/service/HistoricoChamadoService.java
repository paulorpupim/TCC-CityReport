package com.tccinfonet.cityreport.modules.chamado._historico_chamado.service;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.chamado._historico_chamado.dto.AtualizacaoChamadoDTO;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.enums.TipoAtualizacao;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.entity.HistoricoChamado;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.repository.HistoricoChamadoRepository;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoricoChamadoService {

    private final HistoricoChamadoRepository historicoChamadoRepository;

    public List<HistoricoChamado> listarPorChamado(Long chamadoId) {
        return historicoChamadoRepository.findByChamadoIdOrderByDataHoraAscIdAsc(chamadoId);
    }

    // O cidadao recebe apenas interacoes suas com o atendente, sem destaques
    // sobre impedimentos, observacoes e reprovacoes da execucao interna.
    public List<AtualizacaoChamadoDTO> listarAtualizacoesDoCidadao(Long chamadoId) {
        return listarAtualizacoesFiltradas(chamadoId, EnumSet.of(
                TipoAtualizacao.SOLICITACAO_INFORMACOES,
                TipoAtualizacao.RESPOSTA_INFORMACOES,
                TipoAtualizacao.RECUSA_CHAMADO,
                TipoAtualizacao.CANCELAMENTO));
    }

    // Mantem o comportamento anterior: atendente ve todos os tipos e
    // funcionario ve apenas as tres categorias relacionadas a execucao.
    public List<AtualizacaoChamadoDTO> listarAtualizacoes(Long chamadoId, boolean somenteExecucao) {
        EnumSet<TipoAtualizacao> permitidos = somenteExecucao
                ? EnumSet.of(TipoAtualizacao.IMPEDIMENTO,
                             TipoAtualizacao.REPROVACAO_EXECUCAO,
                             TipoAtualizacao.OBSERVACAO_EXECUCAO)
                : EnumSet.allOf(TipoAtualizacao.class);
        return listarAtualizacoesFiltradas(chamadoId, permitidos);
    }

    // Um unico registro (o mais recente) por categoria permitida.
    // A linha do tempo completa continua disponivel separadamente.
    private List<AtualizacaoChamadoDTO> listarAtualizacoesFiltradas(
            Long chamadoId, EnumSet<TipoAtualizacao> permitidos) {
        EnumSet<TipoAtualizacao> encontrados = EnumSet.noneOf(TipoAtualizacao.class);
        List<AtualizacaoChamadoDTO> atualizacoes = new ArrayList<>();

        for (HistoricoChamado registro :
                historicoChamadoRepository.findByChamadoIdOrderByDataHoraDescIdDesc(chamadoId)) {
            TipoAtualizacao tipo = registro.getTipoAtualizacao();
            if (tipo == null) {
                // Registros antigos permanecem consultaveis sem migracao do banco.
                tipo = TipoAtualizacao.identificarDescricaoAntiga(registro.getDescricao());
            }
            if (tipo == null || !permitidos.contains(tipo) || encontrados.contains(tipo)) continue;

            encontrados.add(tipo);
            atualizacoes.add(new AtualizacaoChamadoDTO(
                    tipo, tipo.getTitulo(), tipo.extrairTexto(registro.getDescricao()),
                    registro.getDataHora()));
        }
        return atualizacoes;
    }

    // Mantem a assinatura antiga para os registros comuns da linha do tempo.
    @Transactional
    public HistoricoChamado registrar(Chamado chamado, Usuario usuario, String descricao,
                                     StatusChamado statusAnterior, StatusChamado statusNovo) {
        return registrar(chamado, usuario, descricao, statusAnterior, statusNovo, null);
    }

    @Transactional
    public HistoricoChamado registrar(Chamado chamado, Usuario usuario, String descricao,
                                     StatusChamado statusAnterior, StatusChamado statusNovo,
                                     TipoAtualizacao tipoAtualizacao) {
        if (chamado == null || chamado.getId() == null) {
            throw new IllegalArgumentException("Informe um chamado existente para registrar o historico.");
        }

        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descricao do historico e obrigatoria.");
        }

        String texto = descricao.trim();
        if (texto.length() > 2000) {
            throw new IllegalArgumentException("A descricao do historico deve ter no maximo 2000 caracteres.");
        }

        HistoricoChamado historico = new HistoricoChamado();
        historico.setChamado(chamado);
        historico.setUsuario(usuario);
        historico.setDescricao(texto);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setTipoAtualizacao(tipoAtualizacao);

        return historicoChamadoRepository.save(historico);
    }
}
