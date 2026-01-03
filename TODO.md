# Roadmap & TODO List 📝

## ✅ Phase 1: Foundation (Completed)
- [x] Configure Maven multi-module structure.
- [x] Create immutable DTOs (`PacketEvent`) using Java Records.
- [x] Implement `TrafficBridge` (Concurrent Queue) for inter-thread communication.
- [x] Implement automatic network interface detection (`NetworkSelector`).
- [x] Implement packet capture with Pcap4j (`SnifferService`).
- [x] Validate data flow via Console Logger.

## ✅ Phase 2: The Visual Stage (Completed)
- [x] Create the basic JavaFX window in `netBoundStar-view`.
- [x] Configure the `AnimationTimer` (60 FPS render loop).
- [x] Implement a resizable black `Canvas` that fills the window.
- [x] Connect the render loop to `TrafficBridge` to read events without blocking the UI.

## ✅ Phase 3: Physics and "The Stars" (Completed)
- [x] Create class `StarNode` (represents a remote IP).
- [x] Implement node management logic (if IP is new, create a star; if inactive, remove it).
- [x] Draw connection lines between the "Center" (Localhost) and the Stars.
- [x] Implement fade-out effect (stars disappear when inactive).
- [x] Position stars in random orbit around the center.
- [ ] **Advanced Physics Algorithm (Next Iteration):**
    - [ ] Add gravitational attraction (nodes pulled toward the center).
    - [ ] Add repulsion (nodes push each other to avoid overlap).

## 🎨 Phase 4: Particles & Colors (Completed)
- [x] Implement color system based on `Protocol` enum (TCP = Blue, UDP = Orange, ICMP = Pink).
- [x] Create `PacketParticle`: small dots that travel along the connection line when a packet arrives.
- [x] Detect flow direction (Inbound/Outbound): identify Download or Upload.
- [x] Render particles in correct layers (Lines -> Particles -> Nodes -> Center).

## ✨ Phase 4.5: UX Improvements (Completed)
- [x] **Long Lifetime**: Reduce decay rate from 0.5% to 0.1% per frame (10x slower).
- [x] **Visual Identity**: Show IP next to each star (light gray, with transparency).
- [x] **Physical Feedback**: Pulsing core that grows when it receives traffic (centerHeat).
- [x] **Layered Rendering**: Lines -> Particles -> Stars + IPs -> Pulsing Core.

## 🧠 Phase 5: Asynchronous DNS & Identity (Completed)
- [x] **DnsService**: Asynchronous DNS resolver using Virtual Threads (Java 21+).
- [x] **DNS Cache**: Avoids multiple requests for the same IP.
- [x] **Dynamic displayName**: Stars start with IP and "evolve" to hostname.
- [x] **AppConfig Integration**: All constants centralized and configurable.

## ⚛️ Phase 6: Constellation Physics (Completed)
- [x] **Coulomb Law**: Repulsion between nodes (prevents text overlap).
- [x] **Hooke's Law**: Gravitational attraction to the center (elastic).
- [x] **Vector Velocity**: Each node has vx and vy for smooth movement.
- [x] **Friction**: Nodes gradually slow down (0.9x per frame).
- [x] **Max Speed Limit**: MAX_SPEED prevents teleportation.
- [x] **PhysicsEngine.java**: O(N²) engine for force calculations.
- [x] **Integration on Canvas**: Physics runs before logical updates.

## 🔧 Phase 7: Persistent Settings (Completed)
- [x] **Persistent AppConfig**: Save/load `netboundstar.config` file in the user's folder.
- [x] **Physics Fields**: `repulsionForce`, `attractionForce`, `maxPhysicsSpeed` configurable.
- [x] **SettingsWindow.java**: Floating window with Sliders for live tuning.
- [x] **UI Integration**: "⚙ Config" button in the top-right corner of the main window.
- [x] **Save on Close**: Settings saved automatically on application exit.
- [x] **Dynamic Physics**: PhysicsEngine reads AppConfig values every frame.

## 🎮 Phase 8: Interactivity - Hover & Click (Completed)
- [x] **Port Support**: PacketEvent now includes sourcePort and targetPort.
- [x] **Port Extraction**: SnifferService extracts ports from TCP/UDP packets.
- [x] **Interactive StarNode**: States `isHovered` and `isFrozen` for control.
- [x] **Data Accumulation**: Totals bytes and formats port strings.
- [x] **Hit Detection**: `contains()` method for click/hover detection.
- [x] **PhysicsEngine Respectful**: Frozen nodes do not move.
- [x] **Mouse Tracking**: Event listeners for movement and clicks.
- [x] **Visual Indicators**: Yellow circle (hover) and cyan (frozen).
- [x] **Detailed Tooltip**: Shows host, IP, ports, status and total data.
- [x] **Byte Formatting**: Converts bytes to human-readable KB/MB/GB.

## 🌍 Phase 9: Geolocation & Flags (Completed)
- [x] **MaxMind GeoIP2**: Maven dependency added.
- [x] **FlagCache.java**: In-memory cache for flag images (PNG).
- [x] **GeoService.java**: Asynchronous resolution IP → Country (ISO Code).
- [x] **StarNode Geo**: Integration with geolocation.
- [x] **NetworkCanvas Flags**: Render flags instead of white dots.
- [x] **PNG Support**: JavaFX does not support SVG, only PNG works.

## 📊 Phase 10: Real-time Stats (Completed)
- [x] **StatsManager.java**: Dedicated class for statistics calculation.
- [x] **Speedometer**: Download/Upload in real-time (Bytes/s).
- [x] **Session Totals**: Total downloaded/uploaded.
- [x] **Protocol Counts**: TCP/UDP/ICMP distribution.
- [x] **Traffic History**: LinkedList with last 100 points.
- [x] **Bottom Dashboard**: Semi-transparent panel with speeds and a chart.
- [x] **Top HUD**: Active connections and total packets.

## 🔧 Phase 11: Final Polish (Next)
- [ ] Create an executable JAR with all dependencies.
- [ ] Allow pausing the animation (space bar).
- [ ] Add real FPS to the HUD.
- [ ] Improve performance for high traffic volumes.
