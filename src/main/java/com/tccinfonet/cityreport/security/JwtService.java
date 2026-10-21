package com.tccinfonet.cityreport.security;

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
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();
    }

    public long getDuracaoSegundos() {
        return DURACAO.toSeconds();
    }
}
