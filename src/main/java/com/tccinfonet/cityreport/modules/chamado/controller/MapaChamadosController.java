package com.tccinfonet.cityreport.modules.chamado.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tccinfonet.cityreport.modules.chamado.service.MapaChamadosService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MapaChamadosController {

    private final MapaChamadosService mapaChamadosService;

    // Rota protegida pela regra /atendente/** do Spring Security.
    @GetMapping("/atendente/chamados/mapa")
    public String visualizarMapa(
            @RequestParam(name = "modo", defaultValue = "periodo") String modo,
            @RequestParam(name = "valor", required = false) String valor,
            Model model) {
        var resultado = mapaChamadosService.consultar(modo, valor);
        model.addAttribute("pontos", resultado.pontos());
        model.addAttribute("totalChamados", resultado.total());
        model.addAttribute("semLocalizacao", resultado.semLocalizacao());
        model.addAttribute("modoFiltro", resultado.modo());
        model.addAttribute("valorFiltro", resultado.valor());
        model.addAttribute("anoAtual", resultado.anoAtual());
        model.addAttribute("anosDisponiveis", resultado.anosDisponiveis());
        model.addAttribute("descricaoPeriodo", resultado.descricaoPeriodo());
        return "chamado/administracao/mapa";
    }
}
