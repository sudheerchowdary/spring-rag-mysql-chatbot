#!/bin/bash

# Docker Quick Setup Script for Spring RAG MySQL Chatbot
# This script helps you set up Ollama in Docker and run Spring Boot with proper configuration

set -e

echo "🚀 Spring RAG MySQL Chatbot - Docker Setup"
echo "=========================================="
echo ""

# Detect OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macOS"
    OLLAMA_HOST="http://host.docker.internal:11434"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="Linux"
    OLLAMA_HOST="http://172.17.0.1:11434"
else
    echo "❌ Unsupported OS: $OSTYPE"
    exit 1
fi

echo "📱 Detected OS: $OS"
echo "🔗 Ollama URL: $OLLAMA_HOST"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

echo "✅ Docker is installed"
echo ""

# Check for required environment variables
if [ -z "$MYSQL_PASSWORD" ]; then
    echo "⚠️  MYSQL_PASSWORD not set. Using default: 'password'"
    MYSQL_PASSWORD="password"
fi

if [ -z "$OPEN_AI_API_KEY" ]; then
    echo "❌ ERROR: OPEN_AI_API_KEY environment variable is required!"
    echo "   Set it with: export OPEN_AI_API_KEY=sk-your-api-key"
    exit 1
fi

echo "✅ Environment variables configured"
echo ""

# Start Ollama container
echo "🐳 Starting Ollama Docker container..."
if docker ps --format '{{.Names}}' | grep -q '^ollama$'; then
    echo "   Ollama container already running"
else
    docker run -d --name ollama -p 11434:11434 ollama/ollama
    sleep 3
    echo "   ✅ Ollama container started"
fi

echo ""
echo "📥 Pulling neural-chat model..."
docker exec ollama ollama pull neural-chat > /dev/null 2>&1
echo "   ✅ Model downloaded"

echo ""
echo "🔧 Configuring environment variables..."
export MYSQL_PASSWORD="$MYSQL_PASSWORD"
export OPEN_AI_API_KEY="$OPEN_AI_API_KEY"
export OLLAMA_BASE_URL="$OLLAMA_HOST"

echo "   ✅ OLLAMA_BASE_URL=$OLLAMA_BASE_URL"
echo "   ✅ OPEN_AI_API_KEY=****(set)"
echo "   ✅ MYSQL_PASSWORD=****(set)"

echo ""
echo "✨ Setup complete! Starting Spring Boot application..."
echo ""
echo "📊 Application will start at: http://localhost:8080"
echo "🤖 Ollama is available at: $OLLAMA_HOST"
echo ""
echo "To view logs, run: docker logs -f ollama"
echo "To stop Ollama: docker stop ollama && docker rm ollama"
echo ""

# Run Maven
mvn spring-boot:run

