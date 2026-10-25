package com.tccinfonet.cityreport.modules.usuario.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.tccinfonet.cityreport.modules.chamado._historico_chamado.service.HistoricoChamadoService;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.setor.entity.Setor;
import com.tccinfonet.cityreport.modules.setor.service.SetorService;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SetorService setorService;

    // Recebe o PasswordEncoder (BCrypt) definido no SecurityConfig.
    private final PasswordEncoder passwordEncoder;
    private final ChamadoRepository chamadoRepository;
    private final HistoricoChamadoService historicoChamadoService;
    private final EntityManager entityManager;

    // Retorna os usuarios cadastrados, inclusive os inativos.
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByOrderByNomeAsc();
    }

    // Retorna apenas os usuarios de um perfil especifico.
    public List<Usuario> listarPorPerfil(PerfilUsuario perfil) {
        return usuarioRepository.findByPerfilOrderByNomeAsc(perfil);
    }

    // Usado futuramente para atribuir chamados a funcionarios do setor responsavel.
    public List<Usuario> listarFuncionariosAtivosPorSetor(Long setorId) {
        return usuarioRepository.findByPerfilAndSetorIdAndAtivoTrueAndSetorAtivoTrueOrderByNomeAsc(
                PerfilUsuario.FUNCIONARIO, setorId);
    }

    // Busca pelo identificador, informando erro quando nao encontrar.
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
    }

    // Consulta por e-mail. Tambem sera util na autenticacao futuramente.
    public Usuario buscarPorEmail(String email) {
        String emailNormalizado = validarENormalizarEmail(email);
        return usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
    }

    // Cadastro publico: o perfil e sempre CIDADAO, independentemente do formulario.
    @Transactional
    public Usuario cadastrarCidadao(Usuario dados) {
        return cadastrar(dados, PerfilUsuario.CIDADAO, null);
    }

    // Cadastro administrativo de atendentes.
    @Transactional
    public Usuario cadastrarAtendente(Usuario dados) {
        return cadastrar(dados, PerfilUsuario.ATENDENTE, null);
    }

    // Cadastro administrativo de funcionarios, obrigatoriamente vinculados a um setor ativo.
    @Transactional
    public Usuario cadastrarFuncionario(Usuario dados) {
        Setor setor = validarSetorAtivo(dados.getSetor());
        return cadastrar(dados, PerfilUsuario.FUNCIONARIO, setor);
    }

    // Altera apenas nome e e-mail. Perfil, setor, situacao e senha nao mudam aqui.
    @Transactional
    public Usuario editarDadosPessoais(Long id, Usuario dados) {
        Usuario usuario = buscarPorId(id);
        atualizarNomeEEmail(usuario, dados);
        return usuarioRepository.save(usuario);
    }

    // Edicao administrativa do funcionario: nome, e-mail e setor responsavel.
    @Transactional
    public Usuario editarFuncionario(Long id, Usuario dados) {
        Usuario usuario = buscarPorId(id);

        if (usuario.getPerfil() != PerfilUsuario.FUNCIONARIO) {
            throw new IllegalArgumentException("O usuario informado nao e um funcionario de campo.");
        }

        Setor setor = validarSetorAtivo(dados.getSetor());

        // A troca de setor nao pode deixar chamados ativos com um responsavel
        // que nao pertence mais ao setor da categoria.
        boolean setorMudou = usuario.getSetor() == null
                || !Objects.equals(usuario.getSetor().getId(), setor.getId());

        if (setorMudou && chamadoRepository.existsByFuncionarioResponsavelIdAndStatusIn(
                usuario.getId(), List.of(
                        StatusChamado.ATRIBUIDO,
                        StatusChamado.EM_EXECUCAO,
                        StatusChamado.PENDENTE,
                        StatusChamado.AGUARDANDO_VALIDACAO))) {
            throw new IllegalArgumentException(
                    "Nao e possivel trocar o setor: o funcionario possui chamados em andamento. "
                            + "Reatribua ou conclua esses chamados antes de alterar o setor.");
        }

        atualizarNomeEEmail(usuario, dados);
        usuario.setSetor(setor);

        return usuarioRepository.save(usuario);
    }

    // Exige a senha atual para permitir que o usuario altere a propria senha.
    @Transactional
    public void alterarSenha(Long id, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(id);

        if (senhaAtual == null || !passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }

        validarSenha(novaSenha);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    // Se um funcionario for desativado durante a execucao, pausa seus chamados.
    // O atendente podera retoma-los depois, escolhendo outro responsavel.
    @Transactional
    public void desativar(Long id, String emailAtendente) {
        Usuario atendente = buscarPorEmail(emailAtendente);
        if (atendente.getPerfil() != PerfilUsuario.ATENDENTE
                || !Boolean.TRUE.equals(atendente.getAtivo())) {
            throw new IllegalArgumentException("Apenas atendentes ativos podem desativar usuarios.");
        }

        // Usa a mesma trava do inicio de execucao para evitar uma desativacao
        // simultanea a um novo inicio de servico pelo aplicativo.
        Usuario usuario = entityManager.find(Usuario.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (usuario == null) {
            throw new EntityNotFoundException("Usuario nao encontrado.");
        }
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            return;
        }

        if (usuario.getPerfil() == PerfilUsuario.FUNCIONARIO) {
            List<Chamado> emExecucao = chamadoRepository.findByFuncionarioResponsavelIdAndStatus(
                    id, StatusChamado.EM_EXECUCAO);

            for (Chamado chamado : emExecucao) {
                chamado.setStatus(StatusChamado.PENDENTE);
                chamadoRepository.save(chamado);

                historicoChamadoService.registrar(
                        chamado,
                        atendente,
                        "Execucao pausada porque o funcionario " + usuario.getNome()
                                + " foi desativado. Aguardando nova atribuicao pelo atendente.",
                        StatusChamado.EM_EXECUCAO,
                        StatusChamado.PENDENTE);
            }
        }

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    // Para reativar funcionarios, seu setor precisa continuar ativo.
    @Transactional
    public void ativar(Long id) {
        Usuario usuario = buscarPorId(id);

        if (usuario.getPerfil() == PerfilUsuario.FUNCIONARIO) {
            validarSetorAtivo(usuario.getSetor());
        }

        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    // Reutilizado pelos tres tipos de cadastro. Nunca aceita perfil ou situacao do formulario.
    private Usuario cadastrar(Usuario dados, PerfilUsuario perfil, Setor setor) {
        String nome = validarENormalizarNome(dados.getNome());
        String email = validarENormalizarEmail(dados.getEmail());
        validarSenha(dados.getSenha());

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ja existe um usuario cadastrado com esse e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dados.getSenha()));
        usuario.setPerfil(perfil);
        usuario.setSetor(setor);
        usuario.setAtivo(true);

        return usuarioRepository.save(usuario);
    }

    private void atualizarNomeEEmail(Usuario usuario, Usuario dados) {
        String nome = validarENormalizarNome(dados.getNome());
        String email = validarENormalizarEmail(dados.getEmail());

        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, usuario.getId())) {
            throw new IllegalArgumentException("Ja existe outro usuario com esse e-mail.");
        }

        usuario.setNome(nome);
        usuario.setEmail(email);
    }

    private Setor validarSetorAtivo(Setor setorInformado) {
        if (setorInformado == null || setorInformado.getId() == null) {
            throw new IllegalArgumentException("Selecione um setor para o funcionario.");
        }

        Setor setor = setorService.buscarPorId(setorInformado.getId());
        if (!Boolean.TRUE.equals(setor.getAtivo())) {
            throw new IllegalArgumentException("O setor selecionado esta inativo.");
        }

        return setor;
    }

    private String validarENormalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome e obrigatorio.");
        }

        String nomeNormalizado = nome.trim();
        if (nomeNormalizado.length() > 100) {
            throw new IllegalArgumentException("O nome deve ter no maximo 100 caracteres.");
        }

        return nomeNormalizado;
    }

    private String validarENormalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail e obrigatorio.");
        }

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (emailNormalizado.length() > 150) {
            throw new IllegalArgumentException("O e-mail deve ter no maximo 150 caracteres.");
        }
        if (!emailNormalizado.contains("@") || emailNormalizado.contains(" ")) {
            throw new IllegalArgumentException("Informe um e-mail valido.");
        }

        return emailNormalizado;
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.isBlank() || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }
    }
}
