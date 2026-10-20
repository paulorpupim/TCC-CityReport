package com.tccinfonet.cityreport.modules.categoria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNomeAsc();

    List<Categoria> findByAtivoTrueOrderByNomeAsc();

    List<Categoria> findBySetorIdAndAtivoTrueOrderByNomeAsc(Long setorId);

    boolean existsByNomeIgnoreCaseAndSetorId(String nome, Long setorId);

    boolean existsByNomeIgnoreCaseAndSetorIdAndIdNot(String nome, Long setorId, Long id);
}
