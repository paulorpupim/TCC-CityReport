package com.tccinfonet.cityreport.modules.chamado.historico_chamado.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.historico_chamado.entity.HistoricoChamado;
import com.tccinfonet.cityreport.modules.chamado.historico_chamado.repository.HistoricoChamadoRepository;
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

    @Transactional
    public HistoricoChamado registrar(Chamado chamado, Usuario usuario, String descricao,
                                     StatusChamado statusAnterior, StatusChamado statusNovo) {
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

        return historicoChamadoRepository.save(historico);
    }
}
