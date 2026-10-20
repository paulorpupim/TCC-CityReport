
package com.tccinfonet.cityreport.modules.categoria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.categoria.repository.CategoriaRepository;
import com.tccinfonet.cityreport.modules.setor.entity.Setor;
import com.tccinfonet.cityreport.modules.setor.service.SetorService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SetorService setorService;

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAllByOrderByNomeAsc();
    }

    public List<Categoria> listarAtivas() {
        return categoriaRepository.findByAtivoTrueOrderByNomeAsc();
    }

    public List<Categoria> listarAtivasPorSetor(Long setorId) {
        return categoriaRepository.findBySetorIdAndAtivoTrueOrderByNomeAsc(setorId);
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
    }

    @Transactional
    public Categoria cadastrar(Categoria dados) {
        String nome = validarENormalizarNome(dados.getNome());
        Setor setor = validarSetorAtivo(dados.getSetor());

        if (categoriaRepository.existsByNomeIgnoreCaseAndSetorId(nome, setor.getId())) {
            throw new IllegalArgumentException("Já existe uma categoria com esse nome neste setor.");
        }

        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setSetor(setor);
        categoria.setAtivo(true);

        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria editar(Long id, Categoria dados) {
        Categoria categoria = buscarPorId(id);
        String nome = validarENormalizarNome(dados.getNome());
        Setor setor = validarSetorAtivo(dados.getSetor());

        if (categoriaRepository.existsByNomeIgnoreCaseAndSetorIdAndIdNot(nome, setor.getId(), id)) {
            throw new IllegalArgumentException("Já existe outra categoria com esse nome neste setor.");
        }

        categoria.setNome(nome);
        categoria.setSetor(setor);

        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void ativar(Long id) {
        Categoria categoria = buscarPorId(id);
        validarSetorAtivo(categoria.getSetor());

        categoria.setAtivo(true);
        categoriaRepository.save(categoria);
    }

    @Transactional
    public void desativar(Long id) {
        Categoria categoria = buscarPorId(id);
        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
    }

    private Setor validarSetorAtivo(Setor setorInformado) {
        if (setorInformado == null || setorInformado.getId() == null) {
            throw new IllegalArgumentException("Selecione um setor responsável.");
        }

        Setor setor = setorService.buscarPorId(setorInformado.getId());

        if (!Boolean.TRUE.equals(setor.getAtivo())) {
            throw new IllegalArgumentException("O setor selecionado está inativo.");
        }

        return setor;
    }

    private String validarENormalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }

        String nomeNormalizado = nome.trim();

        if (nomeNormalizado.length() > 100) {
            throw new IllegalArgumentException("O nome da categoria deve ter no máximo 100 caracteres.");
        }

        return nomeNormalizado;
    }
}
