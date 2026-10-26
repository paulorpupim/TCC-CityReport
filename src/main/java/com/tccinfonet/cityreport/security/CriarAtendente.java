package com.tccinfonet.cityreport.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class CriarAtendente implements CommandLineRunner {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public void run(String... args){
        
        String email = "atendente@gmail.com";

        boolean existe = usuarioRepository.findByEmailIgnoreCase(email).isPresent();

        if (existe){
            return;
        }

        Usuario usuario = new Usuario();

        usuario.setNome("atendente");
        usuario.setEmail(email);
        usuario.setPerfil(PerfilUsuario.ATENDENTE);
        usuario.setSenha(passwordEncoder.encode("12345678"));

        usuarioRepository.save(usuario);

        System.out.println("Atendente primário criado!");

    }

}
