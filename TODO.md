# Roadmap & TODO List 📝

## ✅ Fase 1: Fundação (Concluído)
- [x] Configurar estrutura Maven Multi-module.
- [x] Criar DTOs imutáveis (`PacketEvent`) usando Java Records.
- [x] Implementar `TrafficBridge` (Queue Concorrente) para comunicação entre threads.
- [x] Implementar detecção automática de interface de rede (`NetworkSelector`).
- [x] Implementar captura de pacotes com Pcap4j (`SnifferService`).
- [x] Validar fluxo de dados via Console Logger.

## 🚧 Fase 2: O Palco Visual (Próximo Passo)
- [ ] Criar a janela básica JavaFX em `netBoundStar-view`.
- [ ] Configurar o `AnimationTimer` (Loop de Renderização de 60 FPS).
- [ ] Implementar um `Canvas` preto resizável que ocupa a tela toda.
- [ ] Conectar o loop de renderização à `TrafficBridge` para ler os eventos sem travar a UI.

## 🔭 Fase 3: Física e "As Estrelas"
- [ ] Criar classe `StarNode` (representa um IP remoto).
- [ ] Implementar lógica de gerenciamento de nós (se o IP é novo, cria estrela; se inativo, apaga).
- [ ] **Algoritmo de Física:**
    - [ ] Adicionar atração gravitacional (nós são puxados para o centro).
    - [ ] Adicionar repulsão (nós se empurram para não ficarem amontoados).
- [ ] Desenhar linhas de conexão entre o "Centro" (Localhost) e as Estrelas.

## 🎨 Fase 4: Partículas e Cores
- [ ] Implementar sistema de cores baseado no Enum `Protocol` (TCP = Azul, UDP = Laranja, etc).
- [ ] Criar `PacketParticle`: pequenos pontos que viajam na linha de conexão quando um pacote chega.
- [ ] Adicionar efeitos de "Glow" (brilho) quando o tráfego é intenso.

## 🔧 Fase 5: Polimento
- [ ] Adicionar overlay de texto (FPS, Total de Pacotes, Upload/Download Speed).
- [ ] Permitir pausar a animação (barra de espaço).
- [ ] Criar um JAR executável final com todas as dependências.

## 🐛 Questões Abertas
- Qual deve ser a taxa de atualização da UI? (60 FPS padrão)
- Como lidar com IPs privados vs públicos? (Cores diferentes?)
- Deve ter filtro de protocolos na UI?

