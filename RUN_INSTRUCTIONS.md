# 🚀 Como Rodar NetBoundStar

## Pré-requisitos

### Linux/Mac (Recomendado para este ambiente)
- **libpcap** instalada (geralmente vem por padrão)
- **Java 21+** instalado
- **Maven 3.8+** instalado
- Permissões de **root/sudo** (necessárias para capturar pacotes)

### Windows
- **Npcap** instalado (https://npcap.com/#download)
  - ⚠️ **Importante**: Durante a instalação, marque "Install Npcap in WinPcap API-compatible Mode"
- IntelliJ IDEA rodando como Administrador (opcional, mas recomendado)

---

## Opção 1: Rodar via Terminal (Linux/Mac)

### Passo 1: Compilar o projeto
```bash
cd /home/pedrom/IdeaProjects/netBoundStar
mvn clean package -DskipTests
```

### Passo 2: Rodar com sudo (necessário para capturar pacotes)
```bash
# Navegar até o diretório do app
cd netBoundStar-app

# Executar a classe Main com sudo
sudo java -cp target/classes:../netBoundStar-core/target/classes:../netBoundStar-engine/target/classes:/home/pedrom/.m2/repository/org/pcap4j/pcap4j-core/1.8.2/pcap4j-core-1.8.2.jar:/home/pedrom/.m2/repository/org/pcap4j/pcap4j-packetfactory-static/1.8.2/pcap4j-packetfactory-static-1.8.2.jar:/home/pedrom/.m2/repository/net/java/dev/jna/jna/5.11.0/jna-5.11.0.jar:/home/pedrom/.m2/repository/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar:/home/pedrom/.m2/repository/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar com.pedro.netboundstar.app.Main
```

**Alternativa simples (use um script bash para facilitar)**

---

## Opção 2: Rodar via IntelliJ IDEA (Recomendado para desenvolvimento)

### Passo 1: Abrir o projeto
- Abra a pasta `/home/pedrom/IdeaProjects/netBoundStar` no IntelliJ

### Passo 2: Configurar como root
No IntelliJ, você precisa rodar com permissões elevadas:

#### No Linux:
1. Vá a **Run → Edit Configurations**
2. Procure a configuração `Main` (ou crie uma nova)
3. Em **Environment variables**, adicione:
   ```
   JAVA_TOOL_OPTIONS=-Djava.awt.headless=false
   ```
4. Clique em **Run as Administrator** ao executar

**Alternativa (melhor):**
Abra o IntelliJ pelo terminal com sudo:
```bash
cd /home/pedrom/IdeaProjects/netBoundStar
sudo idea .
```

Depois, execute normalmente (Shift + F10 ou clique no play verde).

### Passo 3: Executar
- Navegue até `netBoundStar-app/src/main/java/com/pedro/netboundstar/app/Main.java`
- Clique no ícone Play (▶) verde ao lado da classe
- Ou use **Shift + F10**

---

## Esperado ao rodar

Se tudo está certo, você verá no console:
```
[main] INFO com.pedro.netboundstar.app.Main - === NetBoundStar Iniciando (Modo Console) ===
[Capture-Thread] INFO com.pedro.netboundstar.engine.util.NetworkSelector - Interface selecionada: eth0 - Ethernet
[Capture-Thread] INFO com.pedro.netboundstar.engine.service.SnifferService - A iniciar captura em: Ethernet
[Thread-1] INFO com.pedro.netboundstar.app.Main - Aguardando pacotes...
[TCP] 192.168.1.5 -> 142.250.1.1 | 1500 bytes
[UDP] 192.168.1.5 -> 1.1.1.1 | 64 bytes
[TCP] 192.168.1.5 -> 172.217.0.1 | 2048 bytes
...
```

---

## Troubleshooting

### ❌ `UnsatisfiedLinkError: Unable to load library 'pcap'`
**Solução:**
```bash
# Linux/Mac
sudo apt install libpcap-dev  # Debian/Ubuntu
brew install libpcap          # macOS

# Windows
# Reinstale o Npcap com "WinPcap API-compatible Mode" marcado
```

### ❌ `Permission denied` ou `Operation not permitted`
**Solução:** Rodar com `sudo`
```bash
sudo java -cp ... com.pedro.netboundstar.app.Main
```

### ❌ `"Nenhuma interface de rede encontrada"`
**Solução:** Verificar interfaces disponíveis
```bash
# Linux/Mac
ip link show
# ou
ifconfig

# Windows
ipconfig
```

### ❌ Nenhum pacote aparecendo no console
- Verifique se há tráfego de rede (abra um browser, faça download, etc.)
- Verifique a interface selecionada (deve estar conectada à internet)
- Tente mudar a interface no código (procure por `findActiveInterface()`)

---

## Script bash para facilitar (Linux/Mac)

Crie um arquivo `run.sh`:
```bash
#!/bin/bash

cd /home/pedrom/IdeaProjects/netBoundStar

echo "🔨 Compilando..."
mvn clean compile -DskipTests -q

echo "🚀 Executando com sudo..."
cd netBoundStar-app

CLASSPATH="target/classes"
CLASSPATH="$CLASSPATH:../netBoundStar-core/target/classes"
CLASSPATH="$CLASSPATH:../netBoundStar-engine/target/classes"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/org/pcap4j/pcap4j-core/1.8.2/pcap4j-core-1.8.2.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/org/pcap4j/pcap4j-packetfactory-static/1.8.2/pcap4j-packetfactory-static-1.8.2.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/net/java/dev/jna/jna/5.11.0/jna-5.11.0.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"
CLASSPATH="$CLASSPATH:$HOME/.m2/repository/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"

sudo java -cp "$CLASSPATH" com.pedro.netboundstar.app.Main
```

Depois:
```bash
chmod +x run.sh
./run.sh
```

---

## Status Atual

✅ **Core** (Modelos e TrafficBridge) → Completo
✅ **Engine** (Captura com Pcap4j) → Completo
✅ **App** (Orquestrador de teste) → Completo
⏳ **View** (JavaFX UI) → Próximo passo

