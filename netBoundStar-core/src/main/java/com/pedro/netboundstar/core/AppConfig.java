package com.pedro.netboundstar.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Central de Configuração da Aplicação com Persistência.
 * Salva e carrega configurações de um arquivo netboundstar.config.
 */
public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    // Arquivo de configuração com fallback seguro
    private static final Path CONFIG_PATH = getConfigPath();

    // --- Configurações de Vida da Estrela ---
    private double starLifeSeconds = 10.0;

    // --- Configurações do Núcleo Central ---
    private double centerHeatIncrement = 2.0;
    private double centerHeatMax = 50.0;
    private double centerHeatDecay = 0.90;

    // --- Configurações de Partículas ---
    private double particleSpeedMin = 0.02;
    private double particleSpeedMax = 0.05;

    // --- Configurações de Física ---
    private double repulsionForce = 5000.0;
    private double attractionForce = 0.005;
    private double maxPhysicsSpeed = 10.0;

    private AppConfig() {
        load();
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    // Método auxiliar para calcular o caminho de forma segura
    private static Path getConfigPath() {
        try {
            String userHome = System.getProperty("user.home");
            if (userHome != null && !userHome.isEmpty()) {
                return Paths.get(userHome, "netboundstar.config");
            }
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível obter user.home, usando temp directory");
        }

        try {
            return Paths.get(System.getProperty("java.io.tmpdir"), "netboundstar.config");
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível obter temp directory");
            return null;
        }
    }

    // ========== Star Lifespan ==========
    public double getStarLifeSeconds() {
        return starLifeSeconds;
    }

    public void setStarLifeSeconds(double seconds) {
        this.starLifeSeconds = seconds;
    }

    public double getDecayRatePerFrame() {
        return 1.0 / (60.0 * Math.max(1.0, starLifeSeconds));
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

    public double getRandomParticleSpeed() {
        return particleSpeedMin + Math.random() * (particleSpeedMax - particleSpeedMin);
    }

    // ========== Physics ==========
    public double getRepulsionForce() {
        return repulsionForce;
    }

    public void setRepulsionForce(double force) {
        this.repulsionForce = force;
    }

    public double getAttractionForce() {
        return attractionForce;
    }

    public void setAttractionForce(double force) {
        this.attractionForce = force;
    }

    public double getMaxPhysicsSpeed() {
        return maxPhysicsSpeed;
    }

    public void setMaxPhysicsSpeed(double speed) {
        this.maxPhysicsSpeed = speed;
    }

    // ========== Persistência ==========
    public void save() {
        if (CONFIG_PATH == null) {
            System.out.println("Aviso: CONFIG_PATH é null, configurações não serão persistidas");
            return;
        }

        Properties props = new Properties();
        props.setProperty("star.life", String.valueOf(starLifeSeconds));
        props.setProperty("center.heat.increment", String.valueOf(centerHeatIncrement));
        props.setProperty("center.heat.max", String.valueOf(centerHeatMax));
        props.setProperty("center.heat.decay", String.valueOf(centerHeatDecay));
        props.setProperty("particle.speed.min", String.valueOf(particleSpeedMin));
        props.setProperty("particle.speed.max", String.valueOf(particleSpeedMax));
        props.setProperty("physics.repulsion", String.valueOf(repulsionForce));
        props.setProperty("physics.attraction", String.valueOf(attractionForce));
        props.setProperty("physics.max.speed", String.valueOf(maxPhysicsSpeed));

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            props.store(writer, "NetBoundStar Configuration File");
            System.out.println("✓ Configurações salvas em: " + CONFIG_PATH);
        } catch (IOException e) {
            System.err.println("Erro ao salvar config: " + e.getMessage());
        }
    }

    private void load() {
        if (CONFIG_PATH == null) {
            System.out.println("Aviso: CONFIG_PATH é null, usando valores padrão");
            return;
        }

        try {
            if (!Files.exists(CONFIG_PATH)) {
                System.out.println("Configurações não encontradas, usando padrões");
                return;
            }

            Properties props = new Properties();
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                props.load(reader);

                starLifeSeconds = Double.parseDouble(props.getProperty("star.life", "10.0"));
                centerHeatIncrement = Double.parseDouble(props.getProperty("center.heat.increment", "2.0"));
                centerHeatMax = Double.parseDouble(props.getProperty("center.heat.max", "50.0"));
                centerHeatDecay = Double.parseDouble(props.getProperty("center.heat.decay", "0.90"));
                particleSpeedMin = Double.parseDouble(props.getProperty("particle.speed.min", "0.02"));
                particleSpeedMax = Double.parseDouble(props.getProperty("particle.speed.max", "0.05"));
                repulsionForce = Double.parseDouble(props.getProperty("physics.repulsion", "5000.0"));
                attractionForce = Double.parseDouble(props.getProperty("physics.attraction", "0.005"));
                maxPhysicsSpeed = Double.parseDouble(props.getProperty("physics.max.speed", "10.0"));

                System.out.println("✓ Configurações carregadas de: " + CONFIG_PATH);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar config, usando padrões: " + e.getMessage());
        }
    }
}
