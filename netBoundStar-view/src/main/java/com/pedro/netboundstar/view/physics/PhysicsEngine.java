package com.pedro.netboundstar.view.physics;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.view.StarNode;
import java.util.Collection;
import java.util.List;

/**
 * Optimized Physics Engine for the Node Constellation.
 * Uses parallel processing and distance thresholds to handle hundreds of nodes.
 */
public class PhysicsEngine {

    /**
     * Maximum distance squared to consider for repulsion. 
     * Beyond this, the force is too weak to matter.
     */
    private static final double REPULSION_THRESHOLD_SQ = 400 * 400;

    public void update(Collection<StarNode> nodes, double centerX, double centerY) {
        AppConfig config = AppConfig.get();
        double repulsionForce = config.getRepulsionForce();
        double attractionForce = config.getAttractionForce();
        double maxSpeed = config.getMaxPhysicsSpeed();

        // Convert to list for indexed access or parallel stream
        List<StarNode> nodeList = List.copyOf(nodes);

        // 1. OPTIMIZED REPULSION (Parallel Processing)
        nodeList.parallelStream().forEach(nodeA -> {
            if (nodeA.isFrozen) return;

            for (StarNode nodeB : nodeList) {
                if (nodeA == nodeB) continue;

                double dx = nodeA.x - nodeB.x;
                double dy = nodeA.y - nodeB.y;
                double distSq = dx * dx + dy * dy;

                // Optimization: Skip if nodes are too far apart
                if (distSq > REPULSION_THRESHOLD_SQ) continue;

                if (distSq < 1.0) distSq = 1.0;

                double force = repulsionForce / distSq;
                double dist = Math.sqrt(distSq);
                
                nodeA.vx += (dx / dist) * force;
                nodeA.vy += (dy / dist) * force;
            }
        });

        // 2. ATTRACTION AND MOVEMENT
        for (StarNode node : nodeList) {
            if (node.isFrozen) {
                node.vx = 0;
                node.vy = 0;
                continue;
            }

            double dx = centerX - node.x;
            double dy = centerY - node.y;

            node.vx += dx * attractionForce;
            node.vy += dy * attractionForce;

            double speedSq = node.vx * node.vx + node.vy * node.vy;
            if (speedSq > maxSpeed * maxSpeed) {
                double speed = Math.sqrt(speedSq);
                node.vx = (node.vx / speed) * maxSpeed;
                node.vy = (node.vy / speed) * maxSpeed;
            }

            node.applyPhysics();
        }
    }
}
