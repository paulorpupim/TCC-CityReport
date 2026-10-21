package com.tccinfonet.cityreport.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.repository.UsuarioRepository;

@Configuration
public class ApiSecurityConfig {

    public static final String EMISSOR = "cityreport";
    public static final String PUBLICO = "cityreport-mobile";
    private static final Logger log = LoggerFactory.getLogger(ApiSecurityConfig.class);

    // Esta cadeia cuida SOMENTE das rotas /api/**. A SecurityConfig continua com a web.
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                                      UsuarioRepository usuarioRepository) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable) // A API usa Bearer token, nao cookies de sessao.
                .sessionManagement(sessoes -> sessoes.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .anyRequest().hasRole("FUNCIONARIO"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(conversorDeUsuario(usuarioRepository))));

        return http.build();
    }

    // Reutiliza o UsuarioDetailsService ja registrado no Spring para conferir a senha BCrypt.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao)
            throws Exception {
        return configuracao.getAuthenticationManager();
    }

    // A chave fica fora do repositorio. Sem variavel de ambiente, gera uma chave TEMPORARIA.
    // Isso facilita os testes locais, mas invalida os tokens a cada reinicializacao da aplicacao.
    @Bean
    public SecretKey chaveJwt(@Value("${CITYREPORT_JWT_SECRET_BASE64:}") String segredoConfigurado) {
        byte[] bytes;
        if (segredoConfigurado == null || segredoConfigurado.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT: usando chave temporaria para desenvolvimento; tokens expiram ao reiniciar o backend.");
        } else {
            try {
                bytes = Base64.getDecoder().decode(segredoConfigurado.trim());
            } catch (IllegalArgumentException erro) {
                throw new IllegalStateException("CITYREPORT_JWT_SECRET_BASE64 deve estar em Base64 valido.", erro);
            }
            if (bytes.length < 32) {
                throw new IllegalStateException("A chave JWT precisa conter pelo menos 32 bytes.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Confere a assinatura, a validade, quem emitiu e para qual aplicacao o token foi criado.
        OAuth2TokenValidator<Jwt> validarPublico = jwt ->
                jwt.getAudience().contains(PUBLICO)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token", "Token destinado a outra aplicacao.", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(EMISSOR), validarPublico));
        return decoder;
    }

    // Consulta o banco a cada requisicao para respeitar desativacoes e mudancas de perfil imediatamente.
    // A assinatura do token e validada pelo JwtDecoder antes de chegar neste metodo.
    private JwtAuthenticationConverter conversorDeUsuario(UsuarioRepository usuarioRepository) {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(jwt.getSubject()).orElse(null);
            if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())
                    || usuario.getPerfil() != PerfilUsuario.FUNCIONARIO) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        });
        return conversor;
    }
}
