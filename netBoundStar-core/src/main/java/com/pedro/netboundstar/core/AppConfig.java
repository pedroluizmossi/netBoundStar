package com.pedro.netboundstar.core;

/**
 * Central de Configuração da Aplicação.
 * Gerencia todas as constantes globais em um único lugar.
 * Implementado como Singleton para acesso fácil.
 */
public class AppConfig {
    // Singleton para acesso global fácil
    private static final AppConfig INSTANCE = new AppConfig();

    // Configurações de Vida da Estrela
    private double starLifeSeconds = 5.0; // Quanto tempo a estrela brilha após o último pacote

    // Configurações do Núcleo Central
    private double centerHeatIncrement = 2.0; // Quanto o núcleo cresce por pacote
    private double centerHeatMax = 50.0; // Limite máximo de crescimento
    private double centerHeatDecay = 0.90; // Taxa de decaimento (multiplicador exponencial)

    // Configurações de Partículas
    private double particleSpeedMin = 0.02;
    private double particleSpeedMax = 0.05;

    private AppConfig() {}

    public static AppConfig get() {
        return INSTANCE;
    }

    // ========== Star Lifespan ==========
    public double getStarLifeSeconds() {
        return starLifeSeconds;
    }

    public void setStarLifeSeconds(double seconds) {
        this.starLifeSeconds = seconds;
    }

    /**
     * Calcula o quanto reduzir a atividade da estrela por frame.
     * Assumindo 60 FPS: se queremos durar X segundos, reduzimos 1.0 / (60 * X) por frame.
     */
    public double getDecayRatePerFrame() {
        return 1.0 / (60.0 * starLifeSeconds);
    }

    // ========== Center Heat ==========
    public double getCenterHeatIncrement() {
        return centerHeatIncrement;
    }

    public void setCenterHeatIncrement(double increment) {
        this.centerHeatIncrement = increment;
    }

    public double getCenterHeatMax() {
        return centerHeatMax;
    }

    public void setCenterHeatMax(double max) {
        this.centerHeatMax = max;
    }

    public double getCenterHeatDecay() {
        return centerHeatDecay;
    }

    public void setCenterHeatDecay(double decay) {
        this.centerHeatDecay = decay;
    }

    // ========== Particle Speed ==========
    public double getParticleSpeedMin() {
        return particleSpeedMin;
    }

    public void setParticleSpeedMin(double speed) {
        this.particleSpeedMin = speed;
    }

    public double getParticleSpeedMax() {
        return particleSpeedMax;
    }

    public void setParticleSpeedMax(double speed) {
        this.particleSpeedMax = speed;
    }

    /**
     * Gera uma velocidade aleatória entre min e max.
     */
    public double getRandomParticleSpeed() {
        return particleSpeedMin + Math.random() * (particleSpeedMax - particleSpeedMin);
    }
}

