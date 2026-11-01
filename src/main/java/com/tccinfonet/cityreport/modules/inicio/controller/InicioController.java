package com.tccinfonet.cityreport.modules.inicio.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.tccinfonet.cityreport.modules.chamado.service.ChamadoService;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class InicioController {

    private final UsuarioService usuarioService;
    private final ChamadoService chamadoService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // A rota de entrada continua unica. A pagina exibida depende do perfil autenticado.
    @GetMapping({"/", "/inicio"})
    public String inicio(Authentication autenticacao, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(autenticacao.getName());
        model.addAttribute("nomeUsuario", usuario.getNome());
        model.addAttribute("perfilUsuario", usuario.getPerfil());

        if (usuario.getPerfil() == PerfilUsuario.ATENDENTE) {
            return "inicio/atendente";
        }

        // O funcionario utiliza a web apenas como usuario comum.
        // Cada chamado exibido aqui pertence a conta autenticada.
        var meusChamados = chamadoService.listarDoCidadao(autenticacao.getName());
        model.addAttribute("totalMeusChamados", meusChamados.size());
        model.addAttribute("chamadosRecentes", meusChamados.stream().limit(3).toList());
        return "inicio/cidadao";
    }
}
