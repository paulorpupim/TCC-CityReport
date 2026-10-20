package com.tccinfonet.cityreport.modules.chamado.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.categoria.service.CategoriaService;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.entity.FotoChamado;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.enums.TipoFoto;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.service.FotoChamadoService;
import com.tccinfonet.cityreport.modules.chamado.historico_chamado.service.HistoricoChamadoService;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final HistoricoChamadoService historicoChamadoService;
    private final FotoChamadoService fotoChamadoService;
    private final EntityManager entityManager;

    // Consultas administrativas: lista todos os chamados e busca um pelo ID.
    public List<Chamado> listarTodos() {
        return chamadoRepository.findAllByOrderByDataCriacaoDesc();
    }

    public Chamado buscarPorId(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chamado nao encontrado."));
    }

    // O cidadao so pode consultar os chamados registrados por sua propria conta.
    public List<Chamado> listarDoCidadao(String emailAutenticado) {
        Usuario usuario = usuarioService.buscarPorEmail(emailAutenticado);
        return chamadoRepository.findByCidadaoIdOrderByDataCriacaoDesc(usuario.getId());
    }

    public Chamado buscarDoCidadao(Long chamadoId, String emailAutenticado) {
        Usuario usuario = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarPorId(chamadoId);

        if (!Objects.equals(chamado.getCidadao().getId(), usuario.getId())) {
            throw new IllegalArgumentException("Voce nao pode consultar este chamado.");
        }

        return chamado;
    }

    // O funcionario so pode visualizar os servicos atribuidos a ele.
    public List<Chamado> listarDoFuncionario(String emailAutenticado) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailAutenticado);
        return chamadoRepository.findByFuncionarioResponsavelIdOrderByDataCriacaoDesc(funcionario.getId());
    }

    public Chamado buscarDoFuncionario(Long chamadoId, String emailAutenticado) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailAutenticado);
        Chamado chamado = buscarPorId(chamadoId);

        if (chamado.getFuncionarioResponsavel() == null
                || !Objects.equals(chamado.getFuncionarioResponsavel().getId(), funcionario.getId())) {
            throw new IllegalArgumentException("Este chamado nao esta atribuido a voce.");
        }

        return chamado;
    }

    // Cadastro: nunca aceita do formulario o cidadao, o status ou a prioridade
    // oficial.
    @Transactional
    public Chamado cadastrar(Chamado dados, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        if (!Boolean.TRUE.equals(cidadao.getAtivo())) {
            throw new IllegalArgumentException("O usuario esta inativo.");
        }

        String titulo = validarTexto(dados.getTitulo(), "O titulo", 150);
        String descricao = validarTexto(dados.getDescricao(), "A descricao", 2000);

        if (dados.getUrgenciaPercebida() == null) {
            throw new IllegalArgumentException("Informe a urgencia percebida.");
        }

        if (dados.getCategoria() == null || dados.getCategoria().getId() == null) {
            throw new IllegalArgumentException("Selecione uma categoria.");
        }

        Categoria categoria = categoriaService.buscarPorId(dados.getCategoria().getId());
        validarCategoriaAtiva(categoria);

        String endereco = dados.getEndereco() == null ? "" : dados.getEndereco().trim();
        validarLocalizacao(endereco, dados.getLatitude(), dados.getLongitude());

        Chamado chamado = new Chamado();
        chamado.setTitulo(titulo);
        chamado.setDescricao(descricao);
        chamado.setEndereco(endereco.isEmpty() ? null : endereco);
        chamado.setLatitude(dados.getLatitude());
        chamado.setLongitude(dados.getLongitude());
        chamado.setUrgenciaPercebida(dados.getUrgenciaPercebida());
        chamado.setCategoria(categoria);
        chamado.setCidadao(cidadao);
        chamado.setStatus(StatusChamado.ENVIADO);

        Chamado salvo = chamadoRepository.save(chamado);
        historicoChamadoService.registrar(
                salvo, cidadao, "Chamado registrado.", null, StatusChamado.ENVIADO);

        return salvo;
    }

    // Triagem: o atendente define a categoria final e a prioridade oficial.
    @Transactional
    public Chamado aprovarTriagem(Long chamadoId, Long categoriaId, Prioridade prioridadeOficial,
            String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = buscarPorId(chamadoId);
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
        Chamado chamado = buscarPorId(chamadoId);

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
        Chamado chamado = buscarPorId(chamadoId);

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

    // Somente o responsavel pode iniciar o servico, e apenas um por vez.
    @Transactional
    public Chamado iniciarExecucao(Long chamadoId, String emailAutenticado) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailAutenticado);

        // Trava apenas o registro do funcionario durante a verificacao.
        // Evita que dois chamados diferentes sejam iniciados ao mesmo tempo para ele.
        entityManager.find(Usuario.class, funcionario.getId(), LockModeType.PESSIMISTIC_WRITE);

        Chamado chamado = buscarPorId(chamadoId);
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

    // O envio da foto inicial e opcional. Somente o autor pode anexar uma foto do
    // problema.
    @Transactional
    public FotoChamado adicionarFotoProblema(Long chamadoId, MultipartFile arquivo, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);

        FotoChamado foto = fotoChamadoService.salvarFoto(chamado, cidadao, TipoFoto.PROBLEMA, arquivo);
        historicoChamadoService.registrar(chamado, cidadao, "Foto do problema adicionada.", null, null);
        return foto;
    }

    // O autor pode cancelar enquanto o servico nunca tiver sido iniciado.
    @Transactional
    public Chamado cancelar(Long chamadoId, String justificativa, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);

        if (chamado.getPrimeiroInicioExecucao() != null) {
            throw new IllegalArgumentException("O chamado nao pode ser cancelado depois do inicio da execucao.");
        }
        if (chamado.getStatus() != StatusChamado.ENVIADO
                && chamado.getStatus() != StatusChamado.AGUARDANDO_INFORMACOES
                && chamado.getStatus() != StatusChamado.ABERTO
                && chamado.getStatus() != StatusChamado.ATRIBUIDO) {
            throw new IllegalArgumentException("Nao e possivel cancelar este chamado.");
        }

        String motivo = validarTexto(justificativa, "A justificativa", 1700);
        StatusChamado anterior = chamado.getStatus();
        chamado.setStatus(StatusChamado.CANCELADO);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, cidadao,
                "Chamado cancelado. Justificativa: " + motivo,
                anterior, StatusChamado.CANCELADO);
        return chamado;
    }

    // O atendente pode recusar um chamado durante a triagem, com justificativa.
    @Transactional
    public Chamado recusar(Long chamadoId, String justificativa, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = buscarPorId(chamadoId);

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

    // Vincula um registro duplicado a um chamado original, sem copiar seu
    // andamento.
    @Transactional
    public Chamado marcarDuplicado(Long chamadoId, Long originalId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado duplicado = buscarPorId(chamadoId);
        Chamado original = buscarPorId(originalId);

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

    // Acompanhamento do cidadao: duplicados exibem o andamento do original.
    public StatusChamado consultarAndamento(Long chamadoId, String emailAutenticado) {
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);
        if (chamado.getStatus() == StatusChamado.DUPLICADO) {
            return chamado.getChamadoOriginal().getStatus();
        }
        return chamado.getStatus();
    }

    // Durante a triagem o atendente pode pedir informacoes adicionais.
    @Transactional
    public Chamado solicitarInformacoes(Long chamadoId, String solicitacao, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = buscarPorId(chamadoId);
        exigirStatus(chamado, StatusChamado.ENVIADO);

        String mensagem = validarTexto(solicitacao, "A solicitacao", 1700);
        chamado.setStatus(StatusChamado.AGUARDANDO_INFORMACOES);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, atendente,
                "Informacoes solicitadas: " + mensagem,
                StatusChamado.ENVIADO, StatusChamado.AGUARDANDO_INFORMACOES);
        return chamado;
    }

    // O autor responde e o chamado volta para a fila de triagem.
    @Transactional
    public Chamado complementarInformacoes(Long chamadoId, String resposta, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);
        exigirStatus(chamado, StatusChamado.AGUARDANDO_INFORMACOES);

        String mensagem = validarTexto(resposta, "A resposta", 1700);
        chamado.setStatus(StatusChamado.ENVIADO);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, cidadao,
                "Informacoes complementares: " + mensagem,
                StatusChamado.AGUARDANDO_INFORMACOES, StatusChamado.ENVIADO);
        return chamado;
    }

    // O funcionario registra o impedimento e aguarda decisao do atendente.
    @Transactional
    public Chamado registrarImpedimento(Long chamadoId, String justificativa, String emailFuncionario) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailFuncionario);
        Chamado chamado = buscarDoFuncionario(chamadoId, emailFuncionario);
        exigirStatus(chamado, StatusChamado.EM_EXECUCAO);

        String motivo = validarTexto(justificativa, "A justificativa", 1700);
        chamado.setStatus(StatusChamado.PENDENTE);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, funcionario,
                "Execucao impedida. Justificativa: " + motivo,
                StatusChamado.EM_EXECUCAO, StatusChamado.PENDENTE);
        return chamado;
    }

    // Reabre a atribuicao depois de um impedimento, mantendo ou trocando o
    // funcionario.
    // funcionarioId = null significa manter o mesmo responsavel.
    @Transactional
    public Chamado retomarPendente(Long chamadoId, Long funcionarioId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = buscarPorId(chamadoId);
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

    // O funcionario conclui sua tentativa enviando obrigatoriamente UMA NOVA foto.
    @Transactional
    public Chamado solicitarFinalizacao(Long chamadoId, MultipartFile fotoSolucao, String emailFuncionario) {
        Usuario funcionario = buscarFuncionarioAutenticado(emailFuncionario);
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

    // Aprovacao definitiva pelo atendente. Um chamado resolvido nao e reaberto.
    @Transactional
    public Chamado aprovarConclusao(Long chamadoId, String emailAtendente) {
        Usuario atendente = buscarAtendenteAutenticado(emailAtendente);
        Chamado chamado = buscarPorId(chamadoId);
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
        Chamado chamado = buscarPorId(chamadoId);
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
                ? " Novo responsavel: " + responsavel.getNome() + "."
                : "";
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

    // Confere se o usuario do aplicativo e um funcionario ativo.
    private Usuario buscarFuncionarioAutenticado(String emailAutenticado) {
        Usuario funcionario = usuarioService.buscarPorEmail(emailAutenticado);

        if (funcionario.getPerfil() != PerfilUsuario.FUNCIONARIO
                || !Boolean.TRUE.equals(funcionario.getAtivo())) {
            throw new IllegalArgumentException("Acesso permitido somente a funcionarios ativos.");
        }

        return funcionario;
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

    private void validarLocalizacao(String endereco, Double latitude, Double longitude) {
        if (endereco.length() > 255) {
            throw new IllegalArgumentException("O endereco deve ter no maximo 255 caracteres.");
        }
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Informe latitude e longitude juntas.");
        }
        if (endereco.isBlank() && latitude == null) {
            throw new IllegalArgumentException("Informe um endereco ou selecione o local no mapa.");
        }
        if (latitude != null && (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("As coordenadas informadas sao invalidas.");
        }
    }
}
