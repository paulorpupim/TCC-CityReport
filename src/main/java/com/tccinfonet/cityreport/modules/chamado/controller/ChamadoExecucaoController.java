package com.tccinfonet.cityreport.modules.chamado.controller;

import java.util.List;
import java.util.Map;

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

    @GetMapping
    public List<ChamadoResponseDTO> listar(Authentication autenticacao) {
        return chamadoExecucaoService.listarDoFuncionario(autenticacao.getName())
                .stream()
                .map(ChamadoResponseDTO::de)
                .toList();
    }

    @GetMapping("/{id}")
    public ChamadoResponseDTO detalhes(@PathVariable Long id, Authentication autenticacao) {
        return ChamadoResponseDTO.de(
                chamadoExecucaoService.buscarDoFuncionario(id, autenticacao.getName()));
    }

    @PostMapping("/{id}/iniciar")
    public ChamadoResponseDTO iniciar(@PathVariable Long id, Authentication autenticacao) {
        return ChamadoResponseDTO.de(
                chamadoExecucaoService.iniciarExecucao(id, autenticacao.getName()));
    }

    @PostMapping("/{id}/impedimento")
    public ChamadoResponseDTO registrarImpedimento(
            @PathVariable Long id,
            @Valid @RequestBody ImpedimentoRequestDTO dados,
            Authentication autenticacao) {

        return ChamadoResponseDTO.de(chamadoExecucaoService.registrarImpedimento(
                id, dados.justificativa(), autenticacao.getName()));
    }

    @PostMapping(value = "/{id}/finalizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChamadoResponseDTO finalizar(
            @PathVariable Long id,
            @RequestPart("foto") MultipartFile foto,
            Authentication autenticacao) {

        return ChamadoResponseDTO.de(chamadoExecucaoService.solicitarFinalizacao(
                id, foto, autenticacao.getName()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> tratarNaoEncontrado(EntityNotFoundException erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("erro", erro.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarRegraDeNegocio(IllegalArgumentException erro) {

        HttpStatus status = "Este chamado nao esta atribuido a voce.".equals(erro.getMessage())
                ? HttpStatus.FORBIDDEN
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status)
                .body(Map.of("erro", erro.getMessage()));
    }

    public record ImpedimentoRequestDTO(
            @NotBlank(message = "A justificativa e obrigatoria.")
            @Size(max = 1700, message = "A justificativa deve ter no maximo 1700 caracteres.")
            String justificativa
    ) {}
}
