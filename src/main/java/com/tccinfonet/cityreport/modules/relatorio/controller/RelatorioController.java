package com.tccinfonet.cityreport.modules.relatorio.controller;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.tccinfonet.cityreport.modules.categoria.repository.CategoriaRepository;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.relatorio.dto.FiltroRelatorioDTO;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioService;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioAnaliseService;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioPdfService;
import com.tccinfonet.cityreport.modules.setor.repository.SetorRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/atendente/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final RelatorioAnaliseService relatorioAnaliseService;
    private final RelatorioPdfService relatorioPdfService;
    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;

    @GetMapping
    public String consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String prioridade,
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        FiltroRelatorioDTO filtro;
        try {
            filtro = criarFiltro(dataInicial, dataFinal, setorId, categoriaId, prioridade, status);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("mensagemErro", ex.getMessage());
            filtro = criarFiltro(null, null, null, null, null, null);
        }

        var resultado = relatorioService.consultar(filtro, pagina);
        model.addAttribute("filtro", filtro);
        model.addAttribute("resultado", resultado);
        model.addAttribute("analise", relatorioAnaliseService.analisar(filtro));
        model.addAttribute("paginaResultado", resultado.pagina());
        model.addAttribute("setores", setorRepository.findAllByOrderByNomeAsc());
        model.addAttribute("categorias", categoriaRepository.findAllByOrderByNomeAsc());
        return "relatorio/lista";
    }

    @GetMapping(value = "/csv", produces = "text/csv")
    public void exportarCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String prioridade,
            @RequestParam(required = false) StatusChamado status,
            HttpServletResponse response) throws IOException {

        FiltroRelatorioDTO filtro;
        try {
            filtro = criarFiltro(dataInicial, dataFinal, setorId, categoriaId, prioridade, status);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-chamados-"
                + LocalDate.now() + ".csv");
        // Não fecha a saída do servlet, gerenciada pelo contêiner HTTP.
        OutputStreamWriter escritor = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        relatorioService.exportarCsv(filtro, escritor);
    }

    @GetMapping(value = "/pdf", produces = "application/pdf")
    public void exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String prioridade,
            @RequestParam(required = false) StatusChamado status,
            HttpServletResponse response) throws IOException {

        FiltroRelatorioDTO filtro;
        try {
            filtro = criarFiltro(dataInicial, dataFinal, setorId, categoriaId, prioridade, status);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        // O arquivo é gerado antes de enviar os cabeçalhos, evitando download incompleto
        // quando ocorrer algum erro durante a construção do PDF.
        byte[] pdf = relatorioPdfService.gerar(filtro);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-chamados-"
                + LocalDate.now() + ".pdf");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private FiltroRelatorioDTO criarFiltro(LocalDate inicio, LocalDate fim, Long setorId,
            Long categoriaId, String prioridade, StatusChamado status) {
        return FiltroRelatorioDTO.criar(inicio, fim, setorId, categoriaId,
                prioridade, status, LocalDate.now());
    }
}
