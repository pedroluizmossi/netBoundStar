# Como Rodar o NetBoundStar 🚀

## Pré-requisitos

1. **Java 21+** instalado
2. **Maven 3.8+** instalado
3. **libpcap** instalado:
   - **Linux**: `sudo apt-get install libpcap-dev`
   - **macOS**: `brew install libpcap`
   - **Windows**: [Instale Npcap](https://npcap.com/) com a opção "WinPcap API-compatible Mode"

## Compilação

Na raiz do projeto:

```bash
mvn clean install
```

## Execução

### Opção 1: Via Script (Recomendado)

```bash
./run.sh
```

O script detecta automaticamente o SO e aplica as permissões necessárias.

### Opção 2: Manual (Linux/macOS)

```bash
cd /home/pedrom/IdeaProjects/netBoundStar
sudo mvn exec:java -Dexec.mainClass="com.pedro.netboundstar.app.Main" -pl netBoundStar-app
```

### Opção 3: Via IDE (IntelliJ IDEA)

1. Abra o projeto em IntelliJ
2. Configure uma Run Configuration:
   - **Main class**: `com.pedro.netboundstar.app.Main`
   - **Module**: `netBoundStar-app`
   - **VM options** (se necessário): `-Djna.library.path=/usr/lib` (Linux)

3. **No Linux**: Execute com `sudo`:
   - `Run` → `Edit Configurations`
   - Marque "Execute in terminal"
   - Run como `sudo java -cp ... com.pedro.netboundstar.app.Main`

4. **No Windows**: Execute o IntelliJ como Administrador e rode normalmente

### Opção 4: Via JAR Executável

```bash
# Gerar um JAR com todas as dependências
mvn package -DskipTests

# Executar
cd netBoundStar-app/target
sudo java -jar netBoundStar-app-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

## Solução de Problemas

### "You don't have permission to perform this capture"

**Linux**: Execute com `sudo`:
```bash
sudo mvn exec:java -Dexec.mainClass="com.pedro.netboundstar.app.Main" -pl netBoundStar-app
```

Ou configure permissões permanentes:
```bash
sudo setcap cap_net_raw,cap_net_admin=eip $(which java)
```

**Windows**: Execute o IntelliJ/cmd como **Administrador**.

### "Module not found" / Compilation errors

```bash
# Limpar cache e recompilar
mvn clean install -U
```

### A janela JavaFX não aparece

1. Certifique-se de ter LibGL instalado (Linux):
   ```bash
   sudo apt-get install libgl1-mesa-glx
   ```

2. No Linux com Wayland, tente:
   ```bash
   export GDK_BACKEND=x11
   sudo -E mvn exec:java -Dexec.mainClass="com.pedro.netboundstar.app.Main" -pl netBoundStar-app
   ```

## Esperado ao Iniciar

Ao rodar a aplicação, você verá:

1. Logs informando que o Sniffer iniciou
2. Uma janela JavaFX abrindo com um Canvas preto
3. Um ponto branco no centro (seu computador)
4. Estatísticas no canto superior esquerdo (FPS, Total de Pacotes, Última Conexão)

Qualquer tráfego de rede será exibido em tempo real!

## Desenvolvendo

### Estrutura do Projeto

```
netBoundStar/
├── netBoundStar-core/      # DTOs, TrafficBridge
├── netBoundStar-engine/    # Sniffer Pcap4j
├── netBoundStar-view/      # JavaFX UI
└── netBoundStar-app/       # Main
```

### Compilar Apenas um Módulo

```bash
mvn compile -pl netBoundStar-view
```

### Rodar Testes

```bash
mvn test
```

---

**Dúvidas?** Verifique o `TODO.md` para entender a roadmap do projeto!

