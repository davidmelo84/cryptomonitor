#!/usr/bin/env bash
# exit on error
set -o errexit

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 CRYPTO MONITOR - BUILD SCRIPT"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Garantir permissões do mvnw
chmod +x mvnw

echo "📦 Instalando dependências..."
./mvnw clean install -DskipTests

echo ""
echo "✅ BUILD CONCLUÍDO COM SUCESSO!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"