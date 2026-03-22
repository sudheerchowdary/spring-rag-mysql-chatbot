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
- **Library**: LangChain4j 0.32.0 (`dev.langchain4j.model.openai.OpenAiChatModel`)
- **Model**: GPT-4o with temperature=0.2 (low randomness for deterministic SQL)
- **Prompt Structure**: Static schema info + user question (no in-context examples yet)
- **Output Cleaning**: Removes markdown code blocks before SQL execution
- **Error Handling**: Catches exceptions, returns error message + attempted SQL

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
```

### Local MySQL Setup
1. Create schema: `mysql -u root -p < rag-mysql-chatbot.sql`
2. Server runs on port 8080 (default Spring Boot)
3. Test via `curl -X POST http://localhost:8080/api/chat -H "Content-Type: text/plain" -d "List all products"`

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

### OpenAI Integration
- **Builder pattern**: `OpenAiChatModel.builder().apiKey().modelName().temperature().build()`
- **Instantiation**: Happens in LLMSQLService constructor (service-scoped)
- **API Key**: Injected via `@Value("${openai.api-key}")` from environment
- **Model name**: Hardcoded to "gpt-4o"; upgrade model name in LLMSQLService line 20

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

### Switching to Different LLM
Replace `OpenAiChatModel` in LLMSQLService with:
- `OllamaChatModel` (LangChain4j support, local models)
- `BedrockChatModel` (AWS)
- Check LangChain4j 0.32.0 docs for available model integrations

### Debugging LLM Output
- **Generated SQL**: Printed to stdout via `System.out.println("Generated SQL: " + sql)` (line 47)
- **Check formatting**: Test if LLM returns markdown-wrapped SQL and adjust .replaceAll() patterns if needed
- **Test question**: Use simple questions first ("Show all products") before complex JOINs

### Frontend Integration
- **Endpoint**: POST `/api/chat`
- **Content-Type**: Text/plain (not JSON)
- **Response**: Plain text formatted as "key: value\n\nkey: value\n..."
- **HTML client**: `src/main/resources/static/index.html` (basic chat UI with fetch API)

## Dependency Management

### Key Libraries
- `langchain4j:0.32.0` - LLM orchestration
- `langchain4j-open-ai:0.32.0` - OpenAI integration
- `spring-boot-starter-webmvc:4.0.0-SNAPSHOT` - Web framework
- `spring-boot-starter-data-jpa:4.0.0-SNAPSHOT` - JPA (auto-configured but not actively used)
- `mysql-connector-j:runtime` - JDBC driver
- `spring-boot-devtools:runtime` - Hot reload

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

