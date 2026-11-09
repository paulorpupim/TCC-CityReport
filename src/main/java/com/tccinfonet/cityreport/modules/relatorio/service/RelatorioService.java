package com.tccinfonet.cityreport.modules.relatorio.service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;
import com.tccinfonet.cityreport.modules.relatorio.dto.FiltroRelatorioDTO;
import com.tccinfonet.cityreport.modules.relatorio.dto.LinhaRelatorioDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatorioService {

    private static final int TAMANHO_PAGINA = 20;
    private static final int TAMANHO_LOTE_CSV = 250;
    private static final Sort ORDEM = Sort.by(Sort.Order.desc("dataCriacao"), Sort.Order.desc("id"));
    private static final Set<StatusChamado> ENCERRADOS_SEM_RESOLUCAO = Set.of(
            StatusChamado.CANCELADO, StatusChamado.RECUSADO, StatusChamado.DUPLICADO);
    private static final DateTimeFormatter DATA_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ChamadoRepository chamadoRepository;

    public ResultadoRelatorio consultar(FiltroRelatorioDTO filtro, int paginaSolicitada) {
        Specification<Chamado> especificacao = RelatorioSpecification.criar(filtro);
        int numeroPagina = Math.max(0, paginaSolicitada);
        Page<Chamado> chamados = chamadoRepository.findAll(especificacao,
                PageRequest.of(numeroPagina, TAMANHO_PAGINA, ORDEM));
        if (chamados.getTotalPages() > 0 && numeroPagina >= chamados.getTotalPages()) {
            chamados = chamadoRepository.findAll(especificacao,
                    PageRequest.of(chamados.getTotalPages() - 1, TAMANHO_PAGINA, ORDEM));
        }

        long total = chamados.getTotalElements();
        long resolvidos = chamadoRepository.count(especificacao.and(
                (root, query, cb) -> cb.equal(root.get("status"), StatusChamado.RESOLVIDO)));
        long encerradosSemResolucao = chamadoRepository.count(especificacao.and(
                (root, query, cb) -> root.get("status").in(ENCERRADOS_SEM_RESOLUCAO)));
        long emAndamento = total - resolvidos - encerradosSemResolucao;

        return new ResultadoRelatorio(chamados.map(LinhaRelatorioDTO::de),
                total, resolvidos, emAndamento, encerradosSemResolucao);
    }

    /** Exporta TODOS os resultados do filtro, não apenas a página visível. */
    public void exportarCsv(FiltroRelatorioDTO filtro, Writer destino) throws IOException {
        destino.write("\uFEFF"); // BOM UTF-8: abre corretamente no Excel brasileiro.
        escreverRegistro(destino, "Protocolo", "Título", "Setor", "Categoria",
                "Urgência percebida", "Prioridade oficial", "Status atual",
                "Data de abertura", "Data de conclusão");

        Specification<Chamado> especificacao = RelatorioSpecification.criar(filtro);
        int pagina = 0;
        Page<Chamado> lote;
        do {
            lote = chamadoRepository.findAll(especificacao,
                    PageRequest.of(pagina++, TAMANHO_LOTE_CSV, ORDEM));
            for (Chamado chamado : lote.getContent()) {
                LinhaRelatorioDTO linha = LinhaRelatorioDTO.de(chamado);
                escreverRegistro(destino, linha.protocolo(), linha.titulo(),
                        linha.setor(), linha.categoria(), linha.urgenciaPercebida(),
                        linha.prioridadeOficial(), linha.status(),
                        formatarData(linha.dataCriacao()), formatarData(linha.dataConclusao()));
            }
        } while (lote.hasNext());
        destino.flush();
    }

    private static String formatarData(LocalDateTime data) {
        return data == null ? "" : DATA_CSV.format(data);
    }

    private static void escreverRegistro(Writer destino, String... campos) throws IOException {
        for (int indice = 0; indice < campos.length; indice++) {
            if (indice > 0) destino.write(';');
            destino.write(escaparCampoCsv(campos[indice]));
        }
        destino.write("\r\n");
    }

    /** Protege contra quebra do CSV e execução de fórmulas em planilhas. */
    static String escaparCampoCsv(String valor) {
        String texto = valor == null ? "" : valor;
        String semEspacosIniciais = texto.stripLeading();
        if (!semEspacosIniciais.isEmpty() && "=+-@".indexOf(semEspacosIniciais.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    public record ResultadoRelatorio(
            Page<LinhaRelatorioDTO> pagina,
            long total,
            long resolvidos,
            long emAndamento,
            long encerradosSemResolucao) {}
}
