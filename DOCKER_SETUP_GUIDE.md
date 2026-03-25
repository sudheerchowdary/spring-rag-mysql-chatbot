# 🐳 Ollama Docker Setup - Complete Implementation

## What Was Changed

Your Spring Boot chatbot couldn't reach Ollama in Docker because it was looking at `localhost:11434` from outside the Docker container. I've made the connection URL configurable so it works with Docker.

### Code Changes Made

**1. LLMSQLService.java**
```java
// Before: Hardcoded URL
.baseUrl("http://localhost:11434")

// After: Configurable from environment
@Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl
.baseUrl(ollamaBaseUrl)
```

**2. application.yml** (New line added)
```yaml
ollama:
  base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
```

---

## How to Run NOW

### Step 1: Verify Ollama is Running in Docker

```bash
# Check if container is running
docker ps | grep ollama

# If not running, start it:
docker run -d --name ollama -p 11434:11434 ollama/ollama

# Pull the model
docker exec ollama ollama pull neural-chat
```

### Step 2: Set Environment Variable (IMPORTANT!)

Based on your operating system:

**🍎 macOS (Docker Desktop):**
```bash
export OLLAMA_BASE_URL=http://host.docker.internal:11434
```

**🐧 Linux:**
```bash
export OLLAMA_BASE_URL=http://172.17.0.1:11434
```

**🪟 Windows (WSL2/Docker Desktop):**
```bash
export OLLAMA_BASE_URL=http://host.docker.internal:11434
```

### Step 3: Set Other Required Variables

```bash
export MYSQL_PASSWORD=root_password     # Your MySQL root password
export OPEN_AI_API_KEY=sk-...          # Your OpenAI API key
```

### Step 4: Start Spring Boot

```bash
cd /Users/sowmyagutha/Sudheer/work/git/spring-rag-mysql-chatbot
./mvnw spring-boot:run

# Or if using system Maven:
mvn spring-boot:run
```

### Step 5: Test

Open your browser: **http://localhost:8080**

Try asking: "Show all products"

---

## Docker Networking Explained

| Where Spring Boot Runs | Where Ollama Runs | Connection URL | Why It Works |
|---|---|---|---|
| Local machine | Local machine | `http://localhost:11434` | Direct connection |
| Local machine | Docker (macOS/Windows) | `http://host.docker.internal:11434` | Special Docker DNS name |
| Local machine | Docker (Linux) | `http://172.17.0.1:11434` | Bridge network gateway |
| Docker container | Another Docker (same network) | `http://ollama:11434` | Service discovery |
| Cloud/Kubernetes | Remote Ollama | `http://ollama-service.example.com:11434` | Direct IP or hostname |

---

## Verify Connection Works

Before starting Spring Boot, test the connection:

```bash
# macOS/Windows
curl http://host.docker.internal:11434/api/tags

# Linux
curl http://172.17.0.1:11434/api/tags

# Expected output:
# {"models":[{"name":"neural-chat:latest",...}]}
```

---

## Full Docker Compose Setup (Optional)

If you want to containerize everything:

```bash
# Create Dockerfile in project root
cat > Dockerfile << 'EOF'
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Create docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  ollama:
    image: ollama/ollama
    container_name: ollama
    ports:
      - "11434:11434"
    networks:
      - rag-network

  mysql:
    image: mysql:8.0
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: password
    ports:
      - "3306:3306"
    volumes:
      - ./rag-mysql-chatbot.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - rag-network

  app:
    build: .
    container_name: spring-rag-chatbot
    ports:
      - "8080:8080"
    environment:
      MYSQL_PASSWORD: password
      OPEN_AI_API_KEY: ${OPEN_AI_API_KEY}
      OLLAMA_BASE_URL: http://ollama:11434
    depends_on:
      - ollama
      - mysql
    networks:
      - rag-network

networks:
  rag-network:
    driver: bridge
EOF

# Run it
export OPEN_AI_API_KEY=sk-your-key
docker-compose up --build
```

---

## Troubleshooting

### ❌ "Ollama is not available: Failed to connect"

**Solution:** Check you're using the correct OLLAMA_BASE_URL for your setup:
- macOS/Windows: `http://host.docker.internal:11434` ✅
- Linux: `http://172.17.0.1:11434` ✅
- Docker Compose: `http://ollama:11434` ✅

### ❌ "curl: Failed to connect to host.docker.internal"

**Solution:** You're on Linux. Use instead:
```bash
export OLLAMA_BASE_URL=http://172.17.0.1:11434
```

### ❌ Docker container "ollama" is not running

**Solution:** Start it:
```bash
docker run -d --name ollama -p 11434:11434 ollama/ollama
docker exec ollama ollama pull neural-chat
```

### ❌ "Connection refused" from Docker Compose

**Solution:** Make sure both services are on the same network and use the service name:
```yaml
OLLAMA_BASE_URL: http://ollama:11434  # Use service name, not localhost
```

### ❌ Port 11434 already in use

**Solution:** Use a different port:
```bash
docker run -d --name ollama -p 11435:11434 ollama/ollama
export OLLAMA_BASE_URL=http://host.docker.internal:11435
```

---

## Quick Cheat Sheet

```bash
# Setup (one time)
docker run -d --name ollama -p 11434:11434 ollama/ollama
docker exec ollama ollama pull neural-chat

# Configure (per terminal session)
export OLLAMA_BASE_URL=http://host.docker.internal:11434  # macOS/Windows
export OLLAMA_BASE_URL=http://172.17.0.1:11434           # Linux
export MYSQL_PASSWORD=your_password
export OPEN_AI_API_KEY=sk-your-key

# Run
mvn spring-boot:run

# Test
curl http://localhost:8080
```

---

## How to Rebuild After Changes

```bash
# Compile
./mvnw clean compile

# Package (creates JAR)
./mvnw clean package

# Run packaged JAR
java -jar target/rag-mysql-chatbot-0.0.1-SNAPSHOT.jar
```

---

## Files Updated

- ✅ `src/main/java/com/rag/mysql/rag_mysql_chatbot/service/LLMSQLService.java` - Made URL configurable
- ✅ `src/main/resources/application.yml` - Added ollama.base-url property
- ✅ `AGENTS.md` - Added Docker networking docs
- ✅ `DOCKER_SETUP.md` - Comprehensive guide (created)
- ✅ `DOCKER_QUICK_FIX.md` - Quick reference (created)

---

## Success Indicators

When it's working:

✅ Spring Boot starts without errors  
✅ `curl http://host.docker.internal:11434/api/tags` returns models  
✅ Chat interface loads at `http://localhost:8080`  
✅ Chat responds to queries about your database  
✅ Console shows "Using OpenAI GPT-4o" or "Using Ollama (local LLM) as fallback"  

You're all set! 🚀

