package com.tccinfonet.cityreport.modules.relatorio.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.relatorio.dto.FiltroRelatorioDTO;
import com.tccinfonet.cityreport.modules.setor.entity.Setor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

/** Consolida indicadores sem carregar textos, imagens nem dados dos cidadãos. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatorioAnaliseService {

    private final EntityManager entityManager;

    public AnaliseRelatorio analisar(FiltroRelatorioDTO filtro) {
        TempoResolucao tempo = calcularTempoResolucao(filtro);
        return new AnaliseRelatorio(tempo.quantidade(), tempo.mediaSegundos(),
                listarPorSetor(filtro), listarPorCategoria(filtro));
    }

    /** Considera somente conclusões aprovadas, com datas coerentes, da seleção atual. */
    TempoResolucao calcularTempoResolucao(FiltroRelatorioDTO filtro) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Chamado> root = query.from(Chamado.class);
        Predicate filtroBase = RelatorioSpecification.criar(filtro).toPredicate(root, query, cb);
        query.multiselect(root.get("dataCriacao").alias("abertura"),
                root.get("dataConclusao").alias("conclusao"));
        query.where(cb.and(filtroBase,
                cb.equal(root.get("status"), StatusChamado.RESOLVIDO),
                cb.isNotNull(root.get("dataConclusao"))));

        long quantidade = 0;
        long somaSegundos = 0;
        // A consulta lê apenas duas datas, em vez de carregar as entidades completas.
        try (Stream<Tuple> linhas = entityManager.createQuery(query).getResultStream()) {
            var iterador = linhas.iterator();
            while (iterador.hasNext()) {
                Tuple linha = iterador.next();
                LocalDateTime abertura = linha.get("abertura", LocalDateTime.class);
                LocalDateTime conclusao = linha.get("conclusao", LocalDateTime.class);
                Long segundos = segundosParaResolver(abertura, conclusao);
                if (segundos == null) continue;
                quantidade++;
                somaSegundos += segundos;
            }
        }
        return new TempoResolucao(quantidade,
                quantidade == 0 ? null : Math.round((double) somaSegundos / quantidade));
    }

    private List<Distribuicao> listarPorSetor(FiltroRelatorioDTO filtro) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Chamado> root = query.from(Chamado.class);
        Join<Chamado, Categoria> categoria = root.join("categoria");
        Join<Categoria, Setor> setor = categoria.join("setor");
        Expression<Long> quantidade = cb.count(root);
        query.multiselect(setor.get("nome").alias("nome"), quantidade.alias("quantidade"));
        query.where(RelatorioSpecification.criar(filtro).toPredicate(root, query, cb));
        query.groupBy(setor.get("id"), setor.get("nome"));
        query.orderBy(cb.desc(quantidade), cb.asc(setor.get("nome")));
        return entityManager.createQuery(query).setMaxResults(5).getResultList().stream()
                .map(linha -> new Distribuicao(linha.get("nome", String.class),
                        linha.get("quantidade", Long.class))).toList();
    }

    private List<Distribuicao> listarPorCategoria(FiltroRelatorioDTO filtro) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Chamado> root = query.from(Chamado.class);
        Join<Chamado, Categoria> categoria = root.join("categoria");
        Join<Categoria, Setor> setor = categoria.join("setor");
        Expression<Long> quantidade = cb.count(root);
        query.multiselect(categoria.get("nome").alias("categoria"),
                setor.get("nome").alias("setor"), quantidade.alias("quantidade"));
        query.where(RelatorioSpecification.criar(filtro).toPredicate(root, query, cb));
        query.groupBy(categoria.get("id"), categoria.get("nome"), setor.get("id"), setor.get("nome"));
        query.orderBy(cb.desc(quantidade), cb.asc(categoria.get("nome")));
        return entityManager.createQuery(query).setMaxResults(5).getResultList().stream()
                .map(linha -> new Distribuicao(
                        linha.get("categoria", String.class) + " · " + linha.get("setor", String.class),
                        linha.get("quantidade", Long.class))).toList();
    }

    /** Descarta dados legados ausentes ou com conclusão anterior à abertura. */
    static Long segundosParaResolver(LocalDateTime abertura, LocalDateTime conclusao) {
        if (abertura == null || conclusao == null || conclusao.isBefore(abertura)) return null;
        return Duration.between(abertura, conclusao).getSeconds();
    }

    record TempoResolucao(long quantidade, Long mediaSegundos) {}

    public record Distribuicao(String nome, long quantidade) {}

    public record AnaliseRelatorio(long resolvidosComDatasValidas, Long mediaSegundos,
            List<Distribuicao> setores, List<Distribuicao> categorias) {
        public String tempoMedioFormatado() {
            return formatarDuracao(mediaSegundos);
        }
    }

    /** Uma duração nunca é apresentada como resolução no próprio período de abertura. */
    public static String formatarDuracao(Long segundos) {
        if (segundos == null) return "Não disponível";
        long dias = segundos / 86400;
        long horas = (segundos % 86400) / 3600;
        long minutos = (segundos % 3600) / 60;
        if (dias > 0) return dias + " d " + horas + " h";
        if (horas > 0) return horas + " h " + minutos + " min";
        if (minutos > 0) return minutos + " min";
        return "Menos de 1 min";
    }
}
