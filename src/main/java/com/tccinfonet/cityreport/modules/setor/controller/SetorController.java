package com.tccinfonet.cityreport.modules.setor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tccinfonet.cityreport.modules.setor.entity.Setor;
import com.tccinfonet.cityreport.modules.setor.service.SetorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/atendente/setores")
public class SetorController {

    private final SetorService setorService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("setores", setorService.listarTodos());
        return "setor/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("setor", new Setor());
        return prepararFormulario(model, "Cadastrar setor", "/atendente/setores");
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("setor") Setor setor,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return prepararFormulario(model, "Cadastrar setor", "/atendente/setores");
        }

        try {
            setorService.cadastrar(setor);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Setor cadastrado com sucesso.");
            return "redirect:/atendente/setores";
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("nome", "nome.invalido", e.getMessage());
            return prepararFormulario(model, "Cadastrar setor", "/atendente/setores");
        }
    }

    @GetMapping("/{id}/editar")
    public String formularioEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("setor", setorService.buscarPorId(id));
        return prepararFormulario(model, "Editar setor", "/atendente/setores/" + id + "/editar");
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("setor") Setor setor,
                         BindingResult resultado,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return prepararFormulario(model, "Editar setor", "/atendente/setores/" + id + "/editar");
        }

        try {
            setorService.editar(id, setor);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Setor atualizado com sucesso.");
            return "redirect:/atendente/setores";
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("nome", "nome.invalido", e.getMessage());
            return prepararFormulario(model, "Editar setor", "/atendente/setores/" + id + "/editar");
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        setorService.ativar(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Setor ativado com sucesso.");
        return "redirect:/atendente/setores";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        setorService.desativar(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Setor desativado com sucesso.");
        return "redirect:/atendente/setores";
    }

    private String prepararFormulario(Model model, String titulo, String acao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        return "setor/formulario";
    }
}
