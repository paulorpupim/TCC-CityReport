package com.tccinfonet.cityreport.modules.chamado.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.tccinfonet.cityreport.modules.chamado.service.MapaChamadosService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MapaChamadosController {

    private final MapaChamadosService mapaChamadosService;

    // Rota protegida pela regra /atendente/** do Spring Security.
    @GetMapping("/atendente/chamados/mapa")
    public String visualizarMapa(Model model) {
        var resultado = mapaChamadosService.consultar();
        model.addAttribute("pontos", resultado.pontos());
        model.addAttribute("totalChamados", resultado.total());
        model.addAttribute("semLocalizacao", resultado.semLocalizacao());
        return "chamado/administracao/mapa";
    }
}
