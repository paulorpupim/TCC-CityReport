
package com.tccinfonet.cityreport.modules.chamado.foto_chamado.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tccinfonet.cityreport.modules.chamado.foto_chamado.entity.FotoChamado;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.enums.TipoFoto;

@Repository
public interface FotoChamadoRepository extends JpaRepository<FotoChamado, Long> {

    List<FotoChamado> findByChamadoIdOrderByDataEnvioAscIdAsc(Long chamadoId);

    List<FotoChamado> findByChamadoIdAndTipoOrderByDataEnvioAscIdAsc(Long chamadoId, TipoFoto tipo);

    boolean existsByChamadoIdAndTipo(Long chamadoId, TipoFoto tipo);
}
