# Roadmap & TODO List 📝

## ✅ Fase 1: Fundação (Concluído)
- [x] Configurar estrutura Maven Multi-module.
- [x] Criar DTOs imutáveis (`PacketEvent`) usando Java Records.
- [x] Implementar `TrafficBridge` (Queue Concorrente) para comunicação entre threads.
- [x] Implementar detecção automática de interface de rede (`NetworkSelector`).
- [x] Implementar captura de pacotes com Pcap4j (`SnifferService`).
- [x] Validar fluxo de dados via Console Logger.

## ✅ Fase 2: O Palco Visual (Concluído)
- [x] Criar a janela básica JavaFX em `netBoundStar-view`.
- [x] Configurar o `AnimationTimer` (Loop de Renderização de 60 FPS).
- [x] Implementar um `Canvas` preto resizável que ocupa a tela toda.
- [x] Conectar o loop de renderização à `TrafficBridge` para ler os eventos sem travar a UI.

## ✅ Fase 3: Física e "As Estrelas" (Concluído)
- [x] Criar classe `StarNode` (representa um IP remoto).
- [x] Implementar lógica de gerenciamento de nós (se o IP é novo, cria estrela; se inativo, apaga).
- [x] Desenhar linhas de conexão entre o "Centro" (Localhost) e as Estrelas.
- [x] Implementar efeito de fade-out (stars desaparecem quando inativas).
- [x] Posicionar estrelas em órbita aleatória ao redor do centro.
- [ ] **Algoritmo de Física Avançada (Próxima Iteração):**
    - [ ] Adicionar atração gravitacional (nós são puxados para o centro).
    - [ ] Adicionar repulsão (nós se empurram para não ficarem amontoados).

## 🎨 Fase 4: Partículas e Cores (Concluído)
- [x] Implementar sistema de cores baseado no Enum `Protocol` (TCP = Azul, UDP = Laranja, ICMP = Rosa).
- [x] Criar `PacketParticle`: pequenos pontos que viajam na linha de conexão quando um pacote chega.
- [x] Detectar direção de fluxo (Inbound/Outbound): Identificar se é Download ou Upload.
- [x] Renderizar partículas em camadas corretas (Linhas -> Partículas -> Nós -> Centro).

## ✨ Fase 4.5: Melhorias de UX (Concluído)
- [x] **Vida Longa**: Reduzir decay rate de 0.5% para 0.1% por frame (10x mais lenta).
- [x] **Identidade Visual**: Mostrar IP ao lado de cada estrela (cinza claro, com transparência).
- [x] **Feedback Físico**: Núcleo pulsante que cresce quando recebe tráfego (centerHeat).
- [x] **Renderização em Camadas**: Linhas -> Partículas -> Estrelas + IPs -> Núcleo Pulsante.

## 🧠 Fase 5: DNS Assíncrono e Identidade (Concluído)
- [x] **DnsService**: Resolvedor de DNS assíncrono usando Virtual Threads (Java 21+).
- [x] **Cache de DNS**: Evita múltiplas requisições do mesmo IP.
- [x] **displayName Dinâmico**: Estrelas começam com IP e "evoluem" para hostname.
- [x] **AppConfig Integrado**: Todas as constantes centralizadas e configuráveis.

## ⚛️ Fase 6: Física de Constelação (Concluído)
- [x] **Lei de Coulomb**: Repulsão entre nós (evita sobreposição de texto).
- [x] **Lei de Hooke**: Atração gravitacional para o centro (elástico).
- [x] **Velocidade Vetorial**: Cada nó tem vx e vy para movimento suave.
- [x] **Atrito (Friction)**: Nós desaceleram gradualmente (0.9x por frame).
- [x] **Limite de Velocidade**: MAX_SPEED previne teletransporte.
- [x] **PhysicsEngine.java**: Motor O(N²) para cálculos de força.
- [x] **Integração no Canvas**: Física executada antes de atualizações lógicas.

## 🔧 Fase 7: Configurações Persistentes (Concluído)
- [x] **AppConfig Persistente**: Salva/carrega arquivo `netboundstar.config` na pasta do usuário.
- [x] **Campos de Física**: `repulsionForce`, `attractionForce`, `maxPhysicsSpeed` configuráveis.
- [x] **SettingsWindow.java**: Janela flutuante com Sliders para ajuste em tempo real.
- [x] **UI Integration**: Botão "⚙ Config" no canto superior direito da janela principal.
- [x] **Save on Close**: Configurações salvas automaticamente ao fechar a aplicação.
- [x] **Dynamic Physics**: PhysicsEngine lê valores do AppConfig a cada frame.

## 🎮 Fase 8: Interatividade - Hover & Click (Concluído)
- [x] **Suporte a Portas**: PacketEvent agora inclui sourcePort e targetPort.
- [x] **Extração de Portas**: SnifferService extrai portas de pacotes TCP/UDP.
- [x] **StarNode Interativa**: Estados `isHovered` e `isFrozen` para controle.
- [x] **Acúmulo de Dados**: Totaliza bytes e formata strings de portas.
- [x] **Hit Detection**: Método `contains()` para detecção de clique/hover.
- [x] **PhysicsEngine Respeitador**: Nós congelados não se movem.
- [x] **Mouse Tracking**: Event listeners para movimento e clique.
- [x] **Visual Indicators**: Círculo amarelo (hover) e ciano (frozen).
- [x] **Tooltip Detalhado**: Exibe host, IP, portas, status e total de dados.
- [x] **Formatação de Bytes**: Converte bytes em KB/MB/GB legível.

## 🌍 Fase 9: Geolocalização & Bandeiras (Em Progresso)
- [x] **MaxMind GeoIP2**: Dependência Maven adicionada.
- [x] **FlagCache.java**: Cache em memória de imagens de bandeiras.
- [x] **GeoService.java**: Resolução assíncrona de IP → País (ISO Code).
- [x] **StarNode Geo**: Integração com geolocalização.
- [x] **NetworkCanvas Flags**: Renderização de bandeiras no lugar de bolinhas brancas.
- [ ] **Manual Setup**: Usuário deve baixar GeoLite2-Country.mmdb e flag icons.

## 📊 Fase 10: Estatísticas em Tempo Real (Próximo)
