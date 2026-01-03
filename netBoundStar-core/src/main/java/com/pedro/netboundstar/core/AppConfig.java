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
     * Priority:
     * 1. REAL user's home directory (not root when using sudo)
     * 2. Default Java user home
     * 3. Project directory (user.dir)
     * 4. Temp directory
     *
     * @return The resolved Path for the config file.
     */
    private Path resolveConfigPath() {
        String configFileName = ".netboundstar.config";

        // 1. Try using SUDO_USER to get the real user (when running with sudo)
        String sudoUser = System.getenv("SUDO_USER");
        if (sudoUser != null && !sudoUser.isEmpty()) {
            Path realUserHome = Paths.get("/home", sudoUser, configFileName);
            if (Files.isWritable(realUserHome.getParent()) || !Files.exists(realUserHome.getParent())) {
                System.out.println("Config: Using real user home: " + realUserHome);
                return realUserHome;
            }
        }

        // 2. Try user.home (avoiding /root)
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty() && !userHome.equals("/root")) {
            Path homePath = Paths.get(userHome, configFileName);
            System.out.println("Config: Using user.home: " + homePath);
            return homePath;
        }

        // 3. If it's /root, try /home/[SUDO_USER] directly
        if ("/root".equals(userHome) && sudoUser != null) {
            Path realHome = Paths.get("/home", sudoUser, configFileName);
            System.out.println("Config: Redirecting from /root to: " + realHome);
            return realHome;
        }

        // 4. Fallback: project directory
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            Path projectPath = Paths.get(userDir, configFileName);
            System.out.println("Config: Using project directory: " + projectPath);
            return projectPath;
        }

        // 5. Last fallback: temp
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null) {
            Path tmpPath = Paths.get(tmpDir, configFileName);
            System.out.println("Config: Using temp: " + tmpPath);
            return tmpPath;
        }

        System.err.println("⚠ Config: Could not determine path, configs will not be saved");
        return null;
    }

    // ========== Star Lifespan ==========

    /**
     * Gets the star life duration in seconds.
     * @return Star life in seconds.
     */
    public double getStarLifeSeconds() {
        return starLifeSeconds;
    }

    /**
     * Sets the star life duration in seconds.
     * @param seconds Star life in seconds.
     */
    public void setStarLifeSeconds(double seconds) {
        this.starLifeSeconds = seconds;
    }

    /**
     * Calculates the decay rate per frame based on star life.
     * @return Decay rate per frame.
     */
    public double getDecayRatePerFrame() {
        return 1.0 / (60.0 * Math.max(1.0, starLifeSeconds));
    }

    // ========== Center Heat ==========

    /**
     * Gets the heat increment value for the center core.
     * @return Heat increment.
     */
    public double getCenterHeatIncrement() {
        return centerHeatIncrement;
    }

    /**
     * Sets the heat increment value for the center core.
     * @param increment Heat increment.
     */
    public void setCenterHeatIncrement(double increment) {
        this.centerHeatIncrement = increment;
    }

    /**
     * Gets the maximum heat value for the center core.
     * @return Maximum heat.
     */
    public double getCenterHeatMax() {
        return centerHeatMax;
    }

    /**
     * Sets the maximum heat value for the center core.
     * @param max Maximum heat.
     */
    public void setCenterHeatMax(double max) {
        this.centerHeatMax = max;
    }

    /**
     * Gets the heat decay rate for the center core.
     * @return Heat decay rate.
     */
    public double getCenterHeatDecay() {
        return centerHeatDecay;
    }

    /**
     * Sets the heat decay rate for the center core.
     * @param decay Heat decay rate.
     */
    public void setCenterHeatDecay(double decay) {
        this.centerHeatDecay = decay;
    }

    // ========== Particle Speed ==========

    /**
     * Gets the minimum particle speed.
     * @return Minimum speed.
     */
    public double getParticleSpeedMin() {
        return particleSpeedMin;
    }

    /**
     * Sets the minimum particle speed.
     * @param speed Minimum speed.
     */
    public void setParticleSpeedMin(double speed) {
        this.particleSpeedMin = speed;
    }

    /**
     * Gets the maximum particle speed.
     * @return Maximum speed.
     */
    public double getParticleSpeedMax() {
        return particleSpeedMax;
    }

    /**
     * Sets the maximum particle speed.
     * @param speed Maximum speed.
     */
    public void setParticleSpeedMax(double speed) {
        this.particleSpeedMax = speed;
    }

    /**
     * Generates a random particle speed between min and max.
     * @return A random speed value.
     */
    public double getRandomParticleSpeed() {
        return particleSpeedMin + Math.random() * (particleSpeedMax - particleSpeedMin);
    }

    // ========== Physics ==========

    /**
     * Gets the repulsion force value.
     * @return Repulsion force.
     */
    public double getRepulsionForce() {
        return repulsionForce;
    }

    /**
     * Sets the repulsion force value.
     * @param force Repulsion force.
     */
    public void setRepulsionForce(double force) {
        this.repulsionForce = force;
    }

    /**
     * Gets the attraction force value.
     * @return Attraction force.
     */
    public double getAttractionForce() {
        return attractionForce;
    }

    /**
     * Sets the attraction force value.
     * @param force Attraction force.
     */
    public void setAttractionForce(double force) {
        this.attractionForce = force;
    }

    /**
     * Gets the maximum physics speed.
     * @return Maximum speed.
     */
    public double getMaxPhysicsSpeed() {
        return maxPhysicsSpeed;
    }

    /**
     * Sets the maximum physics speed.
     * @param speed Maximum speed.
     */
    public void setMaxPhysicsSpeed(double speed) {
        this.maxPhysicsSpeed = speed;
    }

    // ========== Persistence ==========

    /**
     * Saves the current configuration to the config file.
     */
    public void save() {
        if (configPath == null) {
            System.out.println("Warning: configPath is null, configurations will not be persisted");
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
            System.out.println("✓ Configurations saved to: " + configPath);
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    /**
     * Loads the configuration from the config file.
     */
    private void load() {
        if (configPath == null) {
            System.out.println("Warning: configPath is null, using default values");
            return;
        }

        try {
            if (!Files.exists(configPath)) {
                System.out.println("Configurations not found at " + configPath + ", using defaults");
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

                System.out.println("✓ Configurations loaded from: " + configPath);
            }
        } catch (Exception e) {
            System.err.println("Error loading config, using defaults: " + e.getMessage());
        }
    }
}
