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

    // Arquivo de configuração - calculado uma vez na inicialização
    private final Path configPath;

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
        this.configPath = resolveConfigPath();
        load();
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    /**
     * Resolve o caminho do arquivo de configuração.
     * Prioridade:
     * 1. Diretório home do usuário REAL (não root quando usando sudo)
     * 2. Diretório home padrão do Java
     * 3. Diretório do projeto (user.dir)
     * 4. Diretório temp
     */
    private Path resolveConfigPath() {
        String configFileName = ".netboundstar.config";

        // 1. Tenta usar SUDO_USER para obter o usuário real (quando rodando com sudo)
        String sudoUser = System.getenv("SUDO_USER");
        if (sudoUser != null && !sudoUser.isEmpty()) {
            Path realUserHome = Paths.get("/home", sudoUser, configFileName);
            if (Files.isWritable(realUserHome.getParent()) || !Files.exists(realUserHome.getParent())) {
                System.out.println("Config: Usando home do usuário real: " + realUserHome);
                return realUserHome;
            }
        }

        // 2. Tenta user.home (mas evita /root)
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty() && !userHome.equals("/root")) {
            Path homePath = Paths.get(userHome, configFileName);
            System.out.println("Config: Usando user.home: " + homePath);
            return homePath;
        }

        // 3. Se é /root, tenta /home/[SUDO_USER] diretamente
        if ("/root".equals(userHome) && sudoUser != null) {
            Path realHome = Paths.get("/home", sudoUser, configFileName);
            System.out.println("Config: Redirecionando de /root para: " + realHome);
            return realHome;
        }

        // 4. Fallback: diretório do projeto
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            Path projectPath = Paths.get(userDir, configFileName);
            System.out.println("Config: Usando diretório do projeto: " + projectPath);
            return projectPath;
        }

        // 5. Último fallback: temp
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null) {
            Path tmpPath = Paths.get(tmpDir, configFileName);
            System.out.println("Config: Usando temp: " + tmpPath);
            return tmpPath;
        }

        System.err.println("⚠ Config: Não foi possível determinar caminho, configs não serão salvas");
        return null;
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
        if (configPath == null) {
            System.out.println("Aviso: configPath é null, configurações não serão persistidas");
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

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            props.store(writer, "NetBoundStar Configuration File");
            System.out.println("✓ Configurações salvas em: " + configPath);
        } catch (IOException e) {
            System.err.println("Erro ao salvar config: " + e.getMessage());
        }
    }

    private void load() {
        if (configPath == null) {
            System.out.println("Aviso: configPath é null, usando valores padrão");
            return;
        }

        try {
            if (!Files.exists(configPath)) {
                System.out.println("Configurações não encontradas em " + configPath + ", usando padrões");
                return;
            }

            Properties props = new Properties();
            try (Reader reader = Files.newBufferedReader(configPath)) {
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

                System.out.println("✓ Configurações carregadas de: " + configPath);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar config, usando padrões: " + e.getMessage());
        }
    }
}
