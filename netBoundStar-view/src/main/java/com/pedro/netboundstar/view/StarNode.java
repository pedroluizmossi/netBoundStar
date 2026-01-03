package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;
import com.pedro.netboundstar.view.util.DnsService;
import com.pedro.netboundstar.view.util.FlagCache;
import com.pedro.netboundstar.view.util.GeoService;
import com.pedro.netboundstar.view.util.TextImageFactory;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a network node (star) in the visualization.
 */
public class StarNode {
    public double x, y;
    public double vx = 0, vy = 0;

    public final String ip;
    public volatile String displayName;
    public volatile String countryCode; 
    public double activity = 1.0;

    public boolean isHovered = false;
    public boolean isFrozen = false;

    public long totalBytes = 0;
    public String lastPorts = "N/A";
    
    // --- Enhanced Metrics ---
    public int connectionCount = 1; 
    
    // Map of Host IP -> Last Seen Timestamp (System.nanoTime())
    private final Map<String, Long> hostLastSeen = new ConcurrentHashMap<>();
    
    public final Map<Protocol, Integer> protocolStats = new ConcurrentHashMap<>();

    private Color lastProtocolColor = Color.WHITE;
    public Image flagImage = null;
    
    // --- Text Caching ---
    private Image cachedLabelImage = null;
    private String lastLabelText = "";

    private final List<PacketParticle> particles = new ArrayList<>();
    private static final Random random = new Random();

    public StarNode(String ip, double centerX, double centerY) {
        this.ip = ip;
        this.displayName = ip;
        this.hostLastSeen.put(ip, System.nanoTime());

        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 200 + random.nextDouble() * 150;
        this.x = centerX + Math.cos(angle) * distance;
        this.y = centerY + Math.sin(angle) * distance;

        this.vx = (random.nextDouble() - 0.5) * 2.0;
        this.vy = (random.nextDouble() - 0.5) * 2.0;

        DnsService.resolve(ip, resolvedName -> this.displayName = resolvedName);
        
        GeoService.resolveCountry(ip, isoCode -> {
            this.countryCode = isoCode;
            this.flagImage = FlagCache.get(isoCode);
        });
    }

    public String getSmartLabel() {
        if (AppConfig.get().isClusterByCountry() && hostLastSeen.size() > 1 && countryCode != null) {
            return "Cluster: " + countryCode;
        }
        return displayName;
    }
    
    /**
     * Returns a cached image of the label to avoid expensive text rendering every frame.
     */
    public Image getLabelImage() {
        String currentText = getSmartLabel();
        if (isCluster()) {
            currentText += " (" + getUniqueHostCount() + ")";
        }
        
        // If text changed or cache is empty, recreate
        if (cachedLabelImage == null || !currentText.equals(lastLabelText)) {
            cachedLabelImage = TextImageFactory.create(currentText);
            lastLabelText = currentText;
        }
        return cachedLabelImage;
    }

    public boolean isCluster() {
        return AppConfig.get().isClusterByCountry() && hostLastSeen.size() > 1;
    }
    
    public int getUniqueHostCount() {
        return hostLastSeen.size();
    }

    public void pulse(PacketEvent event, boolean inbound) {
        this.activity = 1.0;
        PacketParticle p = new PacketParticle(event.protocol(), inbound);
        particles.add(p);
        this.lastProtocolColor = p.color;

        this.totalBytes += event.payloadSize();
        this.connectionCount++;
        
        String remoteIp = inbound ? event.sourceIp() : event.targetIp();
        this.hostLastSeen.put(remoteIp, System.nanoTime());
        
        this.protocolStats.merge(event.protocol(), 1, Integer::sum);
        
        if (inbound) {
            this.lastPorts = event.sourcePort() + " -> " + event.targetPort();
        } else {
            this.lastPorts = event.targetPort() + " -> " + event.sourcePort();
        }
    }

    public void update() {
        // Decay activity
        if (activity > 0) {
            activity -= AppConfig.get().getDecayRatePerFrame();
        }
        
        // Prune expired hosts from the cluster
        long now = System.nanoTime();
        double lifeSeconds = AppConfig.get().getStarLifeSeconds();
        long expirationNanos = (long) (lifeSeconds * 1_000_000_000L);
        
        hostLastSeen.entrySet().removeIf(entry -> (now - entry.getValue()) > expirationNanos);

        // Update particles
        Iterator<PacketParticle> it = particles.iterator();
        while (it.hasNext()) {
            PacketParticle p = it.next();
            p.update();
            if (p.isFinished()) it.remove();
        }
    }

    public void drawParticles(GraphicsContext gc, double centerX, double centerY) {
        for (PacketParticle p : particles) {
            double startX, startY, endX, endY;
            if (p.inbound) {
                startX = this.x; startY = this.y;
                endX = centerX; endY = centerY;
            } else {
                startX = centerX; startY = centerY;
                endX = this.x; endY = this.y;
            }
            double currentX = startX + (endX - startX) * p.progress;
            double currentY = startY + (endY - startY) * p.progress;
            gc.setFill(p.color);
            gc.fillOval(currentX - 2, currentY - 2, 4, 4);
        }
    }

    public Color getLastProtocolColor() { return lastProtocolColor; }

    public void applyPhysics() {
        this.x += this.vx;
        this.y += this.vy;
        this.vx *= 0.90;
        this.vy *= 0.90;
    }

    public boolean contains(double mx, double my) {
        double dx = this.x - mx;
        double dy = this.y - my;
        double radius = 12 + Math.min(20, hostLastSeen.size() * 2.0);
        return (dx * dx + dy * dy) < (radius * radius);
    }

    public boolean isDead() {
        // Node dies if activity is zero AND no particles AND no active hosts
        return activity <= 0 && particles.isEmpty() && hostLastSeen.isEmpty();
    }
}
