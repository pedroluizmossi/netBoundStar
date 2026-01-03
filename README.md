This is the translated and polished version of your `README.md` in English, maintaining the high-level technical tone and structural integrity.

---

# NetBoundStar

> **High-Performance Real-Time Network Telemetry Visualization**

NetBoundStar is a traffic engineering tool that transforms packet analysis into an intuitive visual experience. Utilizing astrophysical metaphors (constellations, gravity, repulsion), it dynamically organizes network connections, allowing for immediate identification of traffic patterns, DDoS attacks, connection anomalies, and geographic data distribution.

Unlike traditional table-based analyzers (such as Wireshark), NetBoundStar focuses on real-time **topology and density**, built on a modern Java 21 architecture optimized for zero-latency.

---

## Performance Engineering & Optimizations

NetBoundStar is engineered to operate on Gigabit links without causing Garbage Collector (GC) pauses or graphical interface jitter. The architecture follows **"Zero-Allocation"** principles across the execution Hot Path.

### 1. Lock-Free Architecture (Ring Buffer)
Instead of traditional queues (`BlockingQueue`), we utilize a fixed-size **Ring Buffer** (131,072 slots) with atomic cursors (`AtomicLong`).
*   **Backpressure:** Implements a *Drop-on-Full* strategy. If the UI cannot keep up with the ingress rate (e.g., during a 10Gbps burst), packets are silently dropped before entering the heap, preventing `OutOfMemoryError` and maintaining application responsiveness.
*   **Bridge:** The `TrafficBridge` acts as a shared memory bus between the Capture Thread (Producer) and the Rendering Thread (Consumer) without locking (`synchronized`).

### 2. Raw Byte Parsing (Bitwise Operations)
Most network libraries instantiate heavy objects (`IPv4Packet`, `TcpPacket`) for every frame. NetBoundStar eliminates this overhead:
*   **Optimized Sniffer:** The `SnifferService` reads the raw `byte[]` directly from the network interface.
*   **Bitwise Parsing:** IPv4/IPv6 headers are decoded using bit shifting (`<<`, `&`) and manual masking. This avoids intermediate object allocation just to read IPs and Ports.
*   **Intelligent Truncation:** `SNAPLEN` is set to 128 bytes. Only headers are copied from Kernel to User Space (JNI), drastically reducing memory copy overhead while preserving `originalLength` for accurate bandwidth statistics.

### 3. Object Pooling & Memory Management
*   **Event Reuse:** `PacketEvent` objects within the Ring Buffer are allocated only during initialization and reused indefinitely.
*   **Generational ZGC:** The application is tuned to run with ZGC (`-XX:+UseZGC`), ideal for managing high allocation rates of temporary byte arrays with sub-millisecond pauses.

### 4. High-Frequency JavaFX Rendering
*   **Bitmap Caching:** Text elements (IPs, Labels) are rendered to images (`WriteableImage`) once and cached. This avoids expensive glyph layout recalculations and font rasterization in every frame (60 FPS).
*   **State Batching:** The Canvas avoids unnecessary `Color` allocations and state changes, using `setGlobalAlpha` for transparency effects instead of deriving new color objects.
*   **Virtual Threads:** DNS resolutions and GeoIP lookups (IO-bound) are delegated to **Virtual Threads** (Project Loom), ensuring the rendering thread is never blocked by external network latency.

---

## System Architecture

The diagram below illustrates the data flow from the network card to the screen pixels, highlighting the optimization layers:

```mermaid
flowchart TD
    subgraph Kernel Space
        NIC[Network Interface Card]
        BPF[BPF Filter - ip or ip6]
    end

    subgraph "NetBoundStar Engine (Daemon Thread)"
        JNI[Pcap4j JNI Wrapper]
        Raw[Raw byte[128]]
        Sniffer[SnifferService]
        BitOp[Bitwise Parsing]
    end

    subgraph "Shared Memory (Lock-Free)"
        RB[("Ring Buffer<br/>(131k Slots)")]
    end

    subgraph "NetBoundStar View (JavaFX Thread)"
        Anim[Animation Loop]
        Pool[Object Pool]
        Physics[Physics Engine]
        Canvas[Canvas Rendering]
        Cache[Bitmap & Font Cache]
    end

    subgraph "Async Workers (Virtual Threads)"
        DNS[DNS Resolver]
        Geo[GeoIP Service]
    end

    NIC -->|Full Packet| BPF
    BPF -->|Truncated Frame| JNI
    JNI -->|Copy| Raw
    Raw --> Sniffer
    Sniffer -->|Extract Headers| BitOp
    BitOp -->|Write - Claim/Commit| RB
    RB -->|Read - Poll| Anim
    Anim -->|Reuse| Pool
    Anim -->|Update Pos| Physics
    Anim -->|Draw| Canvas
    Canvas -.->|Read| Cache
    
    Anim -.->|Request Info| DNS
    Anim -.->|Request Info| Geo
    DNS & Geo -.->|Update State| Anim

    style RB fill:#f9f,stroke:#333,stroke-width:2px
    style BitOp fill:#bbf,stroke:#333,stroke-width:1px
    style Canvas fill:#bfb,stroke:#333,stroke-width:1px
```

---

## Features

*   **Geographic Clustering:** Automatic grouping of nodes by country as traffic density increases.
*   **Integrated Geo-location:** Automatic origin country resolution using a built-in MaxMind database.
*   **Particle Physics:** A visual simulation where each packet is a particle and each host is a celestial body subject to attraction and repulsion forces.
*   **Real-time Metrics:** Monitoring of PPS (Packets Per Second), Upload/Download bandwidth, and protocol distribution (TCP/UDP/ICMP).
*   **Dynamic Configuration:** Adjust physics parameters, node lifetime, and visual sensitivity during runtime.

---

##  Requirements & Installation

### Prerequisites
*   **Java 21 LTS** (Required for Virtual Threads and Generational ZGC support).
*   **Maven 3.8+**.
*   **Libpcap/Npcap**:
    *   *Linux:* `sudo apt install libpcap-dev`
    *   *Windows:* [Npcap](https://npcap.com/) (Install in "WinPcap API-compatible Mode").

### Compilation

```bash
git clone https://github.com/your-user/netBoundStar.git
cd netBoundStar
mvn clean install
```

### Building the Executable JAR

First, build the complete project to generate the executable FAT JAR:

```bash
# From the project root directory
mvn clean package
```

This command:
- Compiles all modules (netBoundStar-core, netBoundStar-engine, netBoundStar-view, netBoundStar-app)
- Packages all dependencies into a single executable JAR
- Places the generated JAR at: `netBoundStar-app/target/netBoundStar-app-1.0.0-SNAPSHOT.jar`

### Execution

**Linux / macOS:**
```bash
# From the project root directory (after mvn clean package)
cd netBoundStar-app/target
sudo java -XX:+UseZGC -XX:+ZGenerational -jar netBoundStar-app-1.0.0-SNAPSHOT.jar
```

**Windows (Run as Administrator):**
```cmd
cd netBoundStar-app\target
java -XX:+UseZGC -XX:+ZGenerational -jar netBoundStar-app-1.0.0-SNAPSHOT.jar
```

#### Alternative: Using Maven Directly

```bash
# From the project root directory
mvn exec:java
```

This option allows testing without building the FAT JAR.

---

## JVM Flags Explained

The following JVM flags are recommended for optimal NetBoundStar performance:

| Flag | Purpose |
| :--- | :--- |
| `-XX:+UseZGC` | Enables the ZGC garbage collector (low-latency, optimized for high throughput) |
| `-XX:+ZGenerational` | Activates generational mode for ZGC (reduces pause times further) |
| `-XX:+UseStringDeduplication` | Reduces memory usage for duplicate strings (optional) |
| `-Dprism.order=d3d,es2` | (Windows) Prioritizes Direct3D for graphics rendering |
| `-Djavafx.animation.fullspeed=true` | Runs animation loop at maximum frequency (60 FPS) |

---

## Configuration

The `.netboundstar.config` configuration file is automatically generated in the user directory. Adjustable parameters include:

| Parameter | Description |
| :--- | :--- |
| `network.interface` | Interface name (e.g., eth0). If empty, it auto-detects. |
| `cluster.by.country` | `true` to group IPs by flag/country. |
| `physics.repulsion` | Repulsion force between nodes (prevents overlap). |
| `star.life` | Time in seconds a node remains visible without active traffic. |

---

## Releasing to GitHub

Follow these steps to create and publish a release on GitHub:

### 1. Prepare the Release (Update Version)

First, update the version in the parent `pom.xml` from `-SNAPSHOT` to a release version:

```bash
# Edit pom.xml and change version from X.X.X-SNAPSHOT to X.X.X
# Example: 1.0.0-SNAPSHOT → 1.0.0
vi pom.xml
```

Or use Maven Release Plugin:
```bash
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit
```

### 2. Build the Executable JAR

```bash
mvn clean package
```

This generates the executable JAR at: `netBoundStar-app/target/netBoundStar-app-1.0.0.jar`

### 3. Commit and Tag the Release

```bash
# Commit the version change
git add .
git commit -m "Release version 1.0.0"

# Create a Git tag for this release
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push commits and tags to GitHub
git push origin master
git push origin v1.0.0
```

### 4. Create a GitHub Release

You have two options:

#### Option A: Using GitHub CLI (Recommended)

```bash
# Install GitHub CLI if not already installed
# Then authenticate:
gh auth login

# Create a release with the JAR file
gh release create v1.0.0 \
  netBoundStar-app/target/netBoundStar-app-1.0.0.jar \
  --title "NetBoundStar v1.0.0" \
  --notes "Release notes here..."
```

#### Option B: Using GitHub Web Interface

1. Go to: `https://github.com/pedroluizmossi/netBoundStar/releases`
2. Click **"Create a new release"**
3. Select the tag `v1.0.0`
4. Fill in the title and description
5. Upload the JAR file: `netBoundStar-app/target/netBoundStar-app-1.0.0.jar`
6. Click **"Publish release"**

### 5. Update Version Back to SNAPSHOT (for Next Development)

After releasing, prepare the project for the next development cycle:

```bash
# Update version to next SNAPSHOT (e.g., 1.1.0-SNAPSHOT)
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
mvn versions:commit

# Commit the change
git add .
git commit -m "Prepare version 1.1.0-SNAPSHOT"
git push origin master
```

### Complete Release Workflow Example

```bash
# 1. Update version
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit

# 2. Build
mvn clean package

# 3. Commit and tag
git add .
git commit -m "Release version 1.0.0"
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin master
git push origin v1.0.0

# 4. Create release (using CLI)
gh release create v1.0.0 \
  netBoundStar-app/target/netBoundStar-app-1.0.0.jar \
  --title "NetBoundStar v1.0.0" \
  --notes "Release notes here..."

# 5. Prepare next version
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
mvn versions:commit
git add .
git commit -m "Prepare version 1.1.0-SNAPSHOT"
git push origin master
```

---

## License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Developed with a focus on high performance and data visualization.*
