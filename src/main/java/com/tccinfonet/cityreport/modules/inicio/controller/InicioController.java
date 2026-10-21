package com.tccinfonet.cityreport.modules.inicio.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class InicioController {

    private final UsuarioService usuarioService;

    // Pagina inicial de qualquer usuario autenticado.
    @GetMapping({"/", "/inicio"})
    public String inicio(Authentication autenticacao, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(autenticacao.getName());

        model.addAttribute("nomeUsuario", usuario.getNome());
        model.addAttribute("perfilUsuario", usuario.getPerfil());

        return "inicio";
    }
}
