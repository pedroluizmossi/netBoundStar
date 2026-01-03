#!/bin/bash
# Script para rodar NetBoundStar com classpath completo do Maven

# Muda para o diretório do projeto
cd /home/pedrom/IdeaProjects/netBoundStar

# Genera o classpath completo com Maven
CLASSPATH=$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout 2>/dev/null | tail -1)

# Adiciona os JARs do projeto compilados
CLASSPATH="netBoundStar-app/target/classes:netBoundStar-engine/target/classes:netBoundStar-core/target/classes:netBoundStar-view/target/classes:$CLASSPATH"

echo "🚀 Executando NetBoundStar com classpath completo..."
echo ""

# Roda a aplicação com o classpath correto
sudo java -cp "$CLASSPATH" com.pedro.netboundstar.app.Main

