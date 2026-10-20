package com.tccinfonet.cityreport.modules.chamado.repository;

import java.util.List;

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

    // util pois um funcionario só pode ter um chamado em execução
    boolean existsByFuncionarioResponsavelIdAndStatus(Long funcionarioId, StatusChamado status);
}
