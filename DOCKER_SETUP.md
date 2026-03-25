# Docker Setup Guide for Spring RAG MySQL Chatbot

## Quick Start: Ollama in Docker + Spring Boot Locally

Since Ollama is running in Docker and Spring Boot is likely running locally, follow these steps:

### macOS/Windows (Docker Desktop)

```bash
# 1. Ensure Ollama Docker container is running
docker run -d --name ollama -p 11434:11434 ollama/ollama

# 2. Pull the neural-chat model (or mistral)
docker exec ollama ollama pull neural-chat

# 3. Set environment variables (macOS/Windows use host.docker.internal)
export MYSQL_PASSWORD=your_mysql_root_password
export OPEN_AI_API_KEY=sk-your-key
export OLLAMA_BASE_URL=http://host.docker.internal:11434

# 4. Run Spring Boot
mvn spring-boot:run

# 5. Open browser
open http://localhost:8080
```

### Linux (Docker)

```bash
# 1. Start Ollama container
docker run -d --name ollama -p 11434:11434 ollama/ollama

# 2. Pull model
docker exec ollama ollama pull neural-chat

# 3. Set environment variables (Linux uses bridge IP)
export MYSQL_PASSWORD=your_mysql_root_password
export OPEN_AI_API_KEY=sk-your-key
export OLLAMA_BASE_URL=http://172.17.0.1:11434

# 4. Run Spring Boot
mvn spring-boot:run

# 5. Test
curl http://localhost:8080
```

## Full Docker Compose Setup (All Services)

Create a `docker-compose.yml` in the project root:

```yaml
version: '3.8'

services:
  # Ollama LLM Service
  ollama:
    image: ollama/ollama:latest
    container_name: rag-ollama
    ports:
      - "11434:11434"
    environment:
      OLLAMA_KEEP_ALIVE: 24h
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - rag-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 10s
      timeout: 5s
      retries: 5

  # MySQL Database
  mysql:
    image: mysql:8.0
    container_name: rag-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD:-password}
      MYSQL_DATABASE: rag-data-schema
    ports:
      - "3306:3306"
    volumes:
      - ./rag-mysql-chatbot.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql_data:/var/lib/mysql
    networks:
      - rag-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: rag-chatbot
    ports:
      - "8080:8080"
    environment:
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-password}
      OPEN_AI_API_KEY: ${OPEN_AI_API_KEY}
      OLLAMA_BASE_URL: http://ollama:11434
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/rag-data-schema
    depends_on:
      ollama:
        condition: service_healthy
      mysql:
        condition: service_healthy
    networks:
      - rag-network

networks:
  rag-network:
    driver: bridge

volumes:
  ollama_data:
  mysql_data:
```

### Create Dockerfile (in project root):

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Run with Docker Compose:

```bash
# Set environment variables
export MYSQL_PASSWORD=your_password
export OPEN_AI_API_KEY=sk-your-key

# Start all services
docker-compose up --build

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

## Environment Variable Configuration

### For Different Setups

| Setup | OLLAMA_BASE_URL |
|-------|-----------------|
| Local (no Docker) | `http://localhost:11434` |
| Docker Desktop (macOS/Windows) + Local Spring | `http://host.docker.internal:11434` |
| Linux Docker + Local Spring | `http://172.17.0.1:11434` |
| Docker Compose (same network) | `http://ollama:11434` |
| Cloud (AWS/GCP) | `http://ollama-service-ip:11434` |

## Debugging Docker Setup

### Check if Ollama is running:
```bash
# From host
curl http://localhost:11434/api/tags

# From Docker container
docker exec -it ollama curl http://localhost:11434/api/tags
```

### Check available models:
```bash
docker exec ollama ollama list
```

### Pull additional models:
```bash
docker exec ollama ollama pull mistral
docker exec ollama ollama pull llama2
```

### View Ollama logs:
```bash
docker logs ollama
```

### Test Spring Boot connection to Ollama:
```bash
# From inside app container
docker exec -it rag-chatbot curl http://ollama:11434/api/tags
```

## Common Issues & Solutions

### Issue: "Ollama is not available: Failed to connect to localhost:11434"

**Solution:** Your Spring Boot is running locally but Ollama is in Docker. Set:
```bash
# macOS/Windows
export OLLAMA_BASE_URL=http://host.docker.internal:11434

# Linux
export OLLAMA_BASE_URL=http://172.17.0.1:11434
```

### Issue: "Connection refused" from Docker Compose

**Solution:** Ensure both services are on the same network and service dependencies are correct. Check `depends_on` in docker-compose.yml.

### Issue: Ollama model not found (404)

**Solution:** Pull the model before running:
```bash
docker exec ollama ollama pull neural-chat
```

### Issue: Port 11434 already in use

**Solution:** Use a different port mapping:
```bash
docker run -d --name ollama -p 11435:11434 ollama/ollama
export OLLAMA_BASE_URL=http://localhost:11435
```

## Production Deployment

For production, consider:

1. **Use named volumes** instead of local mounts for data persistence
2. **Set resource limits** in docker-compose.yml:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 4G
   ```

3. **Use environment-specific configs** (prod.env, dev.env)

4. **Enable SSL/TLS** for API communication

5. **Use secrets management** for API keys (Docker Secrets or external vault)

6. **Set proper health checks** and restart policies

7. **Monitor logs** with ELK stack or similar

## References

- [Ollama Docker Documentation](https://hub.docker.com/r/ollama/ollama)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker)

