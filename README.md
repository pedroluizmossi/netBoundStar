# NetBoundStar

NetBoundStar is a real-time network telemetry visualization tool. It transforms network traffic into an interactive visual experience based on a star constellation metaphor, where each connection is a star and each packet is a particle of energy.

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
- **High Performance**: Built on Java 21 using Virtual Threads for asynchronous I/O operations (DNS, GeoIP) without blocking the rendering loop.
- **Customizable**: Adjust physics parameters, star lifespan, clustering modes, and visual effects in real-time via the settings menu.

## Architecture

The project follows a modular monolith architecture to ensure separation of concerns:

- **netBoundStar-core**: The domain layer containing shared models (PacketEvent, Protocol), the event bus (TrafficBridge), and configuration management. It has no external runtime dependencies.
- **netBoundStar-engine**: The packet capture layer. It uses Pcap4j to sniff network traffic, filter IPv4 packets, and publish events to the core bridge.
- **netBoundStar-view**: The presentation layer built with JavaFX. It handles the rendering loop, particle system, physics engine, and UI components.
- **netBoundStar-app**: The application entry point. It orchestrates the startup process, initializing the background sniffer thread and launching the graphical interface.

## Requirements

- **Java 21** (LTS) or higher.
- **Maven** 3.8 or higher.
- **Packet Capture Library**:
    - **Linux**: `libpcap-dev` (Run with `sudo` to access network devices).
    - **Windows**: [Npcap](https://npcap.com/) (Install with "WinPcap API-compatible Mode").
    - **macOS**: `libpcap` (Pre-installed, may require permissions).

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
   Network capture requires root privileges to access the network interface in promiscuous mode.
   ```bash
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
