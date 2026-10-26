package com.tccinfonet.cityreport.modules.chamado._protocolo_contador.service;

import java.time.Year;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProtocoloService {

    private final EntityManager entityManager;

    // Cada ano comeca em 1. O incremento e atomico no PostgreSQL, mesmo com
    // cadastros simultaneos. Participa da transacao de cadastro do chamado.
    @Transactional
    public String gerarProtocolo() {
        int ano = Year.now(ZoneId.of("America/Sao_Paulo")).getValue();

        Number proximoNumero = (Number) entityManager.createNativeQuery("""
                INSERT INTO protocolo_contador (ano, ultimo_numero)
                VALUES (:ano, 1)
                ON CONFLICT (ano)
                DO UPDATE SET ultimo_numero = protocolo_contador.ultimo_numero + 1
                RETURNING ultimo_numero
                """)
                .setParameter("ano", ano)
                .getSingleResult();

        // 6 digitos e o minimo: depois de 999999 a numeracao segue normalmente.
        return String.format(Locale.ROOT, "%d-%06d", ano, proximoNumero.longValue());
    }
}
