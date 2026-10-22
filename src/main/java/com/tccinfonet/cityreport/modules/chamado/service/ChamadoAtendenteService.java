package com.tccinfonet.cityreport.modules.chamado.service;

import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.categoria.service.CategoriaService;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.service.HistoricoChamadoService;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoAtendenteService {

    private final ChamadoService chamadoService;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final HistoricoChamadoService historicoChamadoService;

    // Triagem: o atendente define a categoria final e a prioridade oficial.
    @Transactional
    public Chamado aprovarTriagem(Long chamadoId, Long categoriaId, Prioridade prioridadeOficial,
                                 String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);
        exigirStatus(chamado, StatusChamado.ENVIADO);

        if (prioridadeOficial == null) {
            throw new IllegalArgumentException("Defina a prioridade oficial.");
        }
        if (categoriaId == null) {
            throw new IllegalArgumentException("Selecione uma categoria.");
        }

        Categoria categoria = categoriaService.buscarPorId(categoriaId);
        validarCategoriaAtiva(categoria);

        chamado.setCategoria(categoria);
        chamado.setPrioridadeOficial(prioridadeOficial);
        chamado.setStatus(StatusChamado.ABERTO);

        Chamado salvo = chamadoRepository.save(chamado);
        historicoChamadoService.registrar(
                salvo, atendente,
                "Triagem aprovada. Categoria: " + categoria.getNome()
                        + ". Prioridade oficial: " + prioridadeOficial + ".",
                StatusChamado.ENVIADO, StatusChamado.ABERTO);

        return salvo;
    }

    // Permite alterar a prioridade de um chamado ainda em atendimento.
    @Transactional
    public Chamado alterarPrioridade(Long chamadoId, Prioridade novaPrioridade,
                                    String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);

        if (novaPrioridade == null) {
            throw new IllegalArgumentException("Informe a prioridade oficial.");
        }
        if (chamado.getStatus() != StatusChamado.ABERTO
                && chamado.getStatus() != StatusChamado.ATRIBUIDO
                && chamado.getStatus() != StatusChamado.EM_EXECUCAO
                && chamado.getStatus() != StatusChamado.PENDENTE) {
            throw new IllegalArgumentException("Nao e possivel alterar a prioridade neste status.");
        }

        Prioridade prioridadeAnterior = chamado.getPrioridadeOficial();
        if (prioridadeAnterior == novaPrioridade) {
            return chamado; // Nao registra alteracao quando a prioridade nao mudou.
        }

        chamado.setPrioridadeOficial(novaPrioridade);
        Chamado salvo = chamadoRepository.save(chamado);
        historicoChamadoService.registrar(
                salvo, atendente,
                "Prioridade oficial alterada de " + prioridadeAnterior + " para " + novaPrioridade + ".",
                null, null); // A prioridade mudou, mas o status nao.

        return salvo;
    }

    // Atribui um funcionario ou troca o responsavel antes do inicio da execucao.
    @Transactional
    public Chamado atribuir(Long chamadoId, Long funcionarioId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);

        if (chamado.getStatus() != StatusChamado.ABERTO
                && chamado.getStatus() != StatusChamado.ATRIBUIDO) {
            throw new IllegalArgumentException("O chamado nao pode ser atribuido neste status.");
        }

        Usuario funcionario = validarFuncionarioDoSetor(funcionarioId, chamado.getCategoria());
        Usuario responsavelAnterior = chamado.getFuncionarioResponsavel();
        StatusChamado statusAnterior = chamado.getStatus();

        if (statusAnterior == StatusChamado.ATRIBUIDO
                && responsavelAnterior != null
                && Objects.equals(responsavelAnterior.getId(), funcionario.getId())) {
            return chamado; // Nao houve mudanca na atribuicao.
        }

        chamado.setFuncionarioResponsavel(funcionario);
        chamado.setStatus(StatusChamado.ATRIBUIDO);

        String descricao = responsavelAnterior == null
                ? "Chamado atribuido ao funcionario " + funcionario.getNome() + "."
                : "Responsavel alterado de " + responsavelAnterior.getNome()
                        + " para " + funcionario.getNome() + ".";

        Chamado salvo = chamadoRepository.save(chamado);
        historicoChamadoService.registrar(
                salvo, atendente, descricao,
                statusAnterior == StatusChamado.ATRIBUIDO ? null : statusAnterior,
                statusAnterior == StatusChamado.ATRIBUIDO ? null : StatusChamado.ATRIBUIDO);

        return salvo;
    }

    // O atendente pode recusar um chamado durante a triagem, com justificativa.
    @Transactional
    public Chamado recusar(Long chamadoId, String justificativa, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);

        if (chamado.getStatus() != StatusChamado.ENVIADO
                && chamado.getStatus() != StatusChamado.AGUARDANDO_INFORMACOES) {
            throw new IllegalArgumentException("Somente chamados em triagem podem ser recusados.");
        }

        String motivo = validarTexto(justificativa, "A justificativa", 1700);
        StatusChamado anterior = chamado.getStatus();
        chamado.setStatus(StatusChamado.RECUSADO);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, atendente,
                "Chamado recusado. Justificativa: " + motivo,
                anterior, StatusChamado.RECUSADO);
        return chamado;
    }

    // Vincula um registro duplicado a um chamado original, sem copiar seu andamento.
    @Transactional
    public Chamado marcarDuplicado(Long chamadoId, Long originalId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado duplicado = chamadoService.buscarPorId(chamadoId);
        Chamado original = chamadoService.buscarPorId(originalId);

        if (duplicado.getStatus() != StatusChamado.ENVIADO
                && duplicado.getStatus() != StatusChamado.AGUARDANDO_INFORMACOES) {
            throw new IllegalArgumentException("A duplicidade so pode ser definida durante a triagem.");
        }
        if (Objects.equals(chamadoId, originalId)) {
            throw new IllegalArgumentException("Um chamado nao pode ser duplicado de si mesmo.");
        }
        if (original.getStatus() == StatusChamado.DUPLICADO
                || original.getStatus() == StatusChamado.RECUSADO
                || original.getStatus() == StatusChamado.CANCELADO
                || original.getChamadoOriginal() != null) {
            throw new IllegalArgumentException("Selecione um chamado original valido.");
        }
        // Impede que um chamado que ja e original de outros vire duplicado.
        if (chamadoRepository.existsByChamadoOriginalId(chamadoId)) {
            throw new IllegalArgumentException("Este chamado ja possui duplicados vinculados.");
        }

        StatusChamado anterior = duplicado.getStatus();
        duplicado.setChamadoOriginal(original);
        duplicado.setStatus(StatusChamado.DUPLICADO);
        chamadoRepository.save(duplicado);
        historicoChamadoService.registrar(duplicado, atendente,
                "Chamado identificado como duplicado do protocolo " + original.getId() + ".",
                anterior, StatusChamado.DUPLICADO);
        return duplicado;
    }

    // Durante a triagem o atendente pode pedir informacoes adicionais.
    @Transactional
    public Chamado solicitarInformacoes(Long chamadoId, String solicitacao, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);
        exigirStatus(chamado, StatusChamado.ENVIADO);

        String mensagem = validarTexto(solicitacao, "A solicitacao", 1700);
        chamado.setStatus(StatusChamado.AGUARDANDO_INFORMACOES);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, atendente,
                "Informacoes solicitadas: " + mensagem,
                StatusChamado.ENVIADO, StatusChamado.AGUARDANDO_INFORMACOES);
        return chamado;
    }

    // Reabre a atribuicao depois de um impedimento, mantendo ou trocando o funcionario.
    // funcionarioId = null significa manter o mesmo responsavel.
    @Transactional
    public Chamado retomarPendente(Long chamadoId, Long funcionarioId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);
        exigirStatus(chamado, StatusChamado.PENDENTE);

        Usuario anterior = chamado.getFuncionarioResponsavel();
        Long idEscolhido = funcionarioId != null ? funcionarioId
                : anterior == null ? null : anterior.getId();
        Usuario responsavel = validarFuncionarioDoSetor(idEscolhido, chamado.getCategoria());

        chamado.setFuncionarioResponsavel(responsavel);
        chamado.setStatus(StatusChamado.ATRIBUIDO);
        chamadoRepository.save(chamado);

        String descricao = anterior != null && !Objects.equals(anterior.getId(), responsavel.getId())
                ? "Chamado retomado e reatribuido de " + anterior.getNome()
                        + " para " + responsavel.getNome() + "."
                : "Chamado retomado para o funcionario " + responsavel.getNome() + ".";
        historicoChamadoService.registrar(chamado, atendente,
                descricao, StatusChamado.PENDENTE, StatusChamado.ATRIBUIDO);
        return chamado;
    }

    // Aprovacao definitiva pelo atendente. Um chamado resolvido nao e reaberto.
    @Transactional
    public Chamado aprovarConclusao(Long chamadoId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);
        exigirStatus(chamado, StatusChamado.AGUARDANDO_VALIDACAO);

        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setDataConclusao(LocalDateTime.now());
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, atendente,
                "Conclusao aprovada pelo atendente.",
                StatusChamado.AGUARDANDO_VALIDACAO, StatusChamado.RESOLVIDO);
        return chamado;
    }

    // Reprovacao com justificativa e opcionalmente outro funcionario responsavel.
    @Transactional
    public Chamado reprovarConclusao(Long chamadoId, String justificativa,
                                     Long novoFuncionarioId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = chamadoService.buscarPorId(chamadoId);
        exigirStatus(chamado, StatusChamado.AGUARDANDO_VALIDACAO);

        String motivo = validarTexto(justificativa, "A justificativa", 1700);
        Usuario anterior = chamado.getFuncionarioResponsavel();
        Long escolhido = novoFuncionarioId != null ? novoFuncionarioId
                : anterior == null ? null : anterior.getId();
        Usuario responsavel = validarFuncionarioDoSetor(escolhido, chamado.getCategoria());
        chamado.setFuncionarioResponsavel(responsavel);
        chamado.setStatus(StatusChamado.ATRIBUIDO);
        chamadoRepository.save(chamado);

        String troca = anterior != null && !Objects.equals(anterior.getId(), responsavel.getId())
                ? " Novo responsavel: " + responsavel.getNome() + "." : "";
        historicoChamadoService.registrar(chamado, atendente,
                "Execucao reprovada. Justificativa: " + motivo + "." + troca,
                StatusChamado.AGUARDANDO_VALIDACAO, StatusChamado.ATRIBUIDO);
        return chamado;
    }

    // Confere se a acao administrativa foi solicitada por um atendente ativo.
    private Usuario buscarAtendenteAutenticado(String emailAutenticado) {
        Usuario atendente = usuarioService.buscarPorEmail(emailAutenticado);

        if (atendente.getPerfil() != PerfilUsuario.ATENDENTE
                || !Boolean.TRUE.equals(atendente.getAtivo())) {
            throw new IllegalArgumentException("Acesso permitido somente a atendentes ativos.");
        }

        return atendente;
    }

    // O funcionario deve estar ativo e pertencer ao setor da categoria.
    private Usuario validarFuncionarioDoSetor(Long funcionarioId, Categoria categoria) {
        if (funcionarioId == null) {
            throw new IllegalArgumentException("Selecione um funcionario.");
        }

        validarCategoriaAtiva(categoria);
        Usuario funcionario = usuarioService.buscarPorId(funcionarioId);

        if (funcionario.getPerfil() != PerfilUsuario.FUNCIONARIO
                || !Boolean.TRUE.equals(funcionario.getAtivo())
                || funcionario.getSetor() == null
                || !Boolean.TRUE.equals(funcionario.getSetor().getAtivo())
                || !Objects.equals(funcionario.getSetor().getId(), categoria.getSetor().getId())) {
            throw new IllegalArgumentException("Selecione um funcionario ativo do setor responsavel.");
        }

        return funcionario;
    }

    private void validarCategoriaAtiva(Categoria categoria) {
        if (!Boolean.TRUE.equals(categoria.getAtivo())
                || categoria.getSetor() == null
                || !Boolean.TRUE.equals(categoria.getSetor().getAtivo())) {
            throw new IllegalArgumentException("A categoria ou seu setor esta inativo.");
        }
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
