package com.pedro.netboundstar.view.physics;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.view.StarNode;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Optimized Physics Engine for the Node Constellation.
 * Uses cached lists and indexed loops to avoid allocation during the physics step.
 */
public class PhysicsEngine {

    /**
     * Maximum distance squared to consider for repulsion. 
     * Beyond this, the force is too weak to matter.
     */
    private static final double REPULSION_THRESHOLD_SQ = 400 * 400;
    
    /**
     * Solid radius around the center that nodes cannot enter.
     * This should roughly match the rendered center visuals.
     */
    private static final double CENTER_SOLID_RADIUS = 30; // base visual radius in NetworkCanvas

    // Reusable list to avoid allocating a new ArrayList/Array every frame
    private final ArrayList<StarNode> nodeList = new ArrayList<>(1000);

    public void update(Collection<StarNode> nodes, double centerX, double centerY) {
        update(nodes, centerX, centerY, CENTER_SOLID_RADIUS);
    }

    public void update(Collection<StarNode> nodes, double centerX, double centerY, double centerSolidRadius) {
        AppConfig config = AppConfig.get();
        double repulsionForce = config.getRepulsionForce();
        double attractionForce = config.getAttractionForce();
        double maxSpeed = config.getMaxPhysicsSpeed();

        // 1. Refresh the working list (Reuse memory)
        nodeList.clear();
        nodeList.addAll(nodes);

        int size = nodeList.size();

        // 2. OPTIMIZED REPULSION
        for (int i = 0; i < size; i++) {
            StarNode nodeA = nodeList.get(i);
            for (int j = i + 1; j < size; j++) {
                StarNode nodeB = nodeList.get(j);

                double dx = nodeA.x - nodeB.x;
                double dy = nodeA.y - nodeB.y;
                if (Math.abs(dx) > 400 || Math.abs(dy) > 400) continue;

                double distSq = dx * dx + dy * dy;
                if (distSq > REPULSION_THRESHOLD_SQ) continue;
                if (distSq < 1.0) distSq = 1.0;

                double force = repulsionForce / distSq;
                double dist = Math.sqrt(distSq);

                double fx = (dx / dist) * force;
                double fy = (dy / dist) * force;

                if (!nodeA.isFrozen) {
                    nodeA.vx += fx;
                    nodeA.vy += fy;
                }

                if (!nodeB.isFrozen) {
                    nodeB.vx -= fx;
                    nodeB.vy -= fy;
                }
            }
        }

        // 3. ATTRACTION AND MOVEMENT + COLLISION
        double minDist = Math.max(0, centerSolidRadius);
        double minDistSq = minDist * minDist;

        for (int i = 0; i < size; i++) {
            StarNode node = nodeList.get(i);

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

            // CENTER COLLISION
            double cx = node.x - centerX;
            double cy = node.y - centerY;
            double distSq = cx * cx + cy * cy;

            if (distSq < minDistSq) {
                double dist = Math.sqrt(Math.max(distSq, 0.0001));
                double nx = cx / dist;
                double ny = cy / dist;

                node.x = centerX + nx * minDist;
                node.y = centerY + ny * minDist;

                double vDotN = node.vx * nx + node.vy * ny;
                if (vDotN < 0) {
                    node.vx -= vDotN * nx;
                    node.vy -= vDotN * ny;
                }

                node.vx *= 0.85;
                node.vy *= 0.85;
            }
        }
    }
}
