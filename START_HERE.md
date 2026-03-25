# 🚀 START HERE - Get Running in 5 Minutes

## Your Setup Complete ✅

All code is modified, tested, and ready to use. Here's exactly what to do:

---

## Step 1: Verify Prerequisites (1 minute)

```bash
# Check Java version (need 17+)
java -version

# Check Docker is running
docker --version

# Check MySQL is running
mysql -u root -p -e "SELECT 1;"
```

---

## Step 2: Start Ollama in Docker (2 minutes)

```bash
# Start Ollama container
docker run -d --name ollama -p 11434:11434 ollama/ollama

# Wait 3 seconds
sleep 3

# Pull the neural-chat model
docker exec ollama ollama pull neural-chat

# Verify it worked
docker exec ollama ollama list
```

Expected output: `neural-chat:latest`

---

## Step 3: Set Environment Variables (1 minute)

**Copy the ENTIRE block below for your OS:**

### 🍎 macOS Users
```bash
export OLLAMA_BASE_URL=http://host.docker.internal:11434
export MYSQL_PASSWORD=root
export OPEN_AI_API_KEY=sk-your-actual-key-here
```

### 🐧 Linux Users
```bash
export OLLAMA_BASE_URL=http://172.17.0.1:11434
export MYSQL_PASSWORD=root
export OPEN_AI_API_KEY=sk-your-actual-key-here
```

### 🪟 Windows (WSL2/Docker Desktop)
```bash
export OLLAMA_BASE_URL=http://host.docker.internal:11434
export MYSQL_PASSWORD=root
export OPEN_AI_API_KEY=sk-your-actual-key-here
```

**⚠️ IMPORTANT:** Replace `sk-your-actual-key-here` with your real OpenAI API key!

---

## Step 4: Start Spring Boot (1 minute)

```bash
cd /Users/sowmyagutha/Sudheer/work/git/spring-rag-mysql-chatbot

./mvnw spring-boot:run
```

**Watch the logs for:**
```
Started RagMysqlChatbotApplication in X seconds
```

---

## Step 5: Test It (DONE! ✅)

Open your browser:
```
http://localhost:8080
```

You should see:
- ✅ Dark sidebar on left
- ✅ Chat interface with "Welcome to SQL Chatbot"
- ✅ Input field at bottom

Try typing:
```
Show all products
```

You should get:
- Thinking indicator animation
- Response with product list formatted nicely
- Chat bubbles with timestamps

---

## Troubleshooting Quick Fixes

### ❌ "Failed to connect to localhost:11434"
**Fix:** You used the wrong OLLAMA_BASE_URL for your OS. Check Step 3 again.

### ❌ "Connection refused"
**Fix:** Ollama container isn't running. Run:
```bash
docker ps | grep ollama
```

If not running:
```bash
docker run -d --name ollama -p 11434:11434 ollama/ollama
docker exec ollama ollama pull neural-chat
```

### ❌ "Model not found"
**Fix:** Pull the model:
```bash
docker exec ollama ollama pull neural-chat
```

### ❌ Spring Boot won't start
**Fix:** Check env vars are set:
```bash
echo $OLLAMA_BASE_URL
echo $MYSQL_PASSWORD
echo $OPEN_AI_API_KEY
```

All three should have values. If not, re-run Step 3.

---

## What You're Running

### Code Changes Made:
- ✅ `LLMSQLService.java` - Made Ollama URL configurable
- ✅ `application.yml` - Added OLLAMA_BASE_URL config
- ✅ `index.html` - Professional agent-chat-UI style interface

### Key Features:
- ✅ Works with Ollama in Docker
- ✅ Automatic OpenAI → Ollama fallback when quota exceeded
- ✅ Beautiful dark-themed chat interface
- ✅ Sidebar with conversation history
- ✅ Thinking indicators while bot responds
- ✅ Formatted database results
- ✅ Fully responsive design

---

## Chat UI Features

| Feature | What It Does |
|---------|-------------|
| Dark sidebar | Shows conversation history |
| "New Chat" button | Start fresh conversation |
| Message bubbles | User (right, gradient) vs Bot (left, white) |
| Thinking animation | Shows when bot is responding |
| Timestamps | When each message was sent |
| Results formatting | Key: value pairs displayed nicely |
| Empty state | Welcome message on startup |

---

## Architecture Overview

```
User Question
    ↓
Web UI (localhost:8080)
    ↓
Spring Boot /api/chat endpoint
    ↓
LLMSQLService.queryDatabase()
    ↓
Try OpenAI GPT-4o
    ↓ [if quota exceeded]
    ↓
Fall back to Ollama (Docker)
    ↓
Query MySQL database
    ↓
Format results
    ↓
Return to UI
    ↓
Display in chat bubbles
```

---

## Next Steps (After Verification)

Once it's working, you can:

1. **Test More Queries**
   - "What are the top 3 products?"
   - "List all orders from the last week"
   - "Show me product details"

2. **Review Documentation**
   - `DOCKER_SETUP_GUIDE.md` - Detailed walkthrough
   - `AGENTS.md` - AI integration documentation
   - `CHECKLIST.md` - Comprehensive testing guide

3. **Deploy to Docker Compose** (Optional)
   - See `DOCKER_SETUP.md` for full containerization
   - Run everything in containers

4. **Production Deployment** (Future)
   - Use Docker Swarm or Kubernetes
   - Add monitoring and logging
   - Set up automated backups

---

## Success Indicators

When working correctly, you'll see:

✅ Chat loads at `http://localhost:8080`
✅ Sidebar with "New Chat" button visible
✅ "Welcome to SQL Chatbot" message appears
✅ Can type a question
✅ Thinking dots animate while bot responds
✅ Get formatted results back
✅ No red error messages
✅ Console shows "Using OpenAI GPT-4o" or "Using Ollama"

---

## Support Resources

**Quick questions?**
→ See `QUICK_REFERENCE.md`

**Step-by-step help?**
→ See `DOCKER_SETUP_GUIDE.md`

**Something broken?**
→ See `DOCKER_QUICK_FIX.md`

**Full technical details?**
→ See `DOCKER_SETUP.md`

**Need verification?**
→ See `CHECKLIST.md`

---

## Environment Variables Explained

| Variable | What It Is | Example |
|----------|-----------|---------|
| `OLLAMA_BASE_URL` | Where Docker Ollama is accessible | `http://host.docker.internal:11434` |
| `MYSQL_PASSWORD` | MySQL root password | `root` |
| `OPEN_AI_API_KEY` | Your OpenAI API key | `sk-abc123...` |

These tell Spring Boot where to find its dependencies.

---

## That's It! 🎉

Your chatbot is ready to use. Follow the 5 steps above and you'll be running in minutes.

**Questions?** Check the documentation files (all in project root).

**Ready to go?** Start with Step 1! ⬆️

