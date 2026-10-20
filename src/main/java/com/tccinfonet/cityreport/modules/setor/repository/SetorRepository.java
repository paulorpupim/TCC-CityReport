package com.tccinfonet.cityreport.modules.setor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tccinfonet.cityreport.modules.setor.entity.Setor;

@Repository
public interface SetorRepository extends JpaRepository<Setor, Long> {

    List<Setor> findAllByOrderByNomeAsc();

    List<Setor> findByAtivoTrueOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
