package com.pedro.netboundstar.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Application Configuration Center with Persistence.
 * Saves and loads configurations from a .netboundstar.config file.
 */
public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    /**
     * Configuration file path - calculated once during initialization.
     */
    private final Path configPath;

    // --- Star Life Settings ---
    private double starLifeSeconds = 10.0;

    // --- Central Core Settings ---
    private double centerHeatIncrement = 2.0;
    private double centerHeatMax = 50.0;
    private double centerHeatDecay = 0.90;

    // --- Particle Settings ---
    private double particleSpeedMin = 0.02;
    private double particleSpeedMax = 0.05;

    // --- Physics Settings ---
    private double repulsionForce = 5000.0;
    private double attractionForce = 0.005;
    private double maxPhysicsSpeed = 10.0;

    // --- Debug Settings ---
    private boolean debugMode = false;

    private AppConfig() {
        this.configPath = resolveConfigPath();
        load();
    }

    /**
     * Returns the singleton instance of AppConfig.
     *
     * @return The AppConfig instance.
     */
    public static AppConfig get() {
        return INSTANCE;
    }

    /**
     * Resolves the configuration file path.
     *
     * @return The resolved Path for the config file.
     */
    private Path resolveConfigPath() {
        String configFileName = ".netboundstar.config";

        String sudoUser = System.getenv("SUDO_USER");
        if (sudoUser != null && !sudoUser.isEmpty()) {
            Path realUserHome = Paths.get("/home", sudoUser, configFileName);
            if (Files.isWritable(realUserHome.getParent()) || !Files.exists(realUserHome.getParent())) {
                return realUserHome;
            }
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty() && !userHome.equals("/root")) {
            return Paths.get(userHome, configFileName);
        }

        if ("/root".equals(userHome) && sudoUser != null) {
            return Paths.get("/home", sudoUser, configFileName);
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            return Paths.get(userDir, configFileName);
        }

        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null) {
            return Paths.get(tmpDir, configFileName);
        }

        return null;
    }

    // ========== Star Lifespan ==========
    public double getStarLifeSeconds() { return starLifeSeconds; }
    public void setStarLifeSeconds(double seconds) { this.starLifeSeconds = seconds; }
    public double getDecayRatePerFrame() { return 1.0 / (60.0 * Math.max(1.0, starLifeSeconds)); }

    // ========== Center Heat ==========
    public double getCenterHeatIncrement() { return centerHeatIncrement; }
    public void setCenterHeatIncrement(double increment) { this.centerHeatIncrement = increment; }
    public double getCenterHeatMax() { return centerHeatMax; }
    public void setCenterHeatMax(double max) { this.centerHeatMax = max; }
    public double getCenterHeatDecay() { return centerHeatDecay; }
    public void setCenterHeatDecay(double decay) { this.centerHeatDecay = decay; }

    // ========== Particle Speed ==========
    public double getParticleSpeedMin() { return particleSpeedMin; }
    public void setParticleSpeedMin(double speed) { this.particleSpeedMin = speed; }
    public double getParticleSpeedMax() { return particleSpeedMax; }
    public void setParticleSpeedMax(double speed) { this.particleSpeedMax = speed; }
    public double getRandomParticleSpeed() { return particleSpeedMin + Math.random() * (particleSpeedMax - particleSpeedMin); }

    // ========== Physics ==========
    public double getRepulsionForce() { return repulsionForce; }
    public void setRepulsionForce(double force) { this.repulsionForce = force; }
    public double getAttractionForce() { return attractionForce; }
    public void setAttractionForce(double force) { this.attractionForce = force; }
    public double getMaxPhysicsSpeed() { return maxPhysicsSpeed; }
    public void setMaxPhysicsSpeed(double speed) { this.maxPhysicsSpeed = speed; }

    // ========== Debug Mode ==========
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

    // ========== Persistence ==========
    public void save() {
        if (configPath == null) return;

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
        props.setProperty("debug.mode", String.valueOf(debugMode));

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            props.store(writer, "NetBoundStar Configuration File");
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    private void load() {
        if (configPath == null || !Files.exists(configPath)) return;

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Properties props = new Properties();
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
            debugMode = Boolean.parseBoolean(props.getProperty("debug.mode", "false"));
        } catch (Exception e) {
            System.err.println("Error loading config: " + e.getMessage());
        }
    }
}
