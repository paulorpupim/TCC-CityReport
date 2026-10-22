
package com.tccinfonet.cityreport.modules.chamado._historico_chamado.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tccinfonet.cityreport.modules.chamado._historico_chamado.entity.HistoricoChamado;

@Repository
public interface HistoricoChamadoRepository extends JpaRepository<HistoricoChamado, Long> {

    List<HistoricoChamado> findByChamadoIdOrderByDataHoraAscIdAsc(Long chamadoId);
    
}
