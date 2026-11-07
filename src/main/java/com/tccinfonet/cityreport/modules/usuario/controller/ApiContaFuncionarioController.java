package com.tccinfonet.cityreport.modules.usuario.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tccinfonet.cityreport.modules.usuario.dto.AlterarSenhaMobileRequestDTO;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/funcionarios")
public class ApiContaFuncionarioController {

    private final UsuarioService usuarioService;

    // O usuario e identificado pelo JWT autenticado, nunca por um ID enviado pelo celular.
    @PostMapping("/minha-senha")
    public ResponseEntity<?> alterarMinhaSenha(
            Authentication autenticacao,
            @Valid @RequestBody AlterarSenhaMobileRequestDTO dados) {

        Usuario usuario = usuarioService.buscarPorEmail(autenticacao.getName());
        if (usuario.getPerfil() != PerfilUsuario.FUNCIONARIO || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas a funcionarios ativos.");
        }

        if (!dados.novaSenha().equals(dados.confirmarNovaSenha())) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "A confirmacao da nova senha nao confere."));
        }

        if (dados.novaSenha().equals(dados.senhaAtual())) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "A nova senha deve ser diferente da atual."));
        }

        try {
            // Reaproveita a verificacao da senha atual e o BCrypt ja usados no perfil web.
            usuarioService.alterarSenha(usuario.getId(), dados.senhaAtual(), dados.novaSenha());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException erro) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", erro.getMessage()));
        }
    }
}
