package com.tccinfonet.cityreport.modules.usuario.dto;

// Resposta enviada ao aplicativo apos um login valido.
public record TokenMobileResponseDTO(
        String token,
        String tipo,
        long expiraEmSegundos
) {}
