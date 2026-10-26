package com.tccinfonet.cityreport.modules.chamado.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tccinfonet.cityreport.modules.chamado._foto_chamado.enums.TipoFoto;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.service.FotoChamadoService;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.enums.TipoAtualizacao;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.service.HistoricoChamadoService;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoExecucaoService {

    private final ChamadoService chamadoService;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioService usuarioService;
    private final HistoricoChamadoService historicoChamadoService;
    private final FotoChamadoService fotoChamadoService;
    private final EntityManager entityManager;

    // O funcionario so pode visualizar os servicos atribuidos a ele.
    public List<Chamado> listarDoFuncionario(String emailAutenticado) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailAutenticado);
        return chamadoRepository.findByFuncionarioResponsavelIdOrderByDataCriacaoDesc(funcionario.getId());
    }

    public Chamado buscarDoFuncionario(Long chamadoId, String emailAutenticado) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailAutenticado);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);

        if (chamado.getFuncionarioResponsavel() == null
                || !Objects.equals(chamado.getFuncionarioResponsavel().getId(), funcionario.getId())) {
            throw new IllegalArgumentException("Este chamado nao esta atribuido a voce.");
        }

        return chamado;
    }

    // Somente o responsavel pode iniciar o servico, e apenas um por vez.
    @Transactional
    public Chamado iniciarExecucao(Long chamadoId, String emailAutenticado) {
        Usuario funcionario = buscarFuncionarioParaOperacao(emailAutenticado);

        Chamado chamado = chamadoService.buscarPorId(chamadoId);
        exigirStatus(chamado, StatusChamado.ATRIBUIDO);

        if (chamado.getFuncionarioResponsavel() == null
                || !Objects.equals(chamado.getFuncionarioResponsavel().getId(), funcionario.getId())) {
            throw new IllegalArgumentException("Este chamado nao esta atribuido a voce.");
        }

        if (chamadoRepository.existsByFuncionarioResponsavelIdAndStatus(
                funcionario.getId(), StatusChamado.EM_EXECUCAO)) {
            throw new IllegalArgumentException("Finalize ou pause o chamado em execucao antes de iniciar outro.");
        }

        if (chamado.getPrimeiroInicioExecucao() == null) {
            chamado.setPrimeiroInicioExecucao(LocalDateTime.now());
        }
        chamado.setStatus(StatusChamado.EM_EXECUCAO);

        Chamado salvo = chamadoRepository.save(chamado);
        historicoChamadoService.registrar(
                salvo, funcionario, "Execucao iniciada pelo funcionario " + funcionario.getNome() + ".",
                StatusChamado.ATRIBUIDO, StatusChamado.EM_EXECUCAO);

        return salvo;
    }

    // Observacoes sobre o trabalho executado: sem alterar o status do chamado.
    // Qualquer quantidade de registros pode ser adicionada durante a execucao.
    @Transactional
    public Chamado registrarObservacao(Long chamadoId, String observacao, String emailFuncionario) {
        Usuario funcionario = buscarFuncionarioParaOperacao(emailFuncionario);
        Chamado chamado = buscarDoFuncionario(chamadoId, emailFuncionario);
        exigirStatus(chamado, StatusChamado.EM_EXECUCAO);

        String texto = validarTexto(observacao, "A observacao", 1700);
        historicoChamadoService.registrar(chamado, funcionario,
                "Observacao de execucao: " + texto,
                null, null, TipoAtualizacao.OBSERVACAO_EXECUCAO);

        return chamado;
    }

    // O funcionario registra o impedimento e aguarda decisao do atendente.
    @Transactional
    public Chamado registrarImpedimento(Long chamadoId, String justificativa, String emailFuncionario) {
        Usuario funcionario = buscarFuncionarioParaOperacao(emailFuncionario);
        Chamado chamado = buscarDoFuncionario(chamadoId, emailFuncionario);
        exigirStatus(chamado, StatusChamado.EM_EXECUCAO);

        String motivo = validarTexto(justificativa, "A justificativa", 1700);
        chamado.setStatus(StatusChamado.PENDENTE);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, funcionario,
                "Execucao impedida. Justificativa: " + motivo,
                StatusChamado.EM_EXECUCAO, StatusChamado.PENDENTE,
                TipoAtualizacao.IMPEDIMENTO);
        return chamado;
    }

    // O funcionario conclui sua tentativa enviando obrigatoriamente UMA NOVA foto.
    @Transactional
    public Chamado solicitarFinalizacao(Long chamadoId, MultipartFile fotoSolucao, String emailFuncionario) {
        Usuario funcionario = buscarFuncionarioParaOperacao(emailFuncionario);
        Chamado chamado = buscarDoFuncionario(chamadoId, emailFuncionario);
        exigirStatus(chamado, StatusChamado.EM_EXECUCAO);

        // A foto e validada e armazenada antes de permitir mudar o status.
        fotoChamadoService.salvarFoto(chamado, funcionario, TipoFoto.SOLUCAO, fotoSolucao);
        chamado.setStatus(StatusChamado.AGUARDANDO_VALIDACAO);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, funcionario,
                "Execucao enviada para validacao, com fotografia da solucao.",
                StatusChamado.EM_EXECUCAO, StatusChamado.AGUARDANDO_VALIDACAO);
        return chamado;
    }

    // A mesma trava usada na desativacao impede que as operacoes se cruzem.
    // O refresh atualiza o estado da conta depois de obter a trava no banco.
    private Usuario buscarFuncionarioParaOperacao(String emailAutenticado) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailAutenticado);
        entityManager.refresh(funcionario, LockModeType.PESSIMISTIC_WRITE);

        if (!Boolean.TRUE.equals(funcionario.getAtivo())) {
            throw new IllegalArgumentException("O funcionario esta inativo.");
        }
        return funcionario;
    }

    // Confere se o usuario do aplicativo e um funcionario ativo.
    private Usuario buscarFuncionarioAutenticado(String emailAutenticado) {
        Usuario funcionario = usuarioService.buscarPorEmail(emailAutenticado);

        if (funcionario.getPerfil() != PerfilUsuario.FUNCIONARIO
                || !Boolean.TRUE.equals(funcionario.getAtivo())) {
            throw new IllegalArgumentException("Acesso permitido somente a funcionarios ativos.");
        }

        return funcionario;
    }

    private void exigirStatus(Chamado chamado, StatusChamado esperado) {
        if (chamado.getStatus() != esperado) {
            throw new IllegalArgumentException("Operacao permitida somente para chamados com status " + esperado + ".");
        }
    }

    private String validarTexto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio.");
        }

        String texto = valor.trim();
        if (texto.length() > limite) {
            throw new IllegalArgumentException(campo + " deve ter no maximo " + limite + " caracteres.");
        }

        return texto;
    }
}
