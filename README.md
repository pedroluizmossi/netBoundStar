# NetBoundStar

NetBoundStar is a high-performance, real-time network telemetry visualization tool. It transforms network traffic into an interactive visual experience based on a star constellation metaphor, where each connection is a star and each packet is a particle of energy.

Unlike traditional packet analyzers that present data in tabular formats, NetBoundStar uses a physics engine to organize network nodes dynamically, providing an intuitive view of traffic density, direction, and origin.

## Features

- **Real-Time Visualization**: Watch packets travel between your local machine and remote hosts instantly.
- **Physics-Based Layout**: Nodes organize themselves using a simulation of Coulomb's Law (repulsion) and Hooke's Law (attraction), creating a harmonious, self-organizing graph.
- **Smart Clustering**: Automatically groups connections by country when traffic density is high. Clusters dynamically expand and shrink based on active unique hosts, keeping the view clean without losing data.
- **Rich Telemetry**: Hover over any node or cluster to see detailed metrics:
  - Unique IP count per region.
  - Dominant protocol distribution (TCP/UDP).
  - Data transfer totals and active ports.
- **Geolocation & DNS**: Automatically resolves IP addresses to hostnames and identifies the country of origin, displaying national flags.
- **Customizable**: Adjust physics parameters, star lifespan, clustering modes, and visual effects in real-time via the settings menu.

## Performance Engineering

NetBoundStar is engineered to handle Gigabit network speeds without GC pauses or UI stuttering. Key optimizations include:

### 1. Zero-Allocation Architecture
- **Object Pooling**: `PacketEvent` objects are pre-allocated at startup and reused indefinitely.
- **Raw Byte Parsing**: The sniffer parses raw Ethernet frames manually using bitwise operations, avoiding the creation of heavy Pcap4j wrapper objects (IpV4Packet, TcpPacket).
- **Truncated Capture**: `SNAPLEN` is set to 128 bytes to minimize native-to-Java memory copying, while `getOriginalLength()` ensures traffic stats remain accurate.

### 2. Ring Buffer & Backpressure
- **LMAX-style Ring Buffer**: Replaces standard queues with a fixed-size circular buffer (16,384 slots) using `AtomicLong` cursors.
- **Drop-on-Full Strategy**: If the UI cannot keep up with the network (e.g., during a massive download), packets are dropped silently to prevent `OutOfMemoryError` and preserve application stability.

### 3. JavaFX Rendering Optimizations
- **Bitmap Caching**: Text labels are rendered to images once and cached, avoiding expensive `GlyphLayout` and `BidiBase` calculations on every frame.
- **State Management**: Replaces `deriveColor()` allocations with `gc.setGlobalAlpha()`, reusing static Color instances.
- **Indexed Loops**: Critical paths use indexed `for` loops instead of Iterators to eliminate `ArrayList$Itr` allocation.

### 4. Garbage Collection
- Tuned for **ZGC Generational** (Java 21+) to handle high allocation rates of short-lived byte arrays with sub-millisecond pauses.

## Architecture

The project follows a modular monolith architecture to ensure separation of concerns:

- **netBoundStar-core**: Domain layer containing shared models and the Ring Buffer implementation. No external runtime dependencies.
- **netBoundStar-engine**: Packet capture layer. Uses Pcap4j with BPF filters (`ip`) to sniff traffic efficiently.
- **netBoundStar-view**: Presentation layer built with JavaFX. Handles the rendering loop, particle system, and physics engine.
- **netBoundStar-app**: Application entry point. Orchestrates startup and JVM tuning.

## Requirements

- **Java 21** (LTS) or higher.
- **Maven** 3.8 or higher.
- **Packet Capture Library**:
    - **Linux**: `libpcap-dev` (Run with `sudo` or `setcap`).
    - **Windows**: [Npcap](https://npcap.com/) (Install with "WinPcap API-compatible Mode").
    - **macOS**: `libpcap` (Pre-installed).

## Installation and Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/netBoundStar.git
   cd netBoundStar
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the application**:

   **Linux / macOS**:
   Network capture requires root privileges or `CAP_NET_RAW` capability.
   ```bash
   # Recommended: Run with ZGC for best performance
   sudo mvn exec:java -Dexec.mainClass="com.pedro.netboundstar.app.Main" -pl netBoundStar-app
   ```

   **Windows**:
   Run your IDE or terminal as Administrator, then execute the main class `com.pedro.netboundstar.app.Main`.

## Configuration

The application creates a configuration file (`.netboundstar.config`) in your user home directory. You can also adjust settings dynamically during runtime by clicking the **Config** button in the application window.

Adjustable parameters include:
- **Clustering**: Toggle "Group by Country" to consolidate traffic from the same region.
- **Physics**: Control repulsion/attraction forces and max speed.
- **Lifespan**: Adjust how long stars/clusters remain visible after activity ceases.
- **Visuals**: Particle speed and core heat sensitivity.

## License

This project is licensed under the MIT License.
