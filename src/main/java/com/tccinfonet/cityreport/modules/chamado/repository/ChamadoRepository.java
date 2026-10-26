package com.tccinfonet.cityreport.modules.chamado.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;


@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    List<Chamado> findAllByOrderByDataCriacaoDesc();

    List<Chamado> findByCidadaoIdOrderByDataCriacaoDesc(Long cidadaoId);

    List<Chamado> findByFuncionarioResponsavelIdOrderByDataCriacaoDesc(Long funcionarioId);

    List<Chamado> findByStatusOrderByDataCriacaoDesc(StatusChamado status);

    List<Chamado> findByPrioridadeOficialOrderByDataCriacaoDesc(Prioridade prioridade);

    List<Chamado> findByCategoriaIdOrderByDataCriacaoDesc(Long categoriaId);

    boolean existsByChamadoOriginalId(Long originalId);

    Optional<Chamado> findByProtocolo(String protocolo);

    boolean existsByFuncionarioResponsavelIdAndStatus(Long funcionarioId, StatusChamado status);

    // Impede trocar o setor de funcionarios com chamados ainda sob sua responsabilidade.
    boolean existsByFuncionarioResponsavelIdAndStatusIn(
            Long funcionarioId, Collection<StatusChamado> status);

    // Chamados em execucao que precisam ser pausados ao desativar um funcionario.
    List<Chamado> findByFuncionarioResponsavelIdAndStatus(Long funcionarioId, StatusChamado status);
}
