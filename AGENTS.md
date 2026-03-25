# AGENTS.md - AI Coding Agent Guide

## Project Overview

A **Spring Boot RAG (Retrieval-Augmented Generation) SQL chatbot** that converts natural language questions into SQL queries, executes them against a MySQL database, and returns human-readable results.

**Architecture Pattern**: Three-stage pipeline
1. **Retriever**: Schema/context provided in LLM prompt
2. **Generator**: OpenAI GPT-4o generates SQL from schema + question
3. **Executor**: JdbcTemplate executes generated SQL, returns formatted results

**Key Files**:
- `src/main/java/com/rag/mysql/rag_mysql_chatbot/service/LLMSQLService.java` - Core RAG logic
- `src/main/java/com/rag/mysql/rag_mysql_chatbot/controller/ChatController.java` - REST endpoint
- `src/main/resources/application.yml` - Config (MySQL + OpenAI credentials)
- `rag-mysql-chatbot.sql` - Database schema and seed data

## Architecture & Data Flow

### Request → Response Cycle
```
POST /api/chat (plain text) 
  ↓ ChatController.sendMessage()
  ↓ LLMSQLService.queryDatabase(question)
  ↓ [Step 1] Build schema context string + user question as LLM prompt
  ↓ [Step 2] llm.generate() returns raw SQL (may include markdown ```sql``` blocks)
  ↓ [Step 3] Strip formatting with .replaceAll() + jdbcTemplate.queryForList()
  ↓ [Step 4] Format results as "key: value\n" and return
  ↓ Response: plain text (charset=UTF-8)
```

### LLM Integration Details
- **Library**: LangChain4j 0.32.0 (OpenAI primary + Ollama fallback)
- **Primary Model**: GPT-4o with temperature=0.2 (low randomness for deterministic SQL)
- **Fallback Model**: Ollama (`neural-chat` model) at `http://localhost:11434`
- **Quota Detection**: Automatically detects OpenAI quota errors and switches to Ollama
- **Prompt Structure**: Static schema info + user question (no in-context examples yet)
- **Output Cleaning**: Removes markdown code blocks before SQL execution
- **Error Handling**: Quota-aware fallback; graceful degradation with actionable error messages instead of crashes
- **Ollama Availability Check**: Verifies Ollama is running before attempting fallback (prevents hanging)

### Database Schema
**Database**: `rag-data-schema` (MySQL)
- **products**: product_id (PK), product_name, description, price, stock_quantity
- **orders**: order_id (PK), customer_name, order_date, total_amount
- **order_items**: order_item_id (PK), order_id (FK), product_id (FK), quantity, price_at_purchase

See `rag-mysql-chatbot.sql` for exact DDL and seed data (4 products, 2 orders, 4 order items).

## Build & Run Workflow

### Maven Commands
```bash
# Build JAR (includes all dependencies)
mvn clean package

# Run Spring Boot (requires MySQL + MYSQL_PASSWORD + OPEN_AI_API_KEY env vars)
mvn spring-boot:run

# Run tests
mvn test
```

### Required Environment Variables
```bash
export MYSQL_PASSWORD=your_mysql_root_password
export OPEN_AI_API_KEY=sk-...
export OLLAMA_BASE_URL=http://localhost:11434  # Optional, defaults to localhost:11434
```

**Docker-specific setup** (when Ollama runs in Docker):
```bash
# If running Spring Boot locally but Ollama in Docker
export OLLAMA_BASE_URL=http://host.docker.internal:11434  # macOS/Windows Docker Desktop
export OLLAMA_BASE_URL=http://172.17.0.1:11434           # Linux (bridge network)

# If both Spring Boot and Ollama in Docker on same network
export OLLAMA_BASE_URL=http://ollama:11434               # Use service name
```

### Local MySQL Setup
1. Create schema: `mysql -u root -p < rag-mysql-chatbot.sql`
2. Server runs on port 8080 (default Spring Boot)
3. Test via `curl -X POST http://localhost:8080/api/chat -H "Content-Type: text/plain" -d "List all products"`

### Docker Setup for Ollama & Spring Boot

**Option 1: Ollama in Docker + Spring Boot Locally**
```bash
# Terminal 1: Start Ollama container
docker run -d --name ollama -p 11434:11434 ollama/ollama

# Terminal 2: Pull model
docker exec ollama ollama pull neural-chat

# Terminal 3: Run Spring Boot
export OLLAMA_BASE_URL=http://host.docker.internal:11434  # macOS/Windows
export OLLAMA_BASE_URL=http://172.17.0.1:11434            # Linux
mvn spring-boot:run
```

**Option 2: Both in Docker (Docker Compose)**
```bash
# Create docker-compose.yml:
version: '3.8'
services:
  ollama:
    image: ollama/ollama
    container_name: ollama
    ports:
      - "11434:11434"
    environment:
      - OLLAMA_KEEP_ALIVE=24h
    volumes:
      - ollama_data:/root/.ollama
    command: serve

  mysql:
    image: mysql:8.0
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: password
    ports:
      - "3306:3306"
    volumes:
      - ./rag-mysql-chatbot.sql:/docker-entrypoint-initdb.d/init.sql

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

volumes:
  ollama_data:

# Run with:
docker-compose up --build
```

**Option 3: Pull Ollama Model Automatically**
```bash
# Use init script that runs before app starts:
docker run -d --name ollama ollama/ollama
sleep 3
docker exec ollama ollama pull neural-chat
docker exec ollama ollama pull mistral  # or other models
```

**Common Docker Networking Issues**
- **Cannot reach localhost:11434 from Spring Boot**: Use `host.docker.internal` (macOS/Windows) or service name in Docker network
- **Docker on Linux**: Use bridge IP `172.17.0.1` or create custom network
- **Check Ollama running**: `curl http://localhost:11434/api/tags` (from host) or `curl http://ollama:11434/api/tags` (from container)

## Project-Specific Patterns

### Spring Boot Configuration
- **Java 17**: Latest LTS, required by Spring Boot 4.0.0-SNAPSHOT
- **JPA/Hibernate**: Enabled but not used (using JdbcTemplate directly for SQL generation)
- **HibernateDialect**: MySQL8Dialect (in application.yml)
- **DDL Auto**: Set to `none` (schema managed externally in .sql file, not by Hibernate)
- **Dev Tools**: Included for hot-reload during development

### Lombok Usage
- Declared as optional in pom.xml with annotation processor config
- Not heavily used in current code; available for entity/DTO generation

### Unusual Choices
- **No Spring Data repositories**: Using JdbcTemplate directly (intentional for RAG pipeline)
- **String response format**: Plain text with key:value lines per row (see `StringBuilder` in LLMSQLService)
- **Schema in code**: Database schema is hardcoded in LLMSQLService prompt string (not retrieved dynamically)
- **No SQL injection protection**: Currently relies on OpenAI's temperature + generation quality (security concern for production)

## Critical Integration Points

### LLM Initialization
- **Dual LLM Architecture**: Both OpenAI and Ollama are instantiated in LLMSQLService constructor
- **OpenAI Builder pattern**: `OpenAiChatModel.builder().apiKey().modelName("gpt-4o").temperature(0.2).build()`
- **Ollama Builder pattern**: `OllamaChatModel.builder().baseUrl("http://localhost:11434").modelName("neural-chat").temperature(0.2).build()`
- **API Key Injection**: OpenAI key via `@Value("${openai.api-key}")` from environment
- **Quota-Aware Fallback Logic** (in `generateSQL()` method):
  1. Try OpenAI first (unless fallback flag is active)
  2. On `Exception`, check if error message contains `insufficient_quota`, `You exceeded your current quota`, or `quota_exceeded`
  3. If quota error: Check Ollama availability via HTTP HEAD request to `localhost:11434/api/tags` (2-second timeout)
  4. If Ollama available: Set `useOllamaFallback=true` flag and generate SQL using Ollama
  5. If Ollama unavailable: Return graceful error with installation/setup instructions
- **Upgrade Model**: Change modelName in OpenAiChatModel.builder() or OllamaChatModel.builder()

### MySQL Connection
- **JdbcTemplate**: Injected into LLMSQLService constructor (auto-configured by Spring)
- **Query execution**: `jdbcTemplate.queryForList(sql)` returns `List<Map<String, Object>>`
- **Connection pool**: Managed by Spring's default HikariCP
- **Error handling**: SQLExceptions caught and returned as error messages

## Common Development Tasks

### Adding a New Table to RAG Context
1. Update schema string in `LLMSQLService.queryDatabase()` (lines 34-40)
2. Update `rag-mysql-chatbot.sql` with CREATE TABLE + INSERT statements
3. Test with `mvn spring-boot:run`

### Switching LLM Models or Architecture
**Changing OpenAI Model**:
- Update `modelName("gpt-4o")` to desired model in LLMSQLService constructor (line 28)
- Examples: `gpt-4-turbo`, `gpt-3.5-turbo`

**Changing Ollama Fallback Model**:
- Update `modelName("neural-chat")` in LLMSQLService constructor (line 35)
- Examples: `llama2`, `mistral`, `neural-chat` (default)
- Note: Ollama model must be pre-pulled via `ollama pull <modelname>`

**Removing Ollama Fallback** (use OpenAI only):
- In `generateSQL()`, remove the quota detection block (lines 72-89)
- Comment out Ollama initialization in constructor (lines 33-37)

**Replacing Both LLMs**:
- Replace `OpenAiChatModel` with alternative (e.g., `BedrockChatModel` for AWS, `AnthropicChatModel`)
- Check LangChain4j 0.32.0 docs for available model integrations
- Update pom.xml dependencies (e.g., `langchain4j-anthropic:0.32.0`)

### Debugging LLM Output & Quota Fallback
- **Generated SQL**: Printed to stdout via `System.out.println("Generated SQL: " + sql)` (line 114)
- **Active LLM**: Console logs show `"Using OpenAI GPT-4o"` or `"Using Ollama (local LLM) as fallback"`
- **Check formatting**: Test if LLM returns markdown-wrapped SQL and adjust `.replaceAll()` patterns if needed
- **Test question**: Use simple questions first (`"Show all products"`) before complex JOINs

**Quota Fallback Diagnostics**:
- When quota exceeded: Console shows `"⚠️ OpenAI quota exceeded. Attempting fallback to Ollama..."`
- If Ollama unavailable: Check Docker/local installation, or see error message in response for setup instructions
- Monitor `useOllamaFallback` flag state—once activated, subsequent requests skip OpenAI and use Ollama directly
- **To reset fallback**: Restart service or implement a `/reset-llm` endpoint to set `useOllamaFallback = false`

**Ollama Health Checks**:
- Verification request: `curl http://localhost:11434/api/tags` (should return JSON list of models)
- Connection timeout: 2 seconds (configurable via `OkHttpClient` in `isOllamaAvailable()`)
- Common issue: Port 11434 already in use—check with `lsof -i :11434`

### Frontend Integration
- **Endpoint**: POST `/api/chat`
- **Content-Type Support**: Accepts both `text/plain` and `application/json`
  - **Plain text**: Send raw question directly as request body
  - **JSON format**: `{"message": "your question"}` - controller extracts `message` field automatically
- **Response**: Plain text formatted as `key: value\n\nkey: value\n...` (UTF-8 charset)
- **HTML client**: `src/main/resources/static/index.html` (basic chat UI with fetch API)

## Dependency Management

### Key Libraries
- `langchain4j:0.32.0` - LLM orchestration core
- `langchain4j-open-ai:0.32.0` - OpenAI integration (primary)
- `langchain4j-ollama:0.32.0` - Ollama integration (quota fallback)
- `spring-boot-starter-webmvc:4.0.0-SNAPSHOT` - Web framework
- `spring-boot-starter-data-jpa:4.0.0-SNAPSHOT` - JPA (auto-configured but not actively used)
- `mysql-connector-j:runtime` - JDBC driver
- `spring-boot-devtools:runtime` - Hot reload
- `okhttp3` - HTTP client for Ollama availability checks (transitive via LangChain4j)

### Maven Repositories
- Spring Snapshots (Spring 4.0.0-SNAPSHOT)
- Maven Central (LangChain4j, MySQL Connector)

## Testing Notes

- Test file: `src/test/java/.../RagMysqlChatbotApplicationTests.java` (minimal/generated)
- **Strategy**: Spring Boot integration tests require MySQL + OpenAI running; recommend manual testing via curl/frontend
- **Mock testing**: Would require mocking `OpenAiChatModel` (LangChain4j integration point)

## Security Considerations

⚠️ **Current vulnerabilities** (development-only, not for production):
1. **No SQL injection protection**: LLM generates SQL directly; relies on OpenAI quality
2. **API key in env var**: Standard practice but visible in process logs
3. **Database credentials**: Plain text in application.yml (MySQL password via env var)
4. **Error messages**: Return raw SQL + exception details to client (information disclosure)

**For production**:
- Add SQL query validation/sanitization before execution
- Use parameterized queries if moving to ORM
- Implement API authentication on /api/chat endpoint
- Restrict DB user permissions to SELECT-only
- Use secrets management (AWS Secrets Manager, HashiCorp Vault)

