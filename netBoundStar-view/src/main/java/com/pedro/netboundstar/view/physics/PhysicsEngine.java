package com.pedro.netboundstar.view.physics;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.view.StarNode;
import java.util.Collection;

/**
 * Physics Engine for the Node Constellation.
 * Implements two physical laws:
 * 1. Coulomb's Law (Repulsion): Nodes repel each other.
 * 2. Hooke's Law (Attraction): Nodes are attracted to the center.
 *
 * Result: A harmonious orbit without text overlapping.
 *
 * NOTE: Force parameters are read dynamically from AppConfig,
 * allowing real-time adjustment via the UI.
 */
public class PhysicsEngine {

    /**
     * Updates physical forces and positions for all nodes.
     * Should be called every frame before rendering.
     *
     * @param nodes   Collection of all active nodes.
     * @param centerX X position of the center.
     * @param centerY Y position of the center.
     */
    public void update(Collection<StarNode> nodes, double centerX, double centerY) {

        // Read dynamic configuration values
        AppConfig config = AppConfig.get();
        double repulsionForce = config.getRepulsionForce();
        double attractionForce = config.getAttractionForce();
        double maxSpeed = config.getMaxPhysicsSpeed();

        // 1. REPULSION (Node vs Node)
        // Complexity is O(N^2).
        for (StarNode nodeA : nodes) {
            if (nodeA.isFrozen) continue; // Skip frozen nodes

            for (StarNode nodeB : nodes) {
                if (nodeA == nodeB) continue; // Do not repel self

                // Vector from B to A
                double dx = nodeA.x - nodeB.x;
                double dy = nodeA.y - nodeB.y;
                double distSq = dx * dx + dy * dy;

                // Avoid division by zero and infinite forces
                if (distSq < 1.0) distSq = 1.0;

                // Force inversely proportional to distance
                double force = repulsionForce / distSq;

                // Normalize vector
                double dist = Math.sqrt(distSq);
                double fx = (dx / dist) * force;
                double fy = (dy / dist) * force;

                // Apply repulsion force
                nodeA.vx += fx;
                nodeA.vy += fy;
            }
        }

        // 2. ATTRACTION (Central Gravity)
        for (StarNode node : nodes) {
            if (node.isFrozen) {
                // If frozen, zero out velocity
                node.vx = 0;
                node.vy = 0;
                continue;
            }

            // Vector pointing from node to center
            double dx = centerX - node.x;
            double dy = centerY - node.y;

            // Pull gently towards the center (Hooke's Law)
            node.vx += dx * attractionForce;
            node.vy += dy * attractionForce;

            // Limit maximum speed (Safety: prevents teleportation)
            double speed = Math.sqrt(node.vx * node.vx + node.vy * node.vy);
            if (speed > maxSpeed) {
                node.vx = (node.vx / speed) * maxSpeed;
                node.vy = (node.vy / speed) * maxSpeed;
            }

            // Apply final movement
            node.applyPhysics();
        }
    }
}
