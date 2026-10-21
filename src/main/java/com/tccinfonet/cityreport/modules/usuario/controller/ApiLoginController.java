package com.tccinfonet.cityreport.modules.usuario.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tccinfonet.cityreport.modules.usuario.dto.LoginMobileRequestDTO;
import com.tccinfonet.cityreport.modules.usuario.dto.TokenMobileResponseDTO;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.repository.UsuarioRepository;
import com.tccinfonet.cityreport.security.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ApiLoginController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @PostMapping("/api/auth/login")
    public TokenMobileResponseDTO login(@Valid @RequestBody LoginMobileRequestDTO dados) {
        try {
            var autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dados.email().trim(), dados.senha()));

            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(autenticacao.getName())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Credenciais invalidas."));

            // Somente funcionarios ativos podem usar o aplicativo de campo.
            if (!Boolean.TRUE.equals(usuario.getAtivo()) || usuario.getPerfil() != PerfilUsuario.FUNCIONARIO) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas.");
            }

            return new TokenMobileResponseDTO(
                    jwtService.gerarToken(usuario), "Bearer", jwtService.getDuracaoSegundos());

        } catch (AuthenticationException erro) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas.");
        }
    }
}
