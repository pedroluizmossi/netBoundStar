# NetBoundStar 🌌

**NetBoundStar** é uma ferramenta de visualização de telemetria de rede em tempo real. Diferente de analisadores convencionais (como Wireshark), ele transforma o tráfego de dados numa experiência visual artística baseada em "Constelações", onde cada conexão é uma estrela e cada pacote é uma partícula de energia.

## 🏛 Arquitetura

O projeto segue um padrão de **Monólito Modular** para garantir desacoplamento entre a captura de baixo nível e a renderização de alto nível.

### Módulos
* **`netBoundStar-core`**: O domínio puro. Contém os DTOs (`PacketEvent`) e o Barramento de Eventos (`TrafficBridge`). Sem dependências externas.
* **`netBoundStar-engine`**: O "Sniffer". Usa `Pcap4j` para interceptar pacotes da placa de rede, filtrá-los e publicá-los no barramento.
* **`netBoundStar-view`**: (Em desenvolvimento) O motor gráfico em JavaFX. Responsável pela renderização do Canvas e cálculos de física.
* **`netBoundStar-app`**: O orquestrador. Inicializa as threads e injeta as dependências.

## 🛠 Requisitos

* **Java 21** (LTS)
* **Maven** 3.8+
* **Driver de Captura de Pacotes (Nativo):**
  * *Windows:* [Npcap](https://npcap.com/) (Instalar com a opção "WinPcap API-compatible Mode").
  * *Linux:* `libpcap-dev` (Geralmente requer execução com `sudo`).
  * *MacOS:* `libpcap`.

## 🚀 Como Rodar (Modo Console)

1. Certifique-se de ter o Npcap/Libpcap instalado.
2. Compile o projeto:
   ```bash
   mvn clean install
   ```

3. Execute a classe Main no módulo app:

   **No Linux/macOS (requer sudo):**
   ```bash
   cd /home/pedrom/IdeaProjects/netBoundStar
   sudo mvn exec:java -Dexec.mainClass="com.pedro.netboundstar.app.Main" -pl netBoundStar-app
   ```

   **No Windows (IDE como Administrador):**
   - Abra o IntelliJ IDEA como Administrador
   - Execute `com.pedro.netboundstar.app.Main` normalmente

   **Via JAR (após compilação):**
   ```bash
   sudo java -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) com.pedro.netboundstar.app.Main
   ```

> **Nota:** No Linux, você pode precisar de permissões elevadas. Se receber erros de permissão, rode com `sudo`.

## 📊 Fluxo de Dados

```
┌─────────────────┐
│  Interface NIC  │ (Placa de Rede)
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│   SnifferService (Thread)   │ (engine)
│  Pcap4j → NetworkSelector   │
└────────┬────────────────────┘
         │ publish()
         ▼
┌─────────────────────────────┐
│      TrafficBridge          │ (core - Queue)
│   ConcurrentLinkedQueue     │
└────────┬────────────────────┘
         │ poll()
         ▼
┌─────────────────────────────┐
│  Console/UI (Thread)        │ (app/view)
│  Renderização ou Logs       │
└─────────────────────────────┘
```

## 🌟 Características Planejadas

- ✅ Captura de pacotes em tempo real
- ✅ Detecção automática de interface de rede
- 🚧 Visualização em Canvas JavaFX
- 🚧 Física de partículas (atração/repulsão)
- 🚧 Efeitos de glow e cores por protocolo
- 🚧 Estatísticas em tempo real (FPS, velocidade)

## 📝 Licença

MIT - Sinta-se livre para usar, modificar e distribuir.

---

**"We are all connected."** 🌐

