package com.tccinfonet.cityreport.modules.setor.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tccinfonet.cityreport.modules.setor.entity.Setor;
import com.tccinfonet.cityreport.modules.setor.repository.SetorRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SetorService {

    private final SetorRepository setorRepository;

    public List<Setor> listarTodos() {
        return setorRepository.findAllByOrderByNomeAsc();
    }

    public List<Setor> listarAtivos() {
        return setorRepository.findByAtivoTrueOrderByNomeAsc();
    }

    public Setor buscarPorId(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado."));
    }
    @Transactional
    public Setor cadastrar(Setor dados) {
        String nome = validarENormalizarNome(dados.getNome());

        if (setorRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException("Já existe um setor com esse nome.");
        }

        Setor setor = new Setor();
        setor.setNome(nome);
        setor.setAtivo(true);

        return setorRepository.save(setor);
    }

    @Transactional
    public Setor editar(Long id, Setor dados) {
        Setor setor = buscarPorId(id);
        String nome = validarENormalizarNome(dados.getNome());

        if (setorRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new IllegalArgumentException("Já existe outro setor com esse nome.");
        }

        setor.setNome(nome);
        return setorRepository.save(setor);
    }

    @Transactional
    public void ativar(Long id) {
        Setor setor = buscarPorId(id);
        setor.setAtivo(true);
        setorRepository.save(setor);
    }

    @Transactional
    public void desativar(Long id) {
        Setor setor = buscarPorId(id);
        setor.setAtivo(false);
        setorRepository.save(setor);
    }

    private String validarENormalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do setor é obrigatório.");
        }

        String nomeNormalizado = nome.trim();
        if (nomeNormalizado.length() > 100) {
            throw new IllegalArgumentException("O nome do setor deve ter no máximo 100 caracteres.");
        }

        return nomeNormalizado;
    }
}
