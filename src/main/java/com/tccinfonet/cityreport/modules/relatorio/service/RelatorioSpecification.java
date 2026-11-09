package com.tccinfonet.cityreport.modules.relatorio.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.relatorio.dto.FiltroRelatorioDTO;

import jakarta.persistence.criteria.Predicate;

/** Filtro compartilhado pela tabela, pelo CSV e pelas estatísticas do PDF. */
final class RelatorioSpecification {

    private RelatorioSpecification() {}

    static Specification<Chamado> criar(FiltroRelatorioDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.greaterThanOrEqualTo(root.get("dataCriacao"),
                    filtro.dataInicial().atStartOfDay()));
            condicoes.add(cb.lessThan(root.get("dataCriacao"),
                    filtro.dataFinal().plusDays(1).atStartOfDay()));

            if (filtro.setorId() != null) {
                condicoes.add(cb.equal(root.get("categoria").get("setor").get("id"), filtro.setorId()));
            }
            if (filtro.categoriaId() != null) {
                condicoes.add(cb.equal(root.get("categoria").get("id"), filtro.categoriaId()));
            }
            if (filtro.prioridade() != null) {
                if ("SEM_PRIORIDADE".equals(filtro.prioridade())) {
                    condicoes.add(cb.isNull(root.get("prioridadeOficial")));
                } else {
                    condicoes.add(cb.equal(root.get("prioridadeOficial"),
                            Prioridade.valueOf(filtro.prioridade())));
                }
            }
            if (filtro.status() != null) {
                condicoes.add(cb.equal(root.get("status"), filtro.status()));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }
}
