#!/bin/bash

# Script para rodar NetBoundStar com permissões necessárias

echo "╔════════════════════════════════════════════════════════════╗"
echo "║           NetBoundStar - Network Visualizer               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "⚠️  Esta aplicação requer permissões de root/admin para"
echo "    capturar pacotes de rede."
echo ""

# Detectar o SO
if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then
    echo "🐧 Sistema Unix detectado. Usando sudo..."
    echo "💡 Dica: Configure sudo NOPASSWD para evitar pedir senha toda vez:"
    echo "   sudo visudo"
    echo "   Adicione: %sudo ALL=(ALL) NOPASSWD: /usr/bin/java"
    echo ""
    sudo mvn exec:java \
        -Dexec.mainClass="com.pedro.netboundstar.app.Main" \
        -pl netBoundStar-app
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    echo "🪟 Windows detectado. Certifique-se de rodar como Administrador!"
    echo ""
    mvn exec:java \
        -Dexec.mainClass="com.pedro.netboundstar.app.Main" \
        -pl netBoundStar-app
else
    echo "⚠️  Sistema operacional não identificado."
    echo "   Tentando rodar normalmente..."
    mvn exec:java \
        -Dexec.mainClass="com.pedro.netboundstar.app.Main" \
        -pl netBoundStar-app
fi

