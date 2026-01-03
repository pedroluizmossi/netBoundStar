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
    
    // Reusable list to avoid allocating a new ArrayList/Array every frame
    private final ArrayList<StarNode> nodeList = new ArrayList<>(1000);

    public void update(Collection<StarNode> nodes, double centerX, double centerY) {
        AppConfig config = AppConfig.get();
        double repulsionForce = config.getRepulsionForce();
        double attractionForce = config.getAttractionForce();
        double maxSpeed = config.getMaxPhysicsSpeed();

        // 1. Refresh the working list (Reuse memory)
        nodeList.clear();
        nodeList.addAll(nodes);
        
        int size = nodeList.size();

        // 2. OPTIMIZED REPULSION (Single Threaded for Zero-Allocation)
        // Parallel streams allocate objects (Tasks, Spliterators). 
        // For < 2000 nodes, a raw loop is often faster and generates 0 garbage.
        for (int i = 0; i < size; i++) {
            StarNode nodeA = nodeList.get(i);
            if (nodeA.isFrozen) continue;

            for (int j = 0; j < size; j++) {
                if (i == j) continue; // Skip self

                StarNode nodeB = nodeList.get(j);

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
        }

        // 3. ATTRACTION AND MOVEMENT
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
        }
    }
}
