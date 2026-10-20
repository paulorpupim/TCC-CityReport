
package com.tccinfonet.cityreport.modules.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Usuario> findAllByOrderByNomeAsc();

    List<Usuario> findByPerfilOrderByNomeAsc(PerfilUsuario perfil);

    List<Usuario> findByPerfilAndSetorIdAndAtivoTrueAndSetorAtivoTrueOrderByNomeAsc(PerfilUsuario perfil, Long setorId);
}
