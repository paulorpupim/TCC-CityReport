package com.tccinfonet.cityreport.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.repository.UsuarioRepository;

@Configuration
public class SecurityConfig {

    // Define quem pode acessar cada area da aplicacao web.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(
                                "/cidadaos/cadastro",
                                "/login",
                                "/error",
                                "/favicon.ico"
                            )
                        .permitAll()

                        .requestMatchers(
                                "/atendente/**"
                            )
                        .hasRole("ATENDENTE")
                        
                        .requestMatchers(
                                "/api/**"
                            )
                        .denyAll()

                        .requestMatchers(
                                "/cidadaos/**"
                            )
                        .authenticated()
                        .anyRequest()
                        .authenticated()
                    )
                .formLogin(login -> login
                        .defaultSuccessUrl("/cidadaos/perfil", false)
                        .permitAll())

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return email -> {
            String emailInformado = email == null ? "" : email.trim();

            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailInformado)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado."));

            return User.withUsername(usuario.getEmail())
                    .password(usuario.getSenha())
                    .roles(usuario.getPerfil().name())
                    .disabled(!Boolean.TRUE.equals(usuario.getAtivo()))
                    .build();
        };
    }

    // Mesmo algoritmo utilizado no cadastro e na verificacao de senhas.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
