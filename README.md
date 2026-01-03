# NetBoundStar 🌌

**NetBoundStar** is a real-time network telemetry visualization tool. Unlike conventional packet analyzers (such as Wireshark), it transforms data traffic into an artistic visual experience based on "Constellations" — each connection is a star and each packet is a particle of energy.

## 🏛 Architecture

The project follows a modular monolith pattern to keep low-level capture decoupled from high-level rendering.

### Modules
* **`netBoundStar-core`**: The pure domain layer. Contains DTOs (`PacketEvent`) and the Event Bus (`TrafficBridge`). No external runtime dependencies.
* **`netBoundStar-engine`**: The "Sniffer". Uses `Pcap4j` to capture network packets, filter them and publish events to the bus.
* **`netBoundStar-view`**: (In development) The JavaFX graphics engine. Responsible for Canvas rendering and physics calculations.
* **`netBoundStar-app`**: The orchestrator. Boots threads and wires dependencies.

## 🛠 Requirements

* **Java 21** (LTS)
* **Maven** 3.8+
* **Native Packet Capture Driver:**
  * *Windows:* [Npcap](https://npcap.com/) (Install with the "WinPcap API-compatible Mode" option).
  * *Linux:* `libpcap-dev` (May require running with `sudo`).
  * *MacOS:* `libpcap`.

## 🚀 How to Run (Console Mode)

1. Make sure Npcap / Libpcap is installed.
2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the Main class in the app module:

   **On Linux/macOS (may require sudo):**
   ```bash
   cd /home/pedrom/IdeaProjects/netBoundStar
   sudo mvn exec:java -Dexec.mainClass="com.pedro.netboundstar.app.Main" -pl netBoundStar-app
   ```

   **On Windows (use IDE or run as Administrator):**
   - Open IntelliJ IDEA as Administrator
   - Run `com.pedro.netboundstar.app.Main` normally

   **Via JAR (after building):**
   ```bash
   sudo java -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) com.pedro.netboundstar.app.Main
   ```

> **Note:** On Linux, elevated permissions may be required to open network interfaces. If you see permission errors, try running with `sudo`.

## 📊 Data Flow

```
┌─────────────────┐
│  Network NIC    │ (Network Interface)
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
│  Rendering or Logs          │
└─────────────────────────────┘
```

## 🌟 Planned Features

- ✅ Real-time packet capture
- ✅ Automatic network interface detection
- 🚧 JavaFX Canvas-based visualization
- 🚧 Particle physics (attraction/repulsion)
- 🚧 Glow and protocol-based coloring effects
- 🚧 Real-time statistics (FPS, throughput)

## 📝 License

MIT - Feel free to use, modify and distribute.

---

**"We are all connected."** 🌐
