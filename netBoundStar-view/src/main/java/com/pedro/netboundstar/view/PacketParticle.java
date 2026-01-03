package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.model.Protocol;
import javafx.scene.paint.Color;

public class PacketParticle {
    public double progress = 0.0; // 0.0 (início) a 1.0 (chegada)
    public final Color color;
    public final boolean inbound; // true = Download (Vem pra mim), false = Upload (Vai pro IP)
    public final double speed;

    public PacketParticle(Protocol protocol, boolean inbound) {
        this.inbound = inbound;
        this.color = getColorByProtocol(protocol);
        // Velocidade configurável via AppConfig
        this.speed = AppConfig.get().getRandomParticleSpeed();
    }

    public void update() {
        progress += speed;
    }

    public boolean isFinished() {
        return progress >= 1.0;
    }

    private Color getColorByProtocol(Protocol p) {
        return switch (p) {
            case TCP -> Color.CYAN;        // Confiável, frio
            case UDP -> Color.ORANGE;      // Rápido, quente
            case ICMP -> Color.MAGENTA;    // Ping
            default -> Color.GRAY;
        };
    }
}

