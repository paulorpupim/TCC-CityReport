package com.tccinfonet.cityreport.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SessaoWebAtivaFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public SessaoWebAtivaFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        // Requisicoes sem login continuam no fluxo normal do Spring Security.
        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(autenticacao.getName())
                .orElse(null);

        boolean contaAtiva = usuario != null && Boolean.TRUE.equals(usuario.getAtivo());
        boolean mesmoPerfil = usuario != null && usuario.getPerfil() != null
                && autenticacao.getAuthorities().stream().anyMatch(autoridade ->
                    ("ROLE_" + usuario.getPerfil().name()).equals(autoridade.getAuthority()));

        if (!contaAtiva || !mesmoPerfil) {
            // Limpa a autenticacao e invalida a sessao/cookie associado.
            new SecurityContextLogoutHandler().logout(request, response, autenticacao);
            response.sendRedirect(request.getContextPath() + "/login?logout");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
