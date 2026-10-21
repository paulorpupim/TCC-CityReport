package com.tccinfonet.cityreport.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

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
                        .defaultSuccessUrl("/", true)
                        .permitAll())

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
