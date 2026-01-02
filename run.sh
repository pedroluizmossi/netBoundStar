#!/bin/bash

# Script para compilar e executar NetBoundStar em modo console
# Uso: ./run.sh ou bash run.sh

set -e  # Exit on error

PROJECT_ROOT="/home/pedrom/IdeaProjects/netBoundStar"
MAVEN_REPO="$HOME/.m2/repository"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  🚀 NetBoundStar - Inicializador"
echo "═══════════════════════════════════════════════════════════════"
echo ""

cd "$PROJECT_ROOT"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}📦 Compilando projeto...${NC}"
mvn clean compile -DskipTests -q 2>/dev/null || {
    echo -e "${RED}❌ Erro ao compilar. Certifique-se de ter Maven instalado.${NC}"
    exit 1
}
echo -e "${GREEN}✅ Compilação concluída${NC}"
echo ""

# Verificar permissões
if [[ $EUID -ne 0 ]]; then
    echo -e "${YELLOW}⚠️  Aviso: Este script precisa ser executado com sudo para capturar pacotes.${NC}"
    echo -e "${YELLOW}   Pressione Ctrl+C se quiser interromper...${NC}"
    echo ""
fi

cd netBoundStar-app

# Construir CLASSPATH
CLASSPATH="target/classes"
CLASSPATH="$CLASSPATH:../netBoundStar-core/target/classes"
CLASSPATH="$CLASSPATH:../netBoundStar-engine/target/classes"
CLASSPATH="$CLASSPATH:$MAVEN_REPO/org/pcap4j/pcap4j-core/1.8.2/pcap4j-core-1.8.2.jar"
CLASSPATH="$CLASSPATH:$MAVEN_REPO/org/pcap4j/pcap4j-packetfactory-static/1.8.2/pcap4j-packetfactory-static-1.8.2.jar"
CLASSPATH="$CLASSPATH:$MAVEN_REPO/net/java/dev/jna/jna/5.11.0/jna-5.11.0.jar"
CLASSPATH="$CLASSPATH:$MAVEN_REPO/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"
CLASSPATH="$CLASSPATH:$MAVEN_REPO/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"

echo -e "${YELLOW}🔍 Procurando interfaces de rede...${NC}"
echo -e "${YELLOW}🎬 Iniciando captura (pressione Ctrl+C para parar)...${NC}"
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Executar
java -cp "$CLASSPATH" com.pedro.netboundstar.app.Main

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo -e "${YELLOW}👋 Captura finalizada${NC}"
echo "═══════════════════════════════════════════════════════════════"

