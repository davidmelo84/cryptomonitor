#!/usr/bin/env bash
# exit on error
set -o errexit

echo "🚀 Iniciando build do projeto..."

# Instalar dependências e compilar
./mvnw clean install -DskipTests

echo "✅ Build concluído com sucesso!"