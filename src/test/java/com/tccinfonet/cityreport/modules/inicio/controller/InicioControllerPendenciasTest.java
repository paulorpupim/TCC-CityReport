package com.tccinfonet.cityreport.modules.inicio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.service.ChamadoService;
import com.tccinfonet.cityreport.modules.inicio.service.InicioService;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class InicioControllerPendenciasTest {

    @Mock private UsuarioService usuarioService;
    @Mock private ChamadoService chamadoService;
    @Mock private InicioService inicioService;
    @Mock private Authentication autenticacao;
    @Mock private Model model;
    @InjectMocks private InicioController controller;

    @Test
    void repassaSomentePendenciasDoUsuarioAutenticadoParaTelaInicial() {
        Usuario cidadao = new Usuario();
        cidadao.setId(8L);
        cidadao.setNome("Maria");
        cidadao.setPerfil(PerfilUsuario.CIDADAO);
        Chamado pendente = new Chamado();
        pendente.setId(21L);
        List<Chamado> lista = List.of(pendente);

        when(autenticacao.getName()).thenReturn("maria@exemplo.com");
        when(usuarioService.buscarPorEmail("maria@exemplo.com")).thenReturn(cidadao);
        when(chamadoService.listarDoCidadao("maria@exemplo.com")).thenReturn(lista);
        when(chamadoService.consultarPendenciasInformacoes(8L))
                .thenReturn(new ChamadoService.PendenciasInformacoes(1L, lista));

        assertEquals("inicio/cidadao", controller.inicio(autenticacao, model));
        verify(model).addAttribute("quantidadePendencias", 1L);
        verify(model).addAttribute("pendenciasChamados", lista);
    }
}
