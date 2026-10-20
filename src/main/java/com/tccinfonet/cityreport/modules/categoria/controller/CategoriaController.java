
package com.tccinfonet.cityreport.modules.categoria.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.categoria.service.CategoriaService;
import com.tccinfonet.cityreport.modules.setor.entity.Setor;
import com.tccinfonet.cityreport.modules.setor.service.SetorService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/atendente/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final SetorService setorService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "categoria/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Categoria categoria = new Categoria();
        categoria.setSetor(new Setor());
        model.addAttribute("categoria", categoria);

        return prepararFormulario(model, "Cadastrar categoria", "/atendente/categorias");
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("categoria") Categoria categoria,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return prepararFormulario(model, "Cadastrar categoria", "/atendente/categorias");
        }

        try {
            categoriaService.cadastrar(categoria);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Categoria cadastrada com sucesso.");
            return "redirect:/atendente/categorias";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            resultado.reject("categoria.invalida", e.getMessage());
            return prepararFormulario(model, "Cadastrar categoria", "/atendente/categorias");
        }
    }

    @GetMapping("/{id}/editar")
    public String formularioEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("categoria", categoriaService.buscarPorId(id));
        return prepararFormulario(model, "Editar categoria", "/atendente/categorias/" + id + "/editar");
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("categoria") Categoria categoria,
                         BindingResult resultado,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return prepararFormulario(model, "Editar categoria", "/atendente/categorias/" + id + "/editar");
        }

        try {
            categoriaService.editar(id, categoria);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Categoria atualizada com sucesso.");
            return "redirect:/atendente/categorias";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            resultado.reject("categoria.invalida", e.getMessage());
            return prepararFormulario(model, "Editar categoria", "/atendente/categorias/" + id + "/editar");
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.ativar(id);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Categoria ativada com sucesso.");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }

        return "redirect:/atendente/categorias";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoriaService.desativar(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Categoria desativada com sucesso.");
        return "redirect:/atendente/categorias";
    }

    private String prepararFormulario(Model model, String titulo, String acao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        model.addAttribute("setores", setorService.listarAtivos());
        return "categoria/formulario";
    }
}
