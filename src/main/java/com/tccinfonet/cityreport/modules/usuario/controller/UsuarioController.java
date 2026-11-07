package com.tccinfonet.cityreport.modules.usuario.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tccinfonet.cityreport.modules.setor.entity.Setor;
import com.tccinfonet.cityreport.modules.setor.service.SetorService;
import com.tccinfonet.cityreport.modules.usuario.dto.UsuarioRequestDTO;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/atendente")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final SetorService setorService;

    @GetMapping("/usuarios")
    public String listar(Model model) {
        model.addAttribute("atendentes", usuarioService.listarPorPerfil(PerfilUsuario.ATENDENTE));
        model.addAttribute("funcionarios", usuarioService.listarPorPerfil(PerfilUsuario.FUNCIONARIO));
        return "usuario/lista";
    }

    @GetMapping("/usuarios/novo")
    public String novo(Model model) {
        model.addAttribute("usuarioDTO", new UsuarioRequestDTO());
        return prepararPagina(model, "Cadastrar usuario", "/atendente/usuarios");
    }

    @PostMapping("/usuarios")
    public String cadastrar(@Valid @ModelAttribute("usuarioDTO") UsuarioRequestDTO usuarioDTO,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return prepararPagina(model, "Cadastrar usuario", "/atendente/usuarios");
        }

        try {
            Usuario dados = converterParaUsuario(usuarioDTO);

            if (usuarioDTO.getPerfil() == PerfilUsuario.ATENDENTE) {
                usuarioService.cadastrarAtendente(dados);
            } else if (usuarioDTO.getPerfil() == PerfilUsuario.FUNCIONARIO) {
                usuarioService.cadastrarFuncionario(dados);
            } else {
                throw new IllegalArgumentException("Selecione Atendente ou Funcionario.");
            }

            redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuario cadastrado com sucesso.");
            return "redirect:/atendente/usuarios";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            resultado.reject("usuario.invalido", e.getMessage());
            return prepararPagina(model, "Cadastrar usuario", "/atendente/usuarios");
        }
    }

    @GetMapping("/usuarios/{id}/editar")
    public String exibirEdicao(@PathVariable Long id, Model model) {
        Usuario usuario = buscarUsuarioInterno(id);

        UsuarioRequestDTO usuarioDTO = new UsuarioRequestDTO();
        usuarioDTO.setNome(usuario.getNome());
        usuarioDTO.setEmail(usuario.getEmail());
        usuarioDTO.setPerfil(usuario.getPerfil());

        if (usuario.getSetor() != null) {
            usuarioDTO.setSetorId(usuario.getSetor().getId());
        }

        model.addAttribute("usuarioDTO", usuarioDTO);
        return prepararPagina(model, "Editar usuario", "/atendente/usuarios/" + id + "/editar");
    }

    @PostMapping("/usuarios/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("usuarioDTO") UsuarioRequestDTO usuarioDTO,
                         BindingResult resultado,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Usuario usuario = buscarUsuarioInterno(id);
        String acao = "/atendente/usuarios/" + id + "/editar";
        usuarioDTO.setPerfil(usuario.getPerfil());

        if (resultado.hasErrors()) {
            return prepararPagina(model, "Editar usuario", acao);
        }

        try {
            Usuario dados = converterParaUsuario(usuarioDTO);

            if (usuario.getPerfil() == PerfilUsuario.FUNCIONARIO) {
                usuarioService.editarFuncionario(id, dados);
            } else {
                usuarioService.editarDadosPessoais(id, dados);
            }

            redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuario atualizado com sucesso.");
            return "redirect:/atendente/usuarios";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            resultado.reject("usuario.invalido", e.getMessage());
            return prepararPagina(model, "Editar usuario", acao);
        }
    }

    @PostMapping("/usuarios/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        buscarUsuarioInterno(id);

        try {
            usuarioService.ativar(id);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuario ativado com sucesso.");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        return "redirect:/atendente/usuarios";
    }

    @PostMapping("/usuarios/{id}/desativar")
    public String desativar(@PathVariable Long id, Authentication autenticacao,
                           RedirectAttributes redirectAttributes) {
        buscarUsuarioInterno(id);

        try {
            usuarioService.desativar(id, autenticacao.getName());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuario desativado com sucesso.");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        return "redirect:/atendente/usuarios";
    }


    // Permite que o atendente altere apenas a senha da propria conta.
    @GetMapping("/minha-senha")
    public String exibirAlteracaoSenha(Authentication autenticacao, Model model) {
        Usuario atendente = usuarioService.buscarPorEmail(autenticacao.getName());
        model.addAttribute("nomeAtendente", atendente.getNome());
        model.addAttribute("emailAtendente", atendente.getEmail());
        return "usuario/minha-senha";
    }

    @PostMapping("/minha-senha")
    public String alterarMinhaSenha(@RequestParam String senhaAtual,
                                    @RequestParam String novaSenha,
                                    @RequestParam String confirmarNovaSenha,
                                    Authentication autenticacao,
                                    RedirectAttributes redirectAttributes) {
        if (novaSenha == null || !novaSenha.equals(confirmarNovaSenha)) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "A confirmação não corresponde à nova senha.");
            return "redirect:/atendente/minha-senha";
        }

        try {
            Usuario atendente = usuarioService.buscarPorEmail(autenticacao.getName());
            usuarioService.alterarSenha(atendente.getId(), senhaAtual, novaSenha);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
            return "redirect:/atendente/minha-senha";
        }

        // Mantem a sessao web ativa, como na alteracao de senha do cidadao.
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Senha alterada com sucesso.");
        return "redirect:/atendente/minha-senha";
    }

    private Usuario converterParaUsuario(UsuarioRequestDTO usuarioDTO) {
        Usuario dados = new Usuario();
        dados.setNome(usuarioDTO.getNome());
        dados.setEmail(usuarioDTO.getEmail());
        dados.setSenha(usuarioDTO.getSenha());

        if (usuarioDTO.getSetorId() != null) {
            Setor setor = new Setor();
            setor.setId(usuarioDTO.getSetorId());
            dados.setSetor(setor);
        }

        return dados;
    }

    // Impede a area administrativa de editar contas de cidadaos.
    private Usuario buscarUsuarioInterno(Long id) {
        Usuario usuario;
        try {
            usuario = usuarioService.buscarPorId(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado.");
        }

        if (usuario.getPerfil() == PerfilUsuario.CIDADAO) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario interno nao encontrado.");
        }

        return usuario;
    }

    // Prepara os dados da pagina HTML compartilhada por cadastro e edicao.
    private String prepararPagina(Model model, String titulo, String acao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        model.addAttribute("setores", setorService.listarAtivos());
        return "usuario/formulario";
    }
}
