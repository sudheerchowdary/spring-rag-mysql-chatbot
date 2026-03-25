# 🚀 Quick Fix: Running with Ollama in Docker

Your error shows Ollama is in Docker but Spring Boot can't reach it at `localhost:11434`. Here's the fix:

## Immediate Solution (Next 2 Minutes)

### Step 1: Set the Correct Ollama URL

Based on your OS, set this environment variable:

**macOS/Windows (Docker Desktop):**
```bash
export OLLAMA_BASE_URL=http://host.docker.internal:11434
```

**Linux:**
```bash
export OLLAMA_BASE_URL=http://172.17.0.1:11434
```

### Step 2: Restart Spring Boot

```bash
# Stop the current instance (Ctrl+C)

# Set all required variables
export MYSQL_PASSWORD=your_password
export OPEN_AI_API_KEY=sk-your-key
export OLLAMA_BASE_URL=http://host.docker.internal:11434  # or Linux IP

# Start again
mvn spring-boot:run
```

### Step 3: Test

Open browser to: `http://localhost:8080`

Try a question like: "Show all products"

## Why This Works

- **Problem:** Spring Boot running locally tried to connect to `localhost:11434` from outside Docker
- **Solution:** Use `host.docker.internal` (macOS/Windows) or bridge network IP (Linux) to reach Docker container from host

## What Changed in Code

✅ `LLMSQLService.java` - Now accepts `ollama.base-url` environment variable  
✅ `application.yml` - Now configures Ollama URL from env var  
✅ `AGENTS.md` - Updated with Docker instructions  

## For Future Docker Compose Setup

See `DOCKER_SETUP.md` for full Docker Compose configuration to run everything in containers.

## Check Ollama is Reachable

```bash
# Test the connection
curl http://host.docker.internal:11434/api/tags  # macOS/Windows
curl http://172.17.0.1:11434/api/tags           # Linux
```

If successful, you'll see your Ollama models listed.

---

**That's it!** Your chatbot should now work with Ollama running in Docker. 🎉

