package com.tccinfonet.cityreport.modules.relatorio.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tccinfonet.cityreport.modules.categoria.repository.CategoriaRepository;
import com.tccinfonet.cityreport.modules.relatorio.dto.FiltroRelatorioDTO;
import com.tccinfonet.cityreport.modules.relatorio.dto.LinhaRelatorioDTO;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioAnaliseService.AnaliseRelatorio;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioAnaliseService.Distribuicao;
import com.tccinfonet.cityreport.modules.relatorio.service.RelatorioService.ResultadoRelatorio;
import com.tccinfonet.cityreport.modules.setor.repository.SetorRepository;

import lombok.RequiredArgsConstructor;

/** PDF sintético para arquivo e reuniões, sem dados pessoais dos cidadãos. */
@Service
@RequiredArgsConstructor
public class RelatorioPdfService {

    private static final Color AZUL = new Color(25, 34, 49);
    private static final Color AZUL_CLARO = new Color(236, 246, 255);
    private static final Color CINZA = new Color(87, 105, 126);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RelatorioService relatorioService;
    private final RelatorioAnaliseService relatorioAnaliseService;
    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;

    public byte[] gerar(FiltroRelatorioDTO filtro) {
        ResultadoRelatorio resultado = relatorioService.consultar(filtro, 0);
        AnaliseRelatorio analise = relatorioAnaliseService.analisar(filtro);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 44, 44, 42, 44);
        try {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            titulo(documento, "CITYREPORT", 19, AZUL);
            titulo(documento, "Relatório administrativo de chamados", 15, AZUL);
            texto(documento, "Emitido em " + DATA.format(LocalDateTime.now()), 9, CINZA);

            subtitulo(documento, "Critérios da consulta");
            texto(documento, "Abertura: " + filtro.dataInicial().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " até " + filtro.dataFinal().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 10, AZUL);
            texto(documento, "Setor: " + nomeSetor(filtro.setorId()) + "  |  Categoria: "
                    + nomeCategoria(filtro.categoriaId()), 9, CINZA);
            texto(documento, "Prioridade oficial: " + prioridade(filtro.prioridade())
                    + "  |  Status atual: " + (filtro.status() == null ? "Todos" :
                            LinhaRelatorioDTO.descricaoStatus(filtro.status())), 9, CINZA);

            subtitulo(documento, "Resumo da seleção");
            PdfPTable resumo = new PdfPTable(2);
            resumo.setWidthPercentage(100);
            adicionarIndicador(resumo, "Chamados encontrados", resultado.total());
            adicionarIndicador(resumo, "Atualmente resolvidos", resultado.resolvidos());
            adicionarIndicador(resumo, "Em andamento", resultado.emAndamento());
            adicionarIndicador(resumo, "Outros encerramentos", resultado.encerradosSemResolucao());
            documento.add(resumo);

            subtitulo(documento, "Tempo de resolução");
            texto(documento, "Tempo médio entre abertura e aprovação do atendimento: "
                    + analise.tempoMedioFormatado(), 10, AZUL);
            texto(documento, "Amostra: " + analise.resolvidosComDatasValidas()
                    + " chamados resolvidos da seleção com datas válidas.", 9, CINZA);
            texto(documento, "O cálculo considera a situação atual dos chamados abertos no intervalo; "
                    + "não mede apenas as resoluções ocorridas dentro desse intervalo.", 9, CINZA);

            distribuicao(documento, "Cinco setores com mais chamados", analise.setores());
            distribuicao(documento, "Cinco categorias com mais chamados", analise.categorias());

            PdfPTable recentes = new PdfPTable(new float[]{2.1f, 3.7f, 2f});
            recentes.setWidthPercentage(100);
            recentes.setKeepTogether(true); // Não separa o título da amostra entre páginas.
            PdfPCell tituloAmostra = new PdfPCell(new Phrase(
                    "Dez registros mais recentes da seleção", fonte(12, true, AZUL)));
            tituloAmostra.setColspan(3);
            tituloAmostra.setBorder(Rectangle.NO_BORDER);
            tituloAmostra.setPaddingTop(15);
            tituloAmostra.setPaddingBottom(7);
            recentes.addCell(tituloAmostra);
            PdfPCell notaAmostra = new PdfPCell(new Phrase(
                    "A relação abaixo é uma amostra. Para obter todos os registros filtrados, "
                    + "utilize a exportação CSV da página de relatórios.", fonte(9, false, CINZA)));
            notaAmostra.setColspan(3);
            notaAmostra.setBorder(Rectangle.NO_BORDER);
            notaAmostra.setPaddingBottom(9);
            recentes.addCell(notaAmostra);
            cabecalho(recentes, "Protocolo", "Categoria", "Status atual");
            List<LinhaRelatorioDTO> linhas = resultado.pagina().getContent();
            for (int i = 0; i < Math.min(10, linhas.size()); i++) {
                LinhaRelatorioDTO linha = linhas.get(i);
                celula(recentes, linha.protocolo(), false);
                celula(recentes, linha.categoria(), false);
                celula(recentes, linha.status(), false);
            }
            if (linhas.isEmpty()) {
                PdfPCell vazia = new PdfPCell(new Phrase("Nenhum chamado encontrado.", fonte(9, false, CINZA)));
                vazia.setColspan(3);
                vazia.setPadding(8);
                recentes.addCell(vazia);
            }
            documento.add(recentes);
            texto(documento, "Relatório gerencial · Os indicadores refletem a situação atual dos "
                    + "chamados abertos no período. Não inclui dados pessoais dos cidadãos.", 8, CINZA);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Não foi possível gerar o relatório PDF.", ex);
        } finally {
            documento.close();
        }
        return saida.toByteArray();
    }

    private String nomeSetor(Long id) {
        if (id == null) return "Todos";
        return setorRepository.findById(id).map(setor -> setor.getNome()).orElse("Não encontrado");
    }

    private String nomeCategoria(Long id) {
        if (id == null) return "Todas";
        return categoriaRepository.findById(id).map(categoria -> categoria.getNome()).orElse("Não encontrada");
    }

    private static String prioridade(String valor) {
        if (valor == null) return "Todas";
        return switch (valor) {
            case "BAIXA" -> "Baixa";
            case "MEDIA" -> "Média";
            case "ALTA" -> "Alta";
            case "CRITICA" -> "Crítica";
            case "SEM_PRIORIDADE" -> "Ainda não definida";
            default -> valor;
        };
    }

    private static void distribuicao(Document documento, String titulo, List<Distribuicao> dados)
            throws DocumentException {
        subtitulo(documento, titulo);
        PdfPTable tabela = new PdfPTable(new float[]{5.5f, 1.1f});
        tabela.setWidthPercentage(100);
        cabecalho(tabela, "Setor / categoria", "Chamados");
        if (dados.isEmpty()) {
            PdfPCell vazia = new PdfPCell(new Phrase("Nenhum registro encontrado.", fonte(9, false, CINZA)));
            vazia.setColspan(2);
            vazia.setPadding(8);
            tabela.addCell(vazia);
        } else {
            for (Distribuicao linha : dados) {
                celula(tabela, linha.nome(), false);
                celula(tabela, Long.toString(linha.quantidade()), false);
            }
        }
        documento.add(tabela);
    }

    private static void adicionarIndicador(PdfPTable tabela, String rotulo, long valor) {
        PdfPCell celula = new PdfPCell();
        celula.setPadding(9);
        celula.setBorderColor(new Color(220, 231, 241));
        celula.addElement(new Phrase(rotulo, fonte(9, false, CINZA)));
        celula.addElement(new Phrase(Long.toString(valor), fonte(17, true, AZUL)));
        tabela.addCell(celula);
    }

    private static void cabecalho(PdfPTable tabela, String... campos) {
        for (String campo : campos) celula(tabela, campo, true);
    }

    private static void celula(PdfPTable tabela, String conteudo, boolean titulo) {
        PdfPCell celula = new PdfPCell(new Phrase(conteudo == null ? "" : conteudo,
                fonte(8.5f, titulo, titulo ? AZUL : CINZA)));
        celula.setPadding(7);
        celula.setBorder(Rectangle.BOTTOM);
        celula.setBorderColor(new Color(222, 231, 240));
        if (titulo) celula.setBackgroundColor(AZUL_CLARO);
        tabela.addCell(celula);
    }

    private static Font fonte(float tamanho, boolean negrito, Color cor) {
        return FontFactory.getFont(negrito ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA,
                tamanho, Font.NORMAL, cor);
    }

    private static void titulo(Document documento, String conteudo, float tamanho, Color cor)
            throws DocumentException {
        Paragraph p = new Paragraph(conteudo, fonte(tamanho, true, cor));
        p.setSpacingAfter(5);
        documento.add(p);
    }

    private static void subtitulo(Document documento, String titulo) throws DocumentException {
        Paragraph p = new Paragraph(titulo, fonte(12, true, AZUL));
        p.setSpacingBefore(16);
        p.setSpacingAfter(8);
        documento.add(p);
    }

    private static void texto(Document documento, String conteudo, float tamanho, Color cor)
            throws DocumentException {
        Paragraph p = new Paragraph(conteudo, fonte(tamanho, false, cor));
        p.setSpacingAfter(5);
        documento.add(p);
    }
}
