package com.tccinfonet.cityreport.modules.chamado.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tccinfonet.cityreport.modules.chamado.dto.PontoChamadoDTO;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {

        // Evita consultas adicionais por linha ao montar e exportar os relatórios.
        @Override
        @EntityGraph(attributePaths = {"categoria", "categoria.setor"})
        Page<Chamado> findAll(Specification<Chamado> especificacao, Pageable pageable);

        List<Chamado> findAllByOrderByDataCriacaoDesc();

        List<Chamado> findByCidadaoIdOrderByDataCriacaoDesc(Long cidadaoId);

        // Aviso da pagina inicial: conta os chamados que aguardam resposta do cidadao.
        long countByCidadaoIdAndStatus(Long cidadaoId, StatusChamado status);

        // Busca apenas os tres chamados que serao exibidos, sem carregar todos os registros.
        List<Chamado> findByCidadaoIdAndStatusOrderByDataCriacaoDesc(
                        Long cidadaoId, StatusChamado status, Pageable pagina);

        List<Chamado> findByFuncionarioResponsavelIdOrderByDataCriacaoDesc(Long funcionarioId);

        List<Chamado> findByStatusOrderByDataCriacaoDesc(StatusChamado status);

        List<Chamado> findByPrioridadeOficialOrderByDataCriacaoDesc(Prioridade prioridade);

        List<Chamado> findByCategoriaIdOrderByDataCriacaoDesc(Long categoriaId);

        boolean existsByChamadoOriginalId(Long originalId);

        Optional<Chamado> findByProtocolo(String protocolo);

        boolean existsByFuncionarioResponsavelIdAndStatus(Long funcionarioId, StatusChamado status);

        boolean existsByFuncionarioResponsavelIdAndStatusIn(
                        Long funcionarioId, Collection<StatusChamado> status);

        boolean existsByCategoriaIdAndStatusIn(
                        Long categoriaId,
                        Collection<StatusChamado> status);

        List<Chamado> findByFuncionarioResponsavelIdAndStatus(Long funcionarioId, StatusChamado status);

        // Consulta apenas os chamados da fila, sem carregar os concluidos.
        List<Chamado> findByFuncionarioResponsavelIdAndStatusInOrderByDataCriacaoDesc(
                        Long funcionarioId, Collection<StatusChamado> status);

        // Historico ordenado pela data da aprovacao do atendente.
        List<Chamado> findByFuncionarioResponsavelIdAndStatusOrderByDataConclusaoDesc(
                        Long funcionarioId, StatusChamado status);

        // Somente os dados necessarios ao mapa. Nao carrega entidades dos cidadaos,
        // fotos nem historicos. Os chamados encerrados e duplicados tambem entram.
        @Query("""
                SELECT new com.tccinfonet.cityreport.modules.chamado.dto.PontoChamadoDTO(
                    c.id, c.protocolo, c.titulo, c.endereco,
                    setor.id, setor.nome, categoria.id, categoria.nome,
                    c.status, c.prioridadeOficial, c.urgenciaPercebida,
                    c.latitude, c.longitude)
                FROM Chamado c
                JOIN c.categoria categoria
                JOIN categoria.setor setor
                WHERE c.dataCriacao >= :inicio AND c.dataCriacao < :fimExclusivo
                  AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
                ORDER BY c.dataCriacao DESC
                """)
        List<PontoChamadoDTO> listarPontosDoMapa(
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fimExclusivo") LocalDateTime fimExclusivo);

        // Filtros de data do mapa (sem carregar entidades inteiras).
        long countByDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(
                        LocalDateTime inicio, LocalDateTime fimExclusivo);

        @Query("SELECT MIN(c.dataCriacao) FROM Chamado c")
        LocalDateTime buscarPrimeiraDataCriacao();

        // Agregacoes do dashboard: evita carregar todos os chamados em memoria.
        @Query("SELECT c.status, COUNT(c) FROM Chamado c GROUP BY c.status")
        List<Object[]> contarPorStatus();

        long countByStatusAndUrgenciaPercebidaIn(
                        StatusChamado status, Collection<Prioridade> prioridades);

        long countByStatusInAndPrioridadeOficialIn(
                        Collection<StatusChamado> status, Collection<Prioridade> prioridades);

        long countByStatusInAndDataCriacaoBefore(
                        Collection<StatusChamado> status, LocalDateTime limite);

        long countByStatusAndDataConclusaoGreaterThanEqual(
                        StatusChamado status, LocalDateTime inicio);

        @Query("""
                SELECT c.categoria.setor.nome, COUNT(c)
                FROM Chamado c
                WHERE c.status IN :status
                GROUP BY c.categoria.setor.id, c.categoria.setor.nome
                ORDER BY COUNT(c) DESC, c.categoria.setor.nome ASC
                """)
        List<Object[]> contarAtivosPorSetor(
                        @Param("status") Collection<StatusChamado> status, Pageable pagina);

        @Query("""
                SELECT c.categoria.nome, c.categoria.setor.nome, COUNT(c)
                FROM Chamado c
                WHERE c.status IN :status
                GROUP BY c.categoria.id, c.categoria.nome, c.categoria.setor.nome
                ORDER BY COUNT(c) DESC, c.categoria.nome ASC
                """)
        List<Object[]> contarAtivosPorCategoria(
                        @Param("status") Collection<StatusChamado> status, Pageable pagina);

        @Query("""
                SELECT c FROM Chamado c
                JOIN FETCH c.categoria categoria
                JOIN FETCH categoria.setor
                WHERE c.status IN :status AND c.prioridadeOficial IN :prioridades
                ORDER BY CASE WHEN c.prioridadeOficial = :critica THEN 0 ELSE 1 END,
                         c.dataCriacao ASC
                """)
        List<Chamado> buscarChamadosPrioritarios(
                        @Param("status") Collection<StatusChamado> status,
                        @Param("prioridades") Collection<Prioridade> prioridades,
                        @Param("critica") Prioridade critica,
                        Pageable pagina);
}
