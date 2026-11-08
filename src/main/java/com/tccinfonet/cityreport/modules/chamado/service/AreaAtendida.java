package com.tccinfonet.cityreport.modules.chamado.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Retangulo aproximado da regiao atendida, configurado no application.properties.
// Nao representa o limite territorial oficial do municipio.
@Component
public class AreaAtendida {

    private final double latitudeMinima;
    private final double latitudeMaxima;
    private final double longitudeMinima;
    private final double longitudeMaxima;

    public AreaAtendida(
            @Value("${cityreport.localizacao.latitude-minima}") double latitudeMinima,
            @Value("${cityreport.localizacao.latitude-maxima}") double latitudeMaxima,
            @Value("${cityreport.localizacao.longitude-minima}") double longitudeMinima,
            @Value("${cityreport.localizacao.longitude-maxima}") double longitudeMaxima) {
        if (!Double.isFinite(latitudeMinima) || !Double.isFinite(latitudeMaxima)
                || !Double.isFinite(longitudeMinima) || !Double.isFinite(longitudeMaxima)
                || latitudeMinima < -90 || latitudeMaxima > 90
                || longitudeMinima < -180 || longitudeMaxima > 180
                || latitudeMinima >= latitudeMaxima || longitudeMinima >= longitudeMaxima) {
            throw new IllegalArgumentException("Limites da area atendida invalidos no application.properties.");
        }
        this.latitudeMinima = latitudeMinima;
        this.latitudeMaxima = latitudeMaxima;
        this.longitudeMinima = longitudeMinima;
        this.longitudeMaxima = longitudeMaxima;
    }

    public boolean contem(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= latitudeMinima && latitude <= latitudeMaxima
                && longitude >= longitudeMinima && longitude <= longitudeMaxima;
    }

    public double getLatitudeMinima() {
        return latitudeMinima;
    }

    public double getLatitudeMaxima() {
        return latitudeMaxima;
    }

    public double getLongitudeMinima() {
        return longitudeMinima;
    }

    public double getLongitudeMaxima() {
        return longitudeMaxima;
    }
}
