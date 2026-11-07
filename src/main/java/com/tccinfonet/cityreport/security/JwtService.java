package com.tccinfonet.cityreport.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final Duration DURACAO = Duration.ofHours(1);

    private final JwtEncoder jwtEncoder;

    // Gera um token assinado depois que o Spring Security autentica o funcionario.
    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet dados = JwtClaimsSet.builder()
                .issuer(ApiSecurityConfig.EMISSOR)
                .audience(List.of(ApiSecurityConfig.PUBLICO))
                .subject(usuario.getEmail())
                .issuedAt(agora)
                .expiresAt(agora.plus(DURACAO))
                .claim("perfil", usuario.getPerfil().name())
                .claim("versaoSenha", versaoSenha(usuario.getSenha()))
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();
    }

    // Identifica a versao da credencial SEM incluir o hash BCrypt no token.
    // Quando a senha muda, todos os tokens anteriores deixam de autorizar acesso.
    public static String versaoSenha(String hashBCrypt) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(hashBCrypt.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resumo);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 nao disponivel.", erro);
        }
    }

    public long getDuracaoSegundos() {
        return DURACAO.toSeconds();
    }
}
