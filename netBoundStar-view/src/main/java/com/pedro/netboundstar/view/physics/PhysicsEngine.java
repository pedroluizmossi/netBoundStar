package com.pedro.netboundstar.view.physics;

import com.pedro.netboundstar.view.StarNode;
import java.util.Collection;

/**
 * Motor de Física para Constelação de Nós.
 * Implementa duas leis da física:
 * 1. Lei de Coulomb (Repulsão): Nós se repelem entre si
 * 2. Lei de Hooke (Atração): Nós são atraídos para o centro
 *
 * Resultado: Uma órbita harmoniosa sem sobreposição de texto
 */
public class PhysicsEngine {

    // CALIBRAGEM (A parte divertida: brinque com esses números depois)
    private static final double REPULSION_FORCE = 5000.0; // Força com que nós se odeiam
    private static final double ATTRACTION_FORCE = 0.005; // Força da gravidade do centro (elástico)
    private static final double MAX_SPEED = 10.0;         // Limite para ninguém teletransportar

    /**
     * Atualiza as forças físicas e posições de todos os nós.
     * Deve ser chamado a cada frame antes de renderizar.
     *
     * @param nodes Coleção de todos os nós ativos
     * @param centerX Posição X do centro (seu computador)
     * @param centerY Posição Y do centro (seu computador)
     */
    public void update(Collection<StarNode> nodes, double centerX, double centerY) {

        // 1. REPULSÃO (Nó contra Nó)
        // Isso é O(N^2), cuidado com muitos nós (+500 pode pesar)
        for (StarNode nodeA : nodes) {
            for (StarNode nodeB : nodes) {
                if (nodeA == nodeB) continue; // Não repelir a si mesmo

                // Vetor de A para B
                double dx = nodeA.x - nodeB.x;
                double dy = nodeA.y - nodeB.y;
                double distSq = dx * dx + dy * dy;

                // Evita divisão por zero e forças infinitas se estiverem muito perto
                if (distSq < 1.0) distSq = 1.0;

                // Força inversamente proporcional à distância (quanto mais perto, mais forte)
                double force = REPULSION_FORCE / distSq;

                // Normaliza o vetor para ficar com magnitude 1
                double dist = Math.sqrt(distSq);
                double fx = (dx / dist) * force;
                double fy = (dy / dist) * force;

                // Aplica a força de repulsão
                nodeA.vx += fx;
                nodeA.vy += fy;
            }
        }

        // 2. ATRAÇÃO (Gravidade Central)
        for (StarNode node : nodes) {
            // Vetor apontando do nó para o centro
            double dx = centerX - node.x;
            double dy = centerY - node.y;

            // Puxa suavemente para o centro (proporcional à distância - Lei de Hooke)
            node.vx += dx * ATTRACTION_FORCE;
            node.vy += dy * ATTRACTION_FORCE;

            // Limita a velocidade máxima (Segurança: previne teletransporte)
            double speed = Math.sqrt(node.vx * node.vx + node.vy * node.vy);
            if (speed > MAX_SPEED) {
                node.vx = (node.vx / speed) * MAX_SPEED;
                node.vy = (node.vy / speed) * MAX_SPEED;
            }

            // Aplica o movimento final
            node.applyPhysics();
        }
    }
}

