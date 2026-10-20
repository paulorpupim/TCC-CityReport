
package com.tccinfonet.cityreport.modules.usuario.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tccinfonet.cityreport.modules.usuario.dto.UsuarioRequestDTO;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cidadaos")
public class CidadaoController {

    private final UsuarioService usuarioService;

    @GetMapping("/cadastro")
    public String novo(Model model) {
        model.addAttribute("usuarioDTO", new UsuarioRequestDTO());
        return "cidadao/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(
            @Valid @ModelAttribute("usuarioDTO") UsuarioRequestDTO usuarioDTO,
            BindingResult resultado) {

        if (resultado.hasErrors()) {
            return "cidadao/cadastro";
        }

        try {
            Usuario dados = converterParaUsuario(usuarioDTO);
            usuarioService.cadastrarCidadao(dados);

            return "redirect:/login?cadastro=sucesso";
        } catch (IllegalArgumentException e) {
            resultado.reject("cadastro.invalido", e.getMessage());
            return "cidadao/cadastro";
        }
    }

    @GetMapping("/perfil")
    public String exibirPerfil(Authentication autenticacao, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(autenticacao.getName());

        UsuarioRequestDTO usuarioDTO = new UsuarioRequestDTO();
        usuarioDTO.setNome(usuario.getNome());
        usuarioDTO.setEmail(usuario.getEmail());

        model.addAttribute("usuarioDTO", usuarioDTO);

        return "cidadao/perfil";
    }

    @PostMapping("/perfil")
    public String editarPerfil(
            @Valid @ModelAttribute("usuarioDTO") UsuarioRequestDTO usuarioDTO,
            BindingResult resultado,
            Authentication autenticacao,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            return "cidadao/perfil";
        }

        Usuario usuario = usuarioService.buscarPorEmail(autenticacao.getName());
        String emailAnterior = usuario.getEmail();

        try {
            Usuario dados = converterParaUsuario(usuarioDTO);
            Usuario atualizado = usuarioService.editarDadosPessoais(
                    usuario.getId(), dados);

            if (!emailAnterior.equals(atualizado.getEmail())) {
                new SecurityContextLogoutHandler()
                        .logout(request, response, autenticacao);

                return "redirect:/login?emailAtualizado";
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Perfil atualizado com sucesso.");

            return "redirect:/cidadaos/perfil";
        } catch (IllegalArgumentException e) {
            resultado.reject("perfil.invalido", e.getMessage());
            return "cidadao/perfil";
        }
    }

    @PostMapping("/perfil/senha")
    public String alterarSenha(
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha,
            Authentication autenticacao,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioService.buscarPorEmail(autenticacao.getName());

        try {
            usuarioService.alterarSenha(
                    usuario.getId(), senhaAtual, novaSenha);

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Senha alterada com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "mensagemErro", e.getMessage());
        }

        return "redirect:/cidadaos/perfil";
    }

    private Usuario converterParaUsuario(UsuarioRequestDTO usuarioDTO) {
        Usuario dados = new Usuario();

        dados.setNome(usuarioDTO.getNome());
        dados.setEmail(usuarioDTO.getEmail());
        dados.setSenha(usuarioDTO.getSenha());

        return dados;
    }
}
