package com.tccinfonet.cityreport.modules.chamado.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tccinfonet.cityreport.modules.chamado.dto.ChamadoResponseDTO;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.dto.AtualizacaoChamadoDTO;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.service.HistoricoChamadoService;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.dto.FotoChamadoResponseDTO;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.entity.FotoChamado;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.enums.TipoFoto;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.service.FotoChamadoService;
import com.tccinfonet.cityreport.modules.chamado.service.ChamadoExecucaoService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chamados")
@RequiredArgsConstructor
public class ChamadoExecucaoController {

    private final ChamadoExecucaoService chamadoExecucaoService;
    private final HistoricoChamadoService historicoChamadoService;
    private final FotoChamadoService fotoChamadoService;

    // A ApiSecurityConfig exige um JWT valido de um FUNCIONARIO ativo em /api/**.
    // O Service confere se o chamado pertence ao funcionario autenticado.
    @GetMapping
    public List<ChamadoResponseDTO> listar(Authentication autenticacao) {
        return chamadoExecucaoService.listarDoFuncionario(autenticacao.getName())
                .stream()
                .map(ChamadoResponseDTO::de)
                .toList();
    }

    // GET /api/chamados/{id}
    @GetMapping("/{id}")
    public ChamadoResponseDTO detalhes(@PathVariable Long id, Authentication autenticacao) {
        return ChamadoResponseDTO.de(
                chamadoExecucaoService.buscarDoFuncionario(id, autenticacao.getName()));
    }

    // GET /api/chamados/{id}/atualizacoes: destaques de execucao, NAO o historico completo.
    @GetMapping("/{id}/atualizacoes")
    public List<AtualizacaoChamadoDTO> listarAtualizacoes(
            @PathVariable Long id, Authentication autenticacao) {
        // Primeiro verifica se o chamado pertence ao funcionario autenticado.
        chamadoExecucaoService.buscarDoFuncionario(id, autenticacao.getName());
        return historicoChamadoService.listarAtualizacoes(id, true);
    }

    // GET /api/chamados/{id}/fotos: apenas fotos do problema.
    // A verificacao de acesso acontece ANTES da consulta das fotos.
    @GetMapping("/{id}/fotos")
    public List<FotoChamadoResponseDTO> listarFotosProblema(
            @PathVariable Long id, Authentication autenticacao) {

        chamadoExecucaoService.buscarDoFuncionario(id, autenticacao.getName());

        return fotoChamadoService.listarPorChamadoETipo(id, TipoFoto.PROBLEMA)
                .stream()
                .map(FotoChamadoResponseDTO::de)
                .toList();
    }

    // GET /api/chamados/{id}/fotos/{fotoId}/imagem
    // O arquivo e devolvido somente ao funcionario responsavel pelo chamado.
    @GetMapping("/{id}/fotos/{fotoId}/imagem")
    public ResponseEntity<Resource> visualizarFotoProblema(
            @PathVariable Long id, @PathVariable Long fotoId, Authentication autenticacao) {

        chamadoExecucaoService.buscarDoFuncionario(id, autenticacao.getName());

        FotoChamado foto = fotoChamadoService.buscarPorId(fotoId);
        if (foto.getChamado() == null
                || !Objects.equals(foto.getChamado().getId(), id)
                || foto.getTipo() != TipoFoto.PROBLEMA) {
            throw new EntityNotFoundException("Fotografia nao encontrada neste chamado.");
        }

        // carregarArquivo valida o nome e o caminho antes de acessar o disco.
        Resource arquivo = fotoChamadoService.carregarArquivo(foto);
        String nome = foto.getCaminhoArquivo();
        MediaType tipo = nome.endsWith(".png") ? MediaType.IMAGE_PNG
                : nome.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(tipo)
                .body(arquivo);
    }

    // POST /api/chamados/{id}/iniciar
    @PostMapping("/{id}/iniciar")
    public ChamadoResponseDTO iniciar(@PathVariable Long id, Authentication autenticacao) {
        return ChamadoResponseDTO.de(
                chamadoExecucaoService.iniciarExecucao(id, autenticacao.getName()));
    }

    // POST /api/chamados/{id}/impedimento (JSON: {"justificativa":"..."})
    @PostMapping("/{id}/impedimento")
    public ChamadoResponseDTO registrarImpedimento(
            @PathVariable Long id,
            @Valid @RequestBody ImpedimentoRequestDTO dados,
            Authentication autenticacao) {

        return ChamadoResponseDTO.de(chamadoExecucaoService.registrarImpedimento(
                id, dados.justificativa(), autenticacao.getName()));
    }

    // POST /api/chamados/{id}/observacoes (JSON: {"observacao":"..."})
    @PostMapping("/{id}/observacoes")
    public ChamadoResponseDTO registrarObservacao(
            @PathVariable Long id,
            @Valid @RequestBody ObservacaoRequestDTO dados,
            Authentication autenticacao) {

        return ChamadoResponseDTO.de(chamadoExecucaoService.registrarObservacao(
                id, dados.observacao(), autenticacao.getName()));
    }

    // POST /api/chamados/{id}/finalizar (multipart/form-data: campo "foto")
    @PostMapping(value = "/{id}/finalizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChamadoResponseDTO finalizar(
            @PathVariable Long id,
            @RequestPart("foto") MultipartFile foto,
            Authentication autenticacao) {

        return ChamadoResponseDTO.de(chamadoExecucaoService.solicitarFinalizacao(
                id, foto, autenticacao.getName()));
    }

    // Chamado inexistente: 404. Regra de negocio invalida: 400.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> tratarNaoEncontrado(EntityNotFoundException erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("erro", erro.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarRegraDeNegocio(IllegalArgumentException erro) {
        // O Service atual utiliza IllegalArgumentException tambem para negar
        // o acesso a chamados de outros funcionarios.
        HttpStatus status = "Este chamado nao esta atribuido a voce.".equals(erro.getMessage())
                ? HttpStatus.FORBIDDEN
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status)
                .body(Map.of("erro", erro.getMessage()));
    }

    // DTO pequeno para observacoes de execucao, sem precisar de um novo arquivo.
    public record ObservacaoRequestDTO(
            @NotBlank(message = "A observacao e obrigatoria.")
            @Size(max = 1700, message = "A observacao deve ter no maximo 1700 caracteres.")
            String observacao
    ) {}

    // DTO pequeno para receber a justificativa em JSON sem criar outro arquivo.
    public record ImpedimentoRequestDTO(
            @NotBlank(message = "A justificativa e obrigatoria.")
            @Size(max = 1700, message = "A justificativa deve ter no maximo 1700 caracteres.")
            String justificativa
    ) {}
}
