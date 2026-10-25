package com.tccinfonet.cityreport.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tccinfonet.cityreport.modules.usuario.repository.UsuarioRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UsuarioRepository usuarioRepository) throws Exception {
        http
                .authorizeHttpRequests(autorizacao -> autorizacao

                        .requestMatchers("/cidadaos/cadastro").anonymous()
                        .requestMatchers("/login", "/error", "/favicon.ico").permitAll()

                        .requestMatchers("/atendente/**").hasRole("ATENDENTE")

                        .requestMatchers("/api/**").denyAll()

                        .requestMatchers(HttpMethod.GET,
                                "/chamados/*/fotos/*/imagem")
                        .hasAnyRole("ATENDENTE", "CIDADAO", "FUNCIONARIO")

                        .requestMatchers("/chamados", "/chamados/**", "/cidadaos", "/cidadaos/**")
                        .hasAnyRole("CIDADAO", "FUNCIONARIO")

                        .anyRequest().authenticated()
                )

                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .addFilterAfter(new SessaoWebAtivaFilter(usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
